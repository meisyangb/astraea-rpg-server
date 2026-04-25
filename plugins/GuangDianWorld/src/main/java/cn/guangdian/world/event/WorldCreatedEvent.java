package cn.guangdian.world.event;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 世界创建事件
 *
 * <p>当世界被创建时触发。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class WorldCreatedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String worldName;
    private final World.Environment environment;
    private final UUID creator;

    public WorldCreatedEvent(String worldName, World.Environment environment, UUID creator) {
        super(!Bukkit.isPrimaryThread());
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

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
