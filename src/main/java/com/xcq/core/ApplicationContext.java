package com.xcq.core;

import com.xcq.plugin.PluginManager;
import com.xcq.theme.ThemeManager;
import com.xcq.xmpp.XMPPClient;
import javax.swing.JFrame;

import java.util.HashMap;
import java.util.Map;

import com.xcq.ui.ContactWindow;

public class ApplicationContext extends Context {
    private final Map<String, Object> context = new HashMap<>();
    private final PluginManager pluginManager;
    private Application application;
    private XMPPClient xmppClient;
    private ContactWindow contactWindow;

    public ApplicationContext() {
        super();
        this.pluginManager = new PluginManager(this);
        this.xmppClient = new XMPPClient(this);
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public Application getApplication() {
        return application;
    }

    public void put(String key, Object value) {
        context.put(key, value);
    }

    public Object get(String key) {
        return context.get(key);
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public ThemeManager getThemeManager() {
        return themeManager;
    }

    public XMPPClient getXmppClient() {
        return xmppClient;
    }

    public void setXmppClient(XMPPClient xmppClient) {
        this.xmppClient = xmppClient;
    }

    public ContactWindow getContactWindow() {
        return contactWindow;
    }

    public void setContactWindow(ContactWindow contactWindow) {
        this.contactWindow = contactWindow;
    }

    @Override
    public Configuration getConfig() {
        return config;
    }

    // 兼容性方法
    public Configuration getConfiguration() {
        return getConfig();
    }
} 