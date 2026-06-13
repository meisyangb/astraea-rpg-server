package cn.guangdian.collection.model;

import org.bukkit.Material;

import java.util.*;

public class CollectionSet {
    
    private final String id;
    private String name;
    private Material icon;
    private int slot;
    private List<String> categoryIds;
    private String description;
    
    public CollectionSet(String id) {
        this.id = id;
        this.categoryIds = new ArrayList<>();
        this.icon = Material.CHEST;
        this.slot = 0;
        this.description = "";
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Material getIcon() {
        return icon;
    }
    
    public void setIcon(Material icon) {
        this.icon = icon;
    }
    
    public int getSlot() {
        return slot;
    }
    
    public void setSlot(int slot) {
        this.slot = slot;
    }
    
    public List<String> getCategoryIds() {
        return categoryIds;
    }
    
    public void setCategoryIds(List<String> categoryIds) {
        this.categoryIds = categoryIds != null ? categoryIds : new ArrayList<>();
    }
    
    public void addCategoryId(String categoryId) {
        if (!categoryIds.contains(categoryId)) {
            categoryIds.add(categoryId);
        }
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}
