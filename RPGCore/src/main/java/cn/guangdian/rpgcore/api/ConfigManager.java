package cn.guangdian.rpgcore.api;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public interface ConfigManager {
    
    <T> T get(String pluginName, String key, T defaultValue);
    
    <T> T get(String pluginName, String key, Class<T> type);
    
    void set(String pluginName, String key, Object value);
    
    void reload(String pluginName);
    
    void reloadAll();
    
    void save(String pluginName);
    
    void saveAll();
    
    Optional<FileConfiguration> getConfig(String pluginName);
    
    void registerConfig(String pluginName, JavaPlugin plugin);
    
    void unregisterConfig(String pluginName);
    
    boolean hasConfig(String pluginName);
}
