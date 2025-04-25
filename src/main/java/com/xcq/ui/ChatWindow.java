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
import java.time.LocalDateTime;
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
    private Timer flashTimer;
    private boolean isFlashing = false;
    private Color originalColor;
    private Clip notificationSound;
    private boolean isListenerRegistered = false;
    private long lastMessageTimestamp = 0;
    private boolean historyLoaded = false;  // 添加历史加载标记
    private XMPPClient.MessageListener listener;  // 将监听器提升为成员变量

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
        originalColor = getBackground();

        // 设置窗口图标
        ImageIcon logoIcon = new ImageIcon(getClass().getResource("/logo.png"));
        setIconImage(logoIcon.getImage());

        // 创建闪烁定时器
        flashTimer = new Timer(500, e -> {
            if (isFlashing) {
                if (getBackground().equals(originalColor)) {
                    setBackground(Color.YELLOW);
                } else {
                    setBackground(originalColor);
                }
            }
        });

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
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 5));

        // 创建工具栏
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorderPainted(false);

        // 添加工具栏按钮
        addToolbarButtons(toolBar);
        bottomPanel.add(toolBar, BorderLayout.NORTH);

        // 创建输入区域
        inputArea = new JTextArea();
        inputArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        
        // 添加快捷键
        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isControlDown() || e.isShiftDown()) {
                        inputArea.append("\n");
                    } else {
                        e.consume();
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
        JButton sendButton = new JButton("发送");
        sendButton.addActionListener(e -> sendMessage());
        buttonPanel.add(sendButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        setContentPane(mainPanel);

        // 加载历史消息
        loadChatHistory();

        // 添加窗口监听器
        addWindowFocusListener(this);
        addWindowListener(this);

        // 创建菜单栏
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("操作");
        
        // 添加删除聊天记录菜单项
        JMenuItem deleteHistoryItem = new JMenuItem("删除聊天记录");
        deleteHistoryItem.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(this,
                "确定要删除与 " + contactName + " 的聊天记录吗？",
                "确认删除",
                JOptionPane.YES_NO_OPTION);
            
            if (result == JOptionPane.YES_OPTION) {
                String currentUserJid = context.getXmppClient().getConnection().getUser().toString();
                ChatDatabase.getInstance().deleteChatHistory(currentUserJid, contactJid);
                chatArea.setText("");
                JOptionPane.showMessageDialog(this,
                    "聊天记录已删除",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        menu.add(deleteHistoryItem);

        // 添加临时消息设置菜单项
        JCheckBoxMenuItem temporaryMessageItem = new JCheckBoxMenuItem("临时消息（不保存聊天记录）");
        temporaryMessageItem.setSelected(Configuration.getInstance().isTemporaryContact(contactJid));
        temporaryMessageItem.addActionListener(e -> {
            if (temporaryMessageItem.isSelected()) {
                Configuration.getInstance().addTemporaryContact(contactJid);
            } else {
                Configuration.getInstance().removeTemporaryContact(contactJid);
            }
        });
        menu.add(temporaryMessageItem);
        
        menuBar.add(menu);
        setJMenuBar(menuBar);
    }

    private void addToolbarButtons(JToolBar toolBar) {
        // 表情按钮
        JButton emojiButton = new JButton("表情");
        emojiButton.setFocusable(false);
        emojiButton.addActionListener(e -> showEmojiPanel(emojiButton));
        
        // 图片按钮
        JButton imageButton = new JButton("图片");
        imageButton.setFocusable(false);
        imageButton.addActionListener(e -> selectAndUploadImage());
        
        // 文件按钮
        JButton fileButton = new JButton("文件");
        fileButton.setFocusable(false);
        fileButton.addActionListener(e -> selectAndUploadFile());

        toolBar.add(emojiButton);
        toolBar.add(imageButton);
        toolBar.add(fileButton);
    }

    private void showEmojiPanel(JButton sourceButton) {
        EmojiPanel.showEmojiPanel(sourceButton, emoji -> {
            inputArea.insert(emoji, inputArea.getCaretPosition());
            inputArea.requestFocus();
        });
    }

    private void selectAndUploadImage() {
        JFileChooser fileChooser = new JFileChooser();
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
            uploadAndSendFile(file, "图片上传中...");
        }
    }

    private void selectAndUploadFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            uploadAndSendFile(file, "文件上传中...");
        }
    }

    private void uploadAndSendFile(File file, String uploadingMessage) {
        // 显示上传中消息
        appendMessage("系统", "", uploadingMessage, true);
        
        // 上传文件
        FileUploader.uploadFile(file, this).thenAccept(url -> {
            if (url != null) {
                // 发送文件URL
                try {
                    context.getXmppClient().sendMessage(contactJid, url);
                    appendMessage("我", "", url, true);
                } catch (Exception e) {
                    logger.error("Error sending file URL", e);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                            "发送文件失败: " + e.getMessage(),
                            "错误",
                            JOptionPane.ERROR_MESSAGE);
                    });
                }
            }
        });
    }

    private void addStylesToDocument(StyledDocument doc) {
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

        Style regular = doc.addStyle("regular", def);
        StyleConstants.setFontFamily(regular, "微软雅黑");
        StyleConstants.setFontSize(regular, 12);

        Style time = doc.addStyle("time", regular);
        StyleConstants.setForeground(time, Color.GRAY);
        StyleConstants.setFontSize(time, 10);

        Style name = doc.addStyle("name", regular);
        StyleConstants.setBold(name, true);

        Style message = doc.addStyle("message", regular);
        
        Style receivedMessage = doc.addStyle("receivedMessage", regular);
        StyleConstants.setForeground(receivedMessage, Color.BLUE);
    }

    public void appendMessage(String fromJid, String toJid, String content, boolean isSent) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 获取当前时间戳
                long currentTime = System.currentTimeMillis();
                
                // 如果消息时间间隔小于100ms，可能是重复消息，跳过
                if (currentTime - lastMessageTimestamp < 100) {
                    return;
                }
                lastMessageTimestamp = currentTime;

                // 如果是收到的消息且窗口不在前台，触发通知和闪烁效果
                if (!isSent && !isFocused()) {
                    startNotification();
                    // 获取 ContactWindow 并触发联系人闪烁
                    ContactWindow contactWindow = context.getContactWindow();
                    if (contactWindow != null) {
                        contactWindow.startContactBlinking(fromJid);
                    }
                }

                // 格式化时间
                String time = LocalDateTime.now().format(timeFormatter);
                String sender = isSent ? "我" : (fromJid != null ? fromJid : "未知用户");
                
                // 添加时间戳
                doc.insertString(doc.getLength(), "[" + time + "] ", doc.getStyle("regular"));
                
                // 添加发送者
                doc.insertString(doc.getLength(), sender + ": ", doc.getStyle("bold"));
                
                // 处理消息内容
                if (isImageUrl(content)) {
                    // 如果是图片URL，显示图片
                    ImageMessageComponent imageComponent = new ImageMessageComponent(content);
                    chatArea.setCaretPosition(doc.getLength());
                    chatArea.insertComponent(imageComponent);
                    doc.insertString(doc.getLength(), "\n", doc.getStyle("regular"));
                } else {
                    // 普通文本消息
                    doc.insertString(doc.getLength(), content + "\n", doc.getStyle("regular"));
                }
                
                // 保存消息到数据库（如果不是临时联系人）
                if (!Configuration.getInstance().isTemporaryContact(contactJid)) {
                    String currentUserJid = context.getXmppClient().getConnection().getUser().toString();
                    ChatDatabase.getInstance().saveMessage(
                        isSent ? currentUserJid : fromJid,  // 发送者
                        isSent ? contactJid : currentUserJid,  // 接收者
                        content,  // 消息内容
                        isSent  // 是否是本地发送
                    );
                }
                
                // 滚动到底部
                chatArea.setCaretPosition(doc.getLength());
                
            } catch (BadLocationException e) {
                logger.error("Error appending message", e);
            }
        });
    }

    private boolean isImageUrl(String text) {
        // 检查是否是图片URL
        String lowerCase = text.toLowerCase();
        return lowerCase.startsWith("http") && (
            lowerCase.endsWith(".jpg") || 
            lowerCase.endsWith(".jpeg") || 
            lowerCase.endsWith(".png") || 
            lowerCase.endsWith(".gif") ||
            (lowerCase.contains("/upload/") && lowerCase.contains(".jpg"))  // 特殊处理 jabber.ru 的图片URL
        );
    }

    private void sendMessage() {
        String text = inputArea.getText().trim();
        if (!text.isEmpty()) {
            try {
                String currentUserJid = context.getXmppClient().getConnection().getUser().toString();
                context.getXmppClient().sendMessage(contactJid, text);
                
                // 如果不是临时消息，则保存到数据库
                if (!Configuration.getInstance().isTemporaryContact(contactJid)) {
                    ChatDatabase.getInstance().saveMessage(currentUserJid, contactJid, text, true);
                }
                
                appendMessage("我", "", text, true);
                inputArea.setText("");
            } catch (Exception e) {
                logger.error("Error sending message", e);
                JOptionPane.showMessageDialog(this,
                    "发送消息失败: " + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadChatHistory() {
        // 如果历史消息已经加载过，直接返回
        if (historyLoaded) {
            return;
        }

        try {
            String currentUserJid = context.getXmppClient().getConnection().getUser().toString();
            List<ChatMessage> messages = ChatDatabase.getInstance().getChatHistory(currentUserJid, contactJid);
            logger.info("Retrieved {} messages between {} and {}", messages.size(), currentUserJid, contactJid);

            for (ChatMessage message : messages) {
                appendMessage(
                    message.getSenderJid(),
                    message.isLocal() ? contactJid : currentUserJid,
                    message.getMessage(),
                    message.isLocal()
                );
            }
            logger.info("成功加载了 {} 条历史消息", messages.size());
            historyLoaded = true;

            // 只在第一次加载时注册消息监听器
            if (!isListenerRegistered) {
                listener = (from, message) -> {
                    if (from.equals(contactJid)) {
                        SwingUtilities.invokeLater(() -> {
                            appendMessage(from, "", message, false);
                            startNotification();
                        });
                    }
                };
                context.getXmppClient().addMessageListener(listener);
                isListenerRegistered = true;
            }
        } catch (Exception e) {
            logger.error("Error loading chat history", e);
        }
    }

    public String getContactJid() {
        return contactJid;
    }

    private void initNotificationSound() {
        try {
            // 使用 ClassLoader 加载资源文件
            var soundURL = getClass().getClassLoader().getResource("message.wav");
            if (soundURL != null) {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundURL);
                notificationSound = AudioSystem.getClip();
                notificationSound.open(audioIn);
                logger.info("提示音文件加载成功");
            } else {
                logger.warn("提示音文件 message.wav 未找到");
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            logger.error("初始化提示音失败", e);
        }
    }

    public void startNotification() {
        if (!isActive()) {
            // 播放提示音
            playNotificationSound();
            
            // 开始窗口闪烁
            startWindowFlashing();
            
            // 设置任务栏图标提醒
            requestWindowAttention();
        }
    }

    public void stopNotification() {
        // 停止窗口闪烁
        stopWindowFlashing();
        
        // 清除任务栏图标提醒
        clearWindowAttention();
        
        // 停止联系人闪烁
        if (context != null && context.getContactWindow() != null) {
            context.getContactWindow().stopContactBlinking(contactJid);
        }
    }

    private void playNotificationSound() {
        logger.debug("开始播放提示音");
        
        if (notificationSound != null && !notificationSound.isRunning()) {
            try {
                notificationSound.setFramePosition(0);
                notificationSound.start();
                logger.debug("提示音播放已启动");
            } catch (Exception e) {
                logger.error("播放提示音失败", e);
            }
        } else {
            logger.warn("提示音未初始化或正在播放");
        }
    }

    private void startWindowFlashing() {
        logger.debug("开始窗口闪烁");
        
        if (!isFlashing) {
            isFlashing = true;
            flashTimer.start();
            logger.debug("窗口闪烁已启动");
        }
    }

    private void stopWindowFlashing() {
        logger.debug("停止窗口闪烁");
        
        if (isFlashing) {
            isFlashing = false;
            flashTimer.stop();
            setBackground(originalColor);
            logger.debug("窗口闪烁已停止");
        }
    }

    private void requestWindowAttention() {
        logger.debug("请求窗口关注");
        
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_BADGE_NUMBER)) {
                    taskbar.setIconBadge("!");
                    logger.debug("任务栏提醒已设置");
                }
            }
        } catch (Exception e) {
            logger.error("设置任务栏提醒失败", e);
        }
    }

    private void clearWindowAttention() {
        logger.debug("清除任务栏提醒");
        
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_BADGE_NUMBER)) {
                    taskbar.setIconBadge(null);
                    logger.debug("任务栏提醒已清除");
                }
            }
        } catch (Exception e) {
            logger.error("清除任务栏提醒失败", e);
        }
    }

    // WindowFocusListener 方法
    @Override
    public void windowGainedFocus(WindowEvent e) {
        stopNotification();
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        // 不需要实现
    }

    // WindowListener 方法
    @Override
    public void windowOpened(WindowEvent e) {
        // 不需要实现
    }

    @Override
    public void windowClosed(WindowEvent e) {
        // 移除消息监听器
        if (isListenerRegistered) {
            context.getXmppClient().removeMessageListener(listener);
            isListenerRegistered = false;
        }
        // 重置历史加载标记
        historyLoaded = false;
        // 释放资源
        if (notificationSound != null) {
            notificationSound.close();
        }
        if (flashTimer != null) {
            flashTimer.stop();
        }
        dispose();
    }

    @Override
    public void windowClosing(WindowEvent e) {
        // 触发窗口关闭事件
        dispose();
    }

    @Override
    public void windowIconified(WindowEvent e) {
        // 不需要实现
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        stopNotification();
    }

    @Override
    public void windowActivated(WindowEvent e) {
        stopNotification();
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        // 不需要实现
    }
} 