package cn.guangdian.armorstats.parser;

import cn.guangdian.armorstats.data.AttributeValue;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    /**
     * 属性缓存：物品哈希 -> 属性 Map
     *
     * 原理：用包含 PDC 数据的物品哈希做缓存 key
     * 当宝石镶嵌/强化/锻造修改 PDC 时，哈希自动变化，缓存自动失效
     * 相同状态的物品（相同 PDC）只解析一次，后续直接查缓存
     *
     * 性能对比：
     *   优化前：每次装备变化 = 1 次 pdc.has + 40+ 次 pdc.getOrDefault
     *   优化后：首次 = 1 次哈希计算 + 40+ 次 pdc.getOrDefault
     *         后续 = 1 次哈希计算 + 1 次 ConcurrentHashMap.get（零 PDC 访问）
     */
    private static final Map<String, Map<String, AttributeValue>> attrCache = new ConcurrentHashMap<>();

    // PDC Keys - 物品标识（RPGItems 创建物品时写入，用作快速判断标记）
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
     * 从 PDC 读取所有属性（带哈希缓存）
     *
     * 缓存策略：用包含 PDC 数据的物品哈希做 key
     * - 首次：计算哈希 + 40+ 次 pdc.getOrDefault
     * - 后续：计算哈希 + 1 次 ConcurrentHashMap.get（零 PDC 访问）
     * - 宝石镶嵌/强化/锻造修改 PDC → 哈希变化 → 缓存自动失效 → 重新解析
     *
     * @param item 物品
     * @return 属性映射，如果没有 PDC 数据则返回空 Map
     */
    public static Map<String, AttributeValue> readFromPDC(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Collections.emptyMap();
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Collections.emptyMap();
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // O(1) 标记检查
        if (!hasAnyRPGItemsData(pdc)) {
            return Collections.emptyMap();
        }

        // 计算物品哈希（包含 PDC 数据）
        String itemHash = cn.guangdian.armorstats.cache.EquipmentHash.calculate(item);

        // 查缓存
        Map<String, AttributeValue> cached = attrCache.get(itemHash);
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，解析 PDC
        Map<String, AttributeValue> attrs = parseAllPDCAttributes(pdc);

        // 写入缓存（上限 1000，避免内存膨胀）
        if (attrCache.size() < 1000) {
            attrCache.put(itemHash, attrs);
        }

        return attrs;
    }

    /**
     * 实际解析所有 PDC 属性（原 readFromPDC 逻辑）
     */
    private static Map<String, AttributeValue> parseAllPDCAttributes(PersistentDataContainer pdc) {
        // 优先读取复合存储格式
        byte[] compoundData = pdc.get(CompoundAttributeCodec.KEY_COMPOUND, PersistentDataType.BYTE_ARRAY);
        if (compoundData != null) {
            return parseFromCompound(compoundData);
        }
        // 回退到旧格式（向后兼容已有物品）
        Map<String, AttributeValue> attrs = new HashMap<>();
        
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
     * 从复合 byte[] 解析所有属性
     *
     * @param data 复合格式的 byte[]（384 字节，48 个 double）
     * @return 属性映射
     */
    private static Map<String, AttributeValue> parseFromCompound(byte[] data) {
        Map<String, AttributeValue> attrs = new HashMap<>();
        double[] values = CompoundAttributeCodec.deserialize(data);

        // 攻击属性（范围）
        double min = values[CompoundAttributeCodec.ATTACK_MIN];
        double max = values[CompoundAttributeCodec.ATTACK_MAX];
        if (min > 0 || max > 0) {
            if (max == 0) max = min;
            attrs.put("攻击力", AttributeValue.ofRange(min, max));
        }

        // 防御属性（范围）
        min = values[CompoundAttributeCodec.DEFENSE_MIN];
        max = values[CompoundAttributeCodec.DEFENSE_MAX];
        if (min > 0 || max > 0) {
            if (max == 0) max = min;
            attrs.put("防御力", AttributeValue.ofRange(min, max));
        }

        // 生命上限
        if (values[CompoundAttributeCodec.MAX_HEALTH] > 0) {
            attrs.put("生命上限", AttributeValue.of(values[CompoundAttributeCodec.MAX_HEALTH]));
        }

        // 生命回复
        if (values[CompoundAttributeCodec.HEALTH_REGEN] > 0) {
            attrs.put("生命回复", AttributeValue.of(values[CompoundAttributeCodec.HEALTH_REGEN]));
        }

        // 暴击几率
        if (values[CompoundAttributeCodec.CRIT_CHANCE] > 0) {
            attrs.put("暴击几率", AttributeValue.ofPercent(values[CompoundAttributeCodec.CRIT_CHANCE]));
        }

        // 暴击伤害
        if (values[CompoundAttributeCodec.CRIT_DAMAGE] > 0) {
            attrs.put("暴击伤害", AttributeValue.ofPercent(values[CompoundAttributeCodec.CRIT_DAMAGE]));
        }

        // 吸血几率
        if (values[CompoundAttributeCodec.LIFESTEAL_CHANCE] > 0) {
            attrs.put("吸血几率", AttributeValue.ofPercent(values[CompoundAttributeCodec.LIFESTEAL_CHANCE]));
        }

        // 吸血倍率
        if (values[CompoundAttributeCodec.LIFESTEAL_MULTIPLIER] > 0) {
            attrs.put("吸血倍率", AttributeValue.of(values[CompoundAttributeCodec.LIFESTEAL_MULTIPLIER]));
        }

        // 闪避
        if (values[CompoundAttributeCodec.DODGE_CHANCE] > 0) {
            attrs.put("闪避", AttributeValue.ofPercent(values[CompoundAttributeCodec.DODGE_CHANCE]));
        }

        // 招架
        if (values[CompoundAttributeCodec.PARRY_CHANCE] > 0) {
            attrs.put("招架", AttributeValue.ofPercent(values[CompoundAttributeCodec.PARRY_CHANCE]));
        }

        // 移动速度
        if (values[CompoundAttributeCodec.MOVE_SPEED] > 0) {
            attrs.put("移动速度", AttributeValue.ofPercent(values[CompoundAttributeCodec.MOVE_SPEED]));
        }

        // 减伤
        if (values[CompoundAttributeCodec.DAMAGE_REDUCTION] > 0) {
            attrs.put("减伤", AttributeValue.ofPercent(values[CompoundAttributeCodec.DAMAGE_REDUCTION]));
        }

        // PVP攻击属性（范围）
        min = values[CompoundAttributeCodec.PVP_ATTACK_MIN];
        max = values[CompoundAttributeCodec.PVP_ATTACK_MAX];
        if (min > 0 || max > 0) {
            if (max == 0) max = min;
            attrs.put("PVP攻击力", AttributeValue.ofRange(min, max));
        }

        // PVP防御属性（范围）
        min = values[CompoundAttributeCodec.PVP_DEFENSE_MIN];
        max = values[CompoundAttributeCodec.PVP_DEFENSE_MAX];
        if (min > 0 || max > 0) {
            if (max == 0) max = min;
            attrs.put("PVP防御力", AttributeValue.ofRange(min, max));
        }

        // 暴击抵抗
        if (values[CompoundAttributeCodec.CRIT_RESIST] > 0) {
            attrs.put("暴击抵抗", AttributeValue.ofPercent(values[CompoundAttributeCodec.CRIT_RESIST]));
        }

        // 暴伤抵抗
        if (values[CompoundAttributeCodec.CRIT_DAMAGE_RESIST] > 0) {
            attrs.put("暴伤抵抗", AttributeValue.ofPercent(values[CompoundAttributeCodec.CRIT_DAMAGE_RESIST]));
        }

        // 吸血抵抗
        if (values[CompoundAttributeCodec.LIFESTEAL_RESIST] > 0) {
            attrs.put("吸血抵抗", AttributeValue.ofPercent(values[CompoundAttributeCodec.LIFESTEAL_RESIST]));
        }

        // 护甲值
        if (values[CompoundAttributeCodec.ARMOR] > 0) {
            attrs.put("护甲值", AttributeValue.of(values[CompoundAttributeCodec.ARMOR]));
        }

        // 护甲强度
        if (values[CompoundAttributeCodec.ARMOR_STRENGTH] > 0) {
            attrs.put("护甲强度", AttributeValue.of(values[CompoundAttributeCodec.ARMOR_STRENGTH]));
        }

        // 护甲穿透
        if (values[CompoundAttributeCodec.ARMOR_PENETRATION] > 0) {
            attrs.put("护甲穿透", AttributeValue.of(values[CompoundAttributeCodec.ARMOR_PENETRATION]));
        }

        // 防御穿透
        if (values[CompoundAttributeCodec.DEFENSE_PENETRATION] > 0) {
            attrs.put("防御穿透", AttributeValue.of(values[CompoundAttributeCodec.DEFENSE_PENETRATION]));
        }

        // 伤害反弹
        if (values[CompoundAttributeCodec.DAMAGE_REFLECT] > 0) {
            attrs.put("伤害反弹", AttributeValue.of(values[CompoundAttributeCodec.DAMAGE_REFLECT]));
        }

        // 反伤比例
        if (values[CompoundAttributeCodec.REFLECT_RATIO] > 0) {
            attrs.put("反伤比例", AttributeValue.ofPercent(values[CompoundAttributeCodec.REFLECT_RATIO]));
        }

        // 中毒
        if (values[CompoundAttributeCodec.POISON_CHANCE] > 0) {
            attrs.put("中毒", AttributeValue.ofPercent(values[CompoundAttributeCodec.POISON_CHANCE]));
        }

        // 冰冻
        if (values[CompoundAttributeCodec.FREEZE_CHANCE] > 0) {
            attrs.put("冰冻", AttributeValue.ofPercent(values[CompoundAttributeCodec.FREEZE_CHANCE]));
        }

        // 致盲
        if (values[CompoundAttributeCodec.BLIND_CHANCE] > 0) {
            attrs.put("致盲", AttributeValue.ofPercent(values[CompoundAttributeCodec.BLIND_CHANCE]));
        }

        // 燃烧
        if (values[CompoundAttributeCodec.BURN_CHANCE] > 0) {
            attrs.put("燃烧", AttributeValue.ofPercent(values[CompoundAttributeCodec.BURN_CHANCE]));
        }

        // 灼烧
        if (values[CompoundAttributeCodec.SCORCH_CHANCE] > 0) {
            attrs.put("灼烧", AttributeValue.ofPercent(values[CompoundAttributeCodec.SCORCH_CHANCE]));
        }

        // 火焰抗性
        if (values[CompoundAttributeCodec.FIRE_RESIST] > 0) {
            attrs.put("火焰抗性", AttributeValue.ofPercent(values[CompoundAttributeCodec.FIRE_RESIST]));
        }

        // 摔落抗性
        if (values[CompoundAttributeCodec.FALL_RESIST] > 0) {
            attrs.put("摔落抗性", AttributeValue.ofPercent(values[CompoundAttributeCodec.FALL_RESIST]));
        }

        // 溺水抗性
        if (values[CompoundAttributeCodec.DROWNING_RESIST] > 0) {
            attrs.put("溺水抗性", AttributeValue.ofPercent(values[CompoundAttributeCodec.DROWNING_RESIST]));
        }

        // 中毒抗性
        if (values[CompoundAttributeCodec.POISON_RESIST] > 0) {
            attrs.put("中毒抗性", AttributeValue.ofPercent(values[CompoundAttributeCodec.POISON_RESIST]));
        }

        // 凋零抗性
        if (values[CompoundAttributeCodec.WITHER_RESIST] > 0) {
            attrs.put("凋零抗性", AttributeValue.ofPercent(values[CompoundAttributeCodec.WITHER_RESIST]));
        }

        // 岩浆抗性
        if (values[CompoundAttributeCodec.LAVA_RESIST] > 0) {
            attrs.put("岩浆抗性", AttributeValue.ofPercent(values[CompoundAttributeCodec.LAVA_RESIST]));
        }

        // 魔法抗性
        if (values[CompoundAttributeCodec.MAGIC_RESIST] > 0) {
            attrs.put("魔法抗性", AttributeValue.ofPercent(values[CompoundAttributeCodec.MAGIC_RESIST]));
        }

        // 爆炸抗性
        if (values[CompoundAttributeCodec.EXPLOSION_RESIST] > 0) {
            attrs.put("爆炸抗性", AttributeValue.ofPercent(values[CompoundAttributeCodec.EXPLOSION_RESIST]));
        }

        // 弹射物抗性
        if (values[CompoundAttributeCodec.PROJECTILE_RESIST] > 0) {
            attrs.put("弹射物抗性", AttributeValue.ofPercent(values[CompoundAttributeCodec.PROJECTILE_RESIST]));
        }

        // 击退抗性
        if (values[CompoundAttributeCodec.KNOCKBACK_RESIST] > 0) {
            attrs.put("击退抗性", AttributeValue.ofPercent(values[CompoundAttributeCodec.KNOCKBACK_RESIST]));
        }

        // 经验加成
        if (values[CompoundAttributeCodec.EXP_BONUS] > 0) {
            attrs.put("经验加成", AttributeValue.ofPercent(values[CompoundAttributeCodec.EXP_BONUS]));
        }

        // 生命恢复
        if (values[CompoundAttributeCodec.HEALTH_REGEN_PERCENT] > 0) {
            attrs.put("生命恢复", AttributeValue.ofPercent(values[CompoundAttributeCodec.HEALTH_REGEN_PERCENT]));
        }

        // 躲避反伤
        if (values[CompoundAttributeCodec.DODGE_REFLECT_CHANCE] > 0) {
            attrs.put("躲避反伤", AttributeValue.ofPercent(values[CompoundAttributeCodec.DODGE_REFLECT_CHANCE]));
        }

        // 躲避反弹比例
        if (values[CompoundAttributeCodec.DODGE_REFLECT_RATIO] > 0) {
            attrs.put("躲避反弹比例", AttributeValue.ofPercent(values[CompoundAttributeCodec.DODGE_REFLECT_RATIO]));
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
        
        // 优先读取复合存储格式
        byte[] compoundData = pdc.get(CompoundAttributeCodec.KEY_COMPOUND, PersistentDataType.BYTE_ARRAY);
        if (compoundData != null) {
            System.out.println(debugPrefix + "检测到复合存储格式，使用快速路径解析");
            return parseFromCompound(compoundData);
        }
        System.out.println(debugPrefix + "使用旧格式（独立 PDC key）解析");
        
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
     * 检查 PDC 是否包含 RPGItems 物品标记
     *
     * 优化：RPGItems 的 ItemFactory 在创建物品时已写入 rpgitems:id 标记 key
     * 只需检查这 1 个 key 即可判断是否是 RPGItems 物品，替代原来 40+ 次 pdc.has()
     * 性能提升：O(1) 单次查找，替代 O(n) 40+ 次查找
     */
    private static boolean hasAnyRPGItemsData(PersistentDataContainer pdc) {
        return pdc.has(KEY_ID, PersistentDataType.STRING);
    }
}
