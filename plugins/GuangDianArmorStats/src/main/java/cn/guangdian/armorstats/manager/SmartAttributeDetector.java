package cn.guangdian.armorstats.manager;

import cn.guangdian.armorstats.data.AttributeValue;
import cn.guangdian.armorstats.parser.LoreParser;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public class SmartAttributeDetector {

    public enum EquipmentCategory {
        WEAPON, ARMOR, ACCESSORY, UNKNOWN
    }

    /**
     * 根据Lore第一行识别装备类型
     * 第一行格式示例:
     * - 武器: §f近战武器                  普通 §f
     * - 防具: §f防具                   普通 §f
     * - 其他格式: §a防具              传说, §9防具                   远古传说
     */
    public EquipmentCategory categorize(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return EquipmentCategory.UNKNOWN;
        }

        // 首先尝试从Lore第一行识别
        EquipmentCategory loreCategory = categorizeByFirstLoreLine(item);
        if (loreCategory != EquipmentCategory.UNKNOWN) {
            return loreCategory;
        }

        // 如果Lore第一行无法识别，则使用属性权重判断（兼容旧格式）
        Map<String, AttributeValue> attrs = parseAttributes(item);

        int attackWeight = calculateAttackRelatedWeight(attrs);
        int defenseWeight = calculateDefenseRelatedWeight(attrs);
        int utilityWeight = calculateUtilityWeight(attrs);

        if (attackWeight > defenseWeight && attackWeight > utilityWeight) {
            return EquipmentCategory.WEAPON;
        } else if (defenseWeight > utilityWeight) {
            return EquipmentCategory.ARMOR;
        } else {
            return EquipmentCategory.ACCESSORY;
        }
    }

    /**
     * 从Lore第一行识别装备类型
     * 格式: §f近战武器                  普通 §f (武器)
     * 格式: §f防具                   普通 §f (防具)
     */
    private EquipmentCategory categorizeByFirstLoreLine(ItemStack item) {
        if (!item.hasItemMeta()) {
            return EquipmentCategory.UNKNOWN;
        }

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) {
            return EquipmentCategory.UNKNOWN;
        }

        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) {
            return EquipmentCategory.UNKNOWN;
        }

        // 获取第一行并移除颜色代码
        String firstLine = lore.get(0);
        String strippedLine = stripColorCodes(firstLine);

        // 检测是否包含"防具"
        if (strippedLine.contains("防具")) {
            return EquipmentCategory.ARMOR;
        }

        // 检测是否包含"近战武器"或"远程武器"
        if (strippedLine.contains("近战武器") || strippedLine.contains("远程武器")) {
            return EquipmentCategory.WEAPON;
        }

        // 单独检测"武器"关键字
        if (strippedLine.contains("武器") && !strippedLine.contains("防具")) {
            return EquipmentCategory.WEAPON;
        }
        
        // 检测"锻造武器"关键字
        if (strippedLine.contains("锻造武器")) {
            return EquipmentCategory.WEAPON;
        }

        return EquipmentCategory.UNKNOWN;
    }

    /**
     * 移除所有颜色代码
     */
    private String stripColorCodes(String text) {
        if (text == null) return "";
        // 移除 § 和 & 后面的颜色代码字符
        return text.replaceAll("[&§][0-9a-fk-or]", "");
    }

    /**
     * 检测物品是否是防具（根据Lore第一行）
     */
    public boolean isArmor(ItemStack item) {
        return categorize(item) == EquipmentCategory.ARMOR;
    }

    /**
     * 检测物品是否是武器（根据Lore第一行）
     */
    public boolean isWeapon(ItemStack item) {
        return categorize(item) == EquipmentCategory.WEAPON;
    }

    /**
     * 解析装备属性
     * 
     * 注意: 宝石属性解析已迁移到 GuangDianSocket 插件
     * 装备属性不再包含宝石属性，由 GuangDianSocket 在镶嵌时直接写入装备Lore
     */
    private Map<String, AttributeValue> parseAttributes(ItemStack item) {
        return LoreParser.parse(item);
    }

    private int calculateAttackRelatedWeight(Map<String, AttributeValue> attrs) {
        int weight = 0;
        
        if (attrs.containsKey("攻击力")) {
            weight += 100;
        }
        if (attrs.containsKey("暴击几率")) {
            weight += 80;
        }
        if (attrs.containsKey("暴击伤害")) {
            weight += 70;
        }
        if (attrs.containsKey("攻击速度")) {
            weight += 60;
        }
        if (attrs.containsKey("穿透")) {
            weight += 50;
        }
        
        return weight;
    }

    private int calculateDefenseRelatedWeight(Map<String, AttributeValue> attrs) {
        int weight = 0;
        
        if (attrs.containsKey("防御力")) {
            weight += 100;
        }
        if (attrs.containsKey("生命上限")) {
            weight += 90;
        }
        if (attrs.containsKey("闪避")) {
            weight += 60;
        }
        if (attrs.containsKey("抗性")) {
            weight += 50;
        }
        if (attrs.containsKey("减伤")) {
            weight += 40;
        }
        
        return weight;
    }

    private int calculateUtilityWeight(Map<String, AttributeValue> attrs) {
        int weight = 0;
        
        if (attrs.containsKey("移动速度")) {
            weight += 80;
        }
        if (attrs.containsKey("每秒回血")) {
            weight += 70;
        }
        if (attrs.containsKey("经验加成")) {
            weight += 50;
        }
        if (attrs.containsKey("幸运值")) {
            weight += 40;
        }
        if (attrs.containsKey("魔法值")) {
            weight += 30;
        }
        
        return weight;
    }
}