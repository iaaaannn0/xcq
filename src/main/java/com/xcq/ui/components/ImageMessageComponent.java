package com.xcq.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageMessageComponent extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(ImageMessageComponent.class);
    private static final int THUMBNAIL_WIDTH = 150;
    private static final int THUMBNAIL_HEIGHT = 150;
    private final String imageUrl;
    private BufferedImage thumbnail;

    public ImageMessageComponent(String imageUrl) {
        this.imageUrl = imageUrl;
        setPreferredSize(new Dimension(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT));
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 在后台线程加载图片
        loadImage();

        // 添加双击事件
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openInBrowser();
                }
            }
        });
    }

    private void loadImage() {
        SwingWorker<BufferedImage, Void> worker = new SwingWorker<>() {
            @Override
            protected BufferedImage doInBackground() {
                try {
                    BufferedImage originalImage = ImageIO.read(new URL(imageUrl));
                    if (originalImage != null) {
                        return createThumbnail(originalImage);
                    }
                } catch (IOException e) {
                    logger.error("Error loading image: " + imageUrl, e);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    thumbnail = get();
                    if (thumbnail != null) {
                        repaint();
                    } else {
                        add(new JLabel("图片加载失败"), BorderLayout.CENTER);
                    }
                } catch (Exception e) {
                    logger.error("Error processing image", e);
                    add(new JLabel("图片加载失败"), BorderLayout.CENTER);
                }
            }
        };
        worker.execute();
    }

    private BufferedImage createThumbnail(BufferedImage original) {
        double scale = Math.min(
            (double) THUMBNAIL_WIDTH / original.getWidth(),
            (double) THUMBNAIL_HEIGHT / original.getHeight()
        );
        int w = (int) (original.getWidth() * scale);
        int h = (int) (original.getHeight() * scale);

        BufferedImage thumbnail = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = thumbnail.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, w, h, null);
        g2d.dispose();

        return thumbnail;
    }

    private void openInBrowser() {
        try {
            Desktop.getDesktop().browse(new URI(imageUrl));
        } catch (Exception e) {
            logger.error("Error opening URL in browser: " + imageUrl, e);
            JOptionPane.showMessageDialog(
                this,
                "无法打开浏览器：" + e.getMessage(),
                "错误",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (thumbnail != null) {
            // 居中显示缩略图
            int x = (getWidth() - thumbnail.getWidth()) / 2;
            int y = (getHeight() - thumbnail.getHeight()) / 2;
            g.drawImage(thumbnail, x, y, null);
        } else {
            // 显示加载中
            String text = "加载中...";
            FontMetrics fm = g.getFontMetrics();
            int textX = (getWidth() - fm.stringWidth(text)) / 2;
            int textY = (getHeight() + fm.getAscent()) / 2;
            g.drawString(text, textX, textY);
        }
    }
} 