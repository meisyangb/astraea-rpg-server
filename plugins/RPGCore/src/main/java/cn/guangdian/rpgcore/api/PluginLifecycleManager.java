package cn.guangdian.rpgcore.api;

import org.bukkit.plugin.java.JavaPlugin;

public interface PluginLifecycleManager {
    
    void registerPlugin(JavaPlugin plugin);
    
    void unregisterPlugin(String pluginName);
    
    void enablePlugin(String pluginName);
    
    void disablePlugin(String pluginName);
    
    void disableAll();
    
    boolean isEnabled(String pluginName);
    
    int getActivePluginCount();
    
    void runStartupHooks();
    
    void runShutdownHooks();
}
