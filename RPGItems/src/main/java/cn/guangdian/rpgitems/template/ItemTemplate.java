package cn.guangdian.rpgitems.template;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;

/**
 * 物品模板
 * 包含：材质、显示名、Lore、选项、属性数据、阶位
 */
public record ItemTemplate(
    String id,
    Material material,
    Component displayName,
    List<Component> lore,
    ItemOptions options,
    Attributes attributes,
    String tier  // 阶位 (如: 一阶装备, 二阶装备, 魔王, 天族 等)
) {

    /**
     * 物品选项
     */
    public record ItemOptions(
        boolean unbreakable,
        boolean hideAttributes,
        Integer customModelData
    ) {}

    /**
     * 物品属性数据
     * 所有属性都存储到 PDC，不依赖 Lore 解析
     */
    public record Attributes(
        // 攻击属性
        double attackMin,
        double attackMax,
        
        // 防御属性
        double defenseMin,
        double defenseMax,
        
        // 生命属性
        double maxHealth,
        double healthRegen,
        
        // 暴击属性
        double critChance,
        double critDamage,
        
        // 生命偷取
        double lifestealChance,
        double lifestealMultiplier,
        
        // 闪避与格挡
        double dodgeChance,
        double parryChance,
        
        // 移动速度
        double moveSpeed,
        
        // 减伤
        double damageReduction,
        
        // PVP属性
        double pvpAttackMin,
        double pvpAttackMax,
        double pvpDefenseMin,
        double pvpDefenseMax,
        
        // 暴击抵抗属性
        double critResist,
        double critDamageResist,
        
        // 吸血抵抗
        double lifestealResist,
        
        // 护甲与穿透系统
        double armor,
        double armorStrength,
        double armorPenetration,
        double defensePenetration,
        
        // 伤害反弹
        double damageReflect,
        double reflectRatio,
        
        // 状态效果概率
        double poisonChance,
        double freezeChance,
        double blindChance,
        double burnChance,
        double scorchChance,
        double igniteChance,    // 点燃几率
        double slowChance,      // 减速几率
        
        // 环境抗性
        double fireResist,
        double fallResist,
        double drowningResist,
        double poisonResist,
        double witherResist,
        double lavaResist,
        double magicResist,
        double explosionResist,
        double projectileResist,
        
        // 其他属性
        double knockbackResist,
        double expBonus,
        double healthRegenPercent,
        double dodgeReflectChance,
        double dodgeReflectRatio,
        
        // 装备等级
        int level,
        
        // 需要职业
        String requiredClass,
        
        // 宝石类型 (用于宝石物品)
        String gemType,
        
        // 槽位定义 (用于装备物品)
        List<String> sockets
    ) {
        /**
         * 默认空属性
         */
        public static Attributes empty() {
            return new Attributes(
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0,
                0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0,
                0, "", null, null
            );
        }
    }
}
