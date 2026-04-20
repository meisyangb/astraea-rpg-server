package cn.guangdian.collection.model;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class CollectionEntry {
    
    private final String id;
    private final String categoryId;
    private String name;
    private final EntryType type;
    private String hint;
    
    private Material material;
    private String mythicId;
    private String rpgItemId;
    private EntityType entityType;
    private int targetCount;
    
    private EntryReward reward;
    private int slot;
    
    public enum EntryType {
        VANILLA_ITEM,
        MYTHICMOBS_ITEM,
        RPGITEMS_ITEM,
        VANILLA_MOB,
        MYTHICMOBS_MOB
    }
    
    public CollectionEntry(String id, String categoryId, String name, EntryType type, String hint) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.type = type;
        this.hint = hint;
        this.slot = 0;
        this.targetCount = 1;
    }
    
    public String getId() { return id; }
    public String getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public EntryType getType() { return type; }
    public String getHint() { return hint; }
    public Material getMaterial() { return material; }
    public String getMythicId() { return mythicId; }
    public String getRpgItemId() { return rpgItemId; }
    public EntityType getEntityType() { return entityType; }
    public int getTargetCount() { return targetCount; }
    public EntryReward getReward() { return reward; }
    public int getSlot() { return slot; }
    
    public void setName(String name) { this.name = name; }
    public void setHint(String hint) { this.hint = hint; }
    public void setMaterial(Material material) { this.material = material; }
    public void setMythicId(String mythicId) { this.mythicId = mythicId; }
    public void setRpgItemId(String rpgItemId) { this.rpgItemId = rpgItemId; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }
    public void setTargetCount(int targetCount) { this.targetCount = targetCount; }
    public void setReward(EntryReward reward) { this.reward = reward; }
    public void setSlot(int slot) { this.slot = slot; }
    
    public boolean isMobEntry() {
        return type == EntryType.VANILLA_MOB || type == EntryType.MYTHICMOBS_MOB;
    }
    
    public boolean isItemEntry() {
        return type == EntryType.VANILLA_ITEM || type == EntryType.MYTHICMOBS_ITEM || type == EntryType.RPGITEMS_ITEM;
    }
    
    public static class EntryReward {
        private double money;
        private long points;
        private List<String> commands;
        private List<String> messages;
        
        public EntryReward() {
            this.commands = new ArrayList<>();
            this.messages = new ArrayList<>();
        }
        
        public double getMoney() { return money; }
        public long getPoints() { return points; }
        public List<String> getCommands() { return commands; }
        public List<String> getMessages() { return messages; }
        
        public void setMoney(double money) { this.money = money; }
        public void setPoints(long points) { this.points = points; }
        public void setCommands(List<String> commands) { this.commands = commands != null ? commands : new ArrayList<>(); }
        public void setMessages(List<String> messages) { this.messages = messages != null ? messages : new ArrayList<>(); }
        
        public void addCommand(String command) {
            commands.add(command);
        }
        
        public void addMessage(String message) {
            messages.add(message);
        }
        
        public boolean hasReward() {
            return money > 0 || points > 0 || !commands.isEmpty() || !messages.isEmpty();
        }
    }
}
