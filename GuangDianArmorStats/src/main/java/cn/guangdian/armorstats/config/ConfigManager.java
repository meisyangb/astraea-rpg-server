package cn.guangdian.armorstats.config;

import cn.guangdian.armorstats.GuangDianArmorStats;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 配置管理器
 * 管理多个独立的配置文件
 */
public class ConfigManager {

    private final GuangDianArmorStats plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final Map<String, File> configFiles = new HashMap<>();

    // 配置文件列表
    public static final String[] CONFIG_FILES = {
        "config",
        "attributes",
        "skills",
        "damage",
        "regen",
        "messages",
        "combat_log"
    };

    public ConfigManager(GuangDianArmorStats plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载所有配置文件
     */
    public void loadAll() {
        for (String name : CONFIG_FILES) {
            loadConfig(name);
        }
        plugin.getLogger().info("已加载 " + configs.size() + " 个配置文件");
    }

    /**
     * 加载指定配置文件
     */
    public void loadConfig(String name) {
        File file = new File(plugin.getDataFolder(), name + ".yml");
        
        // 如果文件不存在，从jar中复制默认配置
        if (!file.exists()) {
            plugin.saveResource(name + ".yml", false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        // 从jar中读取默认配置并合并
        InputStream defaultStream = plugin.getResource(name + ".yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            config.setDefaults(defaultConfig);
        }
        
        configs.put(name, config);
        configFiles.put(name, file);
    }

    /**
     * 重载所有配置文件
     */
    public void reloadAll() {
        for (String name : CONFIG_FILES) {
            loadConfig(name);
        }
        plugin.getLogger().info("所有配置文件已重新加载");
    }

    /**
     * 重载指定配置文件
     */
    public void reloadConfig(String name) {
        loadConfig(name);
    }

    /**
     * 获取配置文件
     */
    public FileConfiguration getConfig(String name) {
        return configs.get(name);
    }

    /**
     * 获取属性配置
     */
    public FileConfiguration getAttributes() {
        return configs.get("attributes");
    }

    /**
     * 获取技能配置
     */
    public FileConfiguration getSkills() {
        return configs.get("skills");
    }

    /**
     * 获取伤害配置
     */
    public FileConfiguration getDamage() {
        return configs.get("damage");
    }

    /**
     * 获取恢复配置
     */
    public FileConfiguration getRegen() {
        return configs.get("regen");
    }

    /**
     * 获取消息配置
     */
    public FileConfiguration getMessages() {
        return configs.get("messages");
    }

    /**
     * 获取战斗日志配置
     */
    public FileConfiguration getCombatLog() {
        return configs.get("combat_log");
    }

    /**
     * 保存配置文件
     */
    public void saveConfig(String name) {
        FileConfiguration config = configs.get(name);
        File file = configFiles.get(name);
        if (config != null && file != null) {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("无法保存配置文件: " + name + ".yml");
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "详细异常信息", e);
            }
        }
    }

    /**
     * 获取所有配置文件名
     */
    public Set<String> getConfigNames() {
        return configs.keySet();
    }
}