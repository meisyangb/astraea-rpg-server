package cn.guangdian.enhance.util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * 【枚举法】属性更新工具类
 * 直接通过PDC操作物品属性，不依赖外部API
 */
public class EnhanceAttributeHelper {

    // 物品标识Key（用于检测是否为RPGItems物品）
    private static final NamespacedKey KEY_ID = new NamespacedKey("rpgitems", "id");
    
    // 攻击属性
    private static final NamespacedKey KEY_ATTACK_MIN = new NamespacedKey("rpgitems", "attack_min");
    private static final NamespacedKey KEY_ATTACK_MAX = new NamespacedKey("rpgitems", "attack_max");
    
    // 防御属性
    private static final NamespacedKey KEY_DEFENSE_MIN = new NamespacedKey("rpgitems", "defense_min");
    private static final NamespacedKey KEY_DEFENSE_MAX = new NamespacedKey("rpgitems", "defense_max");
    
    // 生命属性
    private static final NamespacedKey KEY_MAX_HEALTH = new NamespacedKey("rpgitems", "max_health");
    private static final NamespacedKey KEY_HEALTH_REGEN = new NamespacedKey("rpgitems", "health_regen");
    
    // 暴击属性
    private static final NamespacedKey KEY_CRIT_CHANCE = new NamespacedKey("rpgitems", "crit_chance");
    private static final NamespacedKey KEY_CRIT_DAMAGE = new NamespacedKey("rpgitems", "crit_damage");
    
    // 生命偷取
    private static final NamespacedKey KEY_LIFESTEAL_CHANCE = new NamespacedKey("rpgitems", "lifesteal_chance");
    private static final NamespacedKey KEY_LIFESTEAL_MULTIPLIER = new NamespacedKey("rpgitems", "lifesteal_multiplier");
    
    // 闪避与格挡
    private static final NamespacedKey KEY_DODGE_CHANCE = new NamespacedKey("rpgitems", "dodge_chance");
    private static final NamespacedKey KEY_PARRY_CHANCE = new NamespacedKey("rpgitems", "parry_chance");
    
    // 移动速度
    private static final NamespacedKey KEY_MOVE_SPEED = new NamespacedKey("rpgitems", "move_speed");
    
    // 减伤
    private static final NamespacedKey KEY_DAMAGE_REDUCTION = new NamespacedKey("rpgitems", "damage_reduction");
    
    // PVP属性
    private static final NamespacedKey KEY_PVP_ATTACK_MIN = new NamespacedKey("rpgitems", "pvp_attack_min");
    private static final NamespacedKey KEY_PVP_ATTACK_MAX = new NamespacedKey("rpgitems", "pvp_attack_max");
    private static final NamespacedKey KEY_PVP_DEFENSE_MIN = new NamespacedKey("rpgitems", "pvp_defense_min");
    private static final NamespacedKey KEY_PVP_DEFENSE_MAX = new NamespacedKey("rpgitems", "pvp_defense_max");
    
    // 暴击抵抗
    private static final NamespacedKey KEY_CRIT_RESIST = new NamespacedKey("rpgitems", "crit_resist");
    private static final NamespacedKey KEY_CRIT_DAMAGE_RESIST = new NamespacedKey("rpgitems", "crit_damage_resist");
    
    // 吸血抵抗
    private static final NamespacedKey KEY_LIFESTEAL_RESIST = new NamespacedKey("rpgitems", "lifesteal_resist");
    
    // 护甲与穿透
    private static final NamespacedKey KEY_ARMOR = new NamespacedKey("rpgitems", "armor");
    private static final NamespacedKey KEY_ARMOR_STRENGTH = new NamespacedKey("rpgitems", "armor_strength");
    private static final NamespacedKey KEY_ARMOR_PENETRATION = new NamespacedKey("rpgitems", "armor_penetration");
    private static final NamespacedKey KEY_DEFENSE_PENETRATION = new NamespacedKey("rpgitems", "defense_penetration");
    
    // 伤害反弹
    private static final NamespacedKey KEY_DAMAGE_REFLECT = new NamespacedKey("rpgitems", "damage_reflect");
    private static final NamespacedKey KEY_REFLECT_RATIO = new NamespacedKey("rpgitems", "reflect_ratio");
    
    // 状态效果（不参与强化）
    // 环境抗性（不参与强化）
    // 其他属性（不参与强化）
    
    // 基础属性存储Key（用于保存原始属性值）
    private static final NamespacedKey KEY_BASE_ATTACK_MIN = new NamespacedKey("rpgitems", "base_attack_min");
    private static final NamespacedKey KEY_BASE_ATTACK_MAX = new NamespacedKey("rpgitems", "base_attack_max");
    private static final NamespacedKey KEY_BASE_DEFENSE_MIN = new NamespacedKey("rpgitems", "base_defense_min");
    private static final NamespacedKey KEY_BASE_DEFENSE_MAX = new NamespacedKey("rpgitems", "base_defense_max");
    private static final NamespacedKey KEY_BASE_MAX_HEALTH = new NamespacedKey("rpgitems", "base_max_health");
    private static final NamespacedKey KEY_BASE_CRIT_CHANCE = new NamespacedKey("rpgitems", "base_crit_chance");
    private static final NamespacedKey KEY_BASE_CRIT_DAMAGE = new NamespacedKey("rpgitems", "base_crit_damage");
    
    // 当前倍率Key
    private static final NamespacedKey KEY_CURRENT_MULTIPLIER = new NamespacedKey("rpgitems", "enhance_multiplier");

    /**
     * 检测物品是否可强化
     * 通过检查是否有rpgitems:id PDC Key判断
     */
    public static boolean isEnhanceable(ItemStack item) {
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
    
    /**
     * 获取物品阶位
     */
    public static String getItemTier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey KEY_TIER = new NamespacedKey("rpgitems", "tier");
        if (pdc.has(KEY_TIER, PersistentDataType.STRING)) {
            return pdc.get(KEY_TIER, PersistentDataType.STRING);
        }
        return null;
    }
    
    /**
     * 【枚举法】应用属性倍率
     * 首次强化时保存基础属性，然后按倍率更新当前属性
     * 降级时强制同步属性到对应倍率
     * 
     * @param item 物品
     * @param multiplier 属性倍率（从枚举表获取）
     */
    public static void applyMultiplier(ItemStack item, double multiplier) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 优先使用复合存储格式
        byte[] compoundData = pdc.get(CompoundAttributeCodec.KEY_COMPOUND, PersistentDataType.BYTE_ARRAY);
        if (compoundData != null) {
            // 复合格式
            double[] attrs = CompoundAttributeCodec.deserialize(compoundData);
            
            // 首次强化时保存基础属性
            if (!pdc.has(CompoundAttributeCodec.KEY_BASE_COMPOUND, PersistentDataType.BYTE_ARRAY)) {
                pdc.set(CompoundAttributeCodec.KEY_BASE_COMPOUND, PersistentDataType.BYTE_ARRAY, compoundData);
            }
            
            // 读取基础属性并按倍率更新
            byte[] baseData = pdc.get(CompoundAttributeCodec.KEY_BASE_COMPOUND, PersistentDataType.BYTE_ARRAY);
            double[] baseAttrs = CompoundAttributeCodec.deserialize(baseData);
            
            attrs[CompoundAttributeCodec.ATTACK_MIN] = baseAttrs[CompoundAttributeCodec.ATTACK_MIN] * multiplier;
            attrs[CompoundAttributeCodec.ATTACK_MAX] = baseAttrs[CompoundAttributeCodec.ATTACK_MAX] * multiplier;
            attrs[CompoundAttributeCodec.DEFENSE_MIN] = baseAttrs[CompoundAttributeCodec.DEFENSE_MIN] * multiplier;
            attrs[CompoundAttributeCodec.DEFENSE_MAX] = baseAttrs[CompoundAttributeCodec.DEFENSE_MAX] * multiplier;
            attrs[CompoundAttributeCodec.MAX_HEALTH] = baseAttrs[CompoundAttributeCodec.MAX_HEALTH] * multiplier;
            attrs[CompoundAttributeCodec.CRIT_CHANCE] = baseAttrs[CompoundAttributeCodec.CRIT_CHANCE] * multiplier;
            attrs[CompoundAttributeCodec.CRIT_DAMAGE] = baseAttrs[CompoundAttributeCodec.CRIT_DAMAGE] * multiplier;
            
            // 写回复合数据
            pdc.set(CompoundAttributeCodec.KEY_COMPOUND, PersistentDataType.BYTE_ARRAY, CompoundAttributeCodec.serialize(attrs));
        } else {
            // 旧格式回退（向后兼容已有物品）
            // 首次强化时保存基础属性
            if (!pdc.has(KEY_BASE_ATTACK_MIN, PersistentDataType.DOUBLE)) {
                saveBaseAttributes(pdc);
            }
            updateAttributesWithMultiplier(pdc, multiplier);
        }

        // 保存当前倍率
        pdc.set(KEY_CURRENT_MULTIPLIER, PersistentDataType.DOUBLE, multiplier);
        item.setItemMeta(meta);
    }
    
    /**
     * 【安全检查】验证物品的当前属性是否与强化等级匹配
     * 如果不匹配，强制修正为正确值
     * 
     * @param item 物品
     * @param correctMultiplier 正确的倍率
     * @return 是否进行了修正
     */
    public static boolean validateAndFixMultiplier(ItemStack item, double correctMultiplier) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 检查当前倍率是否匹配
        if (pdc.has(KEY_CURRENT_MULTIPLIER, PersistentDataType.DOUBLE)) {
            double currentMult = pdc.get(KEY_CURRENT_MULTIPLIER, PersistentDataType.DOUBLE);
            if (Math.abs(currentMult - correctMultiplier) < 0.001) {
                return false;
            }
        }

        // 优先使用复合存储格式
        byte[] compoundData = pdc.get(CompoundAttributeCodec.KEY_COMPOUND, PersistentDataType.BYTE_ARRAY);
        if (compoundData != null) {
            double[] attrs = CompoundAttributeCodec.deserialize(compoundData);
            
            if (!pdc.has(CompoundAttributeCodec.KEY_BASE_COMPOUND, PersistentDataType.BYTE_ARRAY)) {
                pdc.set(CompoundAttributeCodec.KEY_BASE_COMPOUND, PersistentDataType.BYTE_ARRAY, compoundData);
            }
            
            byte[] baseData = pdc.get(CompoundAttributeCodec.KEY_BASE_COMPOUND, PersistentDataType.BYTE_ARRAY);
            double[] baseAttrs = CompoundAttributeCodec.deserialize(baseData);
            
            attrs[CompoundAttributeCodec.ATTACK_MIN] = baseAttrs[CompoundAttributeCodec.ATTACK_MIN] * correctMultiplier;
            attrs[CompoundAttributeCodec.ATTACK_MAX] = baseAttrs[CompoundAttributeCodec.ATTACK_MAX] * correctMultiplier;
            attrs[CompoundAttributeCodec.DEFENSE_MIN] = baseAttrs[CompoundAttributeCodec.DEFENSE_MIN] * correctMultiplier;
            attrs[CompoundAttributeCodec.DEFENSE_MAX] = baseAttrs[CompoundAttributeCodec.DEFENSE_MAX] * correctMultiplier;
            attrs[CompoundAttributeCodec.MAX_HEALTH] = baseAttrs[CompoundAttributeCodec.MAX_HEALTH] * correctMultiplier;
            attrs[CompoundAttributeCodec.CRIT_CHANCE] = baseAttrs[CompoundAttributeCodec.CRIT_CHANCE] * correctMultiplier;
            attrs[CompoundAttributeCodec.CRIT_DAMAGE] = baseAttrs[CompoundAttributeCodec.CRIT_DAMAGE] * correctMultiplier;
            
            pdc.set(CompoundAttributeCodec.KEY_COMPOUND, PersistentDataType.BYTE_ARRAY, CompoundAttributeCodec.serialize(attrs));
        } else {
            // 旧格式回退
            if (!pdc.has(KEY_BASE_ATTACK_MIN, PersistentDataType.DOUBLE)) {
                saveBaseAttributes(pdc);
            }
            updateAttributesWithMultiplier(pdc, correctMultiplier);
        }

        pdc.set(KEY_CURRENT_MULTIPLIER, PersistentDataType.DOUBLE, correctMultiplier);
        item.setItemMeta(meta);
        return true;
    }
    
    /**
     * 保存基础属性值
     */
    private static void saveBaseAttributes(PersistentDataContainer pdc) {
        // 攻击
        if (pdc.has(KEY_ATTACK_MIN, PersistentDataType.DOUBLE)) {
            pdc.set(KEY_BASE_ATTACK_MIN, PersistentDataType.DOUBLE, 
                pdc.get(KEY_ATTACK_MIN, PersistentDataType.DOUBLE));
        }
        if (pdc.has(KEY_ATTACK_MAX, PersistentDataType.DOUBLE)) {
            pdc.set(KEY_BASE_ATTACK_MAX, PersistentDataType.DOUBLE, 
                pdc.get(KEY_ATTACK_MAX, PersistentDataType.DOUBLE));
        }
        
        // 防御
        if (pdc.has(KEY_DEFENSE_MIN, PersistentDataType.DOUBLE)) {
            pdc.set(KEY_BASE_DEFENSE_MIN, PersistentDataType.DOUBLE, 
                pdc.get(KEY_DEFENSE_MIN, PersistentDataType.DOUBLE));
        }
        if (pdc.has(KEY_DEFENSE_MAX, PersistentDataType.DOUBLE)) {
            pdc.set(KEY_BASE_DEFENSE_MAX, PersistentDataType.DOUBLE, 
                pdc.get(KEY_DEFENSE_MAX, PersistentDataType.DOUBLE));
        }
        
        // 生命
        if (pdc.has(KEY_MAX_HEALTH, PersistentDataType.DOUBLE)) {
            pdc.set(KEY_BASE_MAX_HEALTH, PersistentDataType.DOUBLE, 
                pdc.get(KEY_MAX_HEALTH, PersistentDataType.DOUBLE));
        }
        
        // 暴击
        if (pdc.has(KEY_CRIT_CHANCE, PersistentDataType.DOUBLE)) {
            pdc.set(KEY_BASE_CRIT_CHANCE, PersistentDataType.DOUBLE, 
                pdc.get(KEY_CRIT_CHANCE, PersistentDataType.DOUBLE));
        }
        if (pdc.has(KEY_CRIT_DAMAGE, PersistentDataType.DOUBLE)) {
            pdc.set(KEY_BASE_CRIT_DAMAGE, PersistentDataType.DOUBLE, 
                pdc.get(KEY_CRIT_DAMAGE, PersistentDataType.DOUBLE));
        }
    }
    
    /**
     * 按倍率更新属性值
     */
    private static void updateAttributesWithMultiplier(PersistentDataContainer pdc, double multiplier) {
        // 攻击
        if (pdc.has(KEY_BASE_ATTACK_MIN, PersistentDataType.DOUBLE)) {
            double base = pdc.get(KEY_BASE_ATTACK_MIN, PersistentDataType.DOUBLE);
            pdc.set(KEY_ATTACK_MIN, PersistentDataType.DOUBLE, base * multiplier);
        }
        if (pdc.has(KEY_BASE_ATTACK_MAX, PersistentDataType.DOUBLE)) {
            double base = pdc.get(KEY_BASE_ATTACK_MAX, PersistentDataType.DOUBLE);
            pdc.set(KEY_ATTACK_MAX, PersistentDataType.DOUBLE, base * multiplier);
        }
        
        // 防御
        if (pdc.has(KEY_BASE_DEFENSE_MIN, PersistentDataType.DOUBLE)) {
            double base = pdc.get(KEY_BASE_DEFENSE_MIN, PersistentDataType.DOUBLE);
            pdc.set(KEY_DEFENSE_MIN, PersistentDataType.DOUBLE, base * multiplier);
        }
        if (pdc.has(KEY_BASE_DEFENSE_MAX, PersistentDataType.DOUBLE)) {
            double base = pdc.get(KEY_BASE_DEFENSE_MAX, PersistentDataType.DOUBLE);
            pdc.set(KEY_DEFENSE_MAX, PersistentDataType.DOUBLE, base * multiplier);
        }
        
        // 生命
        if (pdc.has(KEY_BASE_MAX_HEALTH, PersistentDataType.DOUBLE)) {
            double base = pdc.get(KEY_BASE_MAX_HEALTH, PersistentDataType.DOUBLE);
            pdc.set(KEY_MAX_HEALTH, PersistentDataType.DOUBLE, base * multiplier);
        }
        
        // 暴击
        if (pdc.has(KEY_BASE_CRIT_CHANCE, PersistentDataType.DOUBLE)) {
            double base = pdc.get(KEY_BASE_CRIT_CHANCE, PersistentDataType.DOUBLE);
            pdc.set(KEY_CRIT_CHANCE, PersistentDataType.DOUBLE, base * multiplier);
        }
        if (pdc.has(KEY_BASE_CRIT_DAMAGE, PersistentDataType.DOUBLE)) {
            double base = pdc.get(KEY_BASE_CRIT_DAMAGE, PersistentDataType.DOUBLE);
            pdc.set(KEY_CRIT_DAMAGE, PersistentDataType.DOUBLE, base * multiplier);
        }
    }
    
    /**
     * 获取当前倍率（用于验证）
     */
    public static double getCurrentMultiplier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 1.0;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 1.0;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(KEY_CURRENT_MULTIPLIER, PersistentDataType.DOUBLE)) {
            return pdc.get(KEY_CURRENT_MULTIPLIER, PersistentDataType.DOUBLE);
        }
        return 1.0;
    }
}