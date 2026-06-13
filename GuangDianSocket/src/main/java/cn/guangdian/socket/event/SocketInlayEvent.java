package cn.guangdian.socket.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import cn.guangdian.socket.model.AttributeValue;

import java.util.List;
import java.util.Map;

/**
 * 宝石镶嵌事件
 */
public class SocketInlayEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final ItemStack finalItem;
    private final ItemStack originalItem;
    private final List<ItemStack> gems;
    private final Map<String, AttributeValue> attributes;
    private final boolean isRework;

    public SocketInlayEvent(Player player, ItemStack finalItem, ItemStack originalItem,
                            List<ItemStack> gems, Map<String, AttributeValue> attributes) {
        this.player = player;
        this.finalItem = finalItem;
        this.originalItem = originalItem;
        this.gems = gems;
        this.attributes = attributes;
        this.isRework = false;
    }

    public SocketInlayEvent(Player player, ItemStack finalItem, ItemStack originalItem, boolean isRework) {
        this.player = player;
        this.finalItem = finalItem;
        this.originalItem = originalItem;
        this.gems = null;
        this.attributes = null;
        this.isRework = isRework;
    }

    public Player getPlayer() { return player; }
    public ItemStack getFinalItem() { return finalItem; }
    public ItemStack getOriginalItem() { return originalItem; }
    public List<ItemStack> getGems() { return gems; }
    public Map<String, AttributeValue> getAttributes() { return attributes; }
    public boolean isRework() { return isRework; }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
