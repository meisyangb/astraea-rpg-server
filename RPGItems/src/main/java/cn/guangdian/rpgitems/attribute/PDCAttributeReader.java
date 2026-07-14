package cn.guangdian.rpgitems.attribute;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * PDC 属性读取器
 * 从物品的 PersistentDataContainer 读取属性数据
 */
public class PDCAttributeReader {

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
        if (!pdc.has(RPGItemsKeys.ID, PersistentDataType.STRING)) {
            return null;
        }

        // 优先读取复合存储格式
        byte[] compoundData = pdc.get(RPGItemsKeys.ATTRS, PersistentDataType.BYTE_ARRAY);
        if (compoundData != null) {
            double[] v = CompoundAttributeCodec.deserialize(compoundData);
            return new ItemAttributes(
                v[CompoundAttributeCodec.ATTACK_MIN], v[CompoundAttributeCodec.ATTACK_MAX],
                v[CompoundAttributeCodec.DEFENSE_MIN], v[CompoundAttributeCodec.DEFENSE_MAX],
                v[CompoundAttributeCodec.MAX_HEALTH], v[CompoundAttributeCodec.HEALTH_REGEN],
                v[CompoundAttributeCodec.CRIT_CHANCE], v[CompoundAttributeCodec.CRIT_DAMAGE],
                v[CompoundAttributeCodec.LIFESTEAL_CHANCE], v[CompoundAttributeCodec.LIFESTEAL_MULTIPLIER],
                v[CompoundAttributeCodec.DODGE_CHANCE], v[CompoundAttributeCodec.PARRY_CHANCE],
                v[CompoundAttributeCodec.MOVE_SPEED], v[CompoundAttributeCodec.DAMAGE_REDUCTION],
                v[CompoundAttributeCodec.PVP_ATTACK_MIN], v[CompoundAttributeCodec.PVP_ATTACK_MAX],
                v[CompoundAttributeCodec.PVP_DEFENSE_MIN], v[CompoundAttributeCodec.PVP_DEFENSE_MAX],
                v[CompoundAttributeCodec.CRIT_RESIST], v[CompoundAttributeCodec.CRIT_DAMAGE_RESIST],
                v[CompoundAttributeCodec.LIFESTEAL_RESIST],
                v[CompoundAttributeCodec.ARMOR], v[CompoundAttributeCodec.ARMOR_STRENGTH],
                v[CompoundAttributeCodec.ARMOR_PENETRATION], v[CompoundAttributeCodec.DEFENSE_PENETRATION],
                v[CompoundAttributeCodec.DAMAGE_REFLECT], v[CompoundAttributeCodec.REFLECT_RATIO],
                v[CompoundAttributeCodec.POISON_CHANCE], v[CompoundAttributeCodec.FREEZE_CHANCE],
                v[CompoundAttributeCodec.BLIND_CHANCE], v[CompoundAttributeCodec.BURN_CHANCE],
                v[CompoundAttributeCodec.SCORCH_CHANCE], v[CompoundAttributeCodec.IGNITE_CHANCE],
                v[CompoundAttributeCodec.SLOW_CHANCE],
                v[CompoundAttributeCodec.FIRE_RESIST], v[CompoundAttributeCodec.FALL_RESIST],
                v[CompoundAttributeCodec.DROWNING_RESIST], v[CompoundAttributeCodec.POISON_RESIST],
                v[CompoundAttributeCodec.WITHER_RESIST], v[CompoundAttributeCodec.LAVA_RESIST],
                v[CompoundAttributeCodec.MAGIC_RESIST], v[CompoundAttributeCodec.EXPLOSION_RESIST],
                v[CompoundAttributeCodec.PROJECTILE_RESIST],
                v[CompoundAttributeCodec.KNOCKBACK_RESIST], v[CompoundAttributeCodec.EXP_BONUS],
                v[CompoundAttributeCodec.HEALTH_REGEN_PERCENT],
                v[CompoundAttributeCodec.DODGE_REFLECT_CHANCE], v[CompoundAttributeCodec.DODGE_REFLECT_RATIO],
                pdc.getOrDefault(RPGItemsKeys.LEVEL, PersistentDataType.INTEGER, 0),
                pdc.getOrDefault(RPGItemsKeys.REQUIRED_CLASS, PersistentDataType.STRING, "")
            );
        }
        // 回退到旧格式（向后兼容已有物品）
        return new ItemAttributes(
            pdc.getOrDefault(RPGItemsKeys.ATTACK_MIN, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.ATTACK_MAX, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.DEFENSE_MIN, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.DEFENSE_MAX, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.MAX_HEALTH, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.HEALTH_REGEN, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.CRIT_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.CRIT_DAMAGE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.LIFESTEAL_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.LIFESTEAL_MULTIPLIER, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.DODGE_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.PARRY_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.MOVE_SPEED, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.DAMAGE_REDUCTION, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.PVP_ATTACK_MIN, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.PVP_ATTACK_MAX, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.PVP_DEFENSE_MIN, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.PVP_DEFENSE_MAX, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.CRIT_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.CRIT_DAMAGE_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.LIFESTEAL_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.ARMOR, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.ARMOR_STRENGTH, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.ARMOR_PENETRATION, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.DEFENSE_PENETRATION, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.DAMAGE_REFLECT, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.REFLECT_RATIO, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.POISON_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.FREEZE_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.BLIND_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.BURN_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.SCORCH_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.IGNITE_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.SLOW_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.FIRE_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.FALL_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.DROWNING_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.POISON_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.WITHER_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.LAVA_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.MAGIC_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.EXPLOSION_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.PROJECTILE_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.KNOCKBACK_RESIST, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.EXP_BONUS, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.HEALTH_REGEN_PERCENT, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.DODGE_REFLECT_CHANCE, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.DODGE_REFLECT_RATIO, PersistentDataType.DOUBLE, 0.0),
            pdc.getOrDefault(RPGItemsKeys.LEVEL, PersistentDataType.INTEGER, 0),
            pdc.getOrDefault(RPGItemsKeys.REQUIRED_CLASS, PersistentDataType.STRING, "")
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
        if (pdc.has(RPGItemsKeys.ID, PersistentDataType.STRING)) {
            return pdc.get(RPGItemsKeys.ID, PersistentDataType.STRING);
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
        return pdc.has(RPGItemsKeys.ID, PersistentDataType.STRING);
    }
}
