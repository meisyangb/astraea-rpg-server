package cn.guangdian.rpgcore.config;

import cn.guangdian.rpgcore.api.ConfigManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ConfigManagerImpl implements ConfigManager {
    
    private final JavaPlugin hostPlugin;
    private final Logger logger;
    private final Map<String, JavaPlugin> registeredPlugins = new ConcurrentHashMap<>();
    private final Map<String, FileConfiguration> configCache = new ConcurrentHashMap<>();
    
    public ConfigManagerImpl(JavaPlugin hostPlugin) {
        this.hostPlugin = hostPlugin;
        this.logger = hostPlugin.getLogger();
    }
    
    @Override
    public <T> T get(String pluginName, String key, T defaultValue) {
        FileConfiguration config = configCache.get(pluginName);
        if (config == null) {
            return defaultValue;
        }
        
        try {
            Object value = config.get(key, defaultValue);
            if (value == null) {
                return defaultValue;
            }
            @SuppressWarnings("unchecked")
            T result = (T) value;
            return result;
        } catch (Exception e) {
            logger.warning("[Config] Failed to get " + key + " from " + pluginName + ": " + e.getMessage());
            return defaultValue;
        }
    }
    
    @Override
    public <T> T get(String pluginName, String key, Class<T> type) {
        FileConfiguration config = configCache.get(pluginName);
        if (config == null) {
            return null;
        }
        
        try {
            Object value = config.get(key);
            if (value == null) {
                return null;
            }
            return type.cast(value);
        } catch (ClassCastException e) {
            logger.warning("[Config] Type mismatch for " + key + " in " + pluginName);
            return null;
        }
    }
    
    @Override
    public void set(String pluginName, String key, Object value) {
        FileConfiguration config = configCache.get(pluginName);
        if (config != null) {
            config.set(key, value);
        }
    }
    
    @Override
    public void reload(String pluginName) {
        JavaPlugin plugin = registeredPlugins.get(pluginName);
        if (plugin != null) {
            plugin.reloadConfig();
            configCache.put(pluginName, plugin.getConfig());
            logger.info("[Config] Reloaded: " + pluginName);
        }
    }
    
    @Override
    public void reloadAll() {
        registeredPlugins.keySet().forEach(this::reload);
        logger.info("[Config] All configs reloaded");
    }
    
    @Override
    public void save(String pluginName) {
        JavaPlugin plugin = registeredPlugins.get(pluginName);
        if (plugin != null) {
            plugin.saveConfig();
            logger.fine("[Config] Saved: " + pluginName);
        }
    }
    
    @Override
    public void saveAll() {
        registeredPlugins.keySet().forEach(this::save);
        logger.info("[Config] All configs saved");
    }
    
    @Override
    public Optional<FileConfiguration> getConfig(String pluginName) {
        return Optional.ofNullable(configCache.get(pluginName));
    }
    
    @Override
    public void registerConfig(String pluginName, JavaPlugin plugin) {
        registeredPlugins.put(pluginName, plugin);
        configCache.put(pluginName, plugin.getConfig());
        logger.info("[Config] Registered: " + pluginName);
    }
    
    @Override
    public void unregisterConfig(String pluginName) {
        registeredPlugins.remove(pluginName);
        configCache.remove(pluginName);
        logger.info("[Config] Unregistered: " + pluginName);
    }
    
    @Override
    public boolean hasConfig(String pluginName) {
        return configCache.containsKey(pluginName);
    }
}
