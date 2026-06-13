package cn.guangdian.armorstats.manager;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.data.PlayerStats;
import cn.guangdian.armorstats.debug.DebugLogManager;
import cn.guangdian.armorstats.parser.PDCAttributeReader;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 玩家属性管理器
 * 
 * 简化版：只作为 IncrementalStatsManager 的包装器
 * 所有属性计算都委托给 IncrementalStatsManager
 */
public class StatsManager {

    private final GuangDianArmorStats plugin;

    // 配置参数
    private double defenseDivisor = 15000.0;
    private double maxDamageReduction = 0.90;
    private double minDamage = 1.0;
    private boolean dodgeEnabled = true;
    private double maxDodge = 0.80;
    private boolean critResistEnabled = true;
    private double critResistDamageReduction = 0.25;
    
    public StatsManager(GuangDianArmorStats plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    private void loadConfig() {
        var config = plugin.getConfig();
        var damageSection = config.getConfigurationSection("damage");
        if (damageSection != null) {
            defenseDivisor = damageSection.getDouble("defense_divisor", 15000.0);
            maxDamageReduction = damageSection.getDouble("max_damage_reduction", 0.90);
            minDamage = damageSection.getDouble("min_damage", 1.0);
            maxDodge = damageSection.getDouble("max_dodge", 0.80);
        }
    }

    // ==================== 核心接口 ====================
    
    /**
     * 获取玩家属性
     * 委托给 IncrementalStatsManager
     */
    public PlayerStats getPlayerStats(Player player) {
        // 最高优先级：使用 IncrementalStatsManager 缓存
        if (plugin.getIncrementalStatsManager() != null) {
            PlayerStats stats = plugin.getIncrementalStatsManager().getPlayerStats(player.getUniqueId());
            if (stats != null && stats.hasAnyStats()) {
                return stats;
            }
        }
        
        // 返回空属性
        return new PlayerStats();
    }
    
    /**
     * 获取玩家属性（通过 UUID）
     */
    public PlayerStats getPlayerStats(UUID uuid) {
        if (plugin.getIncrementalStatsManager() != null) {
            return plugin.getIncrementalStatsManager().getPlayerStats(uuid);
        }
        return new PlayerStats();
    }
    
    // ==================== 玩家生命周期 ====================
    
    /**
     * 玩家登录时初始化
     */
    public void onPlayerJoin(Player player) {
        if (plugin.getIncrementalStatsManager() != null) {
            plugin.getIncrementalStatsManager().onPlayerJoin(player);
        }
    }
    
    /**
     * 玩家退出时清理
     */
    public void onPlayerQuit(Player player) {
        if (plugin.getIncrementalStatsManager() != null) {
            plugin.getIncrementalStatsManager().onPlayerQuit(player);
        }
    }
    
    // ==================== 技能相关 ====================
    
    /**
     * 获取玩家技能列表
     * 暂时返回空列表，后续可以从 PDC 读取
     */
    public List<String> getPlayerSkills(Player player) {
        return new ArrayList<>();
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 计算减伤比例
     */
    public double calculateDamageReduction(double defense) {
        if (defenseDivisor <= 0) return 0;
        double reduction = defense / (defense + defenseDivisor);
        return Math.min(reduction, maxDamageReduction);
    }
    
    /**
     * 判断是否闪避
     */
    public boolean shouldDodge(Player defender) {
        if (!dodgeEnabled) return false;
        PlayerStats stats = getPlayerStats(defender);
        double dodgeChance = Math.min(stats.getDodgePercent() / 100.0, maxDodge);
        return ThreadLocalRandom.current().nextDouble() < dodgeChance;
    }
    
    /**
     * 应用暴击抵抗
     */
    public double applyCritResist(double critChance) {
        if (!critResistEnabled) return critChance;
        return Math.max(0, critChance - (critResistDamageReduction * 100));
    }
    
    // ==================== 配置参数 ====================
    
    public double getDefenseDivisor() { return defenseDivisor; }
    public double getMaxDamageReduction() { return maxDamageReduction; }
    public double getMinDamage() { return minDamage; }
    public boolean isDodgeEnabled() { return dodgeEnabled; }
    public double getMaxDodge() { return maxDodge; }
    
    /**
     * 重载配置
     */
    public void reloadConfig() {
        loadConfig();
    }
    
    // ==================== 兼容接口 ====================
    
    /**
     * 创建空的 PlayerStats 对象
     */
    public PlayerStats createEmptyStats() {
        return new PlayerStats();
    }
    
    /**
     * 设置外部配饰属性（由 GuangDianAccessory 调用）
     * 暂时不实现，后续可以扩展 IncrementalStatsManager
     */
    public void setExternalAccessoryStats(Player player, PlayerStats accessoryStats) {
        // TODO: 扩展 IncrementalStatsManager 支持配饰槽位
    }
    
    // ==================== 兼容方法（委托给 IncrementalStatsManager） ====================
    
    /**
     * 刷新玩家完整属性
     */
    public void refreshFullStats(Player player) {
        if (plugin.getIncrementalStatsManager() != null) {
            plugin.getIncrementalStatsManager().onPlayerJoin(player);
        }
    }
    
    /**
     * 刷新玩家属性
     */
    public void refreshPlayerStats(Player player) {
        refreshFullStats(player);
    }
    
    /**
     * 清除玩家属性
     */
    public void clearPlayerAttributes(Player player) {
        if (plugin.getIncrementalStatsManager() != null) {
            plugin.getIncrementalStatsManager().onPlayerQuit(player);
        }
    }
    
    /**
     * 移除玩家
     */
    public void removePlayer(UUID uuid) {
        // IncrementalStatsManager 会自动清理
    }
    
    /**
     * 重置玩家
     */
    public void resetPlayer(Player player) {
        clearPlayerAttributes(player);
        refreshFullStats(player);
    }
    
    /**
     * 加载玩家数据
     */
    public void loadPlayerData(Player player) {
        onPlayerJoin(player);
    }
    
    /**
     * 保存玩家数据
     */
    public void savePlayerData(Player player) {
        // 委托给 PlayerDataStorage（由 ArmorStatsDataHandler 调用）
    }
    
    /**
     * 获取玩家 UUID 对应的属性
     */
    public PlayerStats getPlayerStatsByUUID(UUID uuid) {
        return getPlayerStats(uuid);
    }
    
    /**
     * 应用最大生命值
     */
    public void applyMaxHealth(Player player, PlayerStats stats) {
        // 由 IncrementalStatsManager 处理
    }
    
    /**
     * 检查物品是否有可解析属性
     */
    public boolean hasParsableAttributes(ItemStack item) {
        return PDCAttributeReader.isRPGItemsItem(item);
    }
    
    /**
     * 添加物品属性到 PlayerStats
     */
    public void addItemAttributes(PlayerStats stats, Object slot, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        var attrMap = PDCAttributeReader.readFromPDC(item);
        // 将 Map 属性应用到 PlayerStats
        // TODO: 需要实现 Map -> PlayerStats 转换
    }
}
