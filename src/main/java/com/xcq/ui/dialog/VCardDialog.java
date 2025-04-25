package com.xcq.ui.dialog;

import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class VCardDialog extends JDialog {
    private final RosterEntry entry;
    private final VCard vCard;
    
    private JLabel avatarLabel;
    private JTextField nicknameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JTextField organizationField;
    private JTextArea descriptionArea;

    public VCardDialog(Frame owner, RosterEntry entry, VCard vCard) {
        super(owner, "联系人资料", true);
        this.entry = entry;
        this.vCard = vCard;
        
        initComponents();
        loadVCardData();
        
        setSize(400, 500);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 头像面板
        JPanel avatarPanel = new JPanel(new BorderLayout());
        avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(100, 100));
        avatarLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarPanel.add(avatarLabel, BorderLayout.CENTER);
        mainPanel.add(avatarPanel, BorderLayout.NORTH);

        // 信息面板
        JPanel infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 昵称
        gbc.gridx = 0;
        gbc.gridy = 0;
        infoPanel.add(new JLabel("昵称:"), gbc);
        gbc.gridx = 1;
        nicknameField = new JTextField();
        nicknameField.setEditable(false);
        infoPanel.add(nicknameField, gbc);

        // 邮箱
        gbc.gridx = 0;
        gbc.gridy = 1;
        infoPanel.add(new JLabel("邮箱:"), gbc);
        gbc.gridx = 1;
        emailField = new JTextField();
        emailField.setEditable(false);
        infoPanel.add(emailField, gbc);

        // 电话
        gbc.gridx = 0;
        gbc.gridy = 2;
        infoPanel.add(new JLabel("电话:"), gbc);
        gbc.gridx = 1;
        phoneField = new JTextField();
        phoneField.setEditable(false);
        infoPanel.add(phoneField, gbc);

        // 组织
        gbc.gridx = 0;
        gbc.gridy = 3;
        infoPanel.add(new JLabel("组织:"), gbc);
        gbc.gridx = 1;
        organizationField = new JTextField();
        organizationField.setEditable(false);
        infoPanel.add(organizationField, gbc);

        // 个人简介
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        infoPanel.add(new JLabel("个人简介:"), gbc);
        gbc.gridy = 5;
        descriptionArea = new JTextArea(5, 20);
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(descriptionArea);
        infoPanel.add(scrollPane, gbc);

        mainPanel.add(infoPanel, BorderLayout.CENTER);

        // 关闭按钮
        JButton closeButton = new JButton("关闭");
        closeButton.addActionListener(e -> dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void loadVCardData() {
        if (vCard != null) {
            // 设置头像
            byte[] avatarData = vCard.getAvatar();
            if (avatarData != null) {
                ImageIcon avatar = new ImageIcon(avatarData);
                Image scaledImage = avatar.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                avatarLabel.setIcon(new ImageIcon(scaledImage));
            } else {
                avatarLabel.setIcon(null);
                avatarLabel.setText("无头像");
            }

            // 设置其他信息
            nicknameField.setText(vCard.getNickName());
            emailField.setText(vCard.getEmailHome());
            phoneField.setText(vCard.getPhoneHome("VOICE"));
            organizationField.setText(vCard.getOrganization());
            descriptionArea.setText(vCard.getField("DESC"));
        } else {
            // 如果没有vCard数据，显示基本信息
            avatarLabel.setIcon(null);
            avatarLabel.setText("无头像");
            nicknameField.setText(entry.getName());
            emailField.setText("");
            phoneField.setText("");
            organizationField.setText("");
            descriptionArea.setText("");
        }
    }
} 