package cn.guangdian.rpgitems.attribute;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * PDC 属性读取器
 * 从物品的 PersistentDataContainer 读取属性数据
 */
public class PDCAttributeReader {

    // PDC Keys - 物品标识
    private static final NamespacedKey KEY_ID = new NamespacedKey("rpgitems", "id");
    
    // PDC Keys - 攻击属性
    private static final NamespacedKey KEY_ATTACK_MIN = new NamespacedKey("rpgitems", "attack_min");
    private static final NamespacedKey KEY_ATTACK_MAX = new NamespacedKey("rpgitems", "attack_max");
    
    // PDC Keys - 防御属性
    private static final NamespacedKey KEY_DEFENSE_MIN = new NamespacedKey("rpgitems", "defense_min");
    private static final NamespacedKey KEY_DEFENSE_MAX = new NamespacedKey("rpgitems", "defense_max");
    
    // PDC Keys - 生命属性
    private static final NamespacedKey KEY_MAX_HEALTH = new NamespacedKey("rpgitems", "max_health");
    private static final NamespacedKey KEY_HEALTH_REGEN = new NamespacedKey("rpgitems", "health_regen");
    
    // PDC Keys - 暴击属性
    private static final NamespacedKey KEY_CRIT_CHANCE = new NamespacedKey("rpgitems", "crit_chance");
    private static final NamespacedKey KEY_CRIT_DAMAGE = new NamespacedKey("rpgitems", "crit_damage");
    
    // PDC Keys - 生命偷取
    private static final NamespacedKey KEY_LIFESTEAL_CHANCE = new NamespacedKey("rpgitems", "lifesteal_chance");
    private static final NamespacedKey KEY_LIFESTEAL_MULTIPLIER = new NamespacedKey("rpgitems", "lifesteal_multiplier");
    
    // PDC Keys - 闪避与格挡
    private static final NamespacedKey KEY_DODGE_CHANCE = new NamespacedKey("rpgitems", "dodge_chance");
    private static final NamespacedKey KEY_PARRY_CHANCE = new NamespacedKey("rpgitems", "parry_chance");
    
    // PDC Keys - 移动速度
    private static final NamespacedKey KEY_MOVE_SPEED = new NamespacedKey("rpgitems", "move_speed");
    
    // PDC Keys - 减伤
    private static final NamespacedKey KEY_DAMAGE_REDUCTION = new NamespacedKey("rpgitems", "damage_reduction");
    
    // PDC Keys - PVP属性
    private static final NamespacedKey KEY_PVP_ATTACK_MIN = new NamespacedKey("rpgitems", "pvp_attack_min");
    private static final NamespacedKey KEY_PVP_ATTACK_MAX = new NamespacedKey("rpgitems", "pvp_attack_max");
    private static final NamespacedKey KEY_PVP_DEFENSE_MIN = new NamespacedKey("rpgitems", "pvp_defense_min");
    private static final NamespacedKey KEY_PVP_DEFENSE_MAX = new NamespacedKey("rpgitems", "pvp_defense_max");
    
    // PDC Keys - 暴击抵抗
    private static final NamespacedKey KEY_CRIT_RESIST = new NamespacedKey("rpgitems", "crit_resist");
    private static final NamespacedKey KEY_CRIT_DAMAGE_RESIST = new NamespacedKey("rpgitems", "crit_damage_resist");
    
    // PDC Keys - 吸血抵抗
    private static final NamespacedKey KEY_LIFESTEAL_RESIST = new NamespacedKey("rpgitems", "lifesteal_resist");
    
    // PDC Keys - 护甲与穿透
    private static final NamespacedKey KEY_ARMOR = new NamespacedKey("rpgitems", "armor");
    private static final NamespacedKey KEY_ARMOR_STRENGTH = new NamespacedKey("rpgitems", "armor_strength");
    private static final NamespacedKey KEY_ARMOR_PENETRATION = new NamespacedKey("rpgitems", "armor_penetration");
    private static final NamespacedKey KEY_DEFENSE_PENETRATION = new NamespacedKey("rpgitems", "defense_penetration");
    
    // PDC Keys - 伤害反弹
    private static final NamespacedKey KEY_DAMAGE_REFLECT = new NamespacedKey("rpgitems", "damage_reflect");
    private static final NamespacedKey KEY_REFLECT_RATIO = new NamespacedKey("rpgitems", "reflect_ratio");
    
    // PDC Keys - 状态效果
    private static final NamespacedKey KEY_POISON_CHANCE = new NamespacedKey("rpgitems", "poison_chance");
    private static final NamespacedKey KEY_FREEZE_CHANCE = new NamespacedKey("rpgitems", "freeze_chance");
    private static final NamespacedKey KEY_BLIND_CHANCE = new NamespacedKey("rpgitems", "blind_chance");
    private static final NamespacedKey KEY_BURN_CHANCE = new NamespacedKey("rpgitems", "burn_chance");
    private static final NamespacedKey KEY_SCORCH_CHANCE = new NamespacedKey("rpgitems", "scorch_chance");
    private static final NamespacedKey KEY_IGNITE_CHANCE = new NamespacedKey("rpgitems", "ignite_chance");
    private static final NamespacedKey KEY_SLOW_CHANCE = new NamespacedKey("rpgitems", "slow_chance");
    
    // PDC Keys - 环境抗性
    private static final NamespacedKey KEY_FIRE_RESIST = new NamespacedKey("rpgitems", "fire_resist");
    private static final NamespacedKey KEY_FALL_RESIST = new NamespacedKey("rpgitems", "fall_resist");
    private static final NamespacedKey KEY_DROWNING_RESIST = new NamespacedKey("rpgitems", "drowning_resist");
    private static final NamespacedKey KEY_POISON_RESIST = new NamespacedKey("rpgitems", "poison_resist");
    private static final NamespacedKey KEY_WITHER_RESIST = new NamespacedKey("rpgitems", "wither_resist");
    private static final NamespacedKey KEY_LAVA_RESIST = new NamespacedKey("rpgitems", "lava_resist");
    private static final NamespacedKey KEY_MAGIC_RESIST = new NamespacedKey("rpgitems", "magic_resist");
    private static final NamespacedKey KEY_EXPLOSION_RESIST = new NamespacedKey("rpgitems", "explosion_resist");
    private static final NamespacedKey KEY_PROJECTILE_RESIST = new NamespacedKey("rpgitems", "projectile_resist");
    
    // PDC Keys - 其他属性
    private static final NamespacedKey KEY_KNOCKBACK_RESIST = new NamespacedKey("rpgitems", "knockback_resist");
    private static final NamespacedKey KEY_EXP_BONUS = new NamespacedKey("rpgitems", "exp_bonus");
    private static final NamespacedKey KEY_HEALTH_REGEN_PERCENT = new NamespacedKey("rpgitems", "health_regen_percent");
    private static final NamespacedKey KEY_DODGE_REFLECT_CHANCE = new NamespacedKey("rpgitems", "dodge_reflect_chance");
    private static final NamespacedKey KEY_DODGE_REFLECT_RATIO = new NamespacedKey("rpgitems", "dodge_reflect_ratio");
    
    // PDC Keys - 装备等级和职业
    private static final NamespacedKey KEY_LEVEL = new NamespacedKey("rpgitems", "level");
    private static final NamespacedKey KEY_REQUIRED_CLASS = new NamespacedKey("rpgitems", "required_class");

    /**
     * 从 PDC 读取所有属性
     * 
     * @param item 物品
     * @return 属性对象，如果不是 RPGItems 物品则返回 null
     */
    public static ItemAttributes readAttributes(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        // 检查是否是 RPGItems 物品
        if (!pdc.has(KEY_ID, PersistentDataType.STRING)) {
            return null;
        }
        
        // 读取所有属性
        return new ItemAttributes(
            // 基础属性
            pdc.getOrDefault(KEY_ATTACK_MIN, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_ATTACK_MAX, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_DEFENSE_MIN, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_DEFENSE_MAX, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_MAX_HEALTH, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_HEALTH_REGEN, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_CRIT_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_CRIT_DAMAGE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_LIFESTEAL_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_LIFESTEAL_MULTIPLIER, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_DODGE_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_PARRY_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_MOVE_SPEED, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_DAMAGE_REDUCTION, PersistentDataType.DOUBLE, 0.0),
            // PVP属性
            pdc.getOrDefault(KEY_PVP_ATTACK_MIN, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_PVP_ATTACK_MAX, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_PVP_DEFENSE_MIN, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_PVP_DEFENSE_MAX, PersistentDataType.DOUBLE, 0.0),
            // 暴击抵抗
            pdc.getOrDefault(KEY_CRIT_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_CRIT_DAMAGE_RESIST, PersistentDataType.DOUBLE, 0.0),
            // 吸血抵抗
            pdc.getOrDefault(KEY_LIFESTEAL_RESIST, PersistentDataType.DOUBLE, 0.0),
            // 护甲与穿透
            pdc.getOrDefault(KEY_ARMOR, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_ARMOR_STRENGTH, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_ARMOR_PENETRATION, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_DEFENSE_PENETRATION, PersistentDataType.DOUBLE, 0.0),
            // 伤害反弹
            pdc.getOrDefault(KEY_DAMAGE_REFLECT, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_REFLECT_RATIO, PersistentDataType.DOUBLE, 0.0),
            // 状态效果
            pdc.getOrDefault(KEY_POISON_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_FREEZE_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_BLIND_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_BURN_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_SCORCH_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_IGNITE_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_SLOW_CHANCE, PersistentDataType.DOUBLE, 0.0),
            // 环境抗性
            pdc.getOrDefault(KEY_FIRE_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_FALL_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_DROWNING_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_POISON_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_WITHER_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_LAVA_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_MAGIC_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_EXPLOSION_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_PROJECTILE_RESIST, PersistentDataType.DOUBLE, 0.0),
            // 其他属性
            pdc.getOrDefault(KEY_KNOCKBACK_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_EXP_BONUS, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_HEALTH_REGEN_PERCENT, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_DODGE_REFLECT_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(KEY_DODGE_REFLECT_RATIO, PersistentDataType.DOUBLE, 0.0),
            // 装备等级和职业
            pdc.getOrDefault(KEY_LEVEL, PersistentDataType.INTEGER, 0),
            pdc.getOrDefault(KEY_REQUIRED_CLASS, PersistentDataType.STRING, "")
        );
    }
    
    /**
     * 获取物品 ID
     */
    public static String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(KEY_ID, PersistentDataType.STRING)) {
            return pdc.get(KEY_ID, PersistentDataType.STRING);
        }
        return null;
    }
    
    /**
     * 检查是否是 RPGItems 物品
     */
    public static boolean isRPGItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(KEY_ID, PersistentDataType.STRING);
    }
}
