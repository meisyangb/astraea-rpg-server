package cn.guangdian.rpgitems.attribute;

import org.bukkit.NamespacedKey;

/**
 * RPGItems PDC Key 常量类
 *
 * 所有 "rpgitems" 命名空间的 PDC key 统一在此定义。
 * 其他插件通过依赖 RPGItems 直接引用，无需各自重复定义。
 *
 * 替代了此前 5 个文件共约 240 次重复的 NamespacedKey 声明。
 */
public final class RPGItemsKeys {

    private static final String NS = "rpgitems";

    // ==================== 元数据 (STRING) ====================
    public static final NamespacedKey ID             = new NamespacedKey(NS, "id");
    public static final NamespacedKey TIER           = new NamespacedKey(NS, "tier");
    public static final NamespacedKey REQUIRED_CLASS = new NamespacedKey(NS, "required_class");
    public static final NamespacedKey GEM_TYPE       = new NamespacedKey(NS, "gem_type");
    public static final NamespacedKey ENHANCE_STONE  = new NamespacedKey(NS, "enhance_stone");

    // ==================== 等级 (INTEGER) ====================
    public static final NamespacedKey LEVEL = new NamespacedKey(NS, "level");

    // ==================== 宝石标记 (BYTE) ====================
    public static final NamespacedKey IS_GEM = new NamespacedKey(NS, "is_gem");

    // ==================== 复合属性 (BYTE_ARRAY) ====================
    public static final NamespacedKey ATTRS      = new NamespacedKey(NS, "attrs");
    public static final NamespacedKey BASE_ATTRS = new NamespacedKey(NS, "base_attrs");

    // ==================== 强化系统 (DOUBLE) ====================
    public static final NamespacedKey ENHANCE_MULT     = new NamespacedKey(NS, "enhance_multiplier");
    public static final NamespacedKey BASE_ATTACK_MIN  = new NamespacedKey(NS, "base_attack_min");
    public static final NamespacedKey BASE_ATTACK_MAX  = new NamespacedKey(NS, "base_attack_max");
    public static final NamespacedKey BASE_DEFENSE_MIN = new NamespacedKey(NS, "base_defense_min");
    public static final NamespacedKey BASE_DEFENSE_MAX = new NamespacedKey(NS, "base_defense_max");
    public static final NamespacedKey BASE_MAX_HEALTH  = new NamespacedKey(NS, "base_max_health");
    public static final NamespacedKey BASE_CRIT_CHANCE = new NamespacedKey(NS, "base_crit_chance");
    public static final NamespacedKey BASE_CRIT_DAMAGE = new NamespacedKey(NS, "base_crit_damage");

    // ==================== 旧格式 DOUBLE 属性 Key（48 个，向后兼容已有物品）====================
    public static final NamespacedKey ATTACK_MIN           = new NamespacedKey(NS, "attack_min");
    public static final NamespacedKey ATTACK_MAX           = new NamespacedKey(NS, "attack_max");
    public static final NamespacedKey DEFENSE_MIN          = new NamespacedKey(NS, "defense_min");
    public static final NamespacedKey DEFENSE_MAX          = new NamespacedKey(NS, "defense_max");
    public static final NamespacedKey MAX_HEALTH           = new NamespacedKey(NS, "max_health");
    public static final NamespacedKey HEALTH_REGEN         = new NamespacedKey(NS, "health_regen");
    public static final NamespacedKey CRIT_CHANCE          = new NamespacedKey(NS, "crit_chance");
    public static final NamespacedKey CRIT_DAMAGE          = new NamespacedKey(NS, "crit_damage");
    public static final NamespacedKey LIFESTEAL_CHANCE     = new NamespacedKey(NS, "lifesteal_chance");
    public static final NamespacedKey LIFESTEAL_MULTIPLIER = new NamespacedKey(NS, "lifesteal_multiplier");
    public static final NamespacedKey DODGE_CHANCE         = new NamespacedKey(NS, "dodge_chance");
    public static final NamespacedKey PARRY_CHANCE         = new NamespacedKey(NS, "parry_chance");
    public static final NamespacedKey MOVE_SPEED           = new NamespacedKey(NS, "move_speed");
    public static final NamespacedKey DAMAGE_REDUCTION     = new NamespacedKey(NS, "damage_reduction");
    public static final NamespacedKey PVP_ATTACK_MIN       = new NamespacedKey(NS, "pvp_attack_min");
    public static final NamespacedKey PVP_ATTACK_MAX       = new NamespacedKey(NS, "pvp_attack_max");
    public static final NamespacedKey PVP_DEFENSE_MIN      = new NamespacedKey(NS, "pvp_defense_min");
    public static final NamespacedKey PVP_DEFENSE_MAX      = new NamespacedKey(NS, "pvp_defense_max");
    public static final NamespacedKey CRIT_RESIST          = new NamespacedKey(NS, "crit_resist");
    public static final NamespacedKey CRIT_DAMAGE_RESIST   = new NamespacedKey(NS, "crit_damage_resist");
    public static final NamespacedKey LIFESTEAL_RESIST     = new NamespacedKey(NS, "lifesteal_resist");
    public static final NamespacedKey ARMOR                = new NamespacedKey(NS, "armor");
    public static final NamespacedKey ARMOR_STRENGTH       = new NamespacedKey(NS, "armor_strength");
    public static final NamespacedKey ARMOR_PENETRATION    = new NamespacedKey(NS, "armor_penetration");
    public static final NamespacedKey DEFENSE_PENETRATION  = new NamespacedKey(NS, "defense_penetration");
    public static final NamespacedKey DAMAGE_REFLECT       = new NamespacedKey(NS, "damage_reflect");
    public static final NamespacedKey REFLECT_RATIO        = new NamespacedKey(NS, "reflect_ratio");
    public static final NamespacedKey POISON_CHANCE        = new NamespacedKey(NS, "poison_chance");
    public static final NamespacedKey FREEZE_CHANCE        = new NamespacedKey(NS, "freeze_chance");
    public static final NamespacedKey BLIND_CHANCE         = new NamespacedKey(NS, "blind_chance");
    public static final NamespacedKey BURN_CHANCE          = new NamespacedKey(NS, "burn_chance");
    public static final NamespacedKey SCORCH_CHANCE        = new NamespacedKey(NS, "scorch_chance");
    public static final NamespacedKey IGNITE_CHANCE        = new NamespacedKey(NS, "ignite_chance");
    public static final NamespacedKey SLOW_CHANCE          = new NamespacedKey(NS, "slow_chance");
    public static final NamespacedKey FIRE_RESIST          = new NamespacedKey(NS, "fire_resist");
    public static final NamespacedKey FALL_RESIST          = new NamespacedKey(NS, "fall_resist");
    public static final NamespacedKey DROWNING_RESIST      = new NamespacedKey(NS, "drowning_resist");
    public static final NamespacedKey POISON_RESIST        = new NamespacedKey(NS, "poison_resist");
    public static final NamespacedKey WITHER_RESIST        = new NamespacedKey(NS, "wither_resist");
    public static final NamespacedKey LAVA_RESIST          = new NamespacedKey(NS, "lava_resist");
    public static final NamespacedKey MAGIC_RESIST         = new NamespacedKey(NS, "magic_resist");
    public static final NamespacedKey EXPLOSION_RESIST     = new NamespacedKey(NS, "explosion_resist");
    public static final NamespacedKey PROJECTILE_RESIST    = new NamespacedKey(NS, "projectile_resist");
    public static final NamespacedKey KNOCKBACK_RESIST     = new NamespacedKey(NS, "knockback_resist");
    public static final NamespacedKey EXP_BONUS            = new NamespacedKey(NS, "exp_bonus");
    public static final NamespacedKey HEALTH_REGEN_PERCENT = new NamespacedKey(NS, "health_regen_percent");
    public static final NamespacedKey DODGE_REFLECT_CHANCE = new NamespacedKey(NS, "dodge_reflect_chance");
    public static final NamespacedKey DODGE_REFLECT_RATIO  = new NamespacedKey(NS, "dodge_reflect_ratio");

    // ==================== Socket 系统 (STRING/INTEGER, 7 个槽位) ====================
    public static final NamespacedKey[] GEM      = slotKeys("gem_");
    public static final NamespacedKey[] SOCKET   = slotKeys("socket_");
    public static final NamespacedKey[] LORE_IDX = slotKeys("lore_idx_");

    private static NamespacedKey[] slotKeys(String prefix) {
        NamespacedKey[] arr = new NamespacedKey[7];
        for (int i = 0; i < 7; i++) {
            arr[i] = new NamespacedKey(NS, prefix + i);
        }
        return arr;
    }

    private RPGItemsKeys() {}
}