package com.xcq;

import com.xcq.core.ApplicationContext;
import com.xcq.ui.LoginWindow;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // 设置系统外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 在 EDT 中创建并显示窗口
        SwingUtilities.invokeLater(() -> {
            ApplicationContext context = new ApplicationContext();
            LoginWindow loginWindow = new LoginWindow(context);
            loginWindow.setVisible(true);
        });
    }
} 