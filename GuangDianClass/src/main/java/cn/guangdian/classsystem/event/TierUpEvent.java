package cn.guangdian.classsystem.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TierUpEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final int fromTier;
    private final int toTier;
    private boolean cancelled = false;
    
    public TierUpEvent(Player player, int fromTier, int toTier) {
        this.player = player;
        this.fromTier = fromTier;
        this.toTier = toTier;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public int getFromTier() {
        return fromTier;
    }
    
    public int getToTier() {
        return toTier;
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
