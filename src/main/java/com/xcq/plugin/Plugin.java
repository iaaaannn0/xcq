package com.xcq.plugin;

import com.xcq.core.ApplicationContext;

public interface Plugin {
    String getName();
    String getVersion();
    String getAuthor();
    String getDescription();
    
    void onEnable();
    void onDisable();
    
    ApplicationContext getContext();
} 