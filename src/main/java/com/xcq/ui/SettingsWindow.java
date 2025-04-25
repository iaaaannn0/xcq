package com.xcq.ui;

import com.xcq.core.ApplicationContext;
import com.xcq.db.ChatDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class SettingsWindow extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(SettingsWindow.class);
    private final ApplicationContext context;

    public SettingsWindow(ApplicationContext context) {
        this.context = context;
        setTitle("设置");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        createUI();
    }

    private void createUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        // 添加删除所有聊天记录按钮
        JButton deleteAllHistoryButton = new JButton("删除所有聊天记录");
        deleteAllHistoryButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(this,
                "确定要删除所有聊天记录吗？此操作不可恢复！",
                "确认删除",
                JOptionPane.YES_NO_OPTION);
            
            if (result == JOptionPane.YES_OPTION) {
                ChatDatabase.getInstance().deleteAllChatHistory();
                JOptionPane.showMessageDialog(this,
                    "所有聊天记录已删除",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        buttonPanel.add(deleteAllHistoryButton);

        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        setContentPane(mainPanel);
    }
} 