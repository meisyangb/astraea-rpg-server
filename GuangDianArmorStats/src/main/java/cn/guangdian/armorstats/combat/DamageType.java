package cn.guangdian.armorstats.combat;

public enum DamageType {
    
    PHYSICAL("物理伤害", true, true, false, false),
    MAGICAL("魔法伤害", true, false, false, false),
    TRUE("真实伤害", false, false, false, false),
    PERCENT("百分比伤害", false, false, true, false),
    PERCENT_MAX("最大生命百分比", false, false, true, false),
    FIXED("固定伤害", false, false, false, false),
    REFLECT("反伤", false, false, false, true),
    
    FIRE("火焰伤害", true, true, false, false),
    ICE("冰霜伤害", true, true, false, false),
    LIGHTNING("闪电伤害", true, true, false, false),
    POISON("毒素伤害", true, true, false, false),
    HOLY("神圣伤害", true, false, false, false),
    DARK("暗影伤害", true, false, false, false),
    
    HYBRID("混合伤害", true, true, false, false);

    private final String displayName;
    private final boolean affectedByDefense;
    private final boolean affectedByArmor;
    private final boolean isPercentBased;
    private final boolean isReflect;

    DamageType(String displayName, boolean affectedByDefense, boolean affectedByArmor, 
               boolean isPercentBased, boolean isReflect) {
        this.displayName = displayName;
        this.affectedByDefense = affectedByDefense;
        this.affectedByArmor = affectedByArmor;
        this.isPercentBased = isPercentBased;
        this.isReflect = isReflect;
    }

    public String getDisplayName() { return displayName; }
    public boolean isAffectedByDefense() { return affectedByDefense; }
    public boolean isAffectedByArmor() { return affectedByArmor; }
    public boolean isPercentBased() { return isPercentBased; }
    public boolean isReflect() { return isReflect; }
    
    public boolean isElemental() {
        return this == FIRE || this == ICE || this == LIGHTNING || 
               this == POISON || this == HOLY || this == DARK;
    }
    
    public boolean ignoresAllDefense() {
        return this == TRUE || this == PERCENT || this == PERCENT_MAX || this == FIXED;
    }
}
