package com.xcq.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ChatDatabase {
    private static final Logger logger = LoggerFactory.getLogger(ChatDatabase.class);
    private static final String DB_NAME = "chat.db";
    private static final String DB_DIR = "data";
    private static final String DB_PATH = DB_DIR + File.separator + DB_NAME;
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;
    
    private static final String CREATE_TABLE_SQL = 
        "CREATE TABLE IF NOT EXISTS messages (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
        "sender_jid TEXT NOT NULL," +
        "receiver_jid TEXT NOT NULL," +
        "message TEXT NOT NULL," +
        "is_local BOOLEAN NOT NULL," +
        "is_read BOOLEAN NOT NULL DEFAULT 0," +
        "timestamp BIGINT NOT NULL" + 
        ")";
    
    private static final String CREATE_PARTICIPANTS_INDEX_SQL = 
        "CREATE INDEX IF NOT EXISTS idx_participants ON messages (sender_jid, receiver_jid)";
    
    private static final String CREATE_TIMESTAMP_INDEX_SQL = 
        "CREATE INDEX IF NOT EXISTS idx_timestamp ON messages (timestamp)";

    private static ChatDatabase instance;
    private Connection connection;
    private final ReentrantLock dbLock = new ReentrantLock();

    private ChatDatabase() {
        try {
            File dbDir = new File(DB_DIR);
            if (!dbDir.exists()) {
                if (dbDir.mkdirs()) {
                    logger.info("Created database directory: {}", dbDir.getAbsolutePath());
                } else {
                    logger.error("Failed to create database directory: {}", dbDir.getAbsolutePath());
                }
            }
            
            connection = DriverManager.getConnection(DB_URL);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA synchronous = NORMAL;");
                statement.execute("PRAGMA journal_mode=WAL;"); 
            } catch(SQLException e) {
                 logger.warn("Failed to set PRAGMA settings, using defaults.", e);
            }
            
            connection.setAutoCommit(false); 
            
            initializeSchema();

        } catch (SQLException e) {
            logger.error("Failed to initialize database connection", e);
            connection = null; 
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    private void initializeSchema() throws SQLException {
        dbLock.lock();
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE_SQL);
            logger.info("Executed: {}", CREATE_TABLE_SQL);
            
            statement.execute(CREATE_PARTICIPANTS_INDEX_SQL);
            logger.info("Executed: {}", CREATE_PARTICIPANTS_INDEX_SQL);

            statement.execute(CREATE_TIMESTAMP_INDEX_SQL);
            logger.info("Executed: {}", CREATE_TIMESTAMP_INDEX_SQL);
            
            connection.commit();
            logger.info("Database schema initialized successfully at: {}", new File(DB_PATH).getAbsolutePath());
        } catch (SQLException e) {
            logger.error("Error executing database initialization SQL", e);
            rollbackConnection();
            throw e;
        } finally {
            dbLock.unlock();
        }
    }

    private void rollbackConnection() {
        if (connection != null) {
            try {
                connection.rollback();
                logger.warn("Transaction rolled back.");
            } catch (SQLException ex) {
                logger.error("Error rolling back transaction", ex);
            }
        }
    }

    public static synchronized ChatDatabase getInstance() {
        if (instance == null) {
            try {
                 instance = new ChatDatabase();
            } catch (RuntimeException e) {
                 logger.error("Failed to create ChatDatabase instance", e);
                 return null; 
            }
        }
        return instance;
    }

    public void saveMessage(String senderJid, String receiverJid, String message, boolean isLocal) {
        if (com.xcq.core.Configuration.getInstance().isTemporaryContact(isLocal ? receiverJid : senderJid)) {
             logger.debug("Temporary contact message not saved: {} <-> {}", senderJid, receiverJid);
             return;
        }

        String sql = "INSERT INTO messages (sender_jid, receiver_jid, message, is_local, timestamp, is_read) VALUES (?, ?, ?, ?, ?, ?)";
        dbLock.lock();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, senderJid);
            pstmt.setString(2, receiverJid);
            pstmt.setString(3, message);
            pstmt.setBoolean(4, isLocal);
            pstmt.setLong(5, System.currentTimeMillis());
            pstmt.setBoolean(6, isLocal);
            pstmt.executeUpdate();
            connection.commit();
            logger.debug("Message saved: {} -> {}: {}", senderJid, receiverJid, message.length() > 20 ? message.substring(0, 20) + "..." : message);
        } catch (SQLException e) {
            logger.error("Error saving message", e);
            rollbackConnection();
        } finally {
            dbLock.unlock();
        }
    }

    public List<ChatMessage> getChatHistory(String currentUserJid, String contactJid) {
        List<ChatMessage> messages = new ArrayList<>();
        String sql = "SELECT sender_jid, receiver_jid, message, timestamp, is_read, " +
                     "(sender_jid = ?) AS is_local " +
                     "FROM messages " +
                     "WHERE (sender_jid = ? AND receiver_jid = ?) " +
                     "OR (sender_jid = ? AND receiver_jid = ?) " +
                     "ORDER BY timestamp ASC";
        
        dbLock.lock();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, currentUserJid);
            pstmt.setString(2, currentUserJid);
            pstmt.setString(3, contactJid);
            pstmt.setString(4, contactJid);
            pstmt.setString(5, currentUserJid);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                messages.add(new ChatMessage(
                    rs.getString("sender_jid"),
                    rs.getString("message"),
                    new Timestamp(rs.getLong("timestamp")),
                    rs.getBoolean("is_local"),
                    rs.getBoolean("is_read")
                ));
            }
            logger.info("Retrieved {} messages between {} and {}", 
                messages.size(), currentUserJid, contactJid);
        } catch (SQLException e) {
            logger.error("Error loading chat history between {} and {}", currentUserJid, contactJid, e);
        } finally {
            dbLock.unlock();
        }
        return messages;
    }

    public void deleteChatHistory(String user1Jid, String user2Jid) {
        String sql = "DELETE FROM messages " +
                     "WHERE (sender_jid = ? AND receiver_jid = ?) " +
                     "OR (sender_jid = ? AND receiver_jid = ?)";
        
        dbLock.lock();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user1Jid);
            pstmt.setString(2, user2Jid);
            pstmt.setString(3, user2Jid);
            pstmt.setString(4, user1Jid);
            int deletedRows = pstmt.executeUpdate();
            connection.commit();
            logger.info("Deleted {} messages between {} and {}", deletedRows, user1Jid, user2Jid);
        } catch (SQLException e) {
            logger.error("Error deleting chat history", e);
            rollbackConnection();
        } finally {
             dbLock.unlock();
        }
    }

    public void deleteAllChatHistory() {
        String sql = "DELETE FROM messages";
        dbLock.lock();
        try (Statement stmt = connection.createStatement()) {
            int deletedRows = stmt.executeUpdate(sql);
            connection.commit();
            logger.info("Deleted all {} messages from history", deletedRows);
        } catch (SQLException e) {
            logger.error("Error deleting all chat history", e);
            rollbackConnection();
        } finally {
            dbLock.unlock();
        }
    }

    public void close() {
        dbLock.lock();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed.");
            }
        } catch (SQLException e) {
            logger.error("Error closing database connection", e);
        } finally {
            connection = null;
            instance = null;  
            dbLock.unlock();
        }
    }

    public void markMessagesAsRead(String currentUserJid, String contactJid) {
        String sql = "UPDATE messages SET is_read = 1 " +
                    "WHERE sender_jid = ? AND receiver_jid = ? AND is_read = 0";
        
        dbLock.lock();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, contactJid);
            pstmt.setString(2, currentUserJid);
            int updatedRows = pstmt.executeUpdate();
            connection.commit();
            if (updatedRows > 0) {
                 logger.debug("{} messages marked as read from {} to {}", updatedRows, contactJid, currentUserJid);
            }
        } catch (SQLException e) {
            logger.error("Error marking messages as read", e);
            rollbackConnection();
        } finally {
            dbLock.unlock();
        }
    }

    public int getUnreadMessageCount(String userJid) {
        String sql = "SELECT COUNT(*) as count FROM messages " +
                    "WHERE receiver_jid = ? AND is_read = 0";
        
        int count = 0;
        dbLock.lock();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userJid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt("count");
            }
        } catch (SQLException e) {
            logger.error("Error getting unread message count for {}", userJid, e);
        } finally {
            dbLock.unlock();
        }
        return count;
    }

    public int getUnreadMessageCountFromContact(String currentUserJid, String contactJid) {
        String sql = "SELECT COUNT(*) as count FROM messages " +
                     "WHERE receiver_jid = ? AND sender_jid = ? AND is_read = 0";
        
        int count = 0;
        dbLock.lock();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, currentUserJid);
            pstmt.setString(2, contactJid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt("count");
            }
        } catch (SQLException e) {
            logger.error("Error getting unread message count from {} for {}", contactJid, currentUserJid, e);
        } finally {
            dbLock.unlock();
        }
        return count;
    }

    public static class ChatMessage {
        private final String senderJid;
        private final String message;
        private final Timestamp timestamp;
        private final boolean isLocal;
        private final boolean isRead;

        public ChatMessage(String senderJid, String message, Timestamp timestamp, boolean isLocal, boolean isRead) {
            this.senderJid = senderJid;
            this.message = message;
            this.timestamp = timestamp;
            this.isLocal = isLocal;
            this.isRead = isRead;
        }

        public String getSenderJid() { return senderJid; }
        public String getMessage() { return message; }
        public Timestamp getTimestamp() { return timestamp; }
        public boolean isLocal() { return isLocal; }
        public boolean isRead() { return isRead; }

        @Override
        public String toString() {
            return "ChatMessage{" +
                   "senderJid='" + senderJid + '\'' +
                   ", message='" + (message.length() > 30 ? message.substring(0, 30) + "..." : message) + '\'' +
                   ", timestamp=" + timestamp +
                   ", isLocal=" + isLocal +
                   ", isRead=" + isRead +
                   '}';
        }
    }
} 