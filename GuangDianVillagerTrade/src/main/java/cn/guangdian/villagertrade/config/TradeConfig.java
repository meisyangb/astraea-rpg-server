package cn.guangdian.villagertrade.config;

import cn.guangdian.villagertrade.GuangDianVillagerTrade;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 交易配置管理器
 */
public class TradeConfig {

    private final GuangDianVillagerTrade plugin;
    private FileConfiguration config;

    public TradeConfig(GuangDianVillagerTrade plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    /**
     * 获取是否启用音效
     *
     * @return 是否启用音效
     */
    public boolean isSoundEnabled() {
        return config.getBoolean("settings.sound-enabled", true);
    }

    /**
     * 获取成功音效
     *
     * @return 成功音效
     */
    public String getSuccessSound() {
        return config.getString("settings.sounds.success", "minecraft:block.note_block.pling");
    }

    /**
     * 获取错误音效
     *
     * @return 错误音效
     */
    public String getErrorSound() {
        return config.getString("settings.sounds.error", "minecraft:block.note_block.bass");
    }

    /**
     * 获取点击音效
     *
     * @return 点击音效
     */
    public String getClickSound() {
        return config.getString("settings.sounds.click", "minecraft:ui.button.click");
    }

    /**
     * 获取是否记录日志
     *
     * @return 是否记录日志
     */
    public boolean isLogEnabled() {
        return config.getBoolean("settings.log-trades", true);
    }

    /**
     * 获取是否启用每日限制
     *
     * @return 是否启用每日限制
     */
    public boolean isDailyLimitEnabled() {
        return config.getBoolean("settings.daily-limit-enabled", true);
    }
}
