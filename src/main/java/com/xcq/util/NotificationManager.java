package com.xcq.util;

import java.awt.*;
import javax.swing.*;

public class NotificationManager {
    private static NotificationManager instance;
    
    private NotificationManager() {}
    
    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }
    
    public void showNotification(String title, String message) {
        if (SystemTray.isSupported()) {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                Image image = Toolkit.getDefaultToolkit().createImage(getClass().getResource("/icon.png"));
                TrayIcon trayIcon = new TrayIcon(image, "XCQ");
                trayIcon.setImageAutoSize(true);
                tray.add(trayIcon);
                trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
                
                // 5秒后移除图标
                new Timer(5000, e -> {
                    tray.remove(trayIcon);
                    ((Timer) e.getSource()).stop();
                }).start();
            } catch (Exception e) {
                // 如果系统托盘通知失败，使用对话框
                JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            // 如果系统不支持托盘，使用对话框
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
        }
    }
} 