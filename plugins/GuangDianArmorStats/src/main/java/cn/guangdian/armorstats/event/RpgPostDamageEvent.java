package cn.guangdian.armorstats.event;

import cn.guangdian.armorstats.combat.DamageContext;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RpgPostDamageEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final DamageContext context;
    private final double damageDealt;
    private final boolean killed;
    
    public RpgPostDamageEvent(DamageContext context, double damageDealt, boolean killed) {
        this.context = context;
        this.damageDealt = damageDealt;
        this.killed = killed;
    }
    
    public DamageContext getContext() {
        return context;
    }
    
    public double getDamageDealt() {
        return damageDealt;
    }
    
    public boolean hasKilled() {
        return killed;
    }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
