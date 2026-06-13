package cn.guangdian.rpgcore.event.events;

import cn.guangdian.rpgcore.event.CoreEvent;
import org.bukkit.Location;

import java.util.UUID;

public class NPCCreatedEvent extends CoreEvent {

    private final String npcId;
    private final String npcName;
    private final Location location;
    private final UUID creator;

    public NPCCreatedEvent(String npcId, String npcName, Location location, UUID creator) {
        super(false);
        this.npcId = npcId;
        this.npcName = npcName;
        this.location = location;
        this.creator = creator;
    }

    public String getNpcId() {
        return npcId;
    }

    public String getNpcName() {
        return npcName;
    }

    public Location getLocation() {
        return location;
    }

    public UUID getCreator() {
        return creator;
    }
}
