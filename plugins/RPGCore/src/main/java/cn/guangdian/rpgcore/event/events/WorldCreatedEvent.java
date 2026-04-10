package cn.guangdian.rpgcore.event.events;

import cn.guangdian.rpgcore.event.CoreEvent;
import org.bukkit.World;

import java.util.UUID;

public class WorldCreatedEvent extends CoreEvent {

    private final String worldName;
    private final World.Environment environment;
    private final UUID creator;

    public WorldCreatedEvent(String worldName, World.Environment environment, UUID creator) {
        super(false);
        this.worldName = worldName;
        this.environment = environment;
        this.creator = creator;
    }

    public String getWorldName() {
        return worldName;
    }

    public World.Environment getEnvironment() {
        return environment;
    }

    public UUID getCreator() {
        return creator;
    }
}
