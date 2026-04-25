package cn.guangdian.armorstats.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * 玩家满血事件
 * 
 * <p>当玩家的血量达到最大值时触发此事件。
 * 专门用于满血时的特殊处理，如触发特效、更新显示等。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class PlayerFullHealthEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final UUID playerId;
    private final String playerName;
    private final double maxHealth;
    private final FullHealthReason reason;
    private final long timestamp;

    /**
     * 满血原因
     */
    public enum FullHealthReason {
        REGEN,          // 自然回血达到满血
        HEAL,           // 治疗/药水达到满血
        SKILL,          // 技能效果达到满血
        RESPAWN,        // 重生后满血
        COMMAND,        // 命令恢复满血
        EQUIPMENT,      // 装备变化导致满血
        LOGIN,          // 登录时满血
        OTHER           // 其他原因
    }

    /**
     * 创建满血事件
     * 
     * @param playerId 玩家UUID
     * @param playerName 玩家名称
     * @param maxHealth 最大血量
     * @param reason 满血原因
     */
    public PlayerFullHealthEvent(UUID playerId, String playerName,
                                 double maxHealth, FullHealthReason reason) {
        super(!Bukkit.isPrimaryThread());
        this.playerId = playerId;
        this.playerName = playerName;
        this.maxHealth = maxHealth;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public FullHealthReason getReason() {
        return reason;
    }

    public long getTimestamp() {
        return timestamp;
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
        return "PlayerFullHealthEvent";
    }

    @Override
    public String toString() {
        return String.format("PlayerFullHealthEvent{player=%s, maxHealth=%.1f, reason=%s}",
            playerName, maxHealth, reason);
    }
}
