package cn.guangdian.armorstats.manager;

import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicCacheManager {

    private static class ItemHash {
        private final String type;
        private final String loreHash;

        public ItemHash(ItemStack item) {
            if (item == null) {
                this.type = "";
                this.loreHash = "";
            } else {
                this.type = item.getType().name();
                this.loreHash = item.hasItemMeta() && item.getItemMeta().hasLore() ?
                        item.getItemMeta().getLore().toString() : "";
            }
        }

        @Override
        public int hashCode() {
            return (type + loreHash).hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ItemHash other = (ItemHash) obj;
            return type.equals(other.type) && loreHash.equals(other.loreHash);
        }
    }

    public class UsageStats {
        public int daysUsed;
        public long lastUsed;
        public boolean isPersistent;

        public UsageStats() {
            this.daysUsed = 1;
            this.lastUsed = System.currentTimeMillis();
            this.isPersistent = false;
        }
    }

    private final Map<UUID, Map<ItemHash, UsageStats>> usageHistory = new ConcurrentHashMap<>();

    public UsageStats recordUsage(UUID playerId, ItemStack item) {
        if (item == null) return null;

        ItemHash itemHash = new ItemHash(item);
        Map<ItemHash, UsageStats> playerUsage = usageHistory.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        
        UsageStats stats = playerUsage.computeIfAbsent(itemHash, k -> new UsageStats());
        stats.lastUsed = System.currentTimeMillis();
        
        // 检查是否需要增加天数使用计数
        long dayThreshold = 24 * 60 * 60 * 1000;
        if (System.currentTimeMillis() - stats.lastUsed > dayThreshold) {
            stats.daysUsed++;
        }
        
        // 超过7天每天使用，转为持久缓存
        if (stats.daysUsed >= 7 && !stats.isPersistent) {
            stats.isPersistent = true;
        }
        
        return stats;
    }

    public boolean shouldBePersistent(UUID playerId, ItemStack item) {
        if (item == null) return false;

        ItemHash itemHash = new ItemHash(item);
        Map<ItemHash, UsageStats> playerUsage = usageHistory.get(playerId);
        
        if (playerUsage == null) return false;
        
        UsageStats stats = playerUsage.get(itemHash);
        return stats != null && stats.isPersistent;
    }

    public void removePlayer(UUID playerId) {
        usageHistory.remove(playerId);
    }

    public void clear() {
        usageHistory.clear();
    }
}