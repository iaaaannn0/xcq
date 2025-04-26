package com.xcq.ui;

import com.xcq.core.ApplicationContext;
import com.xcq.core.Configuration;
import com.xcq.db.ChatDatabase;
import com.xcq.db.ChatDatabase.ChatMessage;
import com.xcq.ui.components.EmojiPanel;
import com.xcq.ui.components.ImageMessageComponent;
import com.xcq.util.FileUploader;
import com.xcq.xmpp.XMPPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ChatWindow extends JFrame implements WindowFocusListener, WindowListener {
    private static final Logger logger = LoggerFactory.getLogger(ChatWindow.class);
    private final ApplicationContext context;
    private final String contactJid;
    private final String contactName;
    private final JTextPane chatArea;
    private final JTextArea inputArea;
    private final StyledDocument doc;
    private final DateTimeFormatter timeFormatter;
    private Clip notificationSound;
    private boolean isListenerRegistered = false;
    private long lastMessageTimestamp = 0; // 用于防止重复显示消息
    private boolean historyLoaded = false;
    private XMPPClient.MessageListener listener;
    private boolean isWindowActive = false;
    private boolean isDisposed = false;
    private JPanel bottomPanel;
    private JToolBar toolBar;

    public ChatWindow(ApplicationContext context, String contactJid, String contactName) {
        this.context = context;
        this.contactJid = contactJid;
        this.contactName = contactName != null ? contactName : contactJid;
        this.timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 初始化提示音
        initNotificationSound();

        // 设置窗口属性
        setTitle("与 " + this.contactName + " 聊天中");
        setSize(600, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // 设置窗口图标
        try {
            URL logoUrl = getClass().getResource("/logo.png");
            if (logoUrl != null) {
                ImageIcon logoIcon = new ImageIcon(logoUrl);
                setIconImage(logoIcon.getImage());
            } else {
                logger.warn("Logo resource /logo.png not found.");
            }
        } catch (Exception e) {
            logger.error("Error loading logo icon", e);
        }

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(0, 5));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 创建聊天区域
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        doc = chatArea.getStyledDocument();

        // 设置样式
        addStylesToDocument(doc);

        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setPreferredSize(new Dimension(500, 300));
        mainPanel.add(chatScrollPane, BorderLayout.CENTER);

        // 创建底部面板（包含工具栏和输入区域）
        bottomPanel = new JPanel(new BorderLayout(0, 5));

        // 创建工具栏
        initToolbar();
        bottomPanel.add(toolBar, BorderLayout.NORTH);

        // 创建输入区域
        inputArea = new JTextArea();
        inputArea.setFont(new Font("微软雅黑", Font.PLAIN, 14)); // 稍大字体
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);

        // 添加快捷键 (Ctrl+Enter / Shift+Enter 换行, Enter 发送)
        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isControlDown() || e.isShiftDown()) {
                        inputArea.insert("\n", inputArea.getCaretPosition());
                    } else {
                        e.consume(); // 阻止 Enter 默认行为（换行）
                        sendMessage();
                    }
                }
            }
        });

        JScrollPane inputScrollPane = new JScrollPane(inputArea);
        inputScrollPane.setPreferredSize(new Dimension(500, 100));
        bottomPanel.add(inputScrollPane, BorderLayout.CENTER);

        // 创建发送按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton sendButton = new JButton("发送 (Enter)");
        sendButton.setMnemonic(KeyEvent.VK_S); // Alt+S 快捷键
        sendButton.addActionListener(e -> sendMessage());
        buttonPanel.add(sendButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        setContentPane(mainPanel);

        // 立即注册消息监听器
        registerMessageListener();

        // 加载历史消息 (异步加载防止阻塞UI)
        SwingUtilities.invokeLater(this::loadChatHistory);

        // 添加窗口监听器
        addWindowFocusListener(this);
        addWindowListener(this);

        // 创建菜单栏
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("操作");
        menu.setMnemonic(KeyEvent.VK_O); // Alt+O

        // 添加删除聊天记录菜单项
        JMenuItem deleteHistoryItem = new JMenuItem("删除聊天记录");
        deleteHistoryItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK)); // Ctrl+D
        deleteHistoryItem.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(this,
                "确定要删除与 " + contactName + " 的所有聊天记录吗？\n此操作不可恢复。",
                "确认删除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (result == JOptionPane.YES_OPTION) {
                 ChatDatabase db = ChatDatabase.getInstance();
                 if (db != null) {
                    String currentUserJid = getCurrentUserJid();
                    if (currentUserJid != null) {
                        db.deleteChatHistory(currentUserJid, contactJid);
                        chatArea.setText(""); // 清空显示区域
                        historyLoaded = false; // 重置历史加载状态，虽然已清空，但逻辑上一致
                        JOptionPane.showMessageDialog(this,
                            "聊天记录已删除",
                            "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        showErrorDialog("无法获取当前用户信息，无法删除记录。");
                    }
                 } else {
                     showErrorDialog("数据库连接失败，无法删除记录。");
                 }
            }
        });
        menu.add(deleteHistoryItem);

        // 添加临时消息设置菜单项
        JCheckBoxMenuItem temporaryMessageItem = new JCheckBoxMenuItem("临时会话 (不保存聊天记录)");
        temporaryMessageItem.setToolTipText("勾选后，与此联系人的聊天记录将不会被保存");
        Configuration config = Configuration.getInstance();
        temporaryMessageItem.setSelected(config.isTemporaryContact(contactJid));
        temporaryMessageItem.addActionListener(e -> {
            if (temporaryMessageItem.isSelected()) {
                config.addTemporaryContact(contactJid);
                JOptionPane.showMessageDialog(this, 
                    contactName + " 已设为临时会话，\n之后的聊天记录将不会保存。", 
                    "临时会话", JOptionPane.INFORMATION_MESSAGE);
            } else {
                config.removeTemporaryContact(contactJid);
                 JOptionPane.showMessageDialog(this, 
                    contactName + " 已取消临时会话，\n之后的聊天记录将会保存。", 
                    "临时会话", JOptionPane.INFORMATION_MESSAGE);
            }
             // 可能需要更新配置持久化逻辑
             // config.saveConfiguration(); 
        });
        menu.add(temporaryMessageItem);

        menuBar.add(menu);
        setJMenuBar(menuBar);
    }
    
    // 辅助方法获取当前用户JID
    private String getCurrentUserJid() {
        if (context != null && context.getXmppClient() != null && context.getXmppClient().isConnected()) {
            return context.getXmppClient().getConnection().getUser().toString();
        } 
        logger.warn("Could not get current user JID: XMPP client not connected or context is null.");
        return null;
    }
    
    // 辅助方法显示错误信息
    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "错误", JOptionPane.ERROR_MESSAGE);
    }

    private void initToolbar() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        // 创建按钮（暂时不使用图标）
        JButton emojiButton = new JButton("表情");
        JButton imageButton = new JButton("图片");
        JButton fileButton = new JButton("文件");
        
        // 设置按钮提示文本
        emojiButton.setToolTipText("表情");
        imageButton.setToolTipText("图片");
        fileButton.setToolTipText("文件");
        
        // 添加按钮到工具栏
        toolBar.add(emojiButton);
        toolBar.add(imageButton);
        toolBar.add(fileButton);
        
        // 添加工具栏到底部面板
        bottomPanel.add(toolBar, BorderLayout.NORTH);
    }

    private void showEmojiPanel(JButton sourceButton) {
        EmojiPanel.showEmojiPanel(sourceButton, emoji -> {
            inputArea.insert(emoji, inputArea.getCaretPosition());
            inputArea.requestFocus(); // 将焦点返回输入区域
        });
    }

    private void selectAndUploadImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择要发送的图片");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                       name.endsWith(".png") || name.endsWith(".gif");
            }
            public String getDescription() {
                return "图片文件 (*.jpg, *.jpeg, *.png, *.gif)";
            }
        });

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            uploadAndSendFile(file, "[图片上传中...]");
        }
    }

    private void selectAndUploadFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择要发送的文件");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            uploadAndSendFile(file, "[文件上传中: " + file.getName() + "]");
        }
    }

    private void uploadAndSendFile(File file, String uploadingMessage) {
        // 显示上传中消息 (作为系统消息)
        appendSystemMessage(uploadingMessage);

        // 异步上传文件
        FileUploader.uploadFile(file, this).thenAcceptAsync(url -> {
            if (url != null) {
                // 发送文件URL
                try {
                    context.getXmppClient().sendMessage(contactJid, url);
                    // 界面上直接显示发送的文件URL
                    appendMessage("我", getCurrentUserJid(), url, System.currentTimeMillis(), true);
                } catch (Exception e) {
                    logger.error("Error sending file URL: {}", url, e);
                    SwingUtilities.invokeLater(() -> {
                        showErrorDialog("发送文件失败: " + e.getMessage());
                        // 可以考虑移除之前的"上传中"消息或添加失败消息
                        // appendSystemMessage("[文件发送失败: " + file.getName() + "]");
                    });
                }
            } else {
                 SwingUtilities.invokeLater(() -> {
                      showErrorDialog("文件上传失败: " + file.getName());
                      // appendSystemMessage("[文件上传失败: " + file.getName() + "]");
                 });
            }
        }, SwingUtilities::invokeLater); // 确保回调在EDT执行
    }

    private void addStylesToDocument(StyledDocument doc) {
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

        Style regular = doc.addStyle("regular", def);
        StyleConstants.setFontFamily(regular, "微软雅黑");
        StyleConstants.setFontSize(regular, 14);

        Style timeStyle = doc.addStyle("time", regular);
        StyleConstants.setForeground(timeStyle, Color.GRAY);
        StyleConstants.setFontSize(timeStyle, 10);

        Style senderStyle = doc.addStyle("sender", regular);
        StyleConstants.setBold(senderStyle, true);

        Style mySenderStyle = doc.addStyle("mySender", senderStyle);
        StyleConstants.setForeground(mySenderStyle, new Color(0, 128, 0)); // 深绿色

        Style contactSenderStyle = doc.addStyle("contactSender", senderStyle);
        StyleConstants.setForeground(contactSenderStyle, Color.BLUE);
        
        Style systemStyle = doc.addStyle("system", regular);
        StyleConstants.setForeground(systemStyle, Color.DARK_GRAY);
        StyleConstants.setItalic(systemStyle, true);
        StyleConstants.setFontSize(systemStyle, 12);
    }
    
    // 重构 appendMessage 以处理时间戳和发送者样式
    public void appendMessage(String senderName, String senderJid, String content, long timestamp, boolean isSentByMe) {
        SwingUtilities.invokeLater(() -> {
            if (isDisposed) return; // 如果窗口已销毁，则不处理
            try {
                // 防重处理
                if (timestamp > 0 && timestamp - lastMessageTimestamp < 100 && senderJid != null && senderJid.equals(this.contactJid)) {
                    logger.warn("Skipping potentially duplicate message within 100ms: {}", content);
                    return;
                }
                if (timestamp > 0) {
                    lastMessageTimestamp = timestamp;
                }

                // 如果是收到的消息且窗口不在前台，触发通知
                if (!isSentByMe && !isWindowActive) {
                    playNotificationSound(); // 播放声音
                    // 获取 ContactWindow 并触发联系人闪烁
                    ContactWindow contactWindow = context.getContactWindow();
                    if (contactWindow != null) {
                        contactWindow.startContactBlinking(senderJid); // 触发联系人列表项闪烁
                    }
                }

                // 格式化时间戳
                LocalDateTime messageTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
                String timeStr = messageTime.format(timeFormatter);
                
                // 添加时间戳
                doc.insertString(doc.getLength(), "[" + timeStr + "] ", doc.getStyle("time"));
                
                // 添加发送者名称和样式
                Style senderStyleToUse = isSentByMe ? doc.getStyle("mySender") : doc.getStyle("contactSender");
                doc.insertString(doc.getLength(), senderName + ": ", senderStyleToUse);
                
                // 处理消息内容 (图片或文本)
                if (isImageUrl(content)) {
                    // 插入换行符，让图片在新行显示
                    doc.insertString(doc.getLength(), "\n", doc.getStyle("regular")); 
                    ImageMessageComponent imageComponent = new ImageMessageComponent(content);
                    chatArea.setCaretPosition(doc.getLength());
                    chatArea.insertComponent(imageComponent);
                    doc.insertString(doc.getLength(), "\n", doc.getStyle("regular")); // 图片后再加一个换行
                } else {
                    // 普通文本消息
                    doc.insertString(doc.getLength(), content + "\n", doc.getStyle("regular"));
                }
                
                // 滚动到底部
                chatArea.setCaretPosition(doc.getLength());
                
            } catch (BadLocationException e) {
                logger.error("Error appending message to chat area", e);
            } catch (Exception e) { // Catch other potential runtime errors
                logger.error("Unexpected error in appendMessage", e);
            }
        });
    }
    
    // 添加一个专门用于系统消息的方法
    public void appendSystemMessage(String message) {
         SwingUtilities.invokeLater(() -> {
             if (isDisposed) return;
             try {
                 String timeStr = LocalDateTime.now().format(timeFormatter);
                 doc.insertString(doc.getLength(), "[" + timeStr + "] ", doc.getStyle("time"));
                 doc.insertString(doc.getLength(), message + "\n", doc.getStyle("system"));
                 chatArea.setCaretPosition(doc.getLength());
             } catch (BadLocationException e) {
                 logger.error("Error appending system message", e);
             }
         });
    }

    private boolean isImageUrl(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lowerCase = text.toLowerCase();
        return lowerCase.startsWith("http") && (
            lowerCase.endsWith(".jpg") ||
            lowerCase.endsWith(".jpeg") ||
            lowerCase.endsWith(".png") ||
            lowerCase.endsWith(".gif") ||
            (lowerCase.contains("/upload/") && (lowerCase.contains(".jpg") || lowerCase.contains(".png"))) // 兼容特定服务器URL格式
        );
    }

    private void registerMessageListener() {
        if (!isListenerRegistered && context != null && context.getXmppClient() != null) {
            listener = (from, message) -> {
                // 确保消息来自当前聊天对象
                if (from != null && from.equals(contactJid)) {
                    // 使用 appendMessage 方法处理
                    appendMessage(contactName, from, message, System.currentTimeMillis(), false);
                    
                     // 保存收到的消息到数据库 (如果不是临时会话)
                    if (!Configuration.getInstance().isTemporaryContact(contactJid)) {
                        ChatDatabase db = ChatDatabase.getInstance();
                        String currentUserJid = getCurrentUserJid();
                        if (db != null && currentUserJid != null) {
                            // 注意：isLocal 应该是 false 因为这是收到的消息
                            db.saveMessage(from, currentUserJid, message, false); 
                        } else {
                             logger.warn("Database or currentUserJid is null, message from {} not saved.", from);
                        }
                    }
                }
            };
            context.getXmppClient().addMessageListener(listener);
            isListenerRegistered = true;
            logger.info("Message listener registered for contact: {}", contactJid);
        } else {
             logger.warn("Could not register message listener. Listener already registered or XMPP client is null.");
        }
    }

    private void loadChatHistory() {
        if (historyLoaded || isDisposed) {
            logger.debug("Skipping chat history loading (already loaded or window disposed).");
            return;
        }
        
        logger.info("Attempting to load chat history for contact: {}", contactJid);
        appendSystemMessage("[正在加载历史记录...]");

        ChatDatabase db = ChatDatabase.getInstance();
        String currentUserJid = getCurrentUserJid();

        if (db == null || currentUserJid == null) {
            logger.error("Cannot load chat history: Database instance or current user JID is null.");
            appendSystemMessage("[错误：无法加载历史记录，数据库或用户信息不可用]");
            historyLoaded = true; // 标记为已尝试加载，避免重复尝试
            return;
        }

        try {
            List<ChatMessage> messages = db.getChatHistory(currentUserJid, contactJid);
            logger.info("Retrieved {} historical messages between {} and {}", messages.size(), currentUserJid, contactJid);

            // 在添加历史记录前，清空现有内容可能是个好主意，以防重复加载
            // chatArea.setText(""); // 如果需要清空的话

            for (ChatMessage msg : messages) {
                appendMessage(
                    msg.isLocal() ? "我" : contactName, // 显示名称
                    msg.getSenderJid(), // 实际 JID
                    msg.getMessage(),   // 消息内容
                    msg.getTimestamp().getTime(), // 时间戳 (ms)
                    msg.isLocal()       // 是否由我发送
                );
            }
            logger.info("成功加载了 {} 条历史消息", messages.size());
            appendSystemMessage("[历史记录加载完毕]");
            historyLoaded = true;
            // 加载完历史后滚动到底部
            SwingUtilities.invokeLater(() -> chatArea.setCaretPosition(doc.getLength()));

        } catch (Exception e) {
            logger.error("Error loading chat history for contact {}", contactJid, e);
            appendSystemMessage("[错误：加载历史记录时发生异常]");
            historyLoaded = true; // 标记为已尝试加载，避免重复失败
        }
    }

    public String getContactJid() {
        return contactJid;
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

    // WindowFocusListener 方法
    @Override
    public void windowGainedFocus(WindowEvent e) {
        if (isDisposed) return;
        logger.trace("Window gained focus: {}", contactJid);
        isWindowActive = true;
        // 窗口获得焦点时，标记此对话的消息为已读
        ChatDatabase db = ChatDatabase.getInstance();
        String currentUserJid = getCurrentUserJid();
        if (db != null && currentUserJid != null) {
             db.markMessagesAsRead(currentUserJid, contactJid);
             // 通知 ContactWindow 更新未读状态 (如果 ContactWindow 存在)
             ContactWindow contactWindow = context.getContactWindow();
             if (contactWindow != null) {
                 contactWindow.stopContactBlinking(contactJid); // 停止联系人闪烁
             }
        } else {
             logger.warn("Could not mark messages as read: DB or UserJID null");
        }
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        if (isDisposed) return;
        logger.trace("Window lost focus: {}", contactJid);
        isWindowActive = false;
    }

    // WindowListener 方法
    @Override
    public void windowOpened(WindowEvent e) {
         logger.debug("Chat window opened for: {}", contactJid);
         inputArea.requestFocusInWindow(); // 窗口打开时，让输入框获取焦点
    }

    @Override
    public void windowClosed(WindowEvent e) {
         logger.debug("Chat window closed event for: {}", contactJid);
         // cleanup 应该在 dispose() 中被调用，这里不需要额外操作
    }

    @Override
    public void windowClosing(WindowEvent e) {
         logger.debug("Chat window closing event for: {}", contactJid);
         dispose(); // 触发清理和关闭
    }

    @Override
    public void windowIconified(WindowEvent e) {
        logger.trace("Window iconified: {}", contactJid);
        isWindowActive = false; // 最小化视为失去焦点
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        if (isDisposed) return;
        logger.trace("Window deiconified: {}", contactJid);
        isWindowActive = true; // 恢复时视为获得焦点 (可能需要 GainedFocus 事件再次确认)
         // 标记消息已读逻辑在 GainedFocus 中处理更可靠
        // 移除 stopNotification() 调用
    }

    @Override
    public void windowActivated(WindowEvent e) {
        if (isDisposed) return;
        logger.trace("Window activated: {}", contactJid);
        isWindowActive = true;
        // 标记消息已读逻辑在 GainedFocus 中处理
        // 移除 stopNotification() 调用
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        if (isDisposed) return;
        logger.trace("Window deactivated: {}", contactJid);
        isWindowActive = false;
    }

    private void cleanup() {
        if (isDisposed) {
            logger.warn("Cleanup called on already disposed window: {}", contactJid);
            return;
        }
        logger.info("Cleaning up resources for chat window: {}", contactJid);
        isDisposed = true;
        isWindowActive = false; // 标记为非活动

        // 停止并关闭声音资源
        if (notificationSound != null) {
            try {
                if (notificationSound.isRunning()) {
                    notificationSound.stop();
                }
                if (notificationSound.isOpen()) {
                    notificationSound.close();
                }
                logger.debug("Notification sound closed.");
            } catch (Exception e) {
                logger.error("Error closing notification sound", e);
            }
            notificationSound = null;
        }

        // 移除消息监听器
        if (listener != null && context != null && context.getXmppClient() != null) {
            try {
                context.getXmppClient().removeMessageListener(listener);
                logger.debug("Message listener removed.");
            } catch (Exception e) {
                logger.error("Error removing message listener", e);
            }
            listener = null;
            isListenerRegistered = false;
        }

        // 重置状态
        historyLoaded = false;

        // 调用父类的 dispose，这将释放窗口资源并使其不可见
        super.dispose();
        logger.info("Chat window disposed successfully for: {}", contactJid);
    }

    @Override
    public void dispose() {
        cleanup();
    }

    private void sendMessage() {
        String text = inputArea.getText().trim();
        if (!text.isEmpty()) {
            String currentUserJid = getCurrentUserJid();
            if (currentUserJid == null) {
                 showErrorDialog("无法获取当前用户信息，无法发送消息。");
                 return;
            }
            
            try {
                // 1. 发送消息到服务器
                context.getXmppClient().sendMessage(contactJid, text);
                logger.debug("Message sent to {}: {}", contactJid, text.length() > 20 ? text.substring(0, 20) + "..." : text);

                long timestamp = System.currentTimeMillis();
                
                // 2. 在本地界面显示消息
                appendMessage("我", currentUserJid, text, timestamp, true);
                
                // 3. 保存消息到数据库 (如果不是临时会话)
                ChatDatabase db = ChatDatabase.getInstance();
                if (db != null) {
                     // isLocal = true
                     db.saveMessage(currentUserJid, contactJid, text, true);
                } else {
                     logger.warn("Database instance is null, sent message not saved.");
                }

                // 4. 清空输入框
                inputArea.setText("");
                inputArea.requestFocusInWindow(); // 保持焦点在输入框

            } catch (Exception e) {
                logger.error("Error sending message to {}", contactJid, e);
                showErrorDialog("发送消息失败: " + e.getMessage());
            }
        }
    }
} 