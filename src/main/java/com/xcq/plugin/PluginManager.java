package com.xcq.plugin;

import com.xcq.core.ApplicationContext;

import java.io.File;
import java.util.List;

public class PluginManager {
    private final ApplicationContext context;
    private final File pluginDir;
    private final List<Plugin> loadedPlugins;

    public PluginManager(ApplicationContext context) {
        this.context = context;
        this.pluginDir = new File("plugins");
        this.loadedPlugins = new java.util.ArrayList<>();
        
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
        }
    }

    public void loadPlugins() {
        File[] pluginFiles = pluginDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (pluginFiles != null) {
            for (File pluginFile : pluginFiles) {
                try {
                    Plugin plugin = loadPlugin(pluginFile);
                    if (plugin != null) {
                        loadedPlugins.add(plugin);
                        plugin.onEnable();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void unloadPlugins() {
        for (Plugin plugin : loadedPlugins) {
            try {
                plugin.onDisable();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        loadedPlugins.clear();
    }

    private Plugin loadPlugin(File pluginFile) {
        // TODO: 实现插件加载逻辑
        return null;
    }

    public List<Plugin> getLoadedPlugins() {
        return java.util.Collections.unmodifiableList(loadedPlugins);
    }
} 