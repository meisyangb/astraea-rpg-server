package cn.guangdian.classsystem.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ExpGainEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private long amount;
    private final ExpSource source;
    private boolean cancelled = false;
    
    public enum ExpSource {
        MOB_KILL,
        MONSTER_KILL,  // 兼容旧版
        QUEST_COMPLETE,
        BLOCK_BREAK,
        COMMAND,
        OTHER
    }
    
    public ExpGainEvent(Player player, long amount, ExpSource source) {
        this.player = player;
        this.amount = amount;
        this.source = source;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public long getAmount() {
        return amount;
    }
    
    public void setAmount(long amount) {
        this.amount = Math.max(0, amount);
    }
    
    public ExpSource getSource() {
        return source;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
    
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
