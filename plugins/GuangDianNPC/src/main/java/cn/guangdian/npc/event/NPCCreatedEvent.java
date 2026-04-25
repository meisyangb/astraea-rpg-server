package cn.guangdian.npc.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * NPC 创建事件
 *
 * <p>当 NPC 被创建时触发。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class NPCCreatedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String npcId;
    private final String npcName;
    private final Location location;
    private final String npcType;

    public NPCCreatedEvent(String npcId, String npcName, Location location, String npcType) {
        super(!Bukkit.isPrimaryThread());
        this.npcId = npcId;
        this.npcName = npcName;
        this.location = location;
        this.npcType = npcType;
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

    public String getNpcType() {
        return npcType;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
