package cn.guangdian.classsystem.event;

import cn.guangdian.classsystem.model.AttributeType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AttributeAllocateEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final AttributeType attributeType;
    private int points;
    private final boolean isAllocating;
    private boolean cancelled = false;
    
    public AttributeAllocateEvent(Player player, AttributeType attributeType, int points, boolean isAllocating) {
        this.player = player;
        this.attributeType = attributeType;
        this.points = points;
        this.isAllocating = isAllocating;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public AttributeType getAttributeType() {
        return attributeType;
    }
    
    public int getPoints() {
        return points;
    }
    
    public void setPoints(int points) {
        this.points = Math.max(0, points);
    }
    
    public boolean isAllocating() {
        return isAllocating;
    }
    
    public boolean isDeallocating() {
        return !isAllocating;
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
