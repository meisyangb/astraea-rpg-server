package cn.guangdian.classsystem.event;

import cn.guangdian.classsystem.model.GameClass;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ClassResetEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final GameClass previousClass;
    private final int previousTier;
    private final int previousAdvancement;
    
    public ClassResetEvent(Player player, GameClass previousClass, int previousTier, int previousAdvancement) {
        this.player = player;
        this.previousClass = previousClass;
        this.previousTier = previousTier;
        this.previousAdvancement = previousAdvancement;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public GameClass getPreviousClass() {
        return previousClass;
    }
    
    public int getPreviousTier() {
        return previousTier;
    }
    
    public int getPreviousAdvancement() {
        return previousAdvancement;
    }
    
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
