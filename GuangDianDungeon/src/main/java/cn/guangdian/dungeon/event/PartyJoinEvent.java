package cn.guangdian.dungeon.event;

import cn.guangdian.dungeon.model.DungeonParty;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PartyJoinEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final DungeonParty party;
    private final Player player;
    private boolean cancelled = false;

    public PartyJoinEvent(DungeonParty party, Player player) {
        this.party = party;
        this.player = player;
    }

    public DungeonParty getParty() { return party; }
    public Player getPlayer() { return player; }

    @Override
    public boolean isCancelled() { return cancelled; }
    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
