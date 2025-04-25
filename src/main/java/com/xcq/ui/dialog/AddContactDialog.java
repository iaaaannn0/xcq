package com.xcq.ui.dialog;

import org.jivesoftware.smack.roster.RosterGroup;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Collection;

public class AddContactDialog extends JDialog {
    private final JTextField jidField;
    private final JComboBox<String> groupComboBox;
    private final JTextArea messageArea;
    private boolean approved = false;

    public AddContactDialog(Frame owner, Collection<RosterGroup> groups) {
        super(owner, "添加联系人", true);

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 创建表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // JID输入框
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("联系人JID:"), gbc);
        gbc.gridx = 1;
        jidField = new JTextField(20);
        formPanel.add(jidField, gbc);

        // 分组选择框
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("添加到分组:"), gbc);
        gbc.gridx = 1;
        groupComboBox = new JComboBox<>();
        groupComboBox.addItem("未分组");
        for (RosterGroup group : groups) {
            groupComboBox.addItem(group.getName());
        }
        formPanel.add(groupComboBox, gbc);

        // 请求消息
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        formPanel.add(new JLabel("请求消息:"), gbc);
        gbc.gridy = 3;
        messageArea = new JTextArea(3, 20);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        formPanel.add(scrollPane, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("添加");
        JButton cancelButton = new JButton("取消");

        okButton.addActionListener(e -> {
            if (validateInput()) {
                approved = true;
                dispose();
            }
        });

        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        setSize(400, 300);
        setLocationRelativeTo(owner);
    }

    private boolean validateInput() {
        String jid = jidField.getText().trim();
        if (jid.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "请输入联系人JID",
                "错误",
                JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            JidCreate.from(jid);
        } catch (XmppStringprepException e) {
            JOptionPane.showMessageDialog(this,
                "无效的JID格式",
                "错误",
                JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    public boolean isApproved() {
        return approved;
    }

    public String getJID() {
        return jidField.getText().trim();
    }

    public String getSelectedGroup() {
        return groupComboBox.getSelectedItem().toString();
    }

    public String getMessage() {
        return messageArea.getText().trim();
    }
} 