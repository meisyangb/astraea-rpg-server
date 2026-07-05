package cn.guangdian.classsystem.enums;

/**
 * 分支枚举 - 序列7（一转）时的职业分支
 */
public enum Branch {
    
    // 深渊途径分支
    BERSERKER("狂战士", "berserker", Pathway.ABYSS),
    DEFENDER("守护者", "defender", Pathway.ABYSS),
    
    // 虚空途径分支
    DESTROYER("毁灭者", "destroyer", Pathway.VOID),
    CONTROLLER("支配者", "controller", Pathway.VOID),
    
    // 暗影途径分支
    KILLER("杀手", "killer", Pathway.SHADOW),
    DANCER("舞者", "dancer", Pathway.SHADOW),
    
    // 腐化途径分支
    HEALER("治愈者", "healer", Pathway.CORRUPTION),
    CURSER("诅咒者", "curser", Pathway.CORRUPTION),
    
    // 无分支
    NONE("无分支", "", null);
    
    private final String name;
    private final String id;
    private final Pathway pathway;
    
    Branch(String name, String id, Pathway pathway) {
        this.name = name;
        this.id = id;
        this.pathway = pathway;
    }
    
    public String getName() {
        return name;
    }
    
    public String getId() {
        return id;
    }
    
    public Pathway getPathway() {
        return pathway;
    }
    
    /**
     * 根据ID获取分支
     */
    public static Branch fromId(String id) {
        if (id == null || id.isEmpty()) return NONE;
        switch (id) {
            case "berserker": return BERSERKER;
            case "defender": return DEFENDER;
            case "destroyer": return DESTROYER;
            case "controller": return CONTROLLER;
            case "killer": return KILLER;
            case "dancer": return DANCER;
            case "healer": return HEALER;
            case "curser": return CURSER;
            default: return NONE;
        }
    }
    
    /**
     * 获取途径的所有分支
     */
    public static Branch[] getBranchesByPathway(Pathway pathway) {
        if (pathway == null) return new Branch[]{NONE};
        switch (pathway) {
            case ABYSS: return new Branch[]{BERSERKER, DEFENDER};
            case VOID: return new Branch[]{DESTROYER, CONTROLLER};
            case SHADOW: return new Branch[]{KILLER, DANCER};
            case CORRUPTION: return new Branch[]{HEALER, CURSER};
            default: return new Branch[]{NONE};
        }
    }
}