package cn.guangdian.collection.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCollectionData {
    
    private final UUID playerId;
    private final Map<String, CollectedEntry> collectedItems = new ConcurrentHashMap<>();
    
    private int totalItemsCollected = 0;
    private boolean dirty = false;
    
    public PlayerCollectionData(UUID playerId) {
        this.playerId = playerId;
    }
    
    public UUID getPlayerId() { return playerId; }
    public Map<String, CollectedEntry> getCollectedItems() { return collectedItems; }
    public int getTotalItemsCollected() { return totalItemsCollected; }
    public boolean isDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }
    
    public boolean collectItem(String entryId) {
        if (collectedItems.containsKey(entryId)) {
            return false;
        }
        collectedItems.put(entryId, new CollectedEntry(entryId, System.currentTimeMillis()));
        totalItemsCollected++;
        dirty = true;
        return true;
    }
    
    public boolean hasCollected(String entryId) {
        return collectedItems.containsKey(entryId);
    }
    
    public int getCategoryProgress(String categoryId, CollectionCategory category) {
        int collected = 0;
        for (String entryId : category.getEntries().keySet()) {
            if (hasCollected(categoryId + "." + entryId)) {
                collected++;
            }
        }
        return collected;
    }
    
    public boolean isCategoryComplete(String categoryId, CollectionCategory category) {
        return getCategoryProgress(categoryId, category) >= category.getTotalEntries();
    }
}
