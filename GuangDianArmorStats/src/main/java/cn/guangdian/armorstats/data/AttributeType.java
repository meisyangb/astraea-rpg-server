package cn.guangdian.armorstats.data;

/**
 * 属性类型枚举 - 简化的属性系统
 * 
 * 所有属性采用枚举一一对应，避免硬编码字段
 */
public enum AttributeType {
    // 基础属性
    MAX_HEALTH("生命上限"),
    MIN_ATTACK("攻击力最小值"),
    MAX_ATTACK("攻击力最大值"),
    DEFENSE_MIN("防御力最小值"),
    DEFENSE_MAX("防御力最大值"),
    
    // 暴击属性
    CRIT_CHANCE("暴击几率"),
    CRIT_DAMAGE("暴击伤害"),
    CRIT_RESIST("暴击抵抗"),
    CRIT_DAMAGE_RESIST("暴伤抵抗"),
    
    // 战斗属性
    LIFESTEAL_CHANCE("吸血几率"),
    LIFESTEAL_MULTIPLIER("吸血倍率"),
    LIFESTEAL_RESIST("吸血抵抗"),
    DODGE("闪避"),
    PARRY("招架"),
    DAMAGE_REFLECT("伤害反弹"),
    REFLECT_RATIO("反伤比例"),
    
    // PVP属性
    PVP_MIN_ATTACK("PVP攻击力最小值"),
    PVP_MAX_ATTACK("PVP攻击力最大值"),
    PVP_DEFENSE_MIN("PVP防御力最小值"),
    PVP_DEFENSE_MAX("PVP防御力最大值"),
    
    // 护甲与穿透系统
    ARMOR("护甲值"),
    ARMOR_STRENGTH("护甲强度"),
    ARMOR_PENETRATION("护甲穿透"),
    DEFENSE_PENETRATION("防御穿透"),
    DAMAGE_REDUCTION("减伤"),
    
    // 躲避反伤系统
    DODGE_REFLECT_CHANCE("躲避反伤"),
    DODGE_REFLECT_RATIO("躲避反弹比例"),
    
    // 生命恢复
    HEALTH_REGEN("每秒回血"),
    HEALTH_REGEN_PERCENT("生命恢复"),
    
    // 状态效果
    POISON("中毒"),
    FREEZE("冰冻"),
    BLIND("致盲"),
    BURN("燃烧"),
    SCORCH("灼烧"),
    
    // 移动属性
    MOVE_SPEED("移动速度"),
    
    // 其他
    EXP_BONUS("经验加成"),
    KNOCKBACK_RESIST("击退抗性"),
    
    // 环境抗性
    FIRE_RESIST("火焰抗性"),
    FALL_RESIST("摔落抗性"),
    DROWNING_RESIST("溺水抗性"),
    POISON_RESIST("中毒抗性"),
    WITHER_RESIST("凋零抗性"),
    LAVA_RESIST("岩浆抗性"),
    MAGIC_RESIST("魔法抗性"),
    EXPLOSION_RESIST("爆炸抗性"),
    PROJECTILE_RESIST("弹射物抗性");
    
    private final String displayName;
    
    AttributeType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 根据显示名称获取属性类型
     * 
     * @param displayName 显示名称（如"生命上限"、"暴击几率"）
     * @return 属性类型，如果没有匹配返回null
     */
    public static AttributeType fromDisplayName(String displayName) {
        for (AttributeType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * 检查是否是范围属性（需要min/max两个值）
     * 
     * @return 是否是范围属性
     */
    public boolean isRangeAttribute() {
        return this == MIN_ATTACK || this == MAX_ATTACK ||
               this == DEFENSE_MIN || this == DEFENSE_MAX ||
               this == PVP_MIN_ATTACK || this == PVP_MAX_ATTACK ||
               this == PVP_DEFENSE_MIN || this == PVP_DEFENSE_MAX;
    }
}