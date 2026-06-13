package cn.guangdian.battlepass.model;

import java.util.*;

public class PlayerBattlePass {
    
    private UUID playerId;
    private int seasonId;
    private int level;
    private int currentExp;
    private int totalExp;
    private boolean premium;
    private long premiumPurchaseTime;
    private Set<Integer> claimedFreeRewards;
    private Set<Integer> claimedPremiumRewards;
    private Map<String, Integer> taskProgress;
    private long lastUpdateTime;
    
    public PlayerBattlePass() {
        this.claimedFreeRewards = new HashSet<>();
        this.claimedPremiumRewards = new HashSet<>();
        this.taskProgress = new HashMap<>();
        this.level = 1;
        this.currentExp = 0;
        this.totalExp = 0;
        this.premium = false;
    }
    
    public PlayerBattlePass(UUID playerId, int seasonId) {
        this();
        this.playerId = playerId;
        this.seasonId = seasonId;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public boolean canClaimFreeReward(int level) {
        return this.level >= level && !claimedFreeRewards.contains(level);
    }
    
    public boolean canClaimPremiumReward(int level) {
        return this.level >= level && premium && !claimedPremiumRewards.contains(level);
    }
    
    public void claimFreeReward(int level) {
        claimedFreeRewards.add(level);
    }
    
    public void claimPremiumReward(int level) {
        claimedPremiumRewards.add(level);
    }
    
    public boolean hasClaimedFreeReward(int level) {
        return claimedFreeRewards.contains(level);
    }
    
    public boolean hasClaimedPremiumReward(int level) {
        return claimedPremiumRewards.contains(level);
    }
    
    public int getUnclaimedFreeRewards(int maxLevel) {
        int count = 0;
        for (int i = 1; i <= Math.min(level, maxLevel); i++) {
            if (!claimedFreeRewards.contains(i)) {
                count++;
            }
        }
        return count;
    }
    
    public int getUnclaimedPremiumRewards(int maxLevel) {
        if (!premium) return 0;
        int count = 0;
        for (int i = 1; i <= Math.min(level, maxLevel); i++) {
            if (!claimedPremiumRewards.contains(i)) {
                count++;
            }
        }
        return count;
    }
    
    public void addExp(int exp) {
        this.currentExp += exp;
        this.totalExp += exp;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public void levelUp() {
        this.level++;
        this.currentExp = 0;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }
    
    public int getSeasonId() {
        return seasonId;
    }
    
    public void setSeasonId(int seasonId) {
        this.seasonId = seasonId;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public int getCurrentExp() {
        return currentExp;
    }
    
    public void setCurrentExp(int currentExp) {
        this.currentExp = currentExp;
    }
    
    public int getTotalExp() {
        return totalExp;
    }
    
    public void setTotalExp(int totalExp) {
        this.totalExp = totalExp;
    }
    
    public boolean isPremium() {
        return premium;
    }
    
    public void setPremium(boolean premium) {
        this.premium = premium;
        if (premium) {
            this.premiumPurchaseTime = System.currentTimeMillis();
        }
    }
    
    public long getPremiumPurchaseTime() {
        return premiumPurchaseTime;
    }
    
    public void setPremiumPurchaseTime(long premiumPurchaseTime) {
        this.premiumPurchaseTime = premiumPurchaseTime;
    }
    
    public Set<Integer> getClaimedFreeRewards() {
        return claimedFreeRewards;
    }
    
    public void setClaimedFreeRewards(Set<Integer> claimedFreeRewards) {
        this.claimedFreeRewards = claimedFreeRewards;
    }
    
    public Set<Integer> getClaimedPremiumRewards() {
        return claimedPremiumRewards;
    }
    
    public void setClaimedPremiumRewards(Set<Integer> claimedPremiumRewards) {
        this.claimedPremiumRewards = claimedPremiumRewards;
    }
    
    public Map<String, Integer> getTaskProgress() {
        return taskProgress;
    }
    
    public void setTaskProgress(Map<String, Integer> taskProgress) {
        this.taskProgress = taskProgress;
    }
    
    public void setTaskProgress(String taskId, int progress) {
        taskProgress.put(taskId, progress);
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public int getTaskProgress(String taskId) {
        return taskProgress.getOrDefault(taskId, 0);
    }
    
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}
