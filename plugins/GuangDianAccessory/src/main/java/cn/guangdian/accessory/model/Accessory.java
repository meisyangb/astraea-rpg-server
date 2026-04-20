package cn.guangdian.accessory.model;

import cn.guangdian.accessory.hook.MythicMobsHook;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.regex.Pattern;

public class Accessory {

    // 用于移除颜色代码的正则表达式
    private static final Pattern COLOR_PATTERN = Pattern.compile("[§&][0-9a-fk-orA-FK-OR]");

    private final String id;
    private final String mythicId;
    private final String name;
    private final AccessorySlot slot;
    private final String loreKeyword;
    private final int rarity;

    /**
     * 移除字符串中的颜色代码（替代 ChatColor.stripColor）
     */
    private static String stripColor(String text) {
        if (text == null) return "";
        return COLOR_PATTERN.matcher(text).replaceAll("");
    }
    
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
        String plainText = stripColor(loreText);

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
        String plainText = stripColor(loreText);

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
