package cn.guangdian.rpgcore.lifecycle;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerDataSaveEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String playerName;
    private final java.util.UUID playerId;
    private final boolean isAsync;
    
    public PlayerDataSaveEvent(Player player, boolean isAsync) {
        super(isAsync);
        this.player = player;
        this.playerName = player.getName();
        this.playerId = player.getUniqueId();
        this.isAsync = isAsync;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public java.util.UUID getPlayerId() {
        return playerId;
    }
    
    public boolean isAsync() {
        return isAsync;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
