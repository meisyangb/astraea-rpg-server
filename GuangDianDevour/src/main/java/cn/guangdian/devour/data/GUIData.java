package cn.guangdian.devour.data;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * GUI 数据模型
 * 存储玩家打开GUI时的临时数据
 * 
 * @author Astraea RPG Team
 * @since 1.0.0
 */
public class GUIData {
    
    /** 玩家UUID */
    private final UUID playerUUID;
    
    /** 吞噬剑物品 */
    private final ItemStack devourWeapon;
    
    /** 吞噬数据 */
    private final DevourData devourData;
    
    /** 待吞噬的物品 (从背包选择的) */
    private ItemStack pendingItem;
    
    /** 当前选中的槽位 */
    private int selectedSlot;
    
    public GUIData(UUID playerUUID, ItemStack devourWeapon, DevourData devourData) {
        this.playerUUID = playerUUID;
        this.devourWeapon = devourWeapon;
        this.devourData = devourData;
        this.pendingItem = null;
        this.selectedSlot = -1;
    }
    
    /**
     * 获取玩家UUID
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    /**
     * 获取吞噬剑物品
     */
    public ItemStack getDevourWeapon() {
        return devourWeapon;
    }
    
    /**
     * 获取吞噬数据
     */
    public DevourData getDevourData() {
        return devourData;
    }
    
    /**
     * 获取待吞噬的物品
     */
    public ItemStack getPendingItem() {
        return pendingItem;
    }
    
    /**
     * 设置待吞噬的物品
     */
    public void setPendingItem(ItemStack pendingItem) {
        this.pendingItem = pendingItem;
    }
    
    /**
     * 获取选中的槽位
     */
    public int getSelectedSlot() {
        return selectedSlot;
    }
    
    /**
     * 设置选中的槽位
     */
    public void setSelectedSlot(int selectedSlot) {
        this.selectedSlot = selectedSlot;
    }
    
    /**
     * 是否有待吞噬的物品
     */
    public boolean hasPendingItem() {
        return pendingItem != null && pendingItem.getType().isItem();
    }
}
