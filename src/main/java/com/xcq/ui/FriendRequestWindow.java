package com.xcq.ui;

import com.xcq.core.Context;
import javax.swing.*;
import java.awt.*;

public class FriendRequestWindow extends JDialog {
    private final Context context;
    private final String jid;
    private final String nickname;

    public FriendRequestWindow(JFrame parent, Context context, String jid, String nickname) {
        super(parent, "好友验证", true);
        this.context = context;
        this.jid = jid;
        this.nickname = nickname;
        
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(400, 180);

        // 消息面板
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        String message = String.format("<html>用户 %s (%s) 请求添加您为好友</html>", nickname, jid);
        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        
        messagePanel.add(messageLabel, BorderLayout.CENTER);
        add(messagePanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JButton acceptButton = new JButton("接受");
        JButton rejectButton = new JButton("拒绝");

        Dimension buttonSize = new Dimension(80, 30);
        acceptButton.setPreferredSize(buttonSize);
        rejectButton.setPreferredSize(buttonSize);

        acceptButton.addActionListener(e -> {
            context.getXmppClient().acceptSubscription(jid);
            dispose();
        });

        rejectButton.addActionListener(e -> {
            context.getXmppClient().rejectSubscription(jid);
            dispose();
        });

        buttonPanel.add(acceptButton);
        buttonPanel.add(rejectButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
} 