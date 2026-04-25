package cn.guangdian.npc.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * NPC 交互事件
 *
 * <p>当玩家与 NPC 交互时触发。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class NPCInteractEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String npcId;
    private final String npcName;
    private final InteractType interactType;
    private boolean cancelled = false;

    public enum InteractType {
        RIGHT_CLICK,    // 右键点击
        LEFT_CLICK,     // 左键点击
        SHIFT_RIGHT,    // 蹲下右键
        SHIFT_LEFT      // 蹲下左键
    }

    public NPCInteractEvent(Player player, String npcId, String npcName, InteractType interactType) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.npcId = npcId;
        this.npcName = npcName;
        this.interactType = interactType;
    }

    public Player getPlayer() {
        return player;
    }

    public String getNpcId() {
        return npcId;
    }

    public String getNpcName() {
        return npcName;
    }

    public InteractType getInteractType() {
        return interactType;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
