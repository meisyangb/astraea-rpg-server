package cn.guangdian.auth.config;

import cn.guangdian.auth.GuangDianAuth;
import cn.guangdian.rpgcore.config.ConfigurateManager;

import java.nio.file.Path;

/**
 * 基于 Configurate 的认证配置管理器
 * 替代原有的 AuthConfig
 */
public class AuthConfigurateManager {

    private final GuangDianAuth plugin;
    private ConfigurateManager configManager;
    private AuthConfigData config;

    public AuthConfigurateManager(GuangDianAuth plugin) {
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
            config = configManager.loadConfig("config.yml", AuthConfigData.class);

            plugin.logInfo("Configurate 配置加载成功");
        } catch (Exception e) {
            plugin.logSevere("Configurate 配置加载失败: " + e.getMessage());
            // 降级到默认配置
            config = new AuthConfigData();
        }
    }

    /**
     * 获取配置
     */
    public AuthConfigData getConfig() {
        if (config != null) {
            return config;
        }
        return new AuthConfigData();
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        if (configManager != null) {
            try {
                config = configManager.loadConfig("config.yml", AuthConfigData.class);
                plugin.logInfo("配置已重新加载");
            } catch (Exception e) {
                plugin.logSevere("配置重载失败: " + e.getMessage());
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
                plugin.logSevere("配置保存失败: " + e.getMessage());
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
    public int getMinPasswordLength() {
        return config.getPassword().getMinLength();
    }

    public int getMaxPasswordLength() {
        return config.getPassword().getMaxLength();
    }

    public int getSessionTimeout() {
        return config.getSession().getTimeoutMinutes();
    }

    public int getLoginTimeout() {
        return config.getLogin().getTimeoutSeconds();
    }

    public int getMaxLoginAttempts() {
        return config.getLogin().getMaxAttempts();
    }

    public boolean isKickOnWrongPassword() {
        return config.getLogin().isKickOnWrongPassword();
    }

    public boolean isForceLoginAfterRegister() {
        return config.getRegister().isForceLoginAfter();
    }

    public boolean isAllowMovementBeforeLogin() {
        return config.getRestrictions().isAllowMovement();
    }

    public boolean isAllowChatBeforeLogin() {
        return config.getRestrictions().isAllowChat();
    }
}
