package cn.guangdian.armorstats.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 玩家属性变化事件
 *
 * <p>当玩家的RPG属性发生变化时触发。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class PlayerStatsChangedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final String playerName;
    private final double oldHealth;
    private final double newHealth;
    private final double oldAttack;
    private final double newAttack;
    private final double oldDefense;
    private final double newDefense;
    private final long timestamp;

    public PlayerStatsChangedEvent(UUID playerId, String playerName,
                                   double oldHealth, double newHealth,
                                   double oldAttack, double newAttack,
                                   double oldDefense, double newDefense) {
        super(!Bukkit.isPrimaryThread());
        this.playerId = playerId;
        this.playerName = playerName;
        this.oldHealth = oldHealth;
        this.newHealth = newHealth;
        this.oldAttack = oldAttack;
        this.newAttack = newAttack;
        this.oldDefense = oldDefense;
        this.newDefense = newDefense;
        this.timestamp = System.currentTimeMillis();
    }

    public PlayerStatsChangedEvent(UUID playerId, String playerName) {
        this(playerId, playerName, 0, 0, 0, 0, 0, 0);
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

    public double getOldAttack() {
        return oldAttack;
    }

    public double getNewAttack() {
        return newAttack;
    }

    public double getOldDefense() {
        return oldDefense;
    }

    public double getNewDefense() {
        return newDefense;
    }

    public boolean healthChanged() {
        return oldHealth != newHealth;
    }

    public boolean attackChanged() {
        return oldAttack != newAttack;
    }

    public boolean defenseChanged() {
        return oldDefense != newDefense;
    }

    public boolean hasAnyChange() {
        return healthChanged() || attackChanged() || defenseChanged();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public String toString() {
        return String.format("PlayerStatsChangedEvent{player=%s, health=%.0f->%.0f, attack=%.0f->%.0f, defense=%.0f->%.0f}",
            playerName, oldHealth, newHealth, oldAttack, newAttack, oldDefense, newDefense);
    }
}
