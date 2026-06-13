package cn.guangdian.classsystem.event;

import cn.guangdian.classsystem.model.GameClass;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ClassChooseEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final GameClass gameClass;
    private final boolean isNewPlayer;
    private boolean cancelled = false;
    
    public ClassChooseEvent(Player player, GameClass gameClass, boolean isNewPlayer) {
        this.player = player;
        this.gameClass = gameClass;
        this.isNewPlayer = isNewPlayer;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public GameClass getGameClass() {
        return gameClass;
    }
    
    public boolean isNewPlayer() {
        return isNewPlayer;
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
