package cn.guangdian.armorstats.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * 玩家血量变化事件
 * 
 * <p>当玩家的血量发生变化时触发此事件。
 * 专门用于血量显示更新，比 PlayerStatsChangedEvent 更轻量。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class PlayerHealthChangedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final UUID playerId;
    private final String playerName;
    private final double oldHealth;
    private final double newHealth;
    private final double maxHealth;
    private final ChangeReason changeReason;
    private final long timestamp;

    /**
     * 血量变化原因
     */
    public enum ChangeReason {
        DAMAGE,         // 受到伤害
        REGEN,          // 自然回血
        HEAL,           // 治疗/药水
        SKILL,          // 技能效果
        EQUIPMENT,      // 装备变化
        DEATH,          // 死亡
        RESPAWN,        // 重生
        OTHER           // 其他原因
    }

    /**
     * 创建血量变化事件
     * 
     * @param playerId 玩家UUID
     * @param playerName 玩家名称
     * @param oldHealth 旧血量
     * @param newHealth 新血量
     * @param maxHealth 最大血量
     * @param changeReason 变化原因
     */
    public PlayerHealthChangedEvent(UUID playerId, String playerName,
                                    double oldHealth, double newHealth,
                                    double maxHealth, ChangeReason changeReason) {
        super(!Bukkit.isPrimaryThread());
        this.playerId = playerId;
        this.playerName = playerName;
        this.oldHealth = oldHealth;
        this.newHealth = newHealth;
        this.maxHealth = maxHealth;
        this.changeReason = changeReason;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建简化版血量变化事件
     * 
     * @param playerId 玩家UUID
     * @param playerName 玩家名称
     * @param newHealth 新血量
     * @param maxHealth 最大血量
     */
    public PlayerHealthChangedEvent(UUID playerId, String playerName,
                                    double newHealth, double maxHealth) {
        this(playerId, playerName, 0, newHealth, maxHealth, ChangeReason.OTHER);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public double getOldHealth() {
        return oldHealth;
    }

    public double getNewHealth() {
        return newHealth;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public ChangeReason getChangeReason() {
        return changeReason;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 获取血量变化量
     * 
     * @return 正数表示回血，负数表示掉血
     */
    public double getHealthDelta() {
        return newHealth - oldHealth;
    }

    /**
     * 检查是否是回血
     */
    public boolean isHealing() {
        return newHealth > oldHealth;
    }

    /**
     * 检查是否是掉血
     */
    public boolean isDamaged() {
        return newHealth < oldHealth;
    }

    /**
     * 获取血量百分比
     * 
     * @return 0.0 - 1.0 之间的值
     */
    public double getHealthPercent() {
        if (maxHealth <= 0) return 0.0;
        return Math.min(1.0, Math.max(0.0, newHealth / maxHealth));
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public String getEventName() {
        return "PlayerHealthChangedEvent";
    }

    @Override
    public String toString() {
        return String.format("PlayerHealthChangedEvent{player=%s, health=%.1f->%.1f/%.1f (%.1f%%), reason=%s}",
            playerName, oldHealth, newHealth, maxHealth, getHealthPercent() * 100, changeReason);
    }
}
