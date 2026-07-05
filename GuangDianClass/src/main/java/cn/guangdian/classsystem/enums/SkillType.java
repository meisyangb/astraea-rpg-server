package cn.guangdian.classsystem.enums;

/**
 * 技能类型枚举
 */
public enum SkillType {
    
    ACTIVE("主动技能", "active"),
    PASSIVE("被动技能", "passive");
    
    private final String name;
    private final String id;
    
    SkillType(String name, String id) {
        this.name = name;
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getId() {
        return id;
    }
    
    public static SkillType fromId(String id) {
        if (id == null) return ACTIVE;
        switch (id) {
            case "active": return ACTIVE;
            case "passive": return PASSIVE;
            default: return ACTIVE;
        }
    }
}