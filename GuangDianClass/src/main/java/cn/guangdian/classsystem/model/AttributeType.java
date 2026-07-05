package cn.guangdian.classsystem.model;

public enum AttributeType {
    
    STRENGTH("力量", "strength", "增加攻击力"),
    VITALITY("体质", "vitality", "增加生命上限"),
    AGILITY("敏捷", "agility", "增加暴击几率和闪避"),
    INTELLIGENCE("智力", "intelligence", "增加魔法值和技能伤害"),
    LUCK("幸运", "luck", "增加暴击伤害和掉落率");
    
    private final String displayName;
    private final String id;
    private final String description;
    
    AttributeType(String displayName, String id, String description) {
        this.displayName = displayName;
        this.id = id;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static AttributeType fromId(String id) {
        if (id == null) return null;
        switch (id.toLowerCase()) {
            case "strength": return STRENGTH;
            case "vitality": return VITALITY;
            case "agility": return AGILITY;
            case "intelligence": return INTELLIGENCE;
            case "luck": return LUCK;
            default: return null;
        }
    }
}
