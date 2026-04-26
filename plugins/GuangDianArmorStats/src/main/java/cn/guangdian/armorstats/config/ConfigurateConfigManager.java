package cn.guangdian.armorstats.config;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.rpgcore.config.ConfigurateManager;

import java.io.File;
import java.nio.file.Path;

/**
 * 基于 Configurate 的配置管理器
 * 替代原有的 ConfigManager
 */
public class ConfigurateConfigManager {

    private final GuangDianArmorStats plugin;
    private ConfigurateManager configManager;
    private ArmorStatsConfig mainConfig;

    public ConfigurateConfigManager(GuangDianArmorStats plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载所有配置
     */
    public void loadAll() {
        try {
            // 初始化 ConfigurateManager
            Path configDir = plugin.getDataFolder().toPath();
            configManager = new ConfigurateManager(plugin.getLogger(), configDir);

            // 保存默认配置
            configManager.saveDefaultConfig("config.yml", "config.yml", plugin.getClass().getClassLoader());

            // 加载主配置
            mainConfig = configManager.loadConfig("config.yml", ArmorStatsConfig.class);

            plugin.getLogger().info("Configurate 配置加载成功");
        } catch (Exception e) {
            plugin.getLogger().severe("Configurate 配置加载失败: " + e.getMessage());
            // 降级到默认配置
            mainConfig = new ArmorStatsConfig();
        }
    }

    /**
     * 获取主配置
     */
    public ArmorStatsConfig getConfig() {
        if (mainConfig != null) {
            return mainConfig;
        }
        // 返回默认配置
        return new ArmorStatsConfig();
    }

    /**
     * 重新加载配置
     */
    public void reloadAll() {
        if (configManager != null) {
            try {
                mainConfig = configManager.loadConfig("config.yml", ArmorStatsConfig.class);
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
        if (configManager != null && mainConfig != null) {
            try {
                configManager.saveConfig("config.yml", mainConfig);
            } catch (Exception e) {
                plugin.getLogger().severe("配置保存失败: " + e.getMessage());
            }
        }
    }

    /**
     * 检查 Configurate 是否可用
     */
    public boolean isConfigurateAvailable() {
        return configManager != null && mainConfig != null;
    }
}
