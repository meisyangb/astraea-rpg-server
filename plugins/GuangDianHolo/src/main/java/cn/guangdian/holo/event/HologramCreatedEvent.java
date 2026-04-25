package cn.guangdian.holo.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * 全息图创建事件
 *
 * <p>当全息图被创建时触发。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class HologramCreatedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String hologramId;
    private final String hologramName;
    private final Location location;
    private final int lineCount;

    public HologramCreatedEvent(String hologramId, String hologramName, Location location, int lineCount) {
        super(!Bukkit.isPrimaryThread());
        this.hologramId = hologramId;
        this.hologramName = hologramName;
        this.location = location;
        this.lineCount = lineCount;
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

    public int getLineCount() {
        return lineCount;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
