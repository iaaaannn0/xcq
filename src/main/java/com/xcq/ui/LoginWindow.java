package com.xcq.ui;

import com.xcq.core.ApplicationContext;
import com.xcq.core.Configuration;
import com.xcq.theme.Theme;
import com.xcq.xmpp.XMPPClient;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.stringprep.XmppStringprepException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginWindow extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(LoginWindow.class);
    private final ApplicationContext context;
    private final Configuration config;
    
    private final JTextField serverField;
    private final JTextField portField;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JCheckBox rememberPasswordCheck;
    private final JCheckBox tlsCheck;
    private final JButton loginButton;
    private final JLabel statusLabel;
    private final JButton advancedButton;
    private final JPanel advancedPanel;
    private final JPanel mainPanel;
    
    private final ExecutorService executorService;
    private ContactWindow contactWindow;

    public LoginWindow(ApplicationContext context) {
        this.context = context;
        this.config = context.getConfiguration();
        this.executorService = Executors.newCachedThreadPool();

        // 设置窗口属性
        setTitle("XCQ XMPP 客户端 - 登录");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // 创建主面板
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // 添加标题
        JLabel titleLabel = new JLabel("XCQ");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(20));

        // 创建表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 服务器地址
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("服务器地址:"), gbc);
        gbc.gridx = 1;
        serverField = new JTextField(20);
        serverField.setText(config.get("xmpp.server", ""));
        formPanel.add(serverField, gbc);

        // 端口
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("端口:"), gbc);
        gbc.gridx = 1;
        portField = new JTextField(20);
        portField.setText(String.valueOf(config.get("xmpp.port", 5222)));
        formPanel.add(portField, gbc);

        // 用户名
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("用户名:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(20);
        usernameField.setText(config.get("xmpp.username", ""));
        formPanel.add(usernameField, gbc);

        // 密码
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("密码:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        if (config.get("xmpp.rememberPassword", false)) {
            passwordField.setText(config.get("xmpp.password", ""));
        }
        formPanel.add(passwordField, gbc);

        // 记住密码
        gbc.gridx = 1;
        gbc.gridy = 4;
        rememberPasswordCheck = new JCheckBox("记住密码");
        rememberPasswordCheck.setSelected(config.get("xmpp.rememberPassword", false));
        formPanel.add(rememberPasswordCheck, gbc);

        // 高级设置按钮
        gbc.gridx = 1;
        gbc.gridy = 5;
        advancedButton = new JButton("高级设置");
        advancedButton.addActionListener(e -> toggleAdvancedPanel());
        formPanel.add(advancedButton, gbc);

        // 高级设置面板
        advancedPanel = new JPanel(new GridBagLayout());
        advancedPanel.setVisible(false);
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        tlsCheck = new JCheckBox("启用TLS加密");
        tlsCheck.setSelected(config.get("xmpp.tls", true));
        advancedPanel.add(tlsCheck, gbc);
        formPanel.add(advancedPanel, gbc);

        // 将表单面板添加到主面板
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(formPanel);
        mainPanel.add(Box.createVerticalStrut(10));

        // 状态标签
        statusLabel = new JLabel("准备就绪");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(statusLabel);
        mainPanel.add(Box.createVerticalStrut(10));

        // 登录按钮
        loginButton = new JButton("登录");
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.addActionListener(e -> login());
        mainPanel.add(loginButton);
        mainPanel.add(Box.createVerticalStrut(10));

        // 设置主面板
        setContentPane(mainPanel);
        
        // 调整窗口大小
        setSize(400, 500);
        
        // 应用当前主题
        applyTheme();
    }

    private void toggleAdvancedPanel() {
        advancedPanel.setVisible(!advancedPanel.isVisible());
        pack();
    }

    private void login() {
        String server = serverField.getText().trim();
        String portStr = portField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        boolean rememberPassword = rememberPasswordCheck.isSelected();
        boolean useTLS = tlsCheck.isSelected();

        // 验证输入
        if (server.isEmpty() || username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("请填写所有必填字段");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            statusLabel.setText("端口必须是数字");
            return;
        }

        // 保存配置
        config.set("xmpp.server", server);
        config.set("xmpp.port", port);
        config.set("xmpp.username", username);
        config.set("xmpp.tls", useTLS);
        if (rememberPassword) {
            config.set("xmpp.password", password);
        }
        config.set("xmpp.rememberPassword", rememberPassword);

        // 禁用登录按钮
        loginButton.setEnabled(false);
        statusLabel.setText("正在连接...");

        // 在后台线程中执行登录
        executorService.submit(() -> {
            try {
                // 配置XMPP连接
                XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                        .setHost(server)
                        .setPort(port)
                        .setUsernameAndPassword(username, password)
                        .setXmppDomain(server)  // 使用服务器地址作为XMPP domain
                        .setSecurityMode(useTLS ? 
                            XMPPTCPConnectionConfiguration.SecurityMode.required : 
                            XMPPTCPConnectionConfiguration.SecurityMode.disabled)
                        .build();

                // 初始化XMPP客户端配置
                XMPPClient xmppClient = context.getXmppClient();
                xmppClient.initialize(config);
                xmppClient.connect();

                // 登录成功
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("登录成功");
                    dispose();
                    contactWindow = new ContactWindow(context);
                    contactWindow.setVisible(true);
                });
            } catch (Exception e) {
                logger.error("Login failed", e);
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("登录失败: " + e.getMessage());
                    loginButton.setEnabled(true);
                });
            }
        });
    }

    private void applyTheme() {
        Theme theme = context.getThemeManager().getCurrentTheme();
        if (theme != null) {
            mainPanel.setBackground(theme.getBackgroundColor());
            statusLabel.setForeground(theme.getTextColor());
            loginButton.setBackground(theme.getButtonColor());
            loginButton.setForeground(theme.getTextColor());
            
            Font font = theme.getFont();
            if (font != null) {
                statusLabel.setFont(font);
                loginButton.setFont(font);
            }
        }
    }

    @Override
    public void dispose() {
        executorService.shutdown();
        super.dispose();
    }

    private void loadSavedCredentials() {
        String savedUsername = context.getConfiguration().get("login.username", "");
        boolean rememberMe = context.getConfiguration().get("login.remember", false);
        
        if (!savedUsername.isEmpty() && rememberMe) {
            usernameField.setText(savedUsername);
            rememberPasswordCheck.setSelected(true);
        }
    }

    private void saveCredentials() {
        if (rememberPasswordCheck.isSelected()) {
            config.set("login.username", usernameField.getText());
            config.set("login.remember", true);
        } else {
            config.remove("login.username");
            config.set("login.remember", false);
        }
    }
} 