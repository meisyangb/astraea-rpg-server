package cn.guangdian.cleaner.config;

import cn.guangdian.cleaner.GuangDianCleaner;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/**
 * 配置管理器
 * 负责加载和管理插件配置
 */
public class ConfigManager {

    private final GuangDianCleaner plugin;

    // 基础配置
    private boolean autoCleanEnabled;
    private int autoCleanInterval;
    private int warningTime;
    private boolean cleanByPlayer;

    // 物品过滤配置
    private FilterMode filterMode;
    private Set<Material> itemFilter;
    private Set<String> itemNameFilter;

    // 世界配置
    private WorldMode worldMode;
    private Set<String> worldList;

    // 保护配置
    private boolean protectNamedItems;
    private boolean protectPlayerDrops;
    private int protectPlayerDropsTime;

    // 消息配置
    private String messagePrefix;
    private String messageWarning;
    private String messageCleaned;
    private String messageNoPermission;
    private String messageReload;
    private String messageStatus;
    private String messageStats;

    // 统计数据
    private long totalCleanedItems;
    private long totalCleanedEntities;

    public ConfigManager(GuangDianCleaner plugin) {
        this.plugin = plugin;
        this.itemFilter = new HashSet<>();
        this.itemNameFilter = new HashSet<>();
        this.worldList = new HashSet<>();
    }

    /**
     * 加载配置
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();

        // 基础配置
        autoCleanEnabled = config.getBoolean("auto-clean.enabled", true);
        autoCleanInterval = config.getInt("auto-clean.interval", 300);
        warningTime = config.getInt("auto-clean.warning-time", 30);
        cleanByPlayer = config.getBoolean("auto-clean.clean-by-player", true);

        // 物品过滤配置
        String filterModeStr = config.getString("filter.mode", "none").toUpperCase();
        filterMode = FilterMode.valueOf(filterModeStr);

        itemFilter.clear();
        for (String item : config.getStringList("filter.items")) {
            try {
                Material material = Material.valueOf(item.toUpperCase());
                itemFilter.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("未知的物品类型: " + item);
            }
        }

        itemNameFilter.clear();
        itemNameFilter.addAll(config.getStringList("filter.item-names"));

        // 世界配置
        String worldModeStr = config.getString("worlds.mode", "all").toUpperCase();
        worldMode = WorldMode.valueOf(worldModeStr);

        worldList.clear();
        worldList.addAll(config.getStringList("worlds.list"));

        // 保护配置
        protectNamedItems = config.getBoolean("protection.named-items", true);
        protectPlayerDrops = config.getBoolean("protection.player-drops", true);
        protectPlayerDropsTime = config.getInt("protection.player-drops-time", 60);

        // 消息配置 - 使用 MiniMessage 格式
        messagePrefix = config.getString("messages.prefix", "<gold>[扫地娘] <reset>");
        messageWarning = config.getString("messages.warning", "<yellow>地面物品将在 <red>%time% <yellow>秒后清理!");
        messageCleaned = config.getString("messages.cleaned", "<green>已清理 <yellow>%count% <green>个掉落物!");
        messageNoPermission = config.getString("messages.no-permission", "<red>你没有权限执行此操作!");
        messageReload = config.getString("messages.reload", "<green>配置已重新加载!");
        messageStatus = config.getString("messages.status", "<yellow>扫地娘状态: %status%");
        messageStats = config.getString("messages.stats", "<yellow>本次清理: <green>%items%<yellow>个物品, 累计清理: <green>%total%<yellow>个物品");

        // 加载统计数据
        totalCleanedItems = config.getLong("stats.total-cleaned-items", 0);
        totalCleanedEntities = config.getLong("stats.total-cleaned-entities", 0);
    }

    /**
     * 保存统计数据
     */
    public void saveStats() {
        FileConfiguration config = plugin.getConfig();
        config.set("stats.total-cleaned-items", totalCleanedItems);
        config.set("stats.total-cleaned-entities", totalCleanedEntities);
        plugin.saveConfig();
    }

    /**
     * 增加清理统计
     */
    public void addCleanStats(long items, long entities) {
        this.totalCleanedItems += items;
        this.totalCleanedEntities += entities;
    }

    /**
     * 重置统计数据
     */
    public void resetStats() {
        this.totalCleanedItems = 0;
        this.totalCleanedEntities = 0;
        saveStats();
    }

    /**
     * 将 MiniMessage 格式的字符串转换为 Component
     */
    public Component parseMessage(String message) {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            MiniMessageService miniMessage = rpgCore.getMiniMessageService();
            if (miniMessage != null) {
                return miniMessage.parse(message);
            }
        }
        // 降级方案：使用 LegacyComponentSerializer 转换旧的 & 颜色代码
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }

    /**
     * 获取带占位符替换的消息 Component
     */
    public Component getFormattedMessage(String template, String... placeholders) {
        String message = template;
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return parseMessage(message);
    }

    // Getters

    public boolean isAutoCleanEnabled() {
        return autoCleanEnabled;
    }

    public int getAutoCleanInterval() {
        return autoCleanInterval;
    }

    public void setAutoCleanInterval(int seconds) {
        this.autoCleanInterval = seconds;
        plugin.getConfig().set("auto-clean.interval", seconds);
        plugin.saveConfig();
    }

    public int getWarningTime() {
        return warningTime;
    }

    public boolean isCleanByPlayer() {
        return cleanByPlayer;
    }

    public FilterMode getFilterMode() {
        return filterMode;
    }

    public Set<Material> getItemFilter() {
        return Collections.unmodifiableSet(itemFilter);
    }

    public Set<String> getItemNameFilter() {
        return Collections.unmodifiableSet(itemNameFilter);
    }

    public WorldMode getWorldMode() {
        return worldMode;
    }

    public Set<String> getWorldList() {
        return Collections.unmodifiableSet(worldList);
    }

    public boolean isProtectNamedItems() {
        return protectNamedItems;
    }

    public boolean isProtectPlayerDrops() {
        return protectPlayerDrops;
    }

    public int getProtectPlayerDropsTime() {
        return protectPlayerDropsTime;
    }

    public String getMessagePrefix() {
        return messagePrefix;
    }

    public String getMessageWarning() {
        return messageWarning;
    }

    public String getMessageCleaned() {
        return messageCleaned;
    }

    public String getMessageNoPermission() {
        return messageNoPermission;
    }

    public String getMessageReload() {
        return messageReload;
    }

    public String getMessageStatus() {
        return messageStatus;
    }

    public String getMessageStats() {
        return messageStats;
    }

    public long getTotalCleanedItems() {
        return totalCleanedItems;
    }

    public long getTotalCleanedEntities() {
        return totalCleanedEntities;
    }

    /**
     * 过滤模式枚举
     */
    public enum FilterMode {
        /** 不过滤 */
        NONE,
        /** 黑名单模式 - 只清理列表中的物品 */
        BLACKLIST,
        /** 白名单模式 - 只清理不在列表中的物品 */
        WHITELIST
    }

    /**
     * 世界模式枚举
     */
    public enum WorldMode {
        /** 所有世界 */
        ALL,
        /** 只清理列表中的世界 */
        WHITELIST,
        /** 不清理列表中的世界 */
        BLACKLIST
    }
}