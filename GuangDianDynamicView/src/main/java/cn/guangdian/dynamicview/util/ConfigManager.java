package cn.guangdian.dynamicview.util;

import cn.guangdian.dynamicview.DynamicViewPlugin;
import cn.guangdian.dynamicview.data.PlayerViewData.ViewTier;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 配置管理器 - 按世界配置挡位模式
 */
public class ConfigManager {

    private final DynamicViewPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(DynamicViewPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // ==================== 基础配置 ====================

    public boolean isEnabled() {
        return config.getBoolean("settings.enabled", true);
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("settings.debug", false);
    }

    // ==================== 全局默认挡位 ====================

    public int getDefaultTierViewDistance(ViewTier tier) {
        return switch (tier) {
            case EXPLORING -> config.getInt("default-tiers.exploring", 5);
            case COMBAT -> config.getInt("default-tiers.combat", 3);
            case AFK -> config.getInt("default-tiers.afk", 1);
        };
    }

    // ==================== 世界配置 ====================

    /**
     * 查找世界匹配的配置节路径
     * 支持精确匹配和前缀匹配（如 dungeon_ 匹配 dungeon_fire_temple）
     */
    private String findWorldConfigPath(String worldName) {
        // 1. 精确匹配
        if (config.contains("worlds." + worldName)) {
            return "worlds." + worldName;
        }

        // 2. 前缀匹配：遍历所有世界配置，找最长的前缀匹配
        String bestMatch = null;
        int bestLength = 0;

        if (config.isConfigurationSection("worlds")) {
            for (String key : config.getConfigurationSection("worlds").getKeys(false)) {
                if (key.endsWith("_") && worldName.startsWith(key)) {
                    if (key.length() > bestLength) {
                        bestLength = key.length();
                        bestMatch = "worlds." + key;
                    }
                }
            }
        }

        return bestMatch;
    }

    /**
     * 世界是否使用固定视距
     */
    public boolean isWorldFixed(String worldName) {
        String path = findWorldConfigPath(worldName);
        if (path != null) {
            return config.contains(path + ".fixed");
        }
        return false;
    }

    /**
     * 获取世界固定视距值
     */
    public int getWorldFixedView(String worldName) {
        String path = findWorldConfigPath(worldName);
        if (path != null) {
            return config.getInt(path + ".fixed", 3);
        }
        return 3;
    }

    /**
     * 获取世界指定挡位的视距值
     * 优先使用世界配置，否则使用全局默认
     */
    public int getWorldTierViewDistance(String worldName, ViewTier tier) {
        // 固定视距世界
        if (isWorldFixed(worldName)) {
            return getWorldFixedView(worldName);
        }

        String path = findWorldConfigPath(worldName);
        if (path != null) {
            String tierKey = tier.name().toLowerCase();
            if (config.contains(path + "." + tierKey)) {
                return config.getInt(path + "." + tierKey);
            }
        }

        // 回退到全局默认
        return getDefaultTierViewDistance(tier);
    }

    // ==================== 战斗检测 ====================

    public long getCombatDuration() {
        return config.getLong("combat.duration", 10);
    }

    // ==================== 挂机检测 ====================

    public long getAFKTimeout() {
        return config.getLong("afk.timeout", 30);
    }

    // ==================== 更新间隔 ====================

    public long getUpdateInterval() {
        return config.getLong("update-interval", 20);
    }

    // ==================== 渐进式调整 ====================

    /**
     * 获取渐进式调整间隔（毫秒）
     * 每次视距调整之间的最小间隔
     */
    public long getGradualAdjustInterval() {
        // 默认 500ms，让客户端有时间处理区块变化
        return config.getLong("gradual-adjust.interval", 500);
    }

    /**
     * 是否启用渐进式调整
     */
    public boolean isGradualAdjustEnabled() {
        return config.getBoolean("gradual-adjust.enabled", true);
    }

    // ==================== 消息配置 ====================

    public boolean isMessagesEnabled() {
        return config.getBoolean("messages.enabled", false);
    }

    public String getAFKMessage() {
        return config.getString("messages.text.afk", "&e[动态视距] &f检测到挂机，视距已降低");
    }

    public String getCombatMessage() {
        return config.getString("messages.text.combat", "&c[动态视距] &f战斗状态，视距已调整");
    }

    public String getExploringMessage() {
        return config.getString("messages.text.exploring", "&a[动态视距] &f跑图模式，视距已恢复");
    }

    // ==================== 调试信息 ====================

    public void printDebugInfo() {
        plugin.getLogger().info("========== 配置信息 ==========");
        plugin.getLogger().info("插件启用: " + isEnabled());
        plugin.getLogger().info("全局默认 - 跑图: " + getDefaultTierViewDistance(ViewTier.EXPLORING)
            + " 战斗: " + getDefaultTierViewDistance(ViewTier.COMBAT)
            + " 挂机: " + getDefaultTierViewDistance(ViewTier.AFK));
        plugin.getLogger().info("战斗持续: " + getCombatDuration() + "秒");
        plugin.getLogger().info("挂机判定: " + getAFKTimeout() + "秒");
        plugin.getLogger().info("渐进调整: " + (isGradualAdjustEnabled() ? "启用" : "禁用")
            + " 间隔: " + getGradualAdjustInterval() + "ms");

        // 打印世界配置
        if (config.isConfigurationSection("worlds")) {
            for (String key : config.getConfigurationSection("worlds").getKeys(false)) {
                String path = "worlds." + key;
                if (config.contains(path + ".fixed")) {
                    plugin.getLogger().info("世界[" + key + "]: 固定视距 " + config.getInt(path + ".fixed"));
                } else {
                    plugin.getLogger().info("世界[" + key + "]: 跑图=" + config.getInt(path + ".exploring", 5)
                        + " 战斗=" + config.getInt(path + ".combat", 3)
                        + " 挂机=" + config.getInt(path + ".afk", 1));
                }
            }
        }
        plugin.getLogger().info("==============================");
    }
}
