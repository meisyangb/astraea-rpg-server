package cn.guangdian.rpgcore.module.points;

import java.util.UUID;

/**
 * 玩家点券数据模型
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class PlayerPointsData {

    private final UUID playerId;
    private long balance;
    private long totalEarned;
    private long totalSpent;
    private long createdAt;
    private long updatedAt;

    /**
     * 创建玩家点券数据
     * 
     * @param playerId 玩家UUID
     * @param balance 初始余额
     */
    public PlayerPointsData(UUID playerId, long balance) {
        this.playerId = playerId;
        this.balance = balance;
        this.totalEarned = 0;
        this.totalSpent = 0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    /**
     * 创建默认数据
     * 
     * @param playerId 玩家UUID
     */
    public PlayerPointsData(UUID playerId) {
        this(playerId, 0);
    }

    // ==================== Getters ====================

    public UUID getPlayerId() {
        return playerId;
    }

    public long getBalance() {
        return balance;
    }

    public long getTotalEarned() {
        return totalEarned;
    }

    public long getTotalSpent() {
        return totalSpent;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    // ==================== Setters（用于数据加载） ====================

    /**
     * 设置总获得点券 - 仅用于数据加载
     */
    public void setTotalEarned(long totalEarned) {
        this.totalEarned = Math.max(0, totalEarned);
    }

    /**
     * 设置总消费点券 - 仅用于数据加载
     */
    public void setTotalSpent(long totalSpent) {
        this.totalSpent = Math.max(0, totalSpent);
    }

    /**
     * 设置创建时间 - 仅用于数据加载
     */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    // ==================== 操作方法 ====================

    /**
     * 设置余额
     */
    public void setBalance(long balance) {
        this.balance = Math.max(0, balance);
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 增加余额
     */
    public void addBalance(long amount) {
        if (amount > 0) {
            this.balance += amount;
            this.totalEarned += amount;
            this.updatedAt = System.currentTimeMillis();
        }
    }

    /**
     * 扣除余额
     * 
     * @return 如果扣除成功返回 true
     */
    public boolean removeBalance(long amount) {
        if (amount <= 0 || this.balance < amount) {
            return false;
        }
        this.balance -= amount;
        this.totalSpent += amount;
        this.updatedAt = System.currentTimeMillis();
        return true;
    }

    /**
     * 检查是否有足够余额
     */
    public boolean hasBalance(long amount) {
        return this.balance >= amount && amount > 0;
    }

    @Override
    public String toString() {
        return "PlayerPointsData{" +
            "playerId=" + playerId +
            ", balance=" + balance +
            ", totalEarned=" + totalEarned +
            ", totalSpent=" + totalSpent +
            '}';
    }
}