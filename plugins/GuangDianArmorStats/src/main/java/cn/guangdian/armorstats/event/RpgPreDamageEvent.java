package cn.guangdian.armorstats.event;

import cn.guangdian.armorstats.combat.DamageContext;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RpgPreDamageEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final DamageContext context;
    private boolean cancelled = false;
    
    public RpgPreDamageEvent(DamageContext context) {
        this.context = context;
    }
    
    public DamageContext getContext() {
        return context;
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
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
