package com.xcq.ui;

import com.xcq.core.ApplicationContext;
import com.xcq.db.ChatDatabase;
import com.xcq.ui.dialog.AddContactDialog;
import com.xcq.ui.dialog.VCardDialog;
import com.xcq.ui.dialog.SettingsDialog;
import com.xcq.ui.model.ContactTreeModel;
import com.xcq.ui.renderer.ContactTreeCellRenderer;
import com.xcq.xmpp.XMPPClient;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.StringSelection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

public class ContactWindow extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(ContactWindow.class);
    private final ApplicationContext context;
    private final JTree contactTree;
    private final ContactTreeModel treeModel;
    private final ContactTreeCellRenderer cellRenderer;
    private final JTextField searchField;
    private final JPopupMenu contactPopupMenu;
    private final JLabel statusLabel;
    private final JLabel usernameLabel;
    private final JComboBox<String> statusComboBox;
    private final Map<String, ChatWindow> chatWindowCache = new HashMap<>();  // 添加聊天窗口缓存

    public ContactWindow(ApplicationContext context) {
        this.context = context;
        
        // 设置窗口属性
        setTitle("XCQ - 联系人");
        setSize(358, 727);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 设置窗口图标
        ImageIcon logoIcon = new ImageIcon(getClass().getResource("/logo.png"));
        setIconImage(logoIcon.getImage());

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // 创建顶部面板（包含用户信息和搜索框）
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));

        // 创建用户状态面板
        JPanel userPanel = new JPanel(new BorderLayout(5, 5));
        userPanel.setBorder(new EmptyBorder(0, 0, 5, 0));

        // 用户名和状态图标
        JPanel userInfoPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        
        // 第一行：状态图标和用户名（可点击复制）
        JPanel namePanel = new JPanel(new BorderLayout(5, 0));
        statusLabel = new JLabel("🟢");
        String username = context.getXmppClient().getConnection().getUser().toString();
        usernameLabel = new JLabel(username);
        usernameLabel.setFont(usernameLabel.getFont().deriveFont(Font.BOLD));
        usernameLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        usernameLabel.setToolTipText("点击复制完整 JID");
        usernameLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String jid = context.getXmppClient().getConnection().getUser().toString();
                Toolkit.getDefaultToolkit()
                       .getSystemClipboard()
                       .setContents(new StringSelection(jid), null);
                JOptionPane.showMessageDialog(ContactWindow.this,
                    "已复制 JID: " + jid,
                    "复制成功",
                    JOptionPane.INFORMATION_MESSAGE);
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                usernameLabel.setText("<html><u>" + username + "</u></html>");
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                usernameLabel.setText(username);
            }
        });
        namePanel.add(statusLabel, BorderLayout.WEST);
        namePanel.add(usernameLabel, BorderLayout.CENTER);
        
        userInfoPanel.add(namePanel);
        
        // 第二行：服务器信息
        String serverInfo = context.getXmppClient().getConnection().getXMPPServiceDomain().toString();
        JLabel serverLabel = new JLabel("@" + serverInfo);
        serverLabel.setForeground(Color.GRAY);
        serverLabel.setFont(serverLabel.getFont().deriveFont(Font.PLAIN));
        userInfoPanel.add(serverLabel);
        
        userPanel.add(userInfoPanel, BorderLayout.NORTH);

        // 状态选择下拉框
        statusComboBox = new JComboBox<>(new String[]{"在线 🟢", "离开 🟡", "忙碌 🔴", "离线 ⚫"});
        statusComboBox.setSelectedIndex(0);
        statusComboBox.addActionListener(e -> updateUserStatus());
        userPanel.add(statusComboBox, BorderLayout.SOUTH);

        topPanel.add(userPanel, BorderLayout.NORTH);

        // 创建搜索面板（直接放在状态选择框下方）
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        
        // 搜索框
        searchField = new JTextField();
        searchField.setToolTipText("搜索联系人（支持拼音、昵称或JID）");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateFilter(); }
            public void removeUpdate(DocumentEvent e) { updateFilter(); }
            public void changedUpdate(DocumentEvent e) { updateFilter(); }
        });
        
        // 搜索图标
        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setBorder(new EmptyBorder(0, 5, 0, 5));
        
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchIcon, BorderLayout.EAST);
        
        topPanel.add(searchPanel, BorderLayout.CENTER);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // 创建联系人树
        treeModel = new ContactTreeModel();
        contactTree = new JTree(treeModel);
        treeModel.setTree(contactTree);
        cellRenderer = new ContactTreeCellRenderer(treeModel);
        contactTree.setCellRenderer(cellRenderer);
        contactTree.setRootVisible(false);
        contactTree.setShowsRootHandles(true);
        contactTree.setExpandsSelectedPaths(true);

        // 添加树节点选择监听器
        contactTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path = contactTree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (node.getUserObject() instanceof RosterEntry) {
                        if (e.getClickCount() == 2) {
                            openChatWindow((RosterEntry) node.getUserObject());
                        } else if (SwingUtilities.isRightMouseButton(e)) {
                            showContactPopupMenu(e, (RosterEntry) node.getUserObject());
                        }
                    }
                }
            }
        });

        // 创建滚动面板
        JScrollPane scrollPane = new JScrollPane(contactTree);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // 创建底部工具栏
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        // 左侧按钮面板
        JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // 添加联系人按钮
        JButton addButton = new JButton("添加联系人");
        addButton.addActionListener(e -> showAddContactDialog());
        leftButtonPanel.add(addButton);
        
        // 添加分组按钮
        JButton addGroupButton = new JButton("添加分组");
        addGroupButton.addActionListener(e -> showAddGroupDialog());
        leftButtonPanel.add(addGroupButton);
        
        toolBar.add(leftButtonPanel);
        
        // 添加弹性空间
        toolBar.add(Box.createHorizontalGlue());
        
        // 右侧按钮面板
        JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        // 设置按钮
        JButton settingsButton = new JButton("设置");
        settingsButton.addActionListener(e -> showSettingsDialog());
        rightButtonPanel.add(settingsButton);
        
        // 登出按钮
        JButton logoutButton = new JButton("登出");
        logoutButton.addActionListener(e -> logout());
        rightButtonPanel.add(logoutButton);
        
        toolBar.add(rightButtonPanel);
        mainPanel.add(toolBar, BorderLayout.SOUTH);

        // 创建联系人右键菜单
        contactPopupMenu = createContactPopupMenu();

        setContentPane(mainPanel);
        
        // 初始化XMPP监听器
        setupXMPPListeners();
        
        // 加载联系人列表
        refreshContacts();
    }

    private void setupXMPPListeners() {
        XMPPClient xmppClient = context.getXmppClient();
        if (xmppClient != null && xmppClient.isConnected()) {
            // 添加花名册监听器
            Roster roster = Roster.getInstanceFor(xmppClient.getConnection());
            roster.addRosterListener(new RosterListener() {
                @Override
                public void entriesAdded(Collection<Jid> addresses) {
                    SwingUtilities.invokeLater(() -> refreshContacts());
                }

                @Override
                public void entriesUpdated(Collection<Jid> addresses) {
                    SwingUtilities.invokeLater(() -> refreshContacts());
                }

                @Override
                public void entriesDeleted(Collection<Jid> addresses) {
                    SwingUtilities.invokeLater(() -> refreshContacts());
                }

                @Override
                public void presenceChanged(Presence presence) {
                    SwingUtilities.invokeLater(() -> {
                        String jid = presence.getFrom().asBareJid().toString();
                        cellRenderer.updatePresence(presence.getFrom().asBareJid(), presence.getMode());
                        if (presence.getType() == Presence.Type.unavailable) {
                            cellRenderer.setJidColor(jid, Color.RED);
                        } else {
                            cellRenderer.setJidColor(jid, Color.BLACK);
                        }
                        contactTree.repaint();
                    });
                }
            });

            // 添加消息监听器
            xmppClient.addMessageListener((from, message) -> {
                SwingUtilities.invokeLater(() -> {
                    String fromJid = from.toString();
                    String myJid = xmppClient.getConnection().getUser().asBareJid().toString();
                    String content = message;
                    
                    if (content != null) {
                        // 保存消息到数据库
                        ChatDatabase.getInstance().saveMessage(fromJid, myJid, content, false);
                        
                        // 查找是否有对应的聊天窗口
                        ChatWindow existingWindow = null;
                        for (ChatWindow window : chatWindowCache.values()) {
                            if (window.getContactJid().equals(fromJid)) {
                                existingWindow = window;
                                break;
                            }
                        }
                        
                        if (existingWindow != null) {
                            // 如果聊天窗口存在，显示消息
                            existingWindow.appendMessage(fromJid, myJid, content, false);
                            if (!existingWindow.isActive()) {
                                existingWindow.startNotification();
                            }
                        } else {
                            // 如果聊天窗口不存在，创建新窗口
                            try {
                                RosterEntry entry = roster.getEntry(JidCreate.bareFrom(fromJid));
                                String displayName = entry != null ? entry.getName() : fromJid;
                                ChatWindow newWindow = new ChatWindow(context, fromJid, displayName);
                                chatWindowCache.put(fromJid, newWindow);
                                newWindow.addWindowListener(new WindowAdapter() {
                                    @Override
                                    public void windowClosed(WindowEvent e) {
                                        chatWindowCache.remove(fromJid);
                                    }
                                });
                                newWindow.appendMessage(fromJid, myJid, content, false);
                                newWindow.setVisible(true);
                                newWindow.startNotification();
                            } catch (Exception e) {
                                logger.error("Failed to create chat window", e);
                            }
                        }
                        
                        // 开始联系人闪烁提醒
                        startContactBlinking(fromJid);
                    }
                });
            });
        }
    }

    private void refreshContacts() {
        XMPPClient xmppClient = context.getXmppClient();
        if (xmppClient != null && xmppClient.isConnected()) {
            Roster roster = Roster.getInstanceFor(xmppClient.getConnection());
            treeModel.updateContacts(roster.getGroups(), roster.getUnfiledEntries());
            
            // 展开所有节点
            for (int i = 0; i < contactTree.getRowCount(); i++) {
                contactTree.expandRow(i);
            }
        }
    }

    private void updateFilter() {
        String searchText = searchField.getText().toLowerCase().trim();
        if (searchText.isEmpty()) {
            // 如果搜索框为空，显示所有联系人
            refreshContacts();
            return;
        }

        XMPPClient xmppClient = context.getXmppClient();
        if (xmppClient != null && xmppClient.isConnected()) {
            Roster roster = Roster.getInstanceFor(xmppClient.getConnection());
            // 更新树模型，传入所有分组和未分组联系人，以及搜索文本
            treeModel.updateFilteredContacts(roster.getGroups(), roster.getUnfiledEntries(), searchText);
            
            // 展开所有节点
            for (int i = 0; i < contactTree.getRowCount(); i++) {
                contactTree.expandRow(i);
            }
        }
    }

    private JPopupMenu createContactPopupMenu() {
        JPopupMenu popup = new JPopupMenu();
        
        JMenuItem chatMenuItem = new JMenuItem("发送消息");
        chatMenuItem.addActionListener(e -> {
            TreePath path = contactTree.getSelectionPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.getUserObject() instanceof RosterEntry) {
                    openChatWindow((RosterEntry) node.getUserObject());
                }
            }
        });
        
        JMenuItem viewProfileMenuItem = new JMenuItem("查看资料");
        viewProfileMenuItem.addActionListener(e -> {
            TreePath path = contactTree.getSelectionPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.getUserObject() instanceof RosterEntry) {
                    showVCard((RosterEntry) node.getUserObject());
                }
            }
        });
        
        JMenuItem copyJidMenuItem = new JMenuItem("复制 JID");
        copyJidMenuItem.addActionListener(e -> {
            TreePath path = contactTree.getSelectionPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.getUserObject() instanceof RosterEntry) {
                    RosterEntry entry = (RosterEntry) node.getUserObject();
                    String jid = entry.getJid().toString();
                    Toolkit.getDefaultToolkit()
                           .getSystemClipboard()
                           .setContents(new StringSelection(jid), null);
                }
            }
        });
        
        JMenuItem renameMenuItem = new JMenuItem("修改昵称");
        renameMenuItem.addActionListener(e -> {
            TreePath path = contactTree.getSelectionPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.getUserObject() instanceof RosterEntry) {
                    showRenameDialog((RosterEntry) node.getUserObject());
                }
            }
        });
        
        JMenu moveToGroupMenu = new JMenu("移动到分组");
        // TODO: 实现移动到分组功能
        
        JMenuItem removeMenuItem = new JMenuItem("删除联系人");
        removeMenuItem.addActionListener(e -> {
            TreePath path = contactTree.getSelectionPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.getUserObject() instanceof RosterEntry) {
                    removeContact((RosterEntry) node.getUserObject());
                }
            }
        });
        
        popup.add(chatMenuItem);
        popup.add(viewProfileMenuItem);
        popup.add(copyJidMenuItem);
        popup.addSeparator();
        popup.add(renameMenuItem);
        popup.add(moveToGroupMenu);
        popup.addSeparator();
        popup.add(removeMenuItem);
        
        return popup;
    }

    private void showContactPopupMenu(MouseEvent e, RosterEntry entry) {
        contactPopupMenu.show(contactTree, e.getX(), e.getY());
    }

    private void showAddContactDialog() {
        XMPPClient xmppClient = context.getXmppClient();
        if (xmppClient != null && xmppClient.isConnected()) {
            Roster roster = Roster.getInstanceFor(xmppClient.getConnection());
            AddContactDialog dialog = new AddContactDialog(this, roster.getGroups());
            dialog.setVisible(true);
            
            if (dialog.isApproved()) {
                try {
                    String jid = dialog.getJID();
                    String group = dialog.getSelectedGroup();
                    String message = dialog.getMessage();
                    
                    if (!"未分组".equals(group)) {
                        roster.createGroup(group);
                        roster.createEntry(JidCreate.bareFrom(jid), null, new String[]{group});
                    } else {
                        roster.createEntry(JidCreate.bareFrom(jid), null, new String[0]);
                    }
                    
                    // TODO: 发送订阅请求
                    
                } catch (Exception ex) {
                    logger.error("Failed to add contact", ex);
                    JOptionPane.showMessageDialog(this,
                        "添加联系人失败: " + ex.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void showAddGroupDialog() {
        String groupName = JOptionPane.showInputDialog(this,
            "请输入分组名称:",
            "添加分组",
            JOptionPane.PLAIN_MESSAGE);
            
        if (groupName != null && !groupName.trim().isEmpty()) {
            XMPPClient xmppClient = context.getXmppClient();
            if (xmppClient != null && xmppClient.isConnected()) {
                try {
                    Roster roster = Roster.getInstanceFor(xmppClient.getConnection());
                    roster.createGroup(groupName.trim());
                    refreshContacts();
                } catch (Exception ex) {
                    logger.error("Failed to create group", ex);
                    JOptionPane.showMessageDialog(this,
                        "创建分组失败: " + ex.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void showRenameDialog(RosterEntry entry) {
        // 创建一个面板来容纳多行信息
        JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
        
        // 显示 JID
        panel.add(new JLabel("JID: " + entry.getJid().toString()));
        
        // 显示当前昵称
        String currentName = entry.getName() != null ? entry.getName() : entry.getJid().toString();
        panel.add(new JLabel("当前昵称: " + currentName));
        
        // 输入新昵称
        JTextField nameField = new JTextField(currentName);
        panel.add(nameField);
        
        int result = JOptionPane.showConfirmDialog(this,
            panel,
            "修改昵称",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
            
        if (result == JOptionPane.OK_OPTION) {
            String newName = nameField.getText().trim();
            if (!newName.isEmpty()) {
                try {
                    entry.setName(newName);
                    refreshContacts();
                } catch (Exception ex) {
                    logger.error("Failed to rename contact", ex);
                    JOptionPane.showMessageDialog(this,
                        "修改昵称失败: " + ex.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void removeContact(RosterEntry entry) {
        int result = JOptionPane.showConfirmDialog(this,
            "确定要删除联系人 " + entry.getName() + " 吗？",
            "删除联系人",
            JOptionPane.YES_NO_OPTION);
            
        if (result == JOptionPane.YES_OPTION) {
            try {
                Roster roster = Roster.getInstanceFor(context.getXmppClient().getConnection());
                roster.removeEntry(entry);
                refreshContacts();
            } catch (Exception ex) {
                logger.error("Failed to remove contact", ex);
                JOptionPane.showMessageDialog(this,
                    "删除联系人失败: " + ex.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showVCard(RosterEntry entry) {
        try {
            XMPPClient xmppClient = context.getXmppClient();
            if (xmppClient != null && xmppClient.isConnected()) {
                VCardManager vCardManager = VCardManager.getInstanceFor(xmppClient.getConnection());
                VCard vCard = vCardManager.loadVCard(entry.getJid().asEntityBareJidIfPossible());
                VCardDialog dialog = new VCardDialog(this, entry, vCard);
                dialog.setVisible(true);
            }
        } catch (Exception ex) {
            logger.error("Failed to load vCard", ex);
            JOptionPane.showMessageDialog(this,
                "获取联系人资料失败: " + ex.getMessage(),
                "错误",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openChatWindow(RosterEntry entry) {
        String jid = entry.getJid().toString();
        ChatWindow chatWindow = chatWindowCache.get(jid);
        
        if (chatWindow == null || !chatWindow.isDisplayable()) {
            // 如果窗口不存在或已被销毁，创建新窗口
            chatWindow = new ChatWindow(context, jid, entry.getName());
            chatWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    // 从缓存中移除已关闭的窗口
                    chatWindowCache.remove(jid);
                }
            });
            chatWindowCache.put(jid, chatWindow);
        }
        
        chatWindow.setVisible(true);
        chatWindow.toFront();
        chatWindow.requestFocus();
    }

    private void updateUserStatus() {
        String selectedStatus = (String) statusComboBox.getSelectedItem();
        if (selectedStatus != null) {
            XMPPClient xmppClient = context.getXmppClient();
            if (xmppClient != null && xmppClient.isConnected()) {
                Presence.Mode mode;
                String statusIcon;
                
                switch (selectedStatus) {
                    case "在线 🟢":
                        mode = Presence.Mode.available;
                        statusIcon = "🟢";
                        break;
                    case "离开 🟡":
                        mode = Presence.Mode.away;
                        statusIcon = "🟡";
                        break;
                    case "忙碌 🔴":
                        mode = Presence.Mode.dnd;
                        statusIcon = "🔴";
                        break;
                    default:
                        mode = null;
                        statusIcon = "⚫";
                }

                try {
                    Presence presence;
                    if (mode == null) {
                        presence = new Presence(Presence.Type.unavailable);
                    } else {
                        presence = new Presence(Presence.Type.available);
                        presence.setMode(mode);
                    }
                    
                    xmppClient.getConnection().sendStanza(presence);
                    statusLabel.setText(statusIcon);
                } catch (SmackException.NotConnectedException | InterruptedException e) {
                    logger.error("Failed to update presence", e);
                    JOptionPane.showMessageDialog(this,
                        "更新状态失败: " + e.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void showSettingsDialog() {
        SettingsDialog dialog = new SettingsDialog(this, context);
        dialog.setVisible(true);
    }

    private void logout() {
        int result = JOptionPane.showConfirmDialog(this,
            "确定要退出登录吗？",
            "退出登录",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
            
        if (result == JOptionPane.YES_OPTION) {
            try {
                // 关闭所有聊天窗口
                for (ChatWindow window : chatWindowCache.values()) {
                    window.dispose();
                }
                chatWindowCache.clear();
                
                // 断开 XMPP 连接
                XMPPClient xmppClient = context.getXmppClient();
                if (xmppClient != null && xmppClient.isConnected()) {
                    xmppClient.disconnect();
                }
                
                // 显示登录窗口
                LoginWindow loginWindow = new LoginWindow(context);
                loginWindow.setVisible(true);
                
                // 关闭当前窗口
                dispose();
                
            } catch (Exception ex) {
                logger.error("Failed to logout", ex);
                JOptionPane.showMessageDialog(this,
                    "退出登录失败: " + ex.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void startContactBlinking(String jid) {
        SwingUtilities.invokeLater(() -> {
            cellRenderer.startBlinking(jid);
            contactTree.repaint();
        });
    }

    public void stopContactBlinking(String jid) {
        SwingUtilities.invokeLater(() -> {
            cellRenderer.stopBlinking(jid);
            contactTree.repaint();
        });
    }

    @Override
    public void dispose() {
        // 关闭所有聊天窗口
        for (ChatWindow window : chatWindowCache.values()) {
            window.dispose();
        }
        chatWindowCache.clear();
        super.dispose();
    }
} 