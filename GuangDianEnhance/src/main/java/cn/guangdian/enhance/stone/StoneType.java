package cn.guangdian.enhance.stone;

import org.bukkit.inventory.ItemStack;

public enum StoneType {
    
    MATERIAL("淬炼石", "强化装备的必需材料", StoneEffectType.MATERIAL),
    
    SUCCESS_RATE("幸运石", "增加25%强化成功率", StoneEffectType.SUCCESS_RATE_BONUS),
    
    PROTECTION("保护石", "防止强化失败时降级", StoneEffectType.PREVENT_DEGRADE),
    
    SAFETY("安全石", "防止装备强化失败时破碎", StoneEffectType.PREVENT_DESTROY),
    
    GUARANTEE("必成石", "100%强化成功", StoneEffectType.GUARANTEE_SUCCESS),
    
    LUCK("暴击石", "提高暴击伤害加成", StoneEffectType.LUCK_BONUS);
    
    private final String displayName;
    private final String description;
    private final StoneEffectType effectType;
    
    StoneType(String displayName, String description, StoneEffectType effectType) {
        this.displayName = displayName;
        this.description = description;
        this.effectType = effectType;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public StoneEffectType getEffectType() {
        return effectType;
    }
}
