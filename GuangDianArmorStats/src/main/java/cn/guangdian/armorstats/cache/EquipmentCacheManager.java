package cn.guangdian.armorstats.cache;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.data.AttributeValue;
import cn.guangdian.armorstats.data.PlayerStats;
import cn.guangdian.armorstats.parser.LoreParser;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 装备缓存管理器
 * 缓存装备属性解析结果，避免重复解析Lore
 * 
 * 功能:
 * - 缓存装备Lore解析结果
 * - 基于装备哈希值进行缓存
 * - 提供缓存统计功能（命中率）
 * - 支持缓存失效和预热
 */
public class EquipmentCacheManager {
    
    private final GuangDianArmorStats plugin;
    
    // 装备属性缓存: 哈希值 -> PlayerStats
    private final Map<String, PlayerStats> equipmentCache;
    
    // 缓存统计
    private final AtomicLong cacheHits;
    private final AtomicLong cacheMisses;
    
    // 最大缓存大小
    private final int maxCacheSize;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param maxCacheSize 最大缓存大小（默认1000）
     */
    public EquipmentCacheManager(GuangDianArmorStats plugin, int maxCacheSize) {
        this.plugin = plugin;
        this.equipmentCache = new ConcurrentHashMap<>();
        this.cacheHits = new AtomicLong(0);
        this.cacheMisses = new AtomicLong(0);
        this.maxCacheSize = maxCacheSize;
    }
    
    /**
     * 获取装备属性（带缓存）
     * 
     * 流程:
     * 1. 计算装备哈希
     * 2. 检查缓存
     * 3. 缓存命中: 返回缓存
     * 4. 缓存未命中: 解析Lore并缓存
     * 
     * @param item 装备物品
     * @return 装备属性
     */
    public PlayerStats getEquipmentStats(ItemStack item) {
        if (item == null) {
            return new PlayerStats();
        }
        
        // 计算装备哈希
        String itemHash = EquipmentHash.calculate(item);
        
        // 检查缓存
        PlayerStats cached = equipmentCache.get(itemHash);
        if (cached != null) {
            cacheHits.incrementAndGet();
            return cloneStats(cached);  // 返回副本，避免修改缓存
        }
        
        // 缓存未命中，解析Lore
        cacheMisses.incrementAndGet();
        PlayerStats stats = parseEquipment(item);
        
        // 缓存结果（如果未超过最大大小）
        if (equipmentCache.size() < maxCacheSize) {
            equipmentCache.put(itemHash, cloneStats(stats));  // 缓存副本
        } else {
            // 缓存已满，记录警告
            if (equipmentCache.size() == maxCacheSize) {
                plugin.getLogger().warning("装备缓存已满(" + maxCacheSize + ")，考虑增加缓存大小");
            }
        }
        
        return stats;
    }
    
    /**
     * 解析装备属性
     * 
     * 修复: 正确合并宝石属性，避免覆盖装备属性
     *
     * @param item 装备物品
     * @return 装备属性
     */
    private PlayerStats parseEquipment(ItemStack item) {
        PlayerStats stats = new PlayerStats();
        
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return stats;
        }
        
        // 使用LoreParser解析装备
        Map<String, AttributeValue> attrs = LoreParser.parse(item);

        // 注意: 宝石属性解析已迁移到 GuangDianSocket 插件
        // 装备属性不再包含宝石属性，由 GuangDianSocket 在镶嵌时直接写入装备Lore

        // 添加合并后的所有属性
        stats.addStats(attrs);
        
        return stats;
    }
    
    /**
     * 计算装备哈希值（公开方法，供外部使用）
     * 
     * @param item 装备物品
     * @return 哈希值，如果物品无效返回null
     */
    public String calculateItemHash(ItemStack item) {
        return EquipmentHash.calculate(item);
    }
    
    /**
     * 克隆PlayerStats对象
     * 
     * @param original 原始对象
     * @return 克隆对象
     */
    private PlayerStats cloneStats(PlayerStats original) {
        PlayerStats clone = new PlayerStats();
        clone.addPlayerStats(original);
        return clone;
    }
    
    /**
     * 使缓存失效
     * 
     * @param itemHash 装备哈希
     */
    public void invalidate(String itemHash) {
        equipmentCache.remove(itemHash);
    }
    
    /**
     * 清空所有缓存
     */
    public void clearCache() {
        equipmentCache.clear();
        cacheHits.set(0);
        cacheMisses.set(0);
        plugin.getLogger().info("装备缓存已清空");
    }
    
    /**
     * 预热缓存
     * 服务器启动时预热常用装备
     * 
     * @param commonItems 常用装备列表
     */
    public void warmupCache(List<ItemStack> commonItems) {
        if (commonItems == null || commonItems.isEmpty()) {
            return;
        }
        
        plugin.getLogger().info("开始预热装备缓存...");
        int warmed = 0;
        
        for (ItemStack item : commonItems) {
            if (item != null) {
                getEquipmentStats(item);  // 触发缓存
                warmed++;
            }
        }
        
        plugin.getLogger().info("装备缓存预热完成，预热了 " + warmed + " 个装备");
    }
    
    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计
     */
    public CacheStats getStats() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        int size = equipmentCache.size();
        
        double hitRate = 0.0;
        long total = hits + misses;
        if (total > 0) {
            hitRate = (double) hits / total;
        }
        
        return new CacheStats(hits, misses, size, hitRate);
    }
    
    /**
     * 获取缓存大小
     * 
     * @return 缓存中的装备数量
     */
    public int getCacheSize() {
        return equipmentCache.size();
    }
    
    /**
     * 缓存统计信息类
     */
    public static class CacheStats {
        private final long hits;
        private final long misses;
        private final int size;
        private final double hitRate;
        
        public CacheStats(long hits, long misses, int size, double hitRate) {
            this.hits = hits;
            this.misses = misses;
            this.size = size;
            this.hitRate = hitRate;
        }
        
        public long getHits() {
            return hits;
        }
        
        public long getMisses() {
            return misses;
        }
        
        public int getSize() {
            return size;
        }
        
        public double getHitRate() {
            return hitRate;
        }
        
        @Override
        public String toString() {
            return String.format("CacheStats{hits=%d, misses=%d, size=%d, hitRate=%.2f%%}",
                hits, misses, size, hitRate * 100);
        }
    }
}
