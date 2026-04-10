package cn.guangdian.accessory.model;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Map;

public class Accessory {
    
    private final String id;
    private final String name;
    private final AccessorySlot slot;
    private final ItemStack item;
    private final Map<String, Double> attributes;
    private final int rarity;
    private final String description;
    
    public Accessory(String id, String name, AccessorySlot slot, ItemStack item, 
                     Map<String, Double> attributes, int rarity, String description) {
        this.id = id;
        this.name = name;
        this.slot = slot;
        this.item = item;
        this.attributes = attributes;
        this.rarity = rarity;
        this.description = description;
    }
    
    public static Accessory fromConfig(String id, ConfigurationSection config) {
        String name = config.getString("name", id);
        String slotName = config.getString("slot", "BADGE").toUpperCase();
        AccessorySlot slot = AccessorySlot.valueOf(slotName);
        
        ItemStack item = new ItemStack(
            org.bukkit.Material.valueOf(config.getString("material", "DIAMOND")),
            1
        );
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            item.setItemMeta(meta);
        }
        
        Map<String, Double> attributes = new HashMap<>();
        ConfigurationSection attrSection = config.getConfigurationSection("attributes");
        if (attrSection != null) {
            for (String key : attrSection.getKeys(false)) {
                attributes.put(key, attrSection.getDouble(key));
            }
        }
        
        int rarity = config.getInt("rarity", 1);
        String description = config.getString("description", "");
        
        return new Accessory(id, name, slot, item, attributes, rarity, description);
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public AccessorySlot getSlot() { return slot; }
    public ItemStack getItem() { return item.clone(); }
    public Map<String, Double> getAttributes() { return new HashMap<>(attributes); }
    public int getRarity() { return rarity; }
    public String getDescription() { return description; }
}
