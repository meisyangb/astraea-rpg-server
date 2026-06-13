package cn.guangdian.classsystem.event;

import cn.guangdian.classsystem.model.GameClass;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ClassAdvanceEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final GameClass fromClass;
    private final GameClass toClass;
    private final int advancementLevel;
    private boolean cancelled = false;
    
    public ClassAdvanceEvent(Player player, GameClass fromClass, GameClass toClass, int advancementLevel) {
        this.player = player;
        this.fromClass = fromClass;
        this.toClass = toClass;
        this.advancementLevel = advancementLevel;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public GameClass getFromClass() {
        return fromClass;
    }
    
    public GameClass getToClass() {
        return toClass;
    }
    
    public int getAdvancementLevel() {
        return advancementLevel;
    }
    
    public boolean isDivineAdvancement() {
        return advancementLevel == 4;
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
