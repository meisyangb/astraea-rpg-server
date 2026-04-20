package cn.guangdian.classsystem.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameClass {
    
    private String id;
    private String name;
    private int tier;
    private int advancement;
    private String description;
    private String requiresClass;
    private List<String> nextClasses;
    private Map<String, Double> stats;
    private List<String> skills;
    
    private String classType;
    private List<AttributeType> availableAttributes;
    private Map<AttributeType, AttributeEffect> attributeEffects;
    private int pointsPerLevel;
    private Map<Integer, Integer> bonusAtTiers;
    
    public GameClass() {
        this.nextClasses = new ArrayList<>();
        this.stats = new HashMap<>();
        this.skills = new ArrayList<>();
        this.availableAttributes = new ArrayList<>();
        this.attributeEffects = new HashMap<>();
        this.bonusAtTiers = new HashMap<>();
        this.pointsPerLevel = 3;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getTier() {
        return tier;
    }
    
    public void setTier(int tier) {
        this.tier = tier;
    }
    
    public int getAdvancement() {
        return advancement;
    }
    
    public void setAdvancement(int advancement) {
        this.advancement = advancement;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getRequiresClass() {
        return requiresClass;
    }
    
    public void setRequiresClass(String requiresClass) {
        this.requiresClass = requiresClass;
    }
    
    public List<String> getNextClasses() {
        return nextClasses;
    }
    
    public void setNextClasses(List<String> nextClasses) {
        this.nextClasses = nextClasses != null ? nextClasses : new ArrayList<>();
    }
    
    public Map<String, Double> getStats() {
        return stats;
    }
    
    public void setStats(Map<String, Double> stats) {
        this.stats = stats != null ? stats : new HashMap<>();
    }
    
    public List<String> getSkills() {
        return skills;
    }
    
    public void setSkills(List<String> skills) {
        this.skills = skills != null ? skills : new ArrayList<>();
    }
    
    public String getClassType() {
        return classType;
    }
    
    public void setClassType(String classType) {
        this.classType = classType;
    }
    
    public List<AttributeType> getAvailableAttributes() {
        return availableAttributes;
    }
    
    public void setAvailableAttributes(List<AttributeType> availableAttributes) {
        this.availableAttributes = availableAttributes != null ? availableAttributes : new ArrayList<>();
    }
    
    public Map<AttributeType, AttributeEffect> getAttributeEffects() {
        return attributeEffects;
    }
    
    public void setAttributeEffects(Map<AttributeType, AttributeEffect> attributeEffects) {
        this.attributeEffects = attributeEffects != null ? attributeEffects : new HashMap<>();
    }
    
    public AttributeEffect getAttributeEffect(AttributeType type) {
        return attributeEffects.get(type);
    }
    
    public boolean hasAttribute(AttributeType type) {
        return availableAttributes.contains(type);
    }
    
    public int getPointsPerLevel() {
        return pointsPerLevel;
    }
    
    public void setPointsPerLevel(int pointsPerLevel) {
        this.pointsPerLevel = pointsPerLevel;
    }
    
    public Map<Integer, Integer> getBonusAtTiers() {
        return bonusAtTiers;
    }
    
    public void setBonusAtTiers(Map<Integer, Integer> bonusAtTiers) {
        this.bonusAtTiers = bonusAtTiers != null ? bonusAtTiers : new HashMap<>();
    }
    
    public int getBonusAtTier(int tier) {
        return bonusAtTiers.getOrDefault(tier, 0);
    }
    
    public String getAdvancementName() {
        return switch (advancement) {
            case 1 -> "一转";
            case 2 -> "二转";
            case 3 -> "三转";
            case 4 -> "神级";
            default -> "基础";
        };
    }
    
    public boolean isBaseClass() {
        return advancement == 0 && tier == 1;
    }
    
    public boolean isDivineClass() {
        return advancement == 4 && tier == 9;
    }
    
    public boolean canAdvanceTo(String targetClassId) {
        return nextClasses.contains(targetClassId);
    }
}
