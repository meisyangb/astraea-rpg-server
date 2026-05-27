package cn.guangdian.rpgcore.event.events;

import cn.guangdian.rpgcore.event.CoreEvent;
import org.bukkit.Location;

import java.util.UUID;

public class HologramCreatedEvent extends CoreEvent {

    private final String hologramName;
    private final Location location;
    private final UUID creator;

    public HologramCreatedEvent(String hologramName, Location location, UUID creator) {
        super(false);
        this.hologramName = hologramName;
        this.location = location;
        this.creator = creator;
    }

    public String getHologramName() {
        return hologramName;
    }

    public Location getLocation() {
        return location;
    }

    public UUID getCreator() {
        return creator;
    }
}
