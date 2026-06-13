package cn.guangdian.killaura.model;

import java.util.UUID;

public class KillAuraProfile {

    private final UUID playerId;
    private boolean enabled;
    private TargetStrategy strategy;
    private double attackRange;
    private long attackIntervalTicks;
    private long lastAttackTime;
    private int killCount;
    private double totalDamage;

    public KillAuraProfile(UUID playerId, double defaultRange, long defaultIntervalTicks) {
        this.playerId = playerId;
        this.enabled = false;
        this.strategy = TargetStrategy.NEAREST;
        this.attackRange = defaultRange;
        this.attackIntervalTicks = defaultIntervalTicks;
        this.lastAttackTime = 0;
        this.killCount = 0;
        this.totalDamage = 0;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public TargetStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(TargetStrategy strategy) {
        this.strategy = strategy;
    }

    public double getAttackRange() {
        return attackRange;
    }

    public void setAttackRange(double attackRange) {
        this.attackRange = attackRange;
    }

    public long getAttackIntervalTicks() {
        return attackIntervalTicks;
    }

    public void setAttackIntervalTicks(long attackIntervalTicks) {
        this.attackIntervalTicks = attackIntervalTicks;
    }

    public long getLastAttackTime() {
        return lastAttackTime;
    }

    public void setLastAttackTime(long lastAttackTime) {
        this.lastAttackTime = lastAttackTime;
    }

    public int getKillCount() {
        return killCount;
    }

    public void incrementKillCount() {
        this.killCount++;
    }

    public void addDamage(double damage) {
        this.totalDamage += damage;
    }

    public double getTotalDamage() {
        return totalDamage;
    }

    public void resetStats() {
        this.killCount = 0;
        this.totalDamage = 0;
    }
}
