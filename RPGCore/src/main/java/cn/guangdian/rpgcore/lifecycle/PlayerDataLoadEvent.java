package cn.guangdian.rpgcore.lifecycle;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerDataLoadEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String playerName;
    private final java.util.UUID playerId;
    
    public PlayerDataLoadEvent(Player player) {
        this.player = player;
        this.playerName = player.getName();
        this.playerId = player.getUniqueId();
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
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
