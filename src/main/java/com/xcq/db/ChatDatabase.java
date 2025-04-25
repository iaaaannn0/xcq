package com.xcq.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChatDatabase {
    private static final Logger logger = LoggerFactory.getLogger(ChatDatabase.class);
    private static final String DB_NAME = "chat.db";
    private static final String DB_DIR = "data";
    private static final String DB_PATH = DB_DIR + File.separator + DB_NAME;
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;
    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS messages (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "sender_jid TEXT NOT NULL," +
            "receiver_jid TEXT NOT NULL," +
            "message TEXT NOT NULL," +
            "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "is_local BOOLEAN NOT NULL" +
            ")";

    private static ChatDatabase instance;
    private Connection connection;

    private ChatDatabase() {
        try {
            // 确保数据库目录存在
            File dbDir = new File(DB_DIR);
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            
            connection = DriverManager.getConnection(DB_URL);
            Statement statement = connection.createStatement();
            statement.execute(CREATE_TABLE_SQL);
            logger.info("Database initialized successfully at: {}", new File(DB_PATH).getAbsolutePath());
        } catch (SQLException e) {
            logger.error("Error initializing database", e);
        }
    }

    public static synchronized ChatDatabase getInstance() {
        if (instance == null) {
            instance = new ChatDatabase();
        }
        return instance;
    }

    public void saveMessage(String senderJid, String receiverJid, String message, boolean isLocal) {
        String sql = "INSERT INTO messages (sender_jid, receiver_jid, message, is_local) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, senderJid);
            pstmt.setString(2, receiverJid);
            pstmt.setString(3, message);
            pstmt.setBoolean(4, isLocal);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error saving message", e);
        }
    }

    public List<ChatMessage> getChatHistory(String user1Jid, String user2Jid) {
        List<ChatMessage> messages = new ArrayList<>();
        String sql = "SELECT sender_jid, receiver_jid, message, timestamp, " +
                    "CASE WHEN sender_jid = ? THEN 1 ELSE 0 END as is_local " +
                    "FROM messages " +
                    "WHERE (sender_jid = ? AND receiver_jid = ?) " +
                    "OR (sender_jid = ? AND receiver_jid = ?) " +
                    "ORDER BY timestamp ASC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user1Jid);  // 用于判断是否是本地发送的消息
            pstmt.setString(2, user1Jid);
            pstmt.setString(3, user2Jid);
            pstmt.setString(4, user2Jid);
            pstmt.setString(5, user1Jid);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                messages.add(new ChatMessage(
                    rs.getString("sender_jid"),
                    rs.getString("message"),
                    rs.getTimestamp("timestamp"),
                    rs.getBoolean("is_local")
                ));
            }
            logger.info("Retrieved {} messages between {} and {}", 
                messages.size(), user1Jid, user2Jid);
        } catch (SQLException e) {
            logger.error("Error loading chat history", e);
        }
        return messages;
    }

    public void deleteChatHistory(String user1Jid, String user2Jid) {
        String sql = "DELETE FROM messages " +
                "WHERE (sender_jid = ? AND receiver_jid = ?) " +
                "OR (sender_jid = ? AND receiver_jid = ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user1Jid);
            pstmt.setString(2, user2Jid);
            pstmt.setString(3, user2Jid);
            pstmt.setString(4, user1Jid);
            pstmt.executeUpdate();
            logger.info("Chat history deleted for users: {} and {}", user1Jid, user2Jid);
        } catch (SQLException e) {
            logger.error("Error deleting chat history", e);
        }
    }

    public void deleteAllChatHistory() {
        String sql = "DELETE FROM messages";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            logger.info("All chat history deleted");
        } catch (SQLException e) {
            logger.error("Error deleting all chat history", e);
        }
    }

    public static class ChatMessage {
        private final String senderJid;
        private final String message;
        private final Timestamp timestamp;
        private final boolean isLocal;

        public ChatMessage(String senderJid, String message, Timestamp timestamp, boolean isLocal) {
            this.senderJid = senderJid;
            this.message = message;
            this.timestamp = timestamp;
            this.isLocal = isLocal;
        }

        public String getSenderJid() {
            return senderJid;
        }

        public String getMessage() {
            return message;
        }

        public Timestamp getTimestamp() {
            return timestamp;
        }

        public boolean isLocal() {
            return isLocal;
        }
    }
} 