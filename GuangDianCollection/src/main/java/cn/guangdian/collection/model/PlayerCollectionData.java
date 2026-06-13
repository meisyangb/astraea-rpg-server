package cn.guangdian.collection.model;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCollectionData {
    
    private final UUID playerId;
    private final Map<String, CollectedEntry> collectedItems = new ConcurrentHashMap<>();
    
    // 枚举计数器：分类ID -> 已收集数量
    private final Map<String, Integer> categoryProgress = new ConcurrentHashMap<>();
    
    private int totalItemsCollected = 0;
    
    public PlayerCollectionData(UUID playerId) {
        this.playerId = playerId;
    }
    
    public UUID getPlayerId() { return playerId; }
    public Map<String, CollectedEntry> getCollectedItems() { return collectedItems; }
    public int getTotalItemsCollected() { return totalItemsCollected; }
    
    /**
     * 收集物品 - 直接更新枚举计数器
     */
    public boolean collectItem(String entryId, String categoryId) {
        if (collectedItems.containsKey(entryId)) {
            return false;
        }
        
        // 记录收集
        collectedItems.put(entryId, new CollectedEntry(entryId, System.currentTimeMillis()));
        totalItemsCollected++;
        
        // 枚举更新：直接增加分类进度
        categoryProgress.merge(categoryId, 1, Integer::sum);
        
        return true;
    }
    
    public boolean hasCollected(String entryId) {
        return collectedItems.containsKey(entryId);
    }
    
    /**
     * 获取分类进度 - 直接返回计数器值，无需遍历
     */
    public int getCategoryProgress(String categoryId) {
        return categoryProgress.getOrDefault(categoryId, 0);
    }
    
    /**
     * 判断分类是否完成 - 直接比较计数器
     */
    public boolean isCategoryComplete(String categoryId, int totalEntries) {
        return getCategoryProgress(categoryId) >= totalEntries;
    }
    
    /**
     * 从文件加载时恢复计数器
     */
    public void restoreCategoryProgress(String categoryId, int progress) {
        categoryProgress.put(categoryId, progress);
    }
    
    /**
     * 获取所有分类进度（用于保存）
     */
    public Map<String, Integer> getAllCategoryProgress() {
        return Collections.unmodifiableMap(categoryProgress);
    }
}
