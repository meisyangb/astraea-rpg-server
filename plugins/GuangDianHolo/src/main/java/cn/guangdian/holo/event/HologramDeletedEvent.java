package cn.guangdian.holo.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * 全息图删除事件
 *
 * <p>当全息图被删除时触发。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class HologramDeletedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String hologramId;
    private final String hologramName;
    private final Location location;

    public HologramDeletedEvent(String hologramId, String hologramName, Location location) {
        super(!Bukkit.isPrimaryThread());
        this.hologramId = hologramId;
        this.hologramName = hologramName;
        this.location = location;
    }

    public String getHologramId() {
        return hologramId;
    }

    public String getHologramName() {
        return hologramName;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
