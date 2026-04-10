package cn.guangdian.rpgcore.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 统一配置管理器
 * 
 * <p>提供所有GuangDian插件的统一配置管理接口。</p>
 * <p>支持热重载、默认值、类型安全的配置读取。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private final Map<String, FileConfiguration> configs = new ConcurrentHashMap<>();
    private final Map<String, File> configFiles = new ConcurrentHashMap<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载主配置文件（config.yml）
     */
    public void loadMainConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        configs.put("config", plugin.getConfig());
        configFiles.put("config", new File(plugin.getDataFolder(), "config.yml"));
    }

    /**
     * 加载指定名称的配置文件
     * 
     * @param name 配置文件名称（不含.yml后缀）
     */
    public void loadConfig(String name) {
        File file = new File(plugin.getDataFolder(), name + ".yml");
        
        // 如果文件不存在，从资源中复制
        if (!file.exists()) {
            InputStream resource = plugin.getResource(name + ".yml");
            if (resource != null) {
                plugin.saveResource(name + ".yml", false);
            } else {
                // 创建空文件
                try {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "创建配置文件失败: " + name, e);
                    return;
                }
            }
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        // 设置默认值（从资源文件）
        InputStream defaultStream = plugin.getResource(name + ".yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            );
            config.setDefaults(defaultConfig);
        }
        
        configs.put(name, config);
        configFiles.put(name, file);
    }

    /**
     * 获取主配置
     */
    public FileConfiguration getConfig() {
        return configs.get("config");
    }

    /**
     * 获取指定配置
     */
    public FileConfiguration getConfig(String name) {
        if (!configs.containsKey(name)) {
            loadConfig(name);
        }
        return configs.get(name);
    }

    /**
     * 重载指定配置
     */
    public void reloadConfig(String name) {
        File file = configFiles.get(name);
        if (file != null && file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            configs.put(name, config);
        } else {
            loadConfig(name);
        }
    }

    /**
     * 重载所有配置
     */
    public void reloadAll() {
        for (String name : configs.keySet()) {
            reloadConfig(name);
        }
    }

    /**
     * 保存指定配置
     */
    public void saveConfig(String name) {
        FileConfiguration config = configs.get(name);
        File file = configFiles.get(name);
        
        if (config != null && file != null) {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "保存配置文件失败: " + name, e);
            }
        }
    }

    /**
     * 获取字符串（支持默认值）
     */
    public String getString(String configName, String path, String defaultValue) {
        FileConfiguration config = getConfig(configName);
        return config != null ? config.getString(path, defaultValue) : defaultValue;
    }

    /**
     * 获取整数（支持默认值）
     */
    public int getInt(String configName, String path, int defaultValue) {
        FileConfiguration config = getConfig(configName);
        return config != null ? config.getInt(path, defaultValue) : defaultValue;
    }

    /**
     * 获取布尔值（支持默认值）
     */
    public boolean getBoolean(String configName, String path, boolean defaultValue) {
        FileConfiguration config = getConfig(configName);
        return config != null ? config.getBoolean(path, defaultValue) : defaultValue;
    }

    /**
     * 获取双精度数（支持默认值）
     */
    public double getDouble(String configName, String path, double defaultValue) {
        FileConfiguration config = getConfig(configName);
        return config != null ? config.getDouble(path, defaultValue) : defaultValue;
    }

    /**
     * 获取所有已加载的配置名称
     */
    public Set<String> getLoadedConfigs() {
        return configs.keySet();
    }

    /**
     * 清理所有配置
     */
    public void clear() {
        configs.clear();
        configFiles.clear();
    }
}