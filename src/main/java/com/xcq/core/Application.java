package com.xcq.core;

import com.xcq.plugin.PluginManager;
import com.xcq.theme.ThemeManager;
import com.xcq.xmpp.XMPPClient;
import com.xcq.ui.MainWindow;
import org.jxmpp.stringprep.XmppStringprepException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import java.awt.Point;
import java.awt.Dimension;

public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    private final ApplicationContext context;
    private final PluginManager pluginManager;
    private final ThemeManager themeManager;
    private final XMPPClient xmppClient;
    private MainWindow mainWindow;

    public Application(ApplicationContext context) {
        this.context = context;
        this.pluginManager = context.getPluginManager();
        this.themeManager = context.getThemeManager();
        this.xmppClient = context.getXmppClient();
        context.setApplication(this);
    }

    public void start() {
        try {
            logger.info("Starting application...");
            
            // 加载主题
            logger.info("Loading themes...");
            themeManager.loadThemes();
            
            // 加载插件
            logger.info("Loading plugins...");
            pluginManager.loadPlugins();
            
            logger.info("Application started successfully");
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            throw new RuntimeException("Failed to start application", e);
        }
    }

    public void stop() {
        try {
            logger.info("Stopping application...");
            
            // 保存窗口状态
            logger.info("Saving window state...");
            saveWindowState();
            
            // 保存配置
            logger.info("Saving configuration...");
            context.getConfiguration().saveSettings();
            
            // 卸载插件
            logger.info("Unloading plugins...");
            pluginManager.unloadPlugins();
            
            // 断开XMPP连接
            logger.info("Disconnecting from XMPP server...");
            xmppClient.disconnect();
            
            logger.info("Application stopped successfully");
        } catch (Exception e) {
            logger.error("Error while stopping application", e);
        }
    }

    public ApplicationContext getContext() {
        return context;
    }

    public void setMainWindow(MainWindow window) {
        this.mainWindow = window;
    }

    public void saveWindowState() {
        if (mainWindow != null) {
            Point location = mainWindow.getLocation();
            Dimension size = mainWindow.getSize();
            boolean isMaximized = (mainWindow.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
            
            context.getConfiguration().set("window.x", location.x);
            context.getConfiguration().set("window.y", location.y);
            context.getConfiguration().set("window.width", size.width);
            context.getConfiguration().set("window.height", size.height);
            context.getConfiguration().set("window.maximized", isMaximized);
        }
    }
} 