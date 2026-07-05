package cn.guangdian.armorstats.config;

import cn.guangdian.armorstats.GuangDianArmorStats;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 属性应用日志配置管理器
 * 
 * 控制属性应用、装备变化等日志输出
 * 生产环境建议关闭，避免控制台刷屏
 */
public class AttributeApplyLogConfig {

    private static AttributeApplyLogConfig instance;
    
    private boolean enabled;
    private boolean logApplyDetails;
    private boolean logEquipmentChange;
    private boolean logAttributeClear;

    public AttributeApplyLogConfig() {
        instance = this;
        loadConfig();
    }

    public void loadConfig() {
        FileConfiguration config = GuangDianArmorStats.getInstance().getConfig();
        
        String prefix = "attribute_apply_log.";
        enabled = config.getBoolean(prefix + "enabled", false);
        logApplyDetails = config.getBoolean(prefix + "log_apply_details", true);
        logEquipmentChange = config.getBoolean(prefix + "log_equipment_change", true);
        logAttributeClear = config.getBoolean(prefix + "log_attribute_clear", true);
    }

    public void reloadConfig() {
        loadConfig();
    }

    public static AttributeApplyLogConfig getInstance() {
        if (instance == null) {
            instance = new AttributeApplyLogConfig();
        }
        return instance;
    }

    // ========== 日志输出方法 ==========

    /**
     * 记录属性应用日志
     */
    public void logApply(String message) {
        if (enabled && logApplyDetails) {
            GuangDianArmorStats.getInstance().getLogger().info("[属性应用] " + message);
        }
    }

    /**
     * 记录装备变化日志
     */
    public void logEquipmentChange(String message) {
        if (enabled && logEquipmentChange) {
            GuangDianArmorStats.getInstance().getLogger().info("[装备变化] " + message);
        }
    }

    /**
     * 记录主手变化日志
     */
    public void logMainHandChange(String message) {
        if (enabled && logEquipmentChange) {
            GuangDianArmorStats.getInstance().getLogger().info("[主手变化] " + message);
        }
    }

    /**
     * 记录副手变化日志
     */
    public void logOffHandChange(String message) {
        if (enabled && logEquipmentChange) {
            GuangDianArmorStats.getInstance().getLogger().info("[副手变化] " + message);
        }
    }

    /**
     * 记录属性清理日志
     */
    public void logAttributeClear(String message) {
        if (enabled && logAttributeClear) {
            GuangDianArmorStats.getInstance().getLogger().info("[属性清理] " + message);
        }
    }

    // ========== Getters ==========

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isLogApplyDetails() {
        return logApplyDetails;
    }

    public boolean isLogEquipmentChange() {
        return logEquipmentChange;
    }

    public boolean isLogAttributeClear() {
        return logAttributeClear;
    }
}