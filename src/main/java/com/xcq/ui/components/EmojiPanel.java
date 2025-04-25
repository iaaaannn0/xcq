package com.xcq.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

public class EmojiPanel extends JPanel {
    private static final String[] EMOJIS = {
        "😀", "😃", "😄", "😁", "😅", "😂", "🤣", 
        "😊", "😇", "🙂", "🙃", "😉", "😌", "😍",
        "🥰", "😘", "😗", "😙", "😚", "😋", "😛",
        "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎",
        "🤩", "🥳", "😏", "😒", "😞", "😔", "😟",
        "😕", "🙁", "☹️", "😣", "😖", "😫", "😩"
    };

    private final Consumer<String> onEmojiSelected;
    private static JWindow popupWindow;

    public EmojiPanel(Consumer<String> onEmojiSelected) {
        this.onEmojiSelected = onEmojiSelected;
        setLayout(new GridLayout(6, 7, 2, 2));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setBackground(Color.WHITE);

        for (String emoji : EMOJIS) {
            JLabel label = createEmojiLabel(emoji);
            add(label);
        }
    }

    private JLabel createEmojiLabel(String emoji) {
        JLabel label = new JLabel(emoji, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        label.setOpaque(true);
        label.setBackground(Color.WHITE);

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onEmojiSelected.accept(emoji);
                if (popupWindow != null) {
                    popupWindow.dispose();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                label.setBackground(new Color(230, 230, 230));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setBackground(Color.WHITE);
            }
        });

        return label;
    }

    public static void showEmojiPanel(Component parent, Consumer<String> onEmojiSelected) {
        if (popupWindow != null && popupWindow.isVisible()) {
            popupWindow.dispose();
            return;
        }

        EmojiPanel emojiPanel = new EmojiPanel(onEmojiSelected);
        popupWindow = new JWindow();
        popupWindow.setContentPane(emojiPanel);
        popupWindow.pack();

        Point p = parent.getLocationOnScreen();
        popupWindow.setLocation(p.x, p.y + parent.getHeight());
        popupWindow.setVisible(true);

        // 点击面板外部时关闭面板
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) event;
                if (me.getID() == MouseEvent.MOUSE_CLICKED) {
                    Component clicked = me.getComponent();
                    if (clicked == null || !SwingUtilities.isDescendingFrom(clicked, popupWindow)) {
                        popupWindow.dispose();
                    }
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }
} 