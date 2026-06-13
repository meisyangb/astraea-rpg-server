package cn.guangdian.devour.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * 吞噬完成事件
 * 
 * @author Astraea RPG Team
 * @since 1.0.0
 */
public class DevourCompleteEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final ItemStack finalWeapon;
    private final ItemStack originalWeapon;
    private final int devouredCount;
    
    public DevourCompleteEvent(Player player, ItemStack finalWeapon, ItemStack originalWeapon, int devouredCount) {
        this.player = player;
        this.finalWeapon = finalWeapon;
        this.originalWeapon = originalWeapon;
        this.devouredCount = devouredCount;
    }
    
    /**
     * 获取玩家
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * 获取最终武器
     */
    public ItemStack getFinalWeapon() {
        return finalWeapon;
    }
    
    /**
     * 获取原始武器
     */
    public ItemStack getOriginalWeapon() {
        return originalWeapon;
    }
    
    /**
     * 获取吞噬数量
     */
    public int getDevouredCount() {
        return devouredCount;
    }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
