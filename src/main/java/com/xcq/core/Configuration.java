package com.xcq.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);
    private static final String CONFIG_FILE = "config.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static Configuration instance;
    private Map<String, Object> settings;
    private Set<String> temporaryContacts;
    private boolean autoOpenChat = false;  // 默认不自动打开聊天窗口

    private Configuration() {
        settings = new HashMap<>();
        temporaryContacts = new HashSet<>();
        loadSettings();
    }

    public static synchronized Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    private void loadSettings() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> loaded = gson.fromJson(reader, type);
                if (loaded != null) {
                    settings = loaded;
                    // 加载临时联系人列表
                    Object tempContacts = settings.get("temporary_contacts");
                    if (tempContacts instanceof java.util.Collection) {
                        temporaryContacts = new HashSet<>((java.util.Collection<String>) tempContacts);
                    }
                }
            } catch (IOException e) {
                logger.error("Error loading settings", e);
                settings = new HashMap<>();
                temporaryContacts = new HashSet<>();
            }
        }
    }

    public void saveSettings() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            // 保存临时联系人列表
            settings.put("temporary_contacts", temporaryContacts);
            gson.toJson(settings, writer);
        } catch (IOException e) {
            logger.error("Error saving settings", e);
        }
    }

    public <T> T get(String key, T defaultValue) {
        Object value = settings.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            if (defaultValue instanceof Integer) {
                if (value instanceof Number) {
                    return (T) Integer.valueOf(((Number) value).intValue());
                }
                return (T) Integer.valueOf(value.toString());
            } else if (defaultValue instanceof Boolean) {
                return (T) Boolean.valueOf(value.toString());
            } else if (defaultValue instanceof Double) {
                if (value instanceof Number) {
                    return (T) Double.valueOf(((Number) value).doubleValue());
                }
                return (T) Double.valueOf(value.toString());
            } else if (defaultValue instanceof String) {
                return (T) value.toString();
            } else if (defaultValue instanceof Set) {
                if (value instanceof java.util.Collection) {
                    return (T) new HashSet<>((java.util.Collection<?>) value);
                }
            }
        } catch (Exception e) {
            logger.error("Error converting value for key: " + key, e);
        }
        
        return defaultValue;
    }

    public void set(String key, Object value) {
        settings.put(key, value);
        saveSettings();
    }

    public void remove(String key) {
        settings.remove(key);
        saveSettings();
    }

    public void clear() {
        settings.clear();
        temporaryContacts.clear();
        saveSettings();
    }

    // 临时消息设置相关方法
    public void addTemporaryContact(String jid) {
        temporaryContacts.add(jid);
        saveSettings();
    }

    public void removeTemporaryContact(String jid) {
        temporaryContacts.remove(jid);
        saveSettings();
    }

    public boolean isTemporaryContact(String jid) {
        return temporaryContacts.contains(jid);
    }

    public Set<String> getTemporaryContacts() {
        return new HashSet<>(temporaryContacts);
    }

    public boolean isAutoOpenChat() {
        return autoOpenChat;
    }

    public void setAutoOpenChat(boolean autoOpenChat) {
        this.autoOpenChat = autoOpenChat;
    }
} 