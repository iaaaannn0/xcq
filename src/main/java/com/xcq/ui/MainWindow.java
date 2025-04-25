package com.xcq.ui;

import com.xcq.core.ApplicationContext;
import com.xcq.core.Configuration;
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

        tabbedPane = new JTabbedPane();
        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        contactWindow = new ContactWindow(context);
        tabbedPane.addTab("联系人", contactWindow);

        chatWindow = new ChatWindow(context, null, null);
        tabbedPane.addTab("聊天", chatWindow);

        // 创建联系人树
        contactTree = new JTree(contactTreeModel);
        contactTree.setCellRenderer(new ContactTreeCellRenderer(contactTreeModel));
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

        // 注册消息监听器
        context.getXmppClient().addMessageListener(this);

        // 窗口关闭时清理资源
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                logger.info("正在关闭应用程序...");
                for (ChatWindow window : chatWindows.values()) {
                    window.dispose();
                }
                context.getXmppClient().disconnect();
                System.exit(0);
            }
        });
    }

    private void openChat(String jid, String name) {
        ChatWindow chatWindow = chatWindows.get(jid);
        if (chatWindow == null) {
            chatWindow = new ChatWindow(context, jid, name);
            chatWindows.put(jid, chatWindow);
            chatWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    chatWindows.remove(jid);
                    contactTreeModel.stopBlink(jid);
                }
            });
        }
        chatWindow.setVisible(true);
        chatWindow.toFront();
        chatWindow.requestFocus();
        contactTreeModel.stopBlink(jid);
    }

    @Override
    public void onMessageReceived(String from, String message) {
        SwingUtilities.invokeLater(() -> {
            ChatWindow chatWindow = chatWindows.get(from);
            if (chatWindow == null || !chatWindow.isVisible() || !chatWindow.isFocused()) {
                // 显示系统通知
                String displayName = from;
                NotificationManager.getInstance().showNotification(
                    "新消息",
                    displayName + ": " + message
                );
                // 开始闪烁
                contactTreeModel.startBlink(from);
            }
        });
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