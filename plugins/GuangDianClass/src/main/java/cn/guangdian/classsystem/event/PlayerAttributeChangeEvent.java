package cn.guangdian.classsystem.event;

import cn.guangdian.classsystem.model.AttributeType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerAttributeChangeEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final AttributeType attributeType;
    private final int oldValue;
    private final int newValue;
    private final int delta;
    private final ChangeType changeType;
    
    public enum ChangeType {
        ALLOCATE,
        DEALLOCATE,
        RESET
    }
    
    public PlayerAttributeChangeEvent(Player player, AttributeType attributeType, 
                                       int oldValue, int newValue, ChangeType changeType) {
        this.player = player;
        this.attributeType = attributeType;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.delta = newValue - oldValue;
        this.changeType = changeType;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public AttributeType getAttributeType() {
        return attributeType;
    }
    
    public int getOldValue() {
        return oldValue;
    }
    
    public int getNewValue() {
        return newValue;
    }
    
    public int getDelta() {
        return delta;
    }
    
    public ChangeType getChangeType() {
        return changeType;
    }
    
    public boolean isIncrease() {
        return delta > 0;
    }
    
    public boolean isDecrease() {
        return delta < 0;
    }
    
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
