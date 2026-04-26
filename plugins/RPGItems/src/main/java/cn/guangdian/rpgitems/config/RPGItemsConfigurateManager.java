package cn.guangdian.rpgitems.config;

import cn.guangdian.rpgitems.RPGItems;
import cn.guangdian.rpgcore.config.ConfigurateManager;

import java.nio.file.Path;

/**
 * 基于 Configurate 的 RPGItems 配置管理器
 */
public class RPGItemsConfigurateManager {

    private final RPGItems plugin;
    private ConfigurateManager configManager;
    private RPGItemsConfig config;

    public RPGItemsConfigurateManager(RPGItems plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载配置
     */
    public void load() {
        try {
            // 初始化 ConfigurateManager
            Path configDir = plugin.getDataFolder().toPath();
            configManager = new ConfigurateManager(plugin.getLogger(), configDir);

            // 保存默认配置
            configManager.saveDefaultConfig("config.yml", "config.yml", plugin.getClass().getClassLoader());

            // 加载配置
            config = configManager.loadConfig("config.yml", RPGItemsConfig.class);

            plugin.getLogger().info("Configurate 配置加载成功");
        } catch (Exception e) {
            plugin.getLogger().severe("Configurate 配置加载失败: " + e.getMessage());
            // 降级到默认配置
            config = new RPGItemsConfig();
        }
    }

    /**
     * 获取配置
     */
    public RPGItemsConfig getConfig() {
        if (config != null) {
            return config;
        }
        return new RPGItemsConfig();
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        if (configManager != null) {
            try {
                config = configManager.loadConfig("config.yml", RPGItemsConfig.class);
                plugin.getLogger().info("配置已重新加载");
            } catch (Exception e) {
                plugin.getLogger().severe("配置重载失败: " + e.getMessage());
            }
        }
    }

    /**
     * 保存配置
     */
    public void save() {
        if (configManager != null && config != null) {
            try {
                configManager.saveConfig("config.yml", config);
            } catch (Exception e) {
                plugin.getLogger().severe("配置保存失败: " + e.getMessage());
            }
        }
    }

    /**
     * 检查 Configurate 是否可用
     */
    public boolean isConfigurateAvailable() {
        return configManager != null;
    }

    // 便捷方法
    public boolean isDebug() {
        return config.getGeneral().isDebug();
    }

    public int getAutoSaveInterval() {
        return config.getGeneral().getAutoSaveInterval();
    }

    public boolean isRPGSkillIntegrationEnabled() {
        return config.getIntegration().getRpgSkill().isEnabled();
    }

    public boolean isPlaceholderAPIEnabled() {
        return config.getIntegration().getPlaceholderapi().isEnabled();
    }
}
