package cn.guangdian.armorstats.parser;

import cn.guangdian.armorstats.data.AttributeValue;
import cn.guangdian.rpgitems.attribute.CompoundAttributeCodec;
import cn.guangdian.rpgitems.attribute.RPGItemsKeys;
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
        byte[] compoundData = pdc.get(RPGItemsKeys.ATTRS, PersistentDataType.BYTE_ARRAY);
        if (compoundData != null) {
            return parseFromCompound(compoundData);
        }
        // 回退到旧格式（向后兼容已有物品）
        Map<String, AttributeValue> attrs = new HashMap<>();

        // 攻击属性
        double min = pdc.getOrDefault(RPGItemsKeys.ATTACK_MIN, PersistentDataType.DOUBLE, 0.0);
        double max = pdc.getOrDefault(RPGItemsKeys.ATTACK_MAX, PersistentDataType.DOUBLE, 0.0);
        if (min > 0 || max > 0) {
            if (max == 0) max = min;
            attrs.put("攻击力", AttributeValue.ofRange(min, max));
        }

        // 防御属性
        min = pdc.getOrDefault(RPGItemsKeys.DEFENSE_MIN, PersistentDataType.DOUBLE, 0.0);
        max = pdc.getOrDefault(RPGItemsKeys.DEFENSE_MAX, PersistentDataType.DOUBLE, 0.0);
        if (min > 0 || max > 0) {
            if (max == 0) max = min;
            attrs.put("防御力", AttributeValue.ofRange(min, max));
        }

        // 生命上限
        double value = pdc.getOrDefault(RPGItemsKeys.MAX_HEALTH, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("生命上限", AttributeValue.of(value));
        }

        // 生命恢复
        value = pdc.getOrDefault(RPGItemsKeys.HEALTH_REGEN, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("生命回复", AttributeValue.of(value));
        }

        // 暴击几率
        value = pdc.getOrDefault(RPGItemsKeys.CRIT_CHANCE, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("暴击几率", AttributeValue.ofPercent(value));
        }

        // 暴击伤害
        value = pdc.getOrDefault(RPGItemsKeys.CRIT_DAMAGE, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("暴击伤害", AttributeValue.ofPercent(value));
        }

        // 吸血几率
        value = pdc.getOrDefault(RPGItemsKeys.LIFESTEAL_CHANCE, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("吸血几率", AttributeValue.ofPercent(value));
        }

        // 吸血倍率
        value = pdc.getOrDefault(RPGItemsKeys.LIFESTEAL_MULTIPLIER, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("吸血倍率", AttributeValue.of(value));
        }

        // 闪避
        value = pdc.getOrDefault(RPGItemsKeys.DODGE_CHANCE, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("闪避", AttributeValue.ofPercent(value));
        }

        // 格挡（招架）
        value = pdc.getOrDefault(RPGItemsKeys.PARRY_CHANCE, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("招架", AttributeValue.ofPercent(value));
        }

        // 移动速度
        value = pdc.getOrDefault(RPGItemsKeys.MOVE_SPEED, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("移动速度", AttributeValue.ofPercent(value));
        }

        // 减伤
        value = pdc.getOrDefault(RPGItemsKeys.DAMAGE_REDUCTION, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("减伤", AttributeValue.ofPercent(value));
        }

        // PVP攻击属性
        min = pdc.getOrDefault(RPGItemsKeys.PVP_ATTACK_MIN, PersistentDataType.DOUBLE, 0.0);
        max = pdc.getOrDefault(RPGItemsKeys.PVP_ATTACK_MAX, PersistentDataType.DOUBLE, 0.0);
        if (min > 0 || max > 0) {
            if (max == 0) max = min;
            attrs.put("PVP攻击力", AttributeValue.ofRange(min, max));
        }

        // PVP防御属性
        min = pdc.getOrDefault(RPGItemsKeys.PVP_DEFENSE_MIN, PersistentDataType.DOUBLE, 0.0);
        max = pdc.getOrDefault(RPGItemsKeys.PVP_DEFENSE_MAX, PersistentDataType.DOUBLE, 0.0);
        if (min > 0 || max > 0) {
            if (max == 0) max = min;
            attrs.put("PVP防御力", AttributeValue.ofRange(min, max));
        }

        // 暴击抵抗
        value = pdc.getOrDefault(RPGItemsKeys.CRIT_RESIST, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("暴击抵抗", AttributeValue.ofPercent(value));
        }

        // 暴伤抵抗
        value = pdc.getOrDefault(RPGItemsKeys.CRIT_DAMAGE_RESIST, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("暴伤抵抗", AttributeValue.ofPercent(value));
        }

        // 吸血抵抗
        value = pdc.getOrDefault(RPGItemsKeys.LIFESTEAL_RESIST, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("吸血抵抗", AttributeValue.ofPercent(value));
        }

        // 护甲值
        value = pdc.getOrDefault(RPGItemsKeys.ARMOR, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("护甲值", AttributeValue.of(value));
        }

        // 护甲强度
        value = pdc.getOrDefault(RPGItemsKeys.ARMOR_STRENGTH, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("护甲强度", AttributeValue.of(value));
        }

        // 护甲穿透
        value = pdc.getOrDefault(RPGItemsKeys.ARMOR_PENETRATION, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("护甲穿透", AttributeValue.of(value));
        }

        // 防御穿透
        value = pdc.getOrDefault(RPGItemsKeys.DEFENSE_PENETRATION, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("防御穿透", AttributeValue.of(value));
        }

        // 伤害反弹
        value = pdc.getOrDefault(RPGItemsKeys.DAMAGE_REFLECT, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("伤害反弹", AttributeValue.of(value));
        }

        // 反伤比例
        value = pdc.getOrDefault(RPGItemsKeys.REFLECT_RATIO, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("反伤比例", AttributeValue.ofPercent(value));
        }

        // 中毒几率
        value = pdc.getOrDefault(RPGItemsKeys.POISON_CHANCE, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("中毒", AttributeValue.ofPercent(value));
        }

        // 冰冻几率
        value = pdc.getOrDefault(RPGItemsKeys.FREEZE_CHANCE, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("冰冻", AttributeValue.ofPercent(value));
        }

        // 致盲几率
        value = pdc.getOrDefault(RPGItemsKeys.BLIND_CHANCE, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("致盲", AttributeValue.ofPercent(value));
        }

        // 燃烧几率
        value = pdc.getOrDefault(RPGItemsKeys.BURN_CHANCE, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("燃烧", AttributeValue.ofPercent(value));
        }

        // 灼烧几率
        value = pdc.getOrDefault(RPGItemsKeys.SCORCH_CHANCE, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("灼烧", AttributeValue.ofPercent(value));
        }

        // 火焰抗性
        value = pdc.getOrDefault(RPGItemsKeys.FIRE_RESIST, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("火焰抗性", AttributeValue.ofPercent(value));
        }

        // 摔落抗性
        value = pdc.getOrDefault(RPGItemsKeys.FALL_RESIST, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("摔落抗性", AttributeValue.ofPercent(value));
        }

        // 溺水抗性
        value = pdc.getOrDefault(RPGItemsKeys.DROWNING_RESIST, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("溺水抗性", AttributeValue.ofPercent(value));
        }

        // 中毒抗性
        value = pdc.getOrDefault(RPGItemsKeys.POISON_RESIST, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("中毒抗性", AttributeValue.ofPercent(value));
        }

        // 凋零抗性
        value = pdc.getOrDefault(RPGItemsKeys.WITHER_RESIST, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("凋零抗性", AttributeValue.ofPercent(value));
        }

        // 岩浆抗性
        value = pdc.getOrDefault(RPGItemsKeys.LAVA_RESIST, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("岩浆抗性", AttributeValue.ofPercent(value));
        }

        // 魔法抗性
        value = pdc.getOrDefault(RPGItemsKeys.MAGIC_RESIST, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("魔法抗性", AttributeValue.ofPercent(value));
        }

        // 爆炸抗性
        value = pdc.getOrDefault(RPGItemsKeys.EXPLOSION_RESIST, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("爆炸抗性", AttributeValue.ofPercent(value));
        }

        // 弹射物抗性
        value = pdc.getOrDefault(RPGItemsKeys.PROJECTILE_RESIST, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("弹射物抗性", AttributeValue.ofPercent(value));
        }

        // 击退抗性
        value = pdc.getOrDefault(RPGItemsKeys.KNOCKBACK_RESIST, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("击退抗性", AttributeValue.ofPercent(value));
        }

        // 经验加成
        value = pdc.getOrDefault(RPGItemsKeys.EXP_BONUS, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("经验加成", AttributeValue.ofPercent(value));
        }

        // 生命恢复百分比
        value = pdc.getOrDefault(RPGItemsKeys.HEALTH_REGEN_PERCENT, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("生命恢复", AttributeValue.ofPercent(value));
        }

        // 躲避反伤几率
        value = pdc.getOrDefault(RPGItemsKeys.DODGE_REFLECT_CHANCE, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("躲避反伤", AttributeValue.ofPercent(value));
        }

        // 躲避反弹比例
        value = pdc.getOrDefault(RPGItemsKeys.DODGE_REFLECT_RATIO, PersistentDataType.DOUBLE, 0.0);
        if (value > 0) {
            attrs.put("躲避反弹比例", AttributeValue.ofPercent(value));
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
        byte[] compoundData = pdc.get(RPGItemsKeys.ATTRS, PersistentDataType.BYTE_ARRAY);
        if (compoundData != null) {
            System.out.println(debugPrefix + "检测到复合存储格式，使用快速路径解析");
            return parseFromCompound(compoundData);
        }
        System.out.println(debugPrefix + "使用旧格式（独立 PDC key）解析");
        
        // 攻击属性
        if (pdc.has(RPGItemsKeys.ATTACK_MIN, PersistentDataType.DOUBLE)) {
            double min = pdc.get(RPGItemsKeys.ATTACK_MIN, PersistentDataType.DOUBLE);
            double max = pdc.has(RPGItemsKeys.ATTACK_MAX, PersistentDataType.DOUBLE)
                ? pdc.get(RPGItemsKeys.ATTACK_MAX, PersistentDataType.DOUBLE) : min;
            System.out.println(debugPrefix + "  [PDC读取] 攻击力: " + min + " - " + max);
            if (min > 0 || max > 0) {
                attrs.put("攻击力", AttributeValue.ofRange(min, max));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 攻击力: 无数据");
        }
        
        // 防御属性
        if (pdc.has(RPGItemsKeys.DEFENSE_MIN, PersistentDataType.DOUBLE)) {
            double min = pdc.get(RPGItemsKeys.DEFENSE_MIN, PersistentDataType.DOUBLE);
            double max = pdc.has(RPGItemsKeys.DEFENSE_MAX, PersistentDataType.DOUBLE)
                ? pdc.get(RPGItemsKeys.DEFENSE_MAX, PersistentDataType.DOUBLE) : min;
            System.out.println(debugPrefix + "  [PDC读取] 防御力: " + min + " - " + max);
            if (min > 0 || max > 0) {
                attrs.put("防御力", AttributeValue.ofRange(min, max));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 防御力: 无数据");
        }
        
        // 生命上限
        if (pdc.has(RPGItemsKeys.MAX_HEALTH, PersistentDataType.DOUBLE)) {
            double value = pdc.get(RPGItemsKeys.MAX_HEALTH, PersistentDataType.DOUBLE);
            System.out.println(debugPrefix + "  [PDC读取] 生命上限: " + value);
            if (value > 0) {
                attrs.put("生命上限", AttributeValue.of(value));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 生命上限: 无数据");
        }
        
        // 生命恢复
        if (pdc.has(RPGItemsKeys.HEALTH_REGEN, PersistentDataType.DOUBLE)) {
            double value = pdc.get(RPGItemsKeys.HEALTH_REGEN, PersistentDataType.DOUBLE);
            System.out.println(debugPrefix + "  [PDC读取] 生命回复: " + value);
            if (value > 0) {
                attrs.put("生命回复", AttributeValue.of(value));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 生命回复: 无数据");
        }
        
        // 暴击几率
        if (pdc.has(RPGItemsKeys.CRIT_CHANCE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(RPGItemsKeys.CRIT_CHANCE, PersistentDataType.DOUBLE);
            System.out.println(debugPrefix + "  [PDC读取] 暴击几率: " + value + "%");
            if (value > 0) {
                attrs.put("暴击几率", AttributeValue.ofPercent(value));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 暴击几率: 无数据");
        }
        
        // 暴击伤害
        if (pdc.has(RPGItemsKeys.CRIT_DAMAGE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(RPGItemsKeys.CRIT_DAMAGE, PersistentDataType.DOUBLE);
            System.out.println(debugPrefix + "  [PDC读取] 暴击伤害: " + value + "%");
            if (value > 0) {
                attrs.put("暴击伤害", AttributeValue.ofPercent(value));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 暴击伤害: 无数据");
        }
        
        // 吸血几率
        if (pdc.has(RPGItemsKeys.LIFESTEAL_CHANCE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(RPGItemsKeys.LIFESTEAL_CHANCE, PersistentDataType.DOUBLE);
            System.out.println(debugPrefix + "  [PDC读取] 吸血几率: " + value + "%");
            if (value > 0) {
                attrs.put("吸血几率", AttributeValue.ofPercent(value));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 吸血几率: 无数据");
        }
        
        // 吸血倍率
        if (pdc.has(RPGItemsKeys.LIFESTEAL_MULTIPLIER, PersistentDataType.DOUBLE)) {
            double value = pdc.get(RPGItemsKeys.LIFESTEAL_MULTIPLIER, PersistentDataType.DOUBLE);
            System.out.println(debugPrefix + "  [PDC读取] 吸血倍率: " + value);
            if (value > 0) {
                attrs.put("吸血倍率", AttributeValue.of(value));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 吸血倍率: 无数据");
        }
        
        // 闪避
        if (pdc.has(RPGItemsKeys.DODGE_CHANCE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(RPGItemsKeys.DODGE_CHANCE, PersistentDataType.DOUBLE);
            System.out.println(debugPrefix + "  [PDC读取] 闪避: " + value + "%");
            if (value > 0) {
                attrs.put("闪避", AttributeValue.ofPercent(value));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 闪避: 无数据");
        }
        
        // 格挡（招架）
        if (pdc.has(RPGItemsKeys.PARRY_CHANCE, PersistentDataType.DOUBLE)) {
            double value = pdc.get(RPGItemsKeys.PARRY_CHANCE, PersistentDataType.DOUBLE);
            System.out.println(debugPrefix + "  [PDC读取] 招架: " + value + "%");
            if (value > 0) {
                attrs.put("招架", AttributeValue.ofPercent(value));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 招架: 无数据");
        }
        
        // 移动速度
        if (pdc.has(RPGItemsKeys.MOVE_SPEED, PersistentDataType.DOUBLE)) {
            double value = pdc.get(RPGItemsKeys.MOVE_SPEED, PersistentDataType.DOUBLE);
            System.out.println(debugPrefix + "  [PDC读取] 移动速度: " + value + "%");
            if (value > 0) {
                attrs.put("移动速度", AttributeValue.ofPercent(value));
            }
        } else {
            System.out.println(debugPrefix + "  [PDC读取] 积动速度: 无数据");
        }
        
        // 减伤
        if (pdc.has(RPGItemsKeys.DAMAGE_REDUCTION, PersistentDataType.DOUBLE)) {
            double value = pdc.get(RPGItemsKeys.DAMAGE_REDUCTION, PersistentDataType.DOUBLE);
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
        return pdc.has(RPGItemsKeys.ID, PersistentDataType.STRING);
    }
}
