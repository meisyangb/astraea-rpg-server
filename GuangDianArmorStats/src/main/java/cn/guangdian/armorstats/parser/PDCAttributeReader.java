package cn.guangdian.armorstats.parser;

import cn.guangdian.armorstats.data.AttributeValue;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * PDC 属性读取器
 * 从物品的 PersistentDataContainer 读取属性数据
 * 
 * 性能优势：
 * - O(1) 直接读取，无需正则解析
 * - 类型安全，无需字符串转换
 * - 玩家无法修改，安全性高
 */
public class PDCAttributeReader {

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

    /**
     * 从 PDC 读取所有属性
     * 
     * @param item 物品
     * @return 属性映射，如果没有 PDC 数据则返回空 Map
     */
    public static Map<String, AttributeValue> readFromPDC(ItemStack item) {
        Map<String, AttributeValue> attrs = new HashMap<>();
        
        if (item == null || !item.hasItemMeta()) {
            return attrs;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return attrs;
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        // 检查是否有任何 RPGItems 数据（修复：检查多个可能的属性）
        if (!hasAnyRPGItemsData(pdc)) {
            return attrs; // 不是 RPGItems 物品
        }
        
        // 攻击属性
        if (pdc.has(KEY_ATTACK_MIN, PersistentDataType.DOUBLE)) {
            double min = pdc.get(KEY_ATTACK_MIN, PersistentDataType.DOUBLE);
            double max = pdc.has(KEY_ATTACK_MAX, PersistentDataType.DOUBLE)
                ? pdc.get(KEY_ATTACK_MAX, PersistentDataType.DOUBLE) : min;
            if (min > 0 || max > 0) {
                attrs.put("攻击力", AttributeValue.ofRange(min, max));
            }
        }

        // 防御属性
        if (pdc.has(KEY_DEFENSE_MIN, PersistentDataType.DOUBLE)) {
            double min = pdc.get(KEY_DEFENSE_MIN, PersistentDataType.DOUBLE);
            double max = pdc.has(KEY_DEFENSE_MAX, PersistentDataType.DOUBLE)
                ? pdc.get(KEY_DEFENSE_MAX, PersistentDataType.DOUBLE) : min;
            if (min > 0 || max > 0) {
                attrs.put("防御力", AttributeValue.ofRange(min, max));
            }
        }
        
        // 生命上限
        if (pdc.has(KEY_MAX_HEALTH, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_MAX_HEALTH, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("生命上限", AttributeValue.of(value));
            }
        }
        
        // 生命恢复
        if (pdc.has(KEY_HEALTH_REGEN, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_HEALTH_REGEN, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("生命回复", AttributeValue.of(value));
            }
        }
        
        // 暴击几率
        if (pdc.has(KEY_CRIT_CHANCE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_CRIT_CHANCE, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("暴击几率", AttributeValue.ofPercent(value));
            }
        }
        
        // 暴击伤害
        if (pdc.has(KEY_CRIT_DAMAGE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_CRIT_DAMAGE, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("暴击伤害", AttributeValue.ofPercent(value));
            }
        }
        
        // 吸血几率
        if (pdc.has(KEY_LIFESTEAL_CHANCE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_LIFESTEAL_CHANCE, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("吸血几率", AttributeValue.ofPercent(value));
            }
        }
        
        // 吸血倍率
        if (pdc.has(KEY_LIFESTEAL_MULTIPLIER, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_LIFESTEAL_MULTIPLIER, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("吸血倍率", AttributeValue.of(value));
            }
        }
        
        // 闪避
        if (pdc.has(KEY_DODGE_CHANCE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_DODGE_CHANCE, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("闪避", AttributeValue.ofPercent(value));
            }
        }
        
        // 格挡（招架）
        if (pdc.has(KEY_PARRY_CHANCE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_PARRY_CHANCE, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("招架", AttributeValue.ofPercent(value));
            }
        }
        
        // 移动速度
        if (pdc.has(KEY_MOVE_SPEED, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_MOVE_SPEED, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("移动速度", AttributeValue.ofPercent(value));
            }
        }
        
        // 减伤
        if (pdc.has(KEY_DAMAGE_REDUCTION, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_DAMAGE_REDUCTION, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("减伤", AttributeValue.ofPercent(value));
            }
        }

        // PVP攻击属性
        if (pdc.has(KEY_PVP_ATTACK_MIN, PersistentDataType.DOUBLE)) {
            double min = pdc.get(KEY_PVP_ATTACK_MIN, PersistentDataType.DOUBLE);
            double max = pdc.has(KEY_PVP_ATTACK_MAX, PersistentDataType.DOUBLE)
                ? pdc.get(KEY_PVP_ATTACK_MAX, PersistentDataType.DOUBLE) : min;
            if (min > 0 || max > 0) {
                attrs.put("PVP攻击力", AttributeValue.ofRange(min, max));
            }
        }

        // PVP防御属性
        if (pdc.has(KEY_PVP_DEFENSE_MIN, PersistentDataType.DOUBLE)) {
            double min = pdc.get(KEY_PVP_DEFENSE_MIN, PersistentDataType.DOUBLE);
            double max = pdc.has(KEY_PVP_DEFENSE_MAX, PersistentDataType.DOUBLE)
                ? pdc.get(KEY_PVP_DEFENSE_MAX, PersistentDataType.DOUBLE) : min;
            if (min > 0 || max > 0) {
                attrs.put("PVP防御力", AttributeValue.ofRange(min, max));
            }
        }

        // 暴击抵抗
        if (pdc.has(KEY_CRIT_RESIST, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_CRIT_RESIST, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("暴击抵抗", AttributeValue.ofPercent(value));
            }
        }

        // 暴伤抵抗
        if (pdc.has(KEY_CRIT_DAMAGE_RESIST, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_CRIT_DAMAGE_RESIST, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("暴伤抵抗", AttributeValue.ofPercent(value));
            }
        }

        // 吸血抵抗
        if (pdc.has(KEY_LIFESTEAL_RESIST, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_LIFESTEAL_RESIST, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("吸血抵抗", AttributeValue.ofPercent(value));
            }
        }

        // 护甲值
        if (pdc.has(KEY_ARMOR, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_ARMOR, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("护甲值", AttributeValue.of(value));
            }
        }

        // 护甲强度
        if (pdc.has(KEY_ARMOR_STRENGTH, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_ARMOR_STRENGTH, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("护甲强度", AttributeValue.of(value));
            }
        }

        // 护甲穿透
        if (pdc.has(KEY_ARMOR_PENETRATION, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_ARMOR_PENETRATION, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("护甲穿透", AttributeValue.of(value));
            }
        }

        // 防御穿透
        if (pdc.has(KEY_DEFENSE_PENETRATION, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_DEFENSE_PENETRATION, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("防御穿透", AttributeValue.of(value));
            }
        }

        // 伤害反弹
        if (pdc.has(KEY_DAMAGE_REFLECT, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_DAMAGE_REFLECT, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("伤害反弹", AttributeValue.of(value));
            }
        }

        // 反伤比例
        if (pdc.has(KEY_REFLECT_RATIO, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_REFLECT_RATIO, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("反伤比例", AttributeValue.ofPercent(value));
            }
        }

        // 中毒几率
        if (pdc.has(KEY_POISON_CHANCE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_POISON_CHANCE, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("中毒", AttributeValue.ofPercent(value));
            }
        }

        // 冰冻几率
        if (pdc.has(KEY_FREEZE_CHANCE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_FREEZE_CHANCE, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("冰冻", AttributeValue.ofPercent(value));
            }
        }

        // 致盲几率
        if (pdc.has(KEY_BLIND_CHANCE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_BLIND_CHANCE, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("致盲", AttributeValue.ofPercent(value));
            }
        }

        // 燃烧几率
        if (pdc.has(KEY_BURN_CHANCE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_BURN_CHANCE, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("燃烧", AttributeValue.ofPercent(value));
            }
        }

        // 灼烧几率
        if (pdc.has(KEY_SCORCH_CHANCE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_SCORCH_CHANCE, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("灼烧", AttributeValue.ofPercent(value));
            }
        }

        // 火焰抗性
        if (pdc.has(KEY_FIRE_RESIST, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_FIRE_RESIST, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("火焰抗性", AttributeValue.ofPercent(value));
            }
        }

        // 摔落抗性
        if (pdc.has(KEY_FALL_RESIST, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_FALL_RESIST, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("摔落抗性", AttributeValue.ofPercent(value));
            }
        }

        // 溺水抗性
        if (pdc.has(KEY_DROWNING_RESIST, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_DROWNING_RESIST, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("溺水抗性", AttributeValue.ofPercent(value));
            }
        }

        // 中毒抗性
        if (pdc.has(KEY_POISON_RESIST, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_POISON_RESIST, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("中毒抗性", AttributeValue.ofPercent(value));
            }
        }

        // 凋零抗性
        if (pdc.has(KEY_WITHER_RESIST, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_WITHER_RESIST, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("凋零抗性", AttributeValue.ofPercent(value));
            }
        }

        // 岩浆抗性
        if (pdc.has(KEY_LAVA_RESIST, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_LAVA_RESIST, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("岩浆抗性", AttributeValue.ofPercent(value));
            }
        }

        // 魔法抗性
        if (pdc.has(KEY_MAGIC_RESIST, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_MAGIC_RESIST, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("魔法抗性", AttributeValue.ofPercent(value));
            }
        }

        // 爆炸抗性
        if (pdc.has(KEY_EXPLOSION_RESIST, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_EXPLOSION_RESIST, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("爆炸抗性", AttributeValue.ofPercent(value));
            }
        }

        // 弹射物抗性
        if (pdc.has(KEY_PROJECTILE_RESIST, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_PROJECTILE_RESIST, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("弹射物抗性", AttributeValue.ofPercent(value));
            }
        }

        // 击退抗性
        if (pdc.has(KEY_KNOCKBACK_RESIST, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_KNOCKBACK_RESIST, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("击退抗性", AttributeValue.ofPercent(value));
            }
        }

        // 经验加成
        if (pdc.has(KEY_EXP_BONUS, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_EXP_BONUS, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("经验加成", AttributeValue.ofPercent(value));
            }
        }

        // 生命恢复百分比
        if (pdc.has(KEY_HEALTH_REGEN_PERCENT, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_HEALTH_REGEN_PERCENT, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("生命恢复", AttributeValue.ofPercent(value));
            }
        }

        // 躲避反伤几率
        if (pdc.has(KEY_DODGE_REFLECT_CHANCE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_DODGE_REFLECT_CHANCE, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("躲避反伤", AttributeValue.ofPercent(value));
            }
        }

        // 躲避反弹比例
        if (pdc.has(KEY_DODGE_REFLECT_RATIO, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_DODGE_REFLECT_RATIO, PersistentDataType.DOUBLE);
            if (value > 0) {
                attrs.put("躲避反弹比例", AttributeValue.ofPercent(value));
            }
        }

        return attrs;
    }
    
    /**
     * 从 PDC 读取所有属性（带详细调试日志）
     * 
     * @param item 物品
     * @param debugPrefix 调试日志前缀
     * @return 属性映射，如果没有 PDC 数据则返回空 Map
     */
    public static Map<String, AttributeValue> readFromPDCWithDebug(ItemStack item, String debugPrefix) {
        Map<String, AttributeValue> attrs = new HashMap<>();
        
        if (item == null) {
            System.out.println(debugPrefix + "物品为 null");
            return attrs;
        }
        
        System.out.println(debugPrefix + "物品类型: " + item.getType());
        
        if (!item.hasItemMeta()) {
            System.out.println(debugPrefix + "物品没有 ItemMeta");
            return attrs;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            System.out.println(debugPrefix + "ItemMeta 为 null");
            return attrs;
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        // 输出 PDC 中所有的 keys
        System.out.println(debugPrefix + "PDC Keys 数量: " + pdc.getKeys().size());
        for (org.bukkit.NamespacedKey key : pdc.getKeys()) {
            System.out.println(debugPrefix + "  PDC Key: " + key.toString());
        }
        
        // 检查是否有任何 RPGItems 数据
        if (!hasAnyRPGItemsData(pdc)) {
            System.out.println(debugPrefix + "不是 RPGItems 物品（没有匹配的 PDC Keys）");
            return attrs;
        }
        
        System.out.println(debugPrefix + "=== 开始读取 RPGItems PDC 属性 ===");
        
        // 攻击属性
        if (pdc.has(KEY_ATTACK_MIN, PersistentDataType.DOUBLE)) {
            double min = pdc.get(KEY_ATTACK_MIN, PersistentDataType.DOUBLE);
            double max = pdc.has(KEY_ATTACK_MAX, PersistentDataType.DOUBLE)
                ? pdc.get(KEY_ATTACK_MAX, PersistentDataType.DOUBLE) : min;
            System.out.println(debugPrefix + "  [PDC读取] 攻击力: " + min + " - " + max);
            if (min > 0 || max > 0) {
                attrs.put("攻击力", AttributeValue.ofRange(min, max));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 攻击力: 无数据");
        }
        
        // 防御属性
        if (pdc.has(KEY_DEFENSE_MIN, PersistentDataType.DOUBLE)) {
            double min = pdc.get(KEY_DEFENSE_MIN, PersistentDataType.DOUBLE);
            double max = pdc.has(KEY_DEFENSE_MAX, PersistentDataType.DOUBLE)
                ? pdc.get(KEY_DEFENSE_MAX, PersistentDataType.DOUBLE) : min;
            System.out.println(debugPrefix + "  [PDC读取] 防御力: " + min + " - " + max);
            if (min > 0 || max > 0) {
                attrs.put("防御力", AttributeValue.ofRange(min, max));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 防御力: 无数据");
        }
        
        // 生命上限
        if (pdc.has(KEY_MAX_HEALTH, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_MAX_HEALTH, PersistentDataType.DOUBLE);
            System.out.println(debugPrefix + "  [PDC读取] 生命上限: " + value);
            if (value > 0) {
                attrs.put("生命上限", AttributeValue.of(value));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 生命上限: 无数据");
        }
        
        // 生命恢复
        if (pdc.has(KEY_HEALTH_REGEN, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_HEALTH_REGEN, PersistentDataType.DOUBLE);
            System.out.println(debugPrefix + "  [PDC读取] 生命回复: " + value);
            if (value > 0) {
                attrs.put("生命回复", AttributeValue.of(value));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 生命回复: 无数据");
        }
        
        // 暴击几率
        if (pdc.has(KEY_CRIT_CHANCE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_CRIT_CHANCE, PersistentDataType.DOUBLE);
            System.out.println(debugPrefix + "  [PDC读取] 暴击几率: " + value + "%");
            if (value > 0) {
                attrs.put("暴击几率", AttributeValue.ofPercent(value));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 暴击几率: 无数据");
        }
        
        // 暴击伤害
        if (pdc.has(KEY_CRIT_DAMAGE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_CRIT_DAMAGE, PersistentDataType.DOUBLE);
            System.out.println(debugPrefix + "  [PDC读取] 暴击伤害: " + value + "%");
            if (value > 0) {
                attrs.put("暴击伤害", AttributeValue.ofPercent(value));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 暴击伤害: 无数据");
        }
        
        // 吸血几率
        if (pdc.has(KEY_LIFESTEAL_CHANCE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_LIFESTEAL_CHANCE, PersistentDataType.DOUBLE);
            System.out.println(debugPrefix + "  [PDC读取] 吸血几率: " + value + "%");
            if (value > 0) {
                attrs.put("吸血几率", AttributeValue.ofPercent(value));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 吸血几率: 无数据");
        }
        
        // 吸血倍率
        if (pdc.has(KEY_LIFESTEAL_MULTIPLIER, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_LIFESTEAL_MULTIPLIER, PersistentDataType.DOUBLE);
            System.out.println(debugPrefix + "  [PDC读取] 吸血倍率: " + value);
            if (value > 0) {
                attrs.put("吸血倍率", AttributeValue.of(value));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 吸血倍率: 无数据");
        }
        
        // 闪避
        if (pdc.has(KEY_DODGE_CHANCE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_DODGE_CHANCE, PersistentDataType.DOUBLE);
            System.out.println(debugPrefix + "  [PDC读取] 闪避: " + value + "%");
            if (value > 0) {
                attrs.put("闪避", AttributeValue.ofPercent(value));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 闪避: 无数据");
        }
        
        // 格挡（招架）
        if (pdc.has(KEY_PARRY_CHANCE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_PARRY_CHANCE, PersistentDataType.DOUBLE);
            System.out.println(debugPrefix + "  [PDC读取] 招架: " + value + "%");
            if (value > 0) {
                attrs.put("招架", AttributeValue.ofPercent(value));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 招架: 无数据");
        }
        
        // 移动速度
        if (pdc.has(KEY_MOVE_SPEED, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_MOVE_SPEED, PersistentDataType.DOUBLE);
            System.out.println(debugPrefix + "  [PDC读取] 移动速度: " + value + "%");
            if (value > 0) {
                attrs.put("移动速度", AttributeValue.ofPercent(value));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 积动速度: 无数据");
        }
        
        // 减伤
        if (pdc.has(KEY_DAMAGE_REDUCTION, PersistentDataType.DOUBLE)) {
            double value = pdc.get(KEY_DAMAGE_REDUCTION, PersistentDataType.DOUBLE);
            System.out.println(debugPrefix + "  [PDC读取] 减伤: " + value + "%");
            if (value > 0) {
                attrs.put("减伤", AttributeValue.ofPercent(value));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 减伤: 无数据");
        }
        
        System.out.println(debugPrefix + "=== PDC 属性读取完成，共 " + attrs.size() + " 个属性 ===");
        
        return attrs;
    }
    
    /**
     * 检查物品是否是 RPGItems 物品
     */
    public static boolean isRPGItemsItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return hasAnyRPGItemsData(pdc);
    }
    
    /**
     * 检查 PDC 是否包含任何 RPGItems 属性数据
     *
     * 修复：检查多个可能的属性，避免只有防御属性的物品被误判
     */
    private static boolean hasAnyRPGItemsData(PersistentDataContainer pdc) {
        return pdc.has(KEY_ATTACK_MIN, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_ATTACK_MAX, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_DEFENSE_MIN, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_DEFENSE_MAX, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_MAX_HEALTH, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_HEALTH_REGEN, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_CRIT_CHANCE, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_CRIT_DAMAGE, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_LIFESTEAL_CHANCE, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_LIFESTEAL_MULTIPLIER, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_DODGE_CHANCE, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_PARRY_CHANCE, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_MOVE_SPEED, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_DAMAGE_REDUCTION, PersistentDataType.DOUBLE) ||
               // PVP属性
               pdc.has(KEY_PVP_ATTACK_MIN, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_PVP_ATTACK_MAX, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_PVP_DEFENSE_MIN, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_PVP_DEFENSE_MAX, PersistentDataType.DOUBLE) ||
               // 暴击抵抗
               pdc.has(KEY_CRIT_RESIST, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_CRIT_DAMAGE_RESIST, PersistentDataType.DOUBLE) ||
               // 吸血抵抗
               pdc.has(KEY_LIFESTEAL_RESIST, PersistentDataType.DOUBLE) ||
               // 护甲与穿透
               pdc.has(KEY_ARMOR, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_ARMOR_STRENGTH, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_ARMOR_PENETRATION, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_DEFENSE_PENETRATION, PersistentDataType.DOUBLE) ||
               // 伤害反弹
               pdc.has(KEY_DAMAGE_REFLECT, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_REFLECT_RATIO, PersistentDataType.DOUBLE) ||
               // 状态效果
               pdc.has(KEY_POISON_CHANCE, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_FREEZE_CHANCE, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_BLIND_CHANCE, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_BURN_CHANCE, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_SCORCH_CHANCE, PersistentDataType.DOUBLE) ||
               // 环境抗性
               pdc.has(KEY_FIRE_RESIST, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_FALL_RESIST, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_DROWNING_RESIST, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_POISON_RESIST, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_WITHER_RESIST, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_LAVA_RESIST, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_MAGIC_RESIST, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_EXPLOSION_RESIST, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_PROJECTILE_RESIST, PersistentDataType.DOUBLE) ||
               // 其他属性
               pdc.has(KEY_KNOCKBACK_RESIST, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_EXP_BONUS, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_HEALTH_REGEN_PERCENT, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_DODGE_REFLECT_CHANCE, PersistentDataType.DOUBLE) ||
               pdc.has(KEY_DODGE_REFLECT_RATIO, PersistentDataType.DOUBLE);
    }
}
