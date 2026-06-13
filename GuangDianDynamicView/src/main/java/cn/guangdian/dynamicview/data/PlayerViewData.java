package cn.guangdian.dynamicview.data;

import java.util.UUID;

/**
 * 玩家视距数据
 */
public class PlayerViewData {

    /**
     * 视距挡位
     */
    public enum ViewTier {
        EXPLORING,  // 跑图
        COMBAT,     // 战斗
        AFK         // 挂机
    }

    private final UUID uuid;
    private ViewTier currentTier;
    private long lastCombatTime;
    private long lastActivityTime;

    // 渐进式调整相关
    private int targetViewDistance;      // 目标视距
    private int currentViewDistance;     // 当前实际视距
    private long lastAdjustTime;         // 上次调整时间
    private long lastTierChangeTime;     // 上次挡位切换时间

    public PlayerViewData(UUID uuid) {
        this.uuid = uuid;
        this.currentTier = ViewTier.EXPLORING;
        this.lastActivityTime = System.currentTimeMillis();
        this.lastAdjustTime = 0;
        this.lastTierChangeTime = 0;
        this.currentViewDistance = -1; // -1 表示未初始化
        this.targetViewDistance = -1;
    }

    // Getters and Setters
    public UUID getUuid() { return uuid; }
    public ViewTier getCurrentTier() { return currentTier; }
    public void setCurrentTier(ViewTier currentTier) {
        if (this.currentTier != currentTier) {
            this.lastTierChangeTime = System.currentTimeMillis();
        }
        this.currentTier = currentTier;
    }
    public long getLastCombatTime() { return lastCombatTime; }
    public void setLastCombatTime(long lastCombatTime) { this.lastCombatTime = lastCombatTime; }
    public long getLastActivityTime() { return lastActivityTime; }
    public void setLastActivityTime(long lastActivityTime) { this.lastActivityTime = lastActivityTime; }

    // 渐进式调整相关
    public int getTargetViewDistance() { return targetViewDistance; }
    public void setTargetViewDistance(int targetViewDistance) { this.targetViewDistance = targetViewDistance; }
    public int getCurrentViewDistance() { return currentViewDistance; }
    public void setCurrentViewDistance(int currentViewDistance) { this.currentViewDistance = currentViewDistance; }
    public long getLastAdjustTime() { return lastAdjustTime; }
    public void setLastAdjustTime(long lastAdjustTime) { this.lastAdjustTime = lastAdjustTime; }
    public long getLastTierChangeTime() { return lastTierChangeTime; }

    /**
     * 是否需要调整视距
     */
    public boolean needsAdjustment() {
        return currentViewDistance != targetViewDistance;
    }

    /**
     * 是否已初始化
     */
    public boolean isInitialized() {
        return currentViewDistance >= 0;
    }
}
