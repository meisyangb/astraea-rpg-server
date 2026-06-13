package cn.guangdian.dungeon.event;

import cn.guangdian.dungeon.model.DungeonParty;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PartyDisbandEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final DungeonParty party;

    public PartyDisbandEvent(DungeonParty party) {
        this.party = party;
    }

    public DungeonParty getParty() {
        return party;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
