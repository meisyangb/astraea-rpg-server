package cn.guangdian.collection.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerCollectionData {
    
    private final UUID playerId;
    private final Map<String, CollectedEntry> collectedItems = new HashMap<>();
    private final Map<String, KillRecord> killRecords = new HashMap<>();
    private final List<String> claimedRewards = new ArrayList<>();
    
    private int totalItemsCollected = 0;
    private int totalKills = 0;
    private boolean dirty = false;
    
    public PlayerCollectionData(UUID playerId) {
        this.playerId = playerId;
    }
    
    public UUID getPlayerId() { return playerId; }
    public Map<String, CollectedEntry> getCollectedItems() { return collectedItems; }
    public Map<String, KillRecord> getKillRecords() { return killRecords; }
    public List<String> getClaimedRewards() { return claimedRewards; }
    public int getTotalItemsCollected() { return totalItemsCollected; }
    public int getTotalKills() { return totalKills; }
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
    
    public int addKill(String entryId) {
        KillRecord record = killRecords.computeIfAbsent(entryId, id -> new KillRecord(id));
        record.addKill();
        totalKills++;
        dirty = true;
        return record.getKillCount();
    }
    
    public int getKillCount(String entryId) {
        KillRecord record = killRecords.get(entryId);
        return record != null ? record.getKillCount() : 0;
    }
    
    public boolean isKillTargetMet(String entryId, int target) {
        return getKillCount(entryId) >= target;
    }
    
    public void claimReward(String rewardId) {
        if (!claimedRewards.contains(rewardId)) {
            claimedRewards.add(rewardId);
            dirty = true;
        }
    }
    
    public boolean hasClaimedReward(String rewardId) {
        return claimedRewards.contains(rewardId);
    }
    
    public int getCategoryProgress(String categoryId, CollectionCategory category) {
        int collected = 0;
        for (String entryId : category.getEntries().keySet()) {
            if (category.getType() == CollectionCategory.CategoryType.MOB_KILL) {
                CollectionEntry entry = category.getEntry(entryId);
                if (isKillTargetMet(entryId, entry.getTargetCount())) {
                    collected++;
                }
            } else {
                if (hasCollected(categoryId + "." + entryId)) {
                    collected++;
                }
            }
        }
        return collected;
    }
    
    public boolean isCategoryComplete(String categoryId, CollectionCategory category) {
        return getCategoryProgress(categoryId, category) >= category.getTotalEntries();
    }
}
