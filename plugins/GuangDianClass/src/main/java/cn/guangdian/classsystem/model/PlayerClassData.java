package cn.guangdian.classsystem.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class PlayerClassData {
    
    private UUID playerId;
    private String classId;
    private int tier;
    private long exp;
    private int advancementLevel;
    private long totalExp;
    private long lastUpdateTime;
    
    private int attributePoints;
    private int usedAttributePoints;
    private Map<AttributeType, Integer> allocatedAttributes;
    
    public PlayerClassData() {
        this.tier = 1;
        this.exp = 0;
        this.advancementLevel = 0;
        this.totalExp = 0;
        this.lastUpdateTime = System.currentTimeMillis();
        this.attributePoints = 0;
        this.usedAttributePoints = 0;
        this.allocatedAttributes = new EnumMap<>(AttributeType.class);
        for (AttributeType type : AttributeType.values()) {
            allocatedAttributes.put(type, 0);
        }
    }
    
    public PlayerClassData(UUID playerId) {
        this();
        this.playerId = playerId;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }
    
    public String getClassId() {
        return classId;
    }
    
    public void setClassId(String classId) {
        this.classId = classId;
    }
    
    public int getTier() {
        return tier;
    }
    
    public void setTier(int tier) {
        this.tier = tier;
    }
    
    public long getExp() {
        return exp;
    }
    
    public void setExp(long exp) {
        this.exp = Math.max(0, exp);
    }
    
    public int getAdvancementLevel() {
        return advancementLevel;
    }
    
    public void setAdvancementLevel(int advancementLevel) {
        this.advancementLevel = advancementLevel;
    }
    
    public long getTotalExp() {
        return totalExp;
    }
    
    public void setTotalExp(long totalExp) {
        this.totalExp = Math.max(0, totalExp);
    }
    
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
    
    public void addExp(long amount) {
        this.exp += amount;
        this.totalExp += amount;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public boolean canAdvanceTier(int maxTier) {
        return tier < maxTier;
    }
    
    public boolean canAdvanceClass(int requiredTier, int requiredAdvancement) {
        return tier >= requiredTier && advancementLevel >= requiredAdvancement;
    }
    
    public String getAdvancementName() {
        return switch (advancementLevel) {
            case 1 -> "一转";
            case 2 -> "二转";
            case 3 -> "三转";
            case 4 -> "神级";
            default -> "未转职";
        };
    }
    
    public int getAttributePoints() {
        return attributePoints;
    }
    
    public void setAttributePoints(int attributePoints) {
        this.attributePoints = Math.max(0, attributePoints);
    }
    
    public int getUsedAttributePoints() {
        return usedAttributePoints;
    }
    
    public void setUsedAttributePoints(int usedAttributePoints) {
        this.usedAttributePoints = Math.max(0, usedAttributePoints);
    }
    
    public int getAvailableAttributePoints() {
        return attributePoints - usedAttributePoints;
    }
    
    public int getAllocatedAttribute(AttributeType type) {
        return allocatedAttributes.getOrDefault(type, 0);
    }
    
    public void setAllocatedAttribute(AttributeType type, int value) {
        allocatedAttributes.put(type, Math.max(0, value));
    }
    
    public Map<AttributeType, Integer> getAllocatedAttributes() {
        return new EnumMap<>(allocatedAttributes);
    }
    
    public boolean allocateAttribute(AttributeType type, int points) {
        if (points <= 0) return false;
        if (getAvailableAttributePoints() < points) return false;
        
        int current = getAllocatedAttribute(type);
        allocatedAttributes.put(type, current + points);
        usedAttributePoints += points;
        lastUpdateTime = System.currentTimeMillis();
        return true;
    }
    
    public boolean deallocateAttribute(AttributeType type, int points) {
        if (points <= 0) return false;
        
        int current = getAllocatedAttribute(type);
        if (current < points) return false;
        
        allocatedAttributes.put(type, current - points);
        usedAttributePoints -= points;
        lastUpdateTime = System.currentTimeMillis();
        return true;
    }
    
    public void addAttributePoints(int points) {
        if (points > 0) {
            this.attributePoints += points;
            lastUpdateTime = System.currentTimeMillis();
        }
    }
    
    public void resetAttributes() {
        for (AttributeType type : AttributeType.values()) {
            allocatedAttributes.put(type, 0);
        }
        usedAttributePoints = 0;
        lastUpdateTime = System.currentTimeMillis();
    }
    
    public int getTotalAllocatedPoints() {
        return allocatedAttributes.values().stream().mapToInt(Integer::intValue).sum();
    }
}
