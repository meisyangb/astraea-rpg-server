package cn.guangdian.enhance.util;

import cn.guangdian.rpgitems.attribute.CompoundAttributeCodec;
import cn.guangdian.rpgitems.attribute.RPGItemsKeys;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * 【枚举法】属性更新工具类
 * 直接通过PDC操作物品属性，不依赖外部API
 */
public class EnhanceAttributeHelper {

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
        return pdc.has(RPGItemsKeys.ID, PersistentDataType.STRING);
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
        if (pdc.has(RPGItemsKeys.TIER, PersistentDataType.STRING)) {
            return pdc.get(RPGItemsKeys.TIER, PersistentDataType.STRING);
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
            if (!pdc.has(RPGItemsKeys.BASE_ATTACK_MIN, PersistentDataType.DOUBLE)) {
                saveBaseAttributes(pdc);
            }
            updateAttributesWithMultiplier(pdc, multiplier);
        }

        // 保存当前倍率
        pdc.set(RPGItemsKeys.ENHANCE_MULT, PersistentDataType.DOUBLE, multiplier);
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
        if (pdc.has(RPGItemsKeys.ENHANCE_MULT, PersistentDataType.DOUBLE)) {
            double currentMult = pdc.get(RPGItemsKeys.ENHANCE_MULT, PersistentDataType.DOUBLE);
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
            if (!pdc.has(RPGItemsKeys.BASE_ATTACK_MIN, PersistentDataType.DOUBLE)) {
                saveBaseAttributes(pdc);
            }
            updateAttributesWithMultiplier(pdc, correctMultiplier);
        }

        pdc.set(RPGItemsKeys.ENHANCE_MULT, PersistentDataType.DOUBLE, correctMultiplier);
        item.setItemMeta(meta);
        return true;
    }
    
    /**
     * 保存基础属性值
     */
    private static void saveBaseAttributes(PersistentDataContainer pdc) {
        // 攻击
        if (pdc.has(RPGItemsKeys.ATTACK_MIN, PersistentDataType.DOUBLE)) {
            pdc.set(RPGItemsKeys.BASE_ATTACK_MIN, PersistentDataType.DOUBLE,
                pdc.get(RPGItemsKeys.ATTACK_MIN, PersistentDataType.DOUBLE));
        }
        if (pdc.has(RPGItemsKeys.ATTACK_MAX, PersistentDataType.DOUBLE)) {
            pdc.set(RPGItemsKeys.BASE_ATTACK_MAX, PersistentDataType.DOUBLE,
                pdc.get(RPGItemsKeys.ATTACK_MAX, PersistentDataType.DOUBLE));
        }

        // 防御
        if (pdc.has(RPGItemsKeys.DEFENSE_MIN, PersistentDataType.DOUBLE)) {
            pdc.set(RPGItemsKeys.BASE_DEFENSE_MIN, PersistentDataType.DOUBLE,
                pdc.get(RPGItemsKeys.DEFENSE_MIN, PersistentDataType.DOUBLE));
        }
        if (pdc.has(RPGItemsKeys.DEFENSE_MAX, PersistentDataType.DOUBLE)) {
            pdc.set(RPGItemsKeys.BASE_DEFENSE_MAX, PersistentDataType.DOUBLE,
                pdc.get(RPGItemsKeys.DEFENSE_MAX, PersistentDataType.DOUBLE));
        }

        // 生命
        if (pdc.has(RPGItemsKeys.MAX_HEALTH, PersistentDataType.DOUBLE)) {
            pdc.set(RPGItemsKeys.BASE_MAX_HEALTH, PersistentDataType.DOUBLE,
                pdc.get(RPGItemsKeys.MAX_HEALTH, PersistentDataType.DOUBLE));
        }

        // 暴击
        if (pdc.has(RPGItemsKeys.CRIT_CHANCE, PersistentDataType.DOUBLE)) {
            pdc.set(RPGItemsKeys.BASE_CRIT_CHANCE, PersistentDataType.DOUBLE,
                pdc.get(RPGItemsKeys.CRIT_CHANCE, PersistentDataType.DOUBLE));
        }
        if (pdc.has(RPGItemsKeys.CRIT_DAMAGE, PersistentDataType.DOUBLE)) {
            pdc.set(RPGItemsKeys.BASE_CRIT_DAMAGE, PersistentDataType.DOUBLE,
                pdc.get(RPGItemsKeys.CRIT_DAMAGE, PersistentDataType.DOUBLE));
        }
    }
    
    /**
     * 按倍率更新属性值
     */
    private static void updateAttributesWithMultiplier(PersistentDataContainer pdc, double multiplier) {
        // 攻击
        if (pdc.has(RPGItemsKeys.BASE_ATTACK_MIN, PersistentDataType.DOUBLE)) {
            double base = pdc.get(RPGItemsKeys.BASE_ATTACK_MIN, PersistentDataType.DOUBLE);
            pdc.set(RPGItemsKeys.ATTACK_MIN, PersistentDataType.DOUBLE, base * multiplier);
        }
        if (pdc.has(RPGItemsKeys.BASE_ATTACK_MAX, PersistentDataType.DOUBLE)) {
            double base = pdc.get(RPGItemsKeys.BASE_ATTACK_MAX, PersistentDataType.DOUBLE);
            pdc.set(RPGItemsKeys.ATTACK_MAX, PersistentDataType.DOUBLE, base * multiplier);
        }

        // 防御
        if (pdc.has(RPGItemsKeys.BASE_DEFENSE_MIN, PersistentDataType.DOUBLE)) {
            double base = pdc.get(RPGItemsKeys.BASE_DEFENSE_MIN, PersistentDataType.DOUBLE);
            pdc.set(RPGItemsKeys.DEFENSE_MIN, PersistentDataType.DOUBLE, base * multiplier);
        }
        if (pdc.has(RPGItemsKeys.BASE_DEFENSE_MAX, PersistentDataType.DOUBLE)) {
            double base = pdc.get(RPGItemsKeys.BASE_DEFENSE_MAX, PersistentDataType.DOUBLE);
            pdc.set(RPGItemsKeys.DEFENSE_MAX, PersistentDataType.DOUBLE, base * multiplier);
        }

        // 生命
        if (pdc.has(RPGItemsKeys.BASE_MAX_HEALTH, PersistentDataType.DOUBLE)) {
            double base = pdc.get(RPGItemsKeys.BASE_MAX_HEALTH, PersistentDataType.DOUBLE);
            pdc.set(RPGItemsKeys.MAX_HEALTH, PersistentDataType.DOUBLE, base * multiplier);
        }

        // 暴击
        if (pdc.has(RPGItemsKeys.BASE_CRIT_CHANCE, PersistentDataType.DOUBLE)) {
            double base = pdc.get(RPGItemsKeys.BASE_CRIT_CHANCE, PersistentDataType.DOUBLE);
            pdc.set(RPGItemsKeys.CRIT_CHANCE, PersistentDataType.DOUBLE, base * multiplier);
        }
        if (pdc.has(RPGItemsKeys.BASE_CRIT_DAMAGE, PersistentDataType.DOUBLE)) {
            double base = pdc.get(RPGItemsKeys.BASE_CRIT_DAMAGE, PersistentDataType.DOUBLE);
            pdc.set(RPGItemsKeys.CRIT_DAMAGE, PersistentDataType.DOUBLE, base * multiplier);
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
        if (pdc.has(RPGItemsKeys.ENHANCE_MULT, PersistentDataType.DOUBLE)) {
            return pdc.get(RPGItemsKeys.ENHANCE_MULT, PersistentDataType.DOUBLE);
        }
        return 1.0;
    }
}