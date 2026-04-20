package cn.guangdian.classsystem.model;

import org.bukkit.Material;

public enum AttributeType {
    
    STRENGTH("力量", "strength", "增加攻击力", Material.IRON_SWORD),
    VITALITY("体质", "vitality", "增加生命上限和防御", Material.GOLDEN_APPLE),
    AGILITY("敏捷", "agility", "增加闪避和移动速度", Material.FEATHER),
    INTELLIGENCE("智力", "intelligence", "增加魔法值和技能伤害", Material.BOOK),
    LUCK("幸运", "luck", "增加暴击几率和暴击伤害", Material.EMERALD),
    
    PARRY("招架", "parry", "增加招架几率，完全抵挡攻击", Material.SHIELD),
    ARMOR("护甲", "armor", "增加护甲值，减少受到的伤害", Material.IRON_CHESTPLATE),
    CRIT_DAMAGE("暴击伤害", "crit_damage", "增加暴击时的伤害倍率", Material.DIAMOND_SWORD),
    MAGIC_RESIST("魔法抗性", "magic_resist", "减少受到的魔法伤害", Material.ENCHANTED_BOOK),
    ACCURACY("精准", "accuracy", "增加命中率，减少被闪避", Material.TARGET),
    LIFESTEAL("吸血", "lifesteal", "攻击时恢复生命值", Material.REDSTONE),
    HEALTH_REGEN("生命恢复", "health_regen", "每秒恢复生命值", Material.POTION),
    EXP_BONUS("经验加成", "exp_bonus", "增加获得的经验值", Material.EXPERIENCE_BOTTLE),
    DODGE("闪避", "dodge", "有几率完全躲避攻击", Material.PHANTOM_MEMBRANE),
    MOVE_SPEED("移动速度", "move_speed", "增加移动速度", Material.SUGAR);
    
    private final String displayName;
    private final String id;
    private final String description;
    private final Material icon;
    
    AttributeType(String displayName, String id, String description, Material icon) {
        this.displayName = displayName;
        this.id = id;
        this.description = description;
        this.icon = icon;
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
    
    public Material getIcon() {
        return icon;
    }
    
    public static AttributeType fromId(String id) {
        for (AttributeType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
    
    public boolean isBasicAttribute() {
        return this == STRENGTH || this == VITALITY || this == AGILITY 
            || this == INTELLIGENCE || this == LUCK;
    }
    
    public boolean isAdvancedAttribute() {
        return !isBasicAttribute();
    }
}
