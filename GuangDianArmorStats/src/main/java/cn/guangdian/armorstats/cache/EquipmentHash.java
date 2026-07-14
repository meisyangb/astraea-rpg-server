package cn.guangdian.armorstats.cache;

import cn.guangdian.rpgitems.attribute.RPGItemsKeys;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

/**
 * 装备哈希计算器
 *
 * 枚举法：逐个 key 直接用已知类型读取，不用 getKeys() 循环和类型猜测。
 * 稳定、高性能、无 IllegalArgumentException 风险。
 */
public class EquipmentHash {

    private static final int PRIME = 31;

    /** 旧格式 DOUBLE key 列表（静态常量，避免每次创建） */
    private static final NamespacedKey[] OLD_DOUBLE_KEYS = {
        RPGItemsKeys.ATTACK_MIN, RPGItemsKeys.ATTACK_MAX, RPGItemsKeys.DEFENSE_MIN, RPGItemsKeys.DEFENSE_MAX,
        RPGItemsKeys.MAX_HEALTH, RPGItemsKeys.HEALTH_REGEN, RPGItemsKeys.CRIT_CHANCE, RPGItemsKeys.CRIT_DAMAGE,
        RPGItemsKeys.LIFESTEAL_CHANCE, RPGItemsKeys.LIFESTEAL_MULTIPLIER, RPGItemsKeys.DODGE_CHANCE, RPGItemsKeys.PARRY_CHANCE,
        RPGItemsKeys.MOVE_SPEED, RPGItemsKeys.DAMAGE_REDUCTION, RPGItemsKeys.PVP_ATTACK_MIN, RPGItemsKeys.PVP_ATTACK_MAX,
        RPGItemsKeys.PVP_DEFENSE_MIN, RPGItemsKeys.PVP_DEFENSE_MAX, RPGItemsKeys.CRIT_RESIST, RPGItemsKeys.CRIT_DAMAGE_RESIST,
        RPGItemsKeys.LIFESTEAL_RESIST, RPGItemsKeys.ARMOR, RPGItemsKeys.ARMOR_STRENGTH, RPGItemsKeys.ARMOR_PENETRATION,
        RPGItemsKeys.DEFENSE_PENETRATION, RPGItemsKeys.DAMAGE_REFLECT, RPGItemsKeys.REFLECT_RATIO, RPGItemsKeys.POISON_CHANCE,
        RPGItemsKeys.FREEZE_CHANCE, RPGItemsKeys.BLIND_CHANCE, RPGItemsKeys.BURN_CHANCE, RPGItemsKeys.SCORCH_CHANCE,
        RPGItemsKeys.IGNITE_CHANCE, RPGItemsKeys.SLOW_CHANCE, RPGItemsKeys.FIRE_RESIST, RPGItemsKeys.FALL_RESIST,
        RPGItemsKeys.DROWNING_RESIST, RPGItemsKeys.POISON_RESIST, RPGItemsKeys.WITHER_RESIST, RPGItemsKeys.LAVA_RESIST,
        RPGItemsKeys.MAGIC_RESIST, RPGItemsKeys.EXPLOSION_RESIST, RPGItemsKeys.PROJECTILE_RESIST, RPGItemsKeys.KNOCKBACK_RESIST,
        RPGItemsKeys.EXP_BONUS, RPGItemsKeys.HEALTH_REGEN_PERCENT, RPGItemsKeys.DODGE_REFLECT_CHANCE, RPGItemsKeys.DODGE_REFLECT_RATIO,
    };

    /** 强化基础属性 key 列表 */
    private static final NamespacedKey[] BASE_DOUBLE_KEYS = {
        RPGItemsKeys.BASE_ATTACK_MIN, RPGItemsKeys.BASE_ATTACK_MAX, RPGItemsKeys.BASE_DEFENSE_MIN, RPGItemsKeys.BASE_DEFENSE_MAX,
        RPGItemsKeys.BASE_MAX_HEALTH, RPGItemsKeys.BASE_CRIT_CHANCE, RPGItemsKeys.BASE_CRIT_DAMAGE,
    };

    // ================================================================
    // 公共 API
    // ================================================================

    public static String calculate(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "EMPTY";
        }
        StringBuilder sb = new StringBuilder(64);
        sb.append(item.getType().name()).append(":");
        sb.append(calculateContentHash(item));
        return sb.toString();
    }

    public static String calculateFull(ItemStack item) {
        return calculate(item);
    }

    // ================================================================
    // 内部实现 - 枚举法
    // ================================================================

    private static int calculateContentHash(ItemStack item) {
        int hash = 1;
        hash = hash * PRIME + item.getAmount();
        hash = hash * PRIME + (int) item.getDurability();

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                hash = hash * PRIME + meta.getDisplayName().hashCode();
            }
            if (meta.hasLore()) {
                List<String> lore = meta.getLore();
                if (lore != null) {
                    for (String line : lore) {
                        hash = hash * PRIME + line.hashCode();
                    }
                }
            }
            if (meta.hasEnchants()) {
                hash = hash * PRIME + meta.getEnchants().hashCode();
            }
            if (meta.hasCustomModelData()) {
                hash = hash * PRIME + meta.getCustomModelData();
            }
            hash = hash * PRIME + calculatePDCHash(meta.getPersistentDataContainer());
        }
        return hash;
    }

    /**
     * 枚举法计算 PDC 哈希
     *
     * 每个 key 类型已知，直接用 has + get 读取，不做类型猜测。
     * 无 IllegalArgumentException 风险，稳定高性能。
     */
    private static int calculatePDCHash(PersistentDataContainer pdc) {
        if (pdc == null) return 0;
        int hash = 0;

        // === STRING 类型：元数据 ===
        hash = hashString(pdc, RPGItemsKeys.ID, hash);
        hash = hashString(pdc, RPGItemsKeys.TIER, hash);
        hash = hashString(pdc, RPGItemsKeys.REQUIRED_CLASS, hash);
        hash = hashString(pdc, RPGItemsKeys.GEM_TYPE, hash);

        // === INTEGER 类型 ===
        hash = hashInteger(pdc, RPGItemsKeys.LEVEL, hash);

        // === BYTE 类型 ===
        if (pdc.has(RPGItemsKeys.IS_GEM, PersistentDataType.BYTE)) {
            hash = hash * PRIME + pdc.get(RPGItemsKeys.IS_GEM, PersistentDataType.BYTE);
        }

        // === BYTE_ARRAY 类型：复合属性（核心）===
        hash = hashByteArray(pdc, RPGItemsKeys.ATTRS, hash);
        hash = hashByteArray(pdc, RPGItemsKeys.BASE_ATTRS, hash);

        // === DOUBLE 类型：强化倍率 ===
        hash = hashDouble(pdc, RPGItemsKeys.ENHANCE_MULT, hash);

        // === DOUBLE 类型：强化基础属性（旧格式）===
        for (NamespacedKey key : BASE_DOUBLE_KEYS) {
            hash = hashDouble(pdc, key, hash);
        }

        // === Socket 系统：7 个槽位 ===
        // 槽位 0
        hash = hashString(pdc, RPGItemsKeys.GEM[0], hash);
        hash = hashString(pdc, RPGItemsKeys.SOCKET[0], hash);
        hash = hashInteger(pdc, RPGItemsKeys.LORE_IDX[0], hash);
        // 槽位 1
        hash = hashString(pdc, RPGItemsKeys.GEM[1], hash);
        hash = hashString(pdc, RPGItemsKeys.SOCKET[1], hash);
        hash = hashInteger(pdc, RPGItemsKeys.LORE_IDX[1], hash);
        // 槽位 2
        hash = hashString(pdc, RPGItemsKeys.GEM[2], hash);
        hash = hashString(pdc, RPGItemsKeys.SOCKET[2], hash);
        hash = hashInteger(pdc, RPGItemsKeys.LORE_IDX[2], hash);
        // 槽位 3
        hash = hashString(pdc, RPGItemsKeys.GEM[3], hash);
        hash = hashString(pdc, RPGItemsKeys.SOCKET[3], hash);
        hash = hashInteger(pdc, RPGItemsKeys.LORE_IDX[3], hash);
        // 槽位 4
        hash = hashString(pdc, RPGItemsKeys.GEM[4], hash);
        hash = hashString(pdc, RPGItemsKeys.SOCKET[4], hash);
        hash = hashInteger(pdc, RPGItemsKeys.LORE_IDX[4], hash);
        // 槽位 5
        hash = hashString(pdc, RPGItemsKeys.GEM[5], hash);
        hash = hashString(pdc, RPGItemsKeys.SOCKET[5], hash);
        hash = hashInteger(pdc, RPGItemsKeys.LORE_IDX[5], hash);
        // 槽位 6
        hash = hashString(pdc, RPGItemsKeys.GEM[6], hash);
        hash = hashString(pdc, RPGItemsKeys.SOCKET[6], hash);
        hash = hashInteger(pdc, RPGItemsKeys.LORE_IDX[6], hash);

        // === 旧格式 DOUBLE 属性（向后兼容已有物品）===
        for (NamespacedKey key : OLD_DOUBLE_KEYS) {
            hash = hashDouble(pdc, key, hash);
        }

        return hash;
    }

    // ================================================================
    // 类型安全的哈希辅助方法 - 无类型猜测，直接用已知类型读取
    // ================================================================

    private static int hashString(PersistentDataContainer pdc, NamespacedKey key, int hash) {
        if (pdc.has(key, PersistentDataType.STRING)) {
            return hash * PRIME + pdc.get(key, PersistentDataType.STRING).hashCode();
        }
        return hash;
    }

    private static int hashInteger(PersistentDataContainer pdc, NamespacedKey key, int hash) {
        if (pdc.has(key, PersistentDataType.INTEGER)) {
            return hash * PRIME + pdc.get(key, PersistentDataType.INTEGER);
        }
        return hash;
    }

    private static int hashDouble(PersistentDataContainer pdc, NamespacedKey key, int hash) {
        if (pdc.has(key, PersistentDataType.DOUBLE)) {
            return hash * PRIME + Double.hashCode(pdc.get(key, PersistentDataType.DOUBLE));
        }
        return hash;
    }

    private static int hashByteArray(PersistentDataContainer pdc, NamespacedKey key, int hash) {
        if (pdc.has(key, PersistentDataType.BYTE_ARRAY)) {
            return hash * PRIME + Arrays.hashCode(pdc.get(key, PersistentDataType.BYTE_ARRAY));
        }
        return hash;
    }
}