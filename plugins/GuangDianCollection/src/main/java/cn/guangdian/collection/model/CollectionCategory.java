package cn.guangdian.collection.model;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class CollectionCategory {
    
    private final String id;
    private String name;
    private String description;
    private Material icon;
    private int slot;
    private CategoryType type;
    private final Map<String, CollectionEntry> entries = new HashMap<>();
    
    public enum CategoryType {
        ITEM_COLLECT,
        MOB_KILL
    }
    
    public CollectionCategory(String id) {
        this.id = id;
        this.type = CategoryType.ITEM_COLLECT;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Material getIcon() { return icon; }
    public int getSlot() { return slot; }
    public CategoryType getType() { return type; }
    public Map<String, CollectionEntry> getEntries() { return entries; }
    
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setIcon(Material icon) { this.icon = icon; }
    public void setSlot(int slot) { this.slot = slot; }
    public void setType(CategoryType type) { this.type = type; }
    
    public void addEntry(CollectionEntry entry) {
        entries.put(entry.getId(), entry);
    }
    
    public CollectionEntry getEntry(String entryId) {
        return entries.get(entryId);
    }
    
    public int getTotalEntries() {
        return entries.size();
    }
}
