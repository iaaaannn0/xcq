package com.xcq.theme;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ThemeManager {
    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);
    private final File themeDir;
    private final Map<String, Theme> themes;
    private Theme currentTheme;

    public ThemeManager() {
        this.themeDir = new File("themes");
        this.themes = new HashMap<>();
        this.currentTheme = new Theme("default");
        
        if (!themeDir.exists()) {
            themeDir.mkdirs();
        }
        
        // 添加默认主题
        themes.put("default", currentTheme);
    }

    public void loadThemes() {
        // 加载外部主题
        File[] themeFiles = themeDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (themeFiles != null) {
            for (File themeFile : themeFiles) {
                try {
                    Theme theme = loadTheme(themeFile);
                    if (theme != null) {
                        themes.put(theme.getName(), theme);
                    }
                } catch (Exception e) {
                    logger.error("Failed to load theme from file: " + themeFile.getName(), e);
                }
            }
        }
    }

    private Theme loadTheme(File themeFile) {
        try {
            // TODO: 实现从JSON文件加载主题的逻辑
            return null;
        } catch (Exception e) {
            logger.error("Failed to load theme from file: " + themeFile.getName(), e);
            return null;
        }
    }

    public void setTheme(String themeName) {
        Theme theme = themes.get(themeName);
        if (theme != null) {
            this.currentTheme = theme;
            logger.info("Theme changed to: {}", themeName);
        } else {
            logger.warn("Theme not found: {}, using default theme", themeName);
            this.currentTheme = themes.get("default");
        }
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public Map<String, Theme> getThemes() {
        return themes;
    }

    public void setCurrentTheme(Theme theme) {
        if (theme != null) {
            this.currentTheme = theme;
            logger.info("Theme updated");
        }
    }
} 