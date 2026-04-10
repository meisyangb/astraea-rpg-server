package cn.guangdian.armorstats.listener;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.cache.EquipmentCacheManager;
import cn.guangdian.armorstats.cache.EquipmentHash;
import cn.guangdian.armorstats.event.GemInlayEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

/**
 * 宝石镶嵌缓存联动监听器
 * 
 * <p>监听宝石镶嵌事件，自动刷新装备缓存。</p>
 * 
 * <p>功能：</p>
 * <ul>
 *   <li>镶嵌成功后使旧装备缓存失效</li>
 *   <li>预热新装备缓存</li>
 *   <li>记录缓存刷新日志</li>
 * </ul>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class GemInlayCacheListener implements Listener {

    private final GuangDianArmorStats plugin;
    private final EquipmentCacheManager cacheManager;

    public GemInlayCacheListener(GuangDianArmorStats plugin, EquipmentCacheManager cacheManager) {
        this.plugin = plugin;
        this.cacheManager = cacheManager;
    }

    /**
     * 监听宝石镶嵌事件
     * 
     * <p>处理流程：</p>
     * <ol>
     *   <li>计算旧装备哈希，使缓存失效</li>
     *   <li>预热新装备缓存（立即解析并缓存）</li>
     *   <li>记录缓存刷新日志</li>
     * </ol>
     * 
     * @param event 宝石镶嵌事件
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onGemInlay(GemInlayEvent event) {
        ItemStack equipmentBefore = event.getEquipmentBefore();
        ItemStack equipmentAfter = event.getEquipment();

        if (equipmentBefore == null || equipmentAfter == null) {
            return;
        }

        // 1. 使旧装备缓存失效
        String oldHash = EquipmentHash.calculate(equipmentBefore);
        if (oldHash != null && !oldHash.equals("EMPTY")) {
            cacheManager.invalidate(oldHash);
            plugin.getLogger().fine("[GemInlay] 已使旧装备缓存失效: " + oldHash);
        }

        // 2. 预热新装备缓存
        String newHash = EquipmentHash.calculate(equipmentAfter);
        if (newHash != null && !newHash.equals("EMPTY")) {
            // 直接获取会触发缓存
            cacheManager.getEquipmentStats(equipmentAfter);
            plugin.getLogger().fine("[GemInlay] 已预热新装备缓存: " + newHash);
        }

        // 3. 记录操作日志
        String operation = event.isRework() ? "拆卸" : "镶嵌";
        int gemCount = event.getTotalGems();
        
        if (plugin.getConfig().getBoolean("debug.cache_logging", false)) {
            plugin.getLogger().info("[GemInlay] 玩家 " + event.getPlayer().getName() 
                + " " + operation + " " + gemCount + " 颗宝石"
                + " | 旧缓存失效: " + oldHash
                + " | 新缓存预热: " + newHash);
        }

        // 4. 输出缓存统计（可选）
        if (plugin.getConfig().getBoolean("debug.show_cache_stats", false)) {
            var stats = cacheManager.getStats();
            plugin.getLogger().info("[CacheStats] " + stats.toString());
        }
    }
}