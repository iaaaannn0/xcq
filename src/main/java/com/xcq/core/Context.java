package com.xcq.core;

import com.xcq.xmpp.XMPPClient;
import com.xcq.theme.ThemeManager;
import javax.swing.JFrame;

public class Context {
    protected Configuration config;
    protected XMPPClient xmppClient;
    protected JFrame mainWindow;
    protected ThemeManager themeManager;

    public Context() {
        this.config = Configuration.getInstance();
        this.xmppClient = new XMPPClient(this);
        this.themeManager = new ThemeManager();
    }

    public Configuration getConfig() {
        return config;
    }

    public XMPPClient getXmppClient() {
        return xmppClient;
    }

    public void setMainWindow(JFrame mainWindow) {
        this.mainWindow = mainWindow;
    }

    public JFrame getMainWindow() {
        return mainWindow;
    }

    public ThemeManager getThemeManager() {
        return themeManager;
    }
} 