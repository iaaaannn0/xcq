package com.xcq.xmpp;

import com.xcq.core.Context;
import com.xcq.ui.FriendRequestWindow;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import javax.swing.SwingUtilities;
import com.xcq.db.ChatDatabase;
import com.xcq.ui.MainWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class XMPPClient {
    private static final Logger logger = LoggerFactory.getLogger(XMPPClient.class);
    private final Context context;
    private XMPPTCPConnection connection;
    private final List<MessageListener> messageListeners = new ArrayList<>();
    private ChatManager chatManager;
    private final Map<String, Chat> chatCache = new ConcurrentHashMap<>();

    public XMPPClient(Context context) {
        this.context = context;
    }

    public void initialize(XMPPTCPConnectionConfiguration config) {
        try {
            connection = new XMPPTCPConnection(config);
            connection.connect();
            connection.login();
            
            chatManager = ChatManager.getInstanceFor(connection);
            
            // 设置全局消息监听器
            chatManager.addIncomingListener((from, message, chat) -> {
                String messageBody = message.getBody();
                if (messageBody != null) {
                    // 去除消息末尾的空格
                    messageBody = messageBody.replaceAll("\\s+$", "");
                    
                    // 保存消息到数据库
                    String fromJid = from.asBareJid().toString();
                    String toJid = connection.getUser().asBareJid().toString();
                    ChatDatabase db = ChatDatabase.getInstance();
                    if (db != null) {
                        // 只保存一次消息
                        db.saveMessage(fromJid, toJid, messageBody, false);
                    }
                    
                    // 通知所有消息监听器
                    for (MessageListener listener : messageListeners) {
                        listener.onMessageReceived(from.toString(), messageBody);
                    }
                }
            });
            
            // 添加好友请求监听器
            connection.addAsyncStanzaListener(stanza -> {
                if (stanza instanceof Presence) {
                    Presence presence = (Presence) stanza;
                    if (presence.getType() == Presence.Type.subscribe) {
                        String fromJid = presence.getFrom().asBareJid().toString();
                        String nickname = presence.getFrom().getLocalpartOrNull().toString();
                        
                        // 在 EDT 线程中显示验证窗口
                        SwingUtilities.invokeLater(() -> {
                            FriendRequestWindow window = new FriendRequestWindow(
                                context.getMainWindow(),
                                context,
                                fromJid,
                                nickname
                            );
                            window.setVisible(true);
                        });
                    }
                }
            }, stanza -> stanza instanceof Presence && ((Presence) stanza).getType() == Presence.Type.subscribe);
            
            logger.info("XMPP connection initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize XMPP connection", e);
            throw new RuntimeException("Failed to initialize XMPP connection", e);
        }
    }

    public XMPPTCPConnection getConnection() {
        return connection;
    }

    public void connect() throws SmackException {
        if (connection != null && !connection.isConnected()) {
            try {
                connection.connect();
                connection.login();
                logger.info("Connected to XMPP server successfully");
            } catch (Exception e) {
                logger.error("Failed to connect to XMPP server", e);
                throw new SmackException.NotConnectedException("Failed to connect to XMPP server: " + e.getMessage());
            }
        }
    }

    public void disconnect() {
        if (connection != null && connection.isConnected()) {
            connection.disconnect();
            chatCache.clear();
            logger.info("Disconnected from XMPP server");
        }
    }

    public boolean isConnected() {
        return connection != null && connection.isConnected();
    }

    public void addMessageListener(MessageListener listener) {
        if (!messageListeners.contains(listener)) {
            messageListeners.add(listener);
        }
    }

    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
    }

    public void sendMessage(String to, String messageText) throws Exception {
        if (!isConnected()) {
            throw new SmackException.NotConnectedException("Not connected to XMPP server");
        }

        try {
            EntityBareJid jid = JidCreate.entityBareFrom(to);
            
            // 获取或创建Chat对象
            Chat chat = chatCache.computeIfAbsent(to, k -> chatManager.chatWith(jid));
            
            // 创建并发送消息
            Message message = new Message(jid, Message.Type.chat);
            message.setBody(messageText);
            message.setFrom(connection.getUser());  // 设置发送者
            
            chat.send(message);
            
            // 记录发送的消息
            logger.debug("Sent message to {}: {}", to, messageText);
        } catch (Exception e) {
            logger.error("Failed to send message to {}", to, e);
            throw e;
        }
    }

    public void acceptSubscription(String jid) {
        try {
            // 接受订阅请求
            Presence subscribed = new Presence(Presence.Type.subscribed);
            subscribed.setTo(JidCreate.from(jid));
            connection.sendStanza(subscribed);
            
            // 发送订阅请求
            Presence subscribe = new Presence(Presence.Type.subscribe);
            subscribe.setTo(JidCreate.from(jid));
            connection.sendStanza(subscribe);
            
            logger.info("Accepted subscription from: " + jid);
        } catch (Exception e) {
            logger.error("Error accepting subscription", e);
        }
    }
    
    public void rejectSubscription(String jid) {
        try {
            Presence unsubscribed = new Presence(Presence.Type.unsubscribed);
            unsubscribed.setTo(JidCreate.from(jid));
            connection.sendStanza(unsubscribed);
            logger.info("Rejected subscription from: " + jid);
        } catch (Exception e) {
            logger.error("Error rejecting subscription", e);
        }
    }

    public interface MessageListener {
        void onMessageReceived(String from, String message);
    }
} 