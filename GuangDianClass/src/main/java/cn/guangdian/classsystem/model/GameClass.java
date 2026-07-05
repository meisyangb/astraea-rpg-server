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
    private int attributePoints;
    
    // 克苏鲁风格字段 - 诡秘之主途径系统
    private String pathway;      // 途径: abyss, void, shadow, corruption
    private int sequence;        // 序列: 9(最低) → 0(最高,真神)
    private String branch;       // 分支: berserker, defender, destroyer, controller, killer, dancer, healer, curser
    
    public GameClass() {
        this.nextClasses = new ArrayList<>();
        this.stats = new HashMap<>();
        this.skills = new ArrayList<>();
        this.sequence = 9; // 默认序列9（最低）
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
    
    public int getAttributePoints() {
        return attributePoints;
    }
    
    public void setAttributePoints(int attributePoints) {
        this.attributePoints = attributePoints;
    }
    
    // ========================================
    // 克苏鲁风格途径系统方法
    // ========================================
    
    public String getPathway() {
        return pathway;
    }
    
    public void setPathway(String pathway) {
        this.pathway = pathway;
    }
    
    public int getSequence() {
        return sequence;
    }
    
    public void setSequence(int sequence) {
        this.sequence = sequence;
    }
    
    public String getBranch() {
        return branch;
    }
    
    public void setBranch(String branch) {
        this.branch = branch;
    }
    
    /**
     * 获取途径名称（中文）
     */
    public String getPathwayName() {
        if (pathway == null) return "未知";
        return switch (pathway) {
            case "abyss" -> "深渊";
            case "void" -> "虚空";
            case "shadow" -> "暗影";
            case "corruption" -> "腐化";
            default -> "未知";
        };
    }
    
    /**
     * 获取序列名称（中文）
     */
    public String getSequenceName() {
        return switch (sequence) {
            case 0 -> "序列0（真神）";
            case 1 -> "序列1（神子）";
            case 2 -> "序列2（主宰）";
            case 3 -> "序列3（之王）";
            case 4 -> "序列4（神选）";
            case 5 -> "序列5（暴君）";
            case 6 -> "序列6（领主）";
            case 7 -> "序列7（骑士）";
            case 8 -> "序列8（守卫）";
            case 9 -> "序列9（行者）";
            default -> "序列" + sequence;
        };
    }
    
    /**
     * 获取分支名称（中文）
     */
    public String getBranchName() {
        if (branch == null || branch.isEmpty()) return "";
        return switch (branch) {
            case "berserker" -> "狂战士";
            case "defender" -> "守护者";
            case "destroyer" -> "毁灭者";
            case "controller" -> "支配者";
            case "killer" -> "杀手";
            case "dancer" -> "舞者";
            case "healer" -> "治愈者";
            case "curser" -> "诅咒者";
            default -> branch;
        };
    }
    
    /**
     * 获取完整职业名称（包含途径、序列、分支）
     */
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        sb.append(getPathwayName()).append("途径");
        if (!branch.isEmpty()) {
            sb.append(" - ").append(getBranchName()).append("分支");
        }
        sb.append(" - ").append(getSequenceName());
        sb.append(" - ").append(name);
        return sb.toString();
    }
    
    /**
     * 检查是否是同一途径
     */
    public boolean isSamePathway(GameClass other) {
        if (other == null) return false;
        return this.pathway != null && this.pathway.equals(other.pathway);
    }
    
    /**
     * 检查序列是否高于目标（序列越低越强）
     */
    public boolean isSequenceHigherThan(GameClass other) {
        if (other == null) return false;
        return this.sequence < other.sequence;
    }
    
    /**
     * 检查是否是真神（序列0）
     */
    public boolean isTrueGod() {
        return sequence == 0;
    }
    
    /**
     * 检查是否是半神（序列1-4）
     */
    public boolean isDemigod() {
        return sequence >= 1 && sequence <= 4;
    }
}
