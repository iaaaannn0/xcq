package com.xcq.ui.dialog;

import com.xcq.core.ApplicationContext;
import com.xcq.core.Configuration;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;

public class SettingsDialog extends JDialog {
    private final ApplicationContext context;
    private final JComboBox<String> fontFamilyComboBox;
    private final JComboBox<Integer> fontSizeComboBox;
    private final JCheckBox boldTextCheckBox;
    private boolean approved = false;

    public SettingsDialog(JFrame parent, ApplicationContext context) {
        super(parent, "设置", true);
        this.context = context;

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 创建设置面板
        JPanel settingsPanel = new JPanel(new GridLayout(0, 2, 5, 5));

        // 字体选择
        settingsPanel.add(new JLabel("字体:"));
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();
        fontFamilyComboBox = new JComboBox<>(fontNames);
        fontFamilyComboBox.setSelectedItem(context.getConfiguration().get("font.family", "微软雅黑"));
        settingsPanel.add(fontFamilyComboBox);

        // 字体大小
        settingsPanel.add(new JLabel("字体大小:"));
        Integer[] sizes = {12, 13, 14, 15, 16, 17, 18, 19, 20};
        fontSizeComboBox = new JComboBox<>(sizes);
        fontSizeComboBox.setSelectedItem(context.getConfiguration().get("font.size", 14));
        settingsPanel.add(fontSizeComboBox);

        // 粗体选项
        settingsPanel.add(new JLabel("粗体显示:"));
        boldTextCheckBox = new JCheckBox();
        boldTextCheckBox.setSelected(context.getConfiguration().get("font.bold", false));
        settingsPanel.add(boldTextCheckBox);

        mainPanel.add(settingsPanel, BorderLayout.CENTER);

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton okButton = new JButton("确定");
        okButton.addActionListener(e -> {
            saveSettings();
            approved = true;
            dispose();
        });
        
        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 设置对话框属性
        setContentPane(mainPanel);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void saveSettings() {
        Configuration config = context.getConfiguration();
        
        // 保存字体设置
        config.set("font.family", (String) fontFamilyComboBox.getSelectedItem());
        config.set("font.size", (Integer) fontSizeComboBox.getSelectedItem());
        config.set("font.bold", boldTextCheckBox.isSelected());
        
        // 应用字体设置
        applyFontSettings();
    }

    private void applyFontSettings() {
        Configuration config = context.getConfiguration();
        String fontFamily = config.get("font.family", "微软雅黑");
        int fontSize = config.get("font.size", 14);
        boolean isBold = config.get("font.bold", false);
        
        Font font = new Font(fontFamily, isBold ? Font.BOLD : Font.PLAIN, fontSize);
        
        // 更新 UI 管理器的默认字体
        UIManager.put("Button.font", font);
        UIManager.put("Label.font", font);
        UIManager.put("TextField.font", font);
        UIManager.put("TextArea.font", font);
        UIManager.put("ComboBox.font", font);
        UIManager.put("Menu.font", font);
        UIManager.put("MenuItem.font", font);
        UIManager.put("Tree.font", font);
        
        // 刷新所有窗口的UI
        SwingUtilities.invokeLater(() -> {
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
            }
        });
    }

    public boolean isApproved() {
        return approved;
    }
} 