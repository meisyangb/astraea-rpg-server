package cn.guangdian.soulbag.data;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class SoulBagData {
    
    private final UUID ownerId;
    private final ItemStack[] items;
    private final int size;
    
    public SoulBagData(UUID ownerId, int size) {
        this.ownerId = ownerId;
        this.size = size;
        this.items = new ItemStack[size];
    }
    
    public UUID getOwnerId() {
        return ownerId;
    }
    
    public ItemStack[] getItems() {
        return items;
    }
    
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= size) {
            return null;
        }
        return items[slot];
    }
    
    public void setItem(int slot, ItemStack item) {
        if (slot < 0 || slot >= size) {
            return;
        }
        items[slot] = item;
    }
    
    public int getSize() {
        return size;
    }
    
    public int getUsedSlots() {
        int count = 0;
        for (ItemStack item : items) {
            if (item != null) {
                count++;
            }
        }
        return count;
    }
    
    public int getEmptySlots() {
        return size - getUsedSlots();
    }
    
    public boolean isFull() {
        return getEmptySlots() == 0;
    }
    
    public boolean isEmpty() {
        return getUsedSlots() == 0;
    }
    
    public void clear() {
        for (int i = 0; i < size; i++) {
            items[i] = null;
        }
    }
    
    public int addItem(ItemStack item) {
        if (item == null) {
            return -1;
        }
        
        for (int i = 0; i < size; i++) {
            if (items[i] == null) {
                items[i] = item.clone();
                return i;
            }
        }
        
        return -1;
    }
}
