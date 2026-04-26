package cn.guangdian.rpgskill.config;

import cn.guangdian.rpgcore.config.ConfigurateManager;
import cn.guangdian.rpgskill.RPGSkill;

import java.nio.file.Path;

/**
 * 基于 Configurate 的 RPGSkill 配置管理器
 */
public class RPGSkillConfigurateManager {

    private final RPGSkill plugin;
    private ConfigurateManager configManager;
    private RPGSkillConfig config;

    public RPGSkillConfigurateManager(RPGSkill plugin) {
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
            config = configManager.loadConfig("config.yml", RPGSkillConfig.class);

            plugin.getLogger().info("Configurate 配置加载成功");
        } catch (Exception e) {
            plugin.getLogger().severe("Configurate 配置加载失败: " + e.getMessage());
            // 降级到默认配置
            config = new RPGSkillConfig();
        }
    }

    /**
     * 获取配置
     */
    public RPGSkillConfig getConfig() {
        if (config != null) {
            return config;
        }
        return new RPGSkillConfig();
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        if (configManager != null) {
            try {
                config = configManager.loadConfig("config.yml", RPGSkillConfig.class);
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

    public int getDefaultCooldownSeconds() {
        return config.getGeneral().getDefaultCooldownSeconds();
    }

    public boolean isCooldownPersistAfterLogout() {
        return config.getCooldown().isPersistAfterLogout();
    }

    public boolean isDisplayActionbar() {
        return config.getCooldown().isDisplayActionbar();
    }
}
