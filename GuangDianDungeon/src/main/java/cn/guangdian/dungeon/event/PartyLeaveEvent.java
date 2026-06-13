package cn.guangdian.dungeon.event;

import cn.guangdian.dungeon.model.DungeonParty;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PartyLeaveEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final DungeonParty party;
    private final Player player;

    public PartyLeaveEvent(DungeonParty party, Player player) {
        this.party = party;
        this.player = player;
    }

    public DungeonParty getParty() { return party; }
    public Player getPlayer() { return player; }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
