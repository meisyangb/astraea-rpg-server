package cn.guangdian.collection.model;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.time.Instant;
import java.util.UUID;

public class CollectionEntry {
    
    private final String id;
    private final String categoryId;
    private final String name;
    private final EntryType type;
    private final String hint;
    
    private Material material;
    private String mythicId;
    private EntityType entityType;
    private int targetCount;
    
    public enum EntryType {
        VANILLA_ITEM,
        MYTHICMOBS_ITEM,
        VANILLA_MOB,
        MYTHICMOBS_MOB
    }
    
    public CollectionEntry(String id, String categoryId, String name, EntryType type, String hint) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.type = type;
        this.hint = hint;
    }
    
    public String getId() { return id; }
    public String getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public EntryType getType() { return type; }
    public String getHint() { return hint; }
    public Material getMaterial() { return material; }
    public String getMythicId() { return mythicId; }
    public EntityType getEntityType() { return entityType; }
    public int getTargetCount() { return targetCount; }
    
    public void setMaterial(Material material) { this.material = material; }
    public void setMythicId(String mythicId) { this.mythicId = mythicId; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }
    public void setTargetCount(int targetCount) { this.targetCount = targetCount; }
    
    public boolean isMobEntry() {
        return type == EntryType.VANILLA_MOB || type == EntryType.MYTHICMOBS_MOB;
    }
    
    public boolean isItemEntry() {
        return type == EntryType.VANILLA_ITEM || type == EntryType.MYTHICMOBS_ITEM;
    }
}
