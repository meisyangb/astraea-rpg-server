package cn.guangdian.accessory.model;

import cn.guangdian.accessory.hook.MythicMobsHook;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class Accessory {
    
    private final String id;
    private final String mythicId;
    private final String name;
    private final AccessorySlot slot;
    private final String loreKeyword;
    private final int rarity;
    
    public Accessory(String id, String mythicId, String name, AccessorySlot slot, String loreKeyword, int rarity) {
        this.id = id;
        this.mythicId = mythicId;
        this.name = name;
        this.slot = slot;
        this.loreKeyword = loreKeyword;
        this.rarity = rarity;
    }
    
    public static Accessory fromConfig(String id, ConfigurationSection config) {
        String mythicId = config.getString("mythic-id", id);
        String name = config.getString("name", id);
        
        String slotName = config.getString("slot", "BADGE").toUpperCase();
        AccessorySlot slot = AccessorySlot.valueOf(slotName);
        
        String loreKeyword = config.getString("lore-keyword", "");
        if (loreKeyword.isEmpty()) {
            loreKeyword = name;
        }
        
        int rarity = config.getInt("rarity", 1);
        
        return new Accessory(id, mythicId, name, slot, loreKeyword, rarity);
    }
    
    public static Accessory fromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        
        if (!item.getItemMeta().hasLore()) {
            return null;
        }
        
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null) {
            return null;
        }
        
        String loreText = String.join(" ", lore);
        String plainText = ChatColor.stripColor(loreText);
        
        return AccessorySlot.findAccessoryByLoreKeyword(plainText);
    }
    
    public boolean matchesItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        if (!item.getItemMeta().hasLore()) {
            return false;
        }
        
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null) {
            return false;
        }
        
        String loreText = String.join(" ", lore);
        String plainText = ChatColor.stripColor(loreText);
        
        return plainText.contains(loreKeyword);
    }
    
    public ItemStack getMythicItem(int amount) {
        if (!MythicMobsHook.getInstance().isEnabled()) {
            return null;
        }
        return MythicMobsHook.getInstance().getMythicItem(mythicId, amount);
    }
    
    public String getId() { return id; }
    public String getMythicId() { return mythicId; }
    public String getName() { return name; }
    public AccessorySlot getSlot() { return slot; }
    public String getLoreKeyword() { return loreKeyword; }
    public int getRarity() { return rarity; }
}
