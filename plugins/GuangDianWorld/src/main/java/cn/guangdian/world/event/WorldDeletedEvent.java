package cn.guangdian.world.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 世界删除事件
 *
 * <p>当世界被删除时触发。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class WorldDeletedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String worldName;
    private final UUID deleter;

    public WorldDeletedEvent(String worldName, UUID deleter) {
        super(!Bukkit.isPrimaryThread());
        this.worldName = worldName;
        this.deleter = deleter;
    }

    public String getWorldName() {
        return worldName;
    }

    public UUID getDeleter() {
        return deleter;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
