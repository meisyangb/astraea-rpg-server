package cn.guangdian.accessory.event;

import cn.guangdian.accessory.model.AccessorySlot;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AccessoryEquipEvent extends Event {
    
    private final Player player;
    private final AccessorySlot slot;
    private final String accessoryId;
    private final boolean equipped;
    
    public AccessoryEquipEvent(Player player, AccessorySlot slot, String accessoryId, boolean equipped) {
        this.player = player;
        this.slot = slot;
        this.accessoryId = accessoryId;
        this.equipped = equipped;
    }
    
    public Player getPlayer() { return player; }
    public AccessorySlot getSlot() { return slot; }
    public String getAccessoryId() { return accessoryId; }
    public boolean isEquipped() { return equipped; }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    private static final HandlerList handlers = new HandlerList();
}
