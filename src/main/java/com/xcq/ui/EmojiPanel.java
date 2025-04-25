package com.xcq.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class EmojiPanel extends JPanel {
    private static final String[] EMOJIS = {
        "😀", "😃", "😄", "😁", "😅", "😂", "🤣", "😊",
        "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘",
        "😗", "😙", "😚", "😋", "😛", "😝", "😜", "🤪",
        "🤨", "🧐", "🤓", "😎", "🤩", "🥳", "😏", "😒",
        "😞", "😔", "😟", "😕", "🙁", "☹️", "😣", "😖",
        "😫", "😩", "🥺", "😢"
    };

    private final Consumer<String> onEmojiSelected;

    private EmojiPanel(Consumer<String> onEmojiSelected) {
        this.onEmojiSelected = onEmojiSelected;
        setLayout(new GridLayout(6, 7, 2, 2));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        for (String emoji : EMOJIS) {
            JLabel label = createEmojiLabel(emoji);
            add(label);
        }
    }

    private JLabel createEmojiLabel(String emoji) {
        JLabel label = new JLabel(emoji, SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(20f));
        label.setOpaque(true);
        label.setBackground(Color.WHITE);
        
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                label.setBackground(new Color(230, 230, 230));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setBackground(Color.WHITE);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                onEmojiSelected.accept(emoji);
                SwingUtilities.getWindowAncestor(EmojiPanel.this).dispose();
            }
        });

        return label;
    }

    public static void showEmojiPanel(JComponent source, Consumer<String> onEmojiSelected) {
        EmojiPanel panel = new EmojiPanel(onEmojiSelected);
        JPopupMenu popup = new JPopupMenu();
        popup.add(panel);
        popup.show(source, 0, source.getHeight());
    }
} 