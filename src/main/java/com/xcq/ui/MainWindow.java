package com.xcq.ui;

import com.xcq.core.ApplicationContext;
import com.xcq.core.Configuration;
import com.xcq.db.ChatDatabase;
import com.xcq.ui.model.ContactTreeModel;
import com.xcq.ui.renderer.ContactTreeCellRenderer;
import com.xcq.util.NotificationManager;
import com.xcq.xmpp.XMPPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.net.URL;

public class MainWindow extends JFrame implements XMPPClient.MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);
    private final ApplicationContext context;
    private final ContactTreeModel contactTreeModel;
    private final JTree contactTree;
    private final Map<String, ChatWindow> chatWindows;
    private final Configuration config;
    private final JTabbedPane tabbedPane;
    private final ContactWindow contactWindow;
    private final ChatWindow chatWindow;
    private final ContactTreeCellRenderer contactTreeCellRenderer;
    private Clip notificationSound;

    public MainWindow(ApplicationContext context) {
        this.context = context;
        this.config = context.getConfig();
        this.chatWindows = new HashMap<>();
        this.contactTreeModel = new ContactTreeModel();

        setTitle("XCQ - XMPP Client");
        ImageIcon logoIcon = new ImageIcon(getClass().getResource("/logo.png"));
        setIconImage(logoIcon.getImage());
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 初始化提示音
        initNotificationSound();

        tabbedPane = new JTabbedPane();
        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        contactWindow = new ContactWindow(context);
        tabbedPane.addTab("联系人", contactWindow);

        chatWindow = new ChatWindow(context, null, null);
        tabbedPane.addTab("聊天", chatWindow);

        // 创建联系人树
        contactTree = new JTree(contactTreeModel);
        contactTreeCellRenderer = new ContactTreeCellRenderer(contactTreeModel);
        contactTree.setCellRenderer(contactTreeCellRenderer);
        contactTree.setRootVisible(false);
        contactTree.setShowsRootHandles(true);

        // 添加双击事件
        contactTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = contactTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (node.getUserObject() instanceof ContactTreeModel.Contact) {
                            ContactTreeModel.Contact contact = (ContactTreeModel.Contact) node.getUserObject();
                            openChat(contact.getJid(), contact.getName());
                        }
                    }
                }
            }
        });

        // 创建滚动面板
        JScrollPane scrollPane = new JScrollPane(contactTree);
        add(scrollPane);

        // 注册全局消息监听器
        if (context.getXmppClient() != null) {
            context.getXmppClient().addMessageListener(this);
        }

        // 窗口关闭时清理资源
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                logger.info("正在关闭应用程序...");
                // 移除消息监听器
                if (context.getXmppClient() != null) {
                    context.getXmppClient().removeMessageListener(MainWindow.this);
                }
                for (ChatWindow window : chatWindows.values()) {
                    window.dispose();
                }
                context.getXmppClient().disconnect();
                System.exit(0);
            }
        });
    }

    private void initNotificationSound() {
        try {
            URL soundUrl = getClass().getResource("/sounds/message.wav");
            if (soundUrl == null) {
                logger.warn("Notification sound file not found: /sounds/message.wav");
                return;
            }
            
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundUrl);
            notificationSound = AudioSystem.getClip();
            notificationSound.open(audioIn);
            logger.info("Notification sound initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize notification sound", e);
        }
    }

    private void playNotificationSound() {
        if (notificationSound != null) {
            try {
                notificationSound.setFramePosition(0);
                notificationSound.start();
                logger.debug("Playing notification sound");
            } catch (Exception e) {
                logger.error("Failed to play notification sound", e);
            }
        } else {
            logger.warn("Notification sound not initialized");
        }
    }

    @Override
    public void onMessageReceived(String from, String message) {
        SwingUtilities.invokeLater(() -> {
            // 获取联系人昵称
            String nickname = from;  // 默认使用JID
            ContactTreeModel.Contact contact = contactTreeModel.findContact(from);
            if (contact != null) {
                nickname = contact.getName();
            }

            // 播放提示音
            playNotificationSound();
            
            // 在联系人列表中闪烁该联系人
            contactTreeCellRenderer.startBlinking(from);
            contactTree.repaint();

            // 保存消息到数据库
            if (!Configuration.getInstance().isTemporaryContact(from)) {
                ChatDatabase db = ChatDatabase.getInstance();
                String currentUserJid = getCurrentUserJid();
                if (db != null && currentUserJid != null) {
                    db.saveMessage(from, currentUserJid, message, false);
                }
            }

            // 如果聊天窗口已经打开，则显示消息
            ChatWindow chatWindow = chatWindows.get(from);
            if (chatWindow != null) {
                chatWindow.appendMessage(nickname, from, message, System.currentTimeMillis(), false);
            }

            // 显示系统通知
            String notificationMessage = String.format("收到来自 %s 的新消息", nickname);
            logger.info(notificationMessage);
            
            // 自动打开聊天窗口（如果配置允许）
            if (config.isAutoOpenChat()) {
                openChat(from, nickname);
            }
        });
    }

    private String getCurrentUserJid() {
        if (context != null && context.getXmppClient() != null && context.getXmppClient().isConnected()) {
            return context.getXmppClient().getConnection().getUser().toString();
        }
        return null;
    }

    public void openChat(String jid, String name) {
        ChatWindow chatWindow = chatWindows.get(jid);
        if (chatWindow == null) {
            chatWindow = new ChatWindow(context, jid, name);
            chatWindows.put(jid, chatWindow);
            chatWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    chatWindows.remove(jid);
                    contactTreeCellRenderer.stopBlinking(jid);
                }
            });
        }
        chatWindow.setVisible(true);
        chatWindow.toFront();
        chatWindow.requestFocus();
        contactTreeCellRenderer.stopBlinking(jid);
    }

    public void updateContacts(Iterable<org.jivesoftware.smack.roster.RosterGroup> groups, Iterable<org.jivesoftware.smack.roster.RosterEntry> entries) {
        contactTreeModel.updateContacts(groups, entries);
    }

    public ContactWindow getContactWindow() {
        return contactWindow;
    }

    public ChatWindow getChatWindow() {
        return chatWindow;
    }
} 