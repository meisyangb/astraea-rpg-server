package cn.guangdian.devour.parser;

import cn.guangdian.devour.GuangDianDevour;
import cn.guangdian.devour.data.AttributeValue;
import cn.guangdian.rpgcore.util.TextStripper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 吞噬属性解析器
 * 解析武器Lore中的所有属性
 * 
 * @author Astraea RPG Team
 * @since 1.0.0
 */
public class DevourParser {
    
    private final GuangDianDevour plugin;
    
    // 属性正则模式
    private static final Map<String, Pattern> ATTRIBUTE_PATTERNS = new HashMap<>();
    
    static {
        // 攻击类属性
        ATTRIBUTE_PATTERNS.put("攻击力", Pattern.compile("攻击力[：:][\\s]*\\+?([\\d]+)(?:-([\\d]+))?"));
        ATTRIBUTE_PATTERNS.put("PVP攻击力", Pattern.compile("【PVP】攻击力[：:][\\s]*\\+?([\\d]+)(?:-([\\d]+))?"));
        ATTRIBUTE_PATTERNS.put("暴击几率", Pattern.compile("暴击几率[：:][\\s]*\\+?([\\d.]+)%?"));
        ATTRIBUTE_PATTERNS.put("暴击伤害", Pattern.compile("暴击伤害[：:][\\s]*\\+?([\\d.]+)%?"));
        ATTRIBUTE_PATTERNS.put("命中", Pattern.compile("命中[：:][\\s]*\\+?([\\d.]+)%?"));
        ATTRIBUTE_PATTERNS.put("招架", Pattern.compile("招架[：:][\\s]*\\+?([\\d.]+)%?"));
        ATTRIBUTE_PATTERNS.put("穿透", Pattern.compile("穿透[：:][\\s]*\\+?([\\d.]+)%?"));
        ATTRIBUTE_PATTERNS.put("破甲", Pattern.compile("破甲[：:][\\s]*\\+?([\\d.]+)%?"));
        ATTRIBUTE_PATTERNS.put("真实伤害", Pattern.compile("真实伤害[：:][\\s]*\\+?([\\d.]+)%?"));
        
        // 吸血类属性
        ATTRIBUTE_PATTERNS.put("吸血几率", Pattern.compile("吸血几率[：:][\\s]*\\+?([\\d.]+)%?"));
        ATTRIBUTE_PATTERNS.put("吸血倍率", Pattern.compile("吸血倍率[：:][\\s]*\\+?([\\d.]+)%?"));
        
        // 速度类属性
        ATTRIBUTE_PATTERNS.put("攻速", Pattern.compile("攻速[：:][\\s]*\\+?([\\d.]+)%?"));
        ATTRIBUTE_PATTERNS.put("移动速度", Pattern.compile("移动速度[：:][\\s]*\\+?([\\d.]+)%?"));
        
        // 状态效果属性
        ATTRIBUTE_PATTERNS.put("冰冻", Pattern.compile("冰冻[：:][\\s]*\\+?([\\d.]+)%?"));
        ATTRIBUTE_PATTERNS.put("致盲", Pattern.compile("致盲[：:][\\s]*\\+?([\\d.]+)%?"));
        ATTRIBUTE_PATTERNS.put("中毒", Pattern.compile("中毒[：:][\\s]*\\+?([\\d.]+)%?"));
        ATTRIBUTE_PATTERNS.put("燃烧", Pattern.compile("燃烧[：:][\\s]*\\+?([\\d.]+)%?"));
        ATTRIBUTE_PATTERNS.put("灼烧", Pattern.compile("灼烧[：:][\\s]*\\+?([\\d.]+)%?"));
        
        // 防御类属性
        ATTRIBUTE_PATTERNS.put("护甲强度", Pattern.compile("护甲强度[：:][\\s]*\\+?([\\d.]+)%?"));
        ATTRIBUTE_PATTERNS.put("生命上限", Pattern.compile("生命上限[：:][\\s]*\\+?([\\d]+)"));
        ATTRIBUTE_PATTERNS.put("每秒回血", Pattern.compile("每秒回血[：:][\\s]*\\+?([\\d]+)"));
        ATTRIBUTE_PATTERNS.put("韧性", Pattern.compile("韧性[：:][\\s]*\\+?([\\d.]+)%?"));
        ATTRIBUTE_PATTERNS.put("伤害减免", Pattern.compile("伤害减免[：:][\\s]*\\+?([\\d.]+)%?"));
        ATTRIBUTE_PATTERNS.put("闪避", Pattern.compile("闪避[：:][\\s]*\\+?([\\d.]+)%?"));
        ATTRIBUTE_PATTERNS.put("格挡", Pattern.compile("格挡[：:][\\s]*\\+?([\\d.]+)%?"));
        ATTRIBUTE_PATTERNS.put("反伤", Pattern.compile("反伤[：:][\\s]*\\+?([\\d.]+)%?"));
        ATTRIBUTE_PATTERNS.put("反伤比例", Pattern.compile("反伤比例[：:][\\s]*\\+?([\\d.]+)%?"));
        
        // 抵抗类属性
        ATTRIBUTE_PATTERNS.put("暴击抵抗", Pattern.compile("暴击抵抗[：:][\\s]*\\+?([\\d.]+)%?"));
        ATTRIBUTE_PATTERNS.put("暴伤抵抗", Pattern.compile("暴伤抵抗[：:][\\s]*\\+?([\\d.]+)%?"));
        ATTRIBUTE_PATTERNS.put("吸血抵抗", Pattern.compile("吸血抵抗[：:][\\s]*\\+?([\\d.]+)%?"));
    }
    
    public DevourParser(GuangDianDevour plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 解析武器属性
     */
    public Map<String, AttributeValue> parseWeaponAttributes(ItemStack item) {
        Map<String, AttributeValue> attributes = new HashMap<>();
        
        if (item == null || !item.hasItemMeta()) {
            return attributes;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) {
            return attributes;
        }
        
        List<Component> lore = meta.lore();
        if (lore == null) {
            return attributes;
        }
        
        // 遍历Lore解析属性
        for (Component line : lore) {
            String strippedLine = PlainTextComponentSerializer.plainText().serialize(line);
            
            // 尝试匹配所有属性模式
            for (Map.Entry<String, Pattern> entry : ATTRIBUTE_PATTERNS.entrySet()) {
                String attrName = entry.getKey();
                Pattern pattern = entry.getValue();
                
                Matcher matcher = pattern.matcher(strippedLine);
                if (matcher.find()) {
                    AttributeValue value = parseValue(attrName, matcher);
                    if (value != null) {
                        // 检查是否为可吞噬属性
                        if (plugin.getConfigManager().isDevourableAttribute(attrName)) {
                            attributes.put(attrName, value);
                        }
                    }
                    break;  // 找到匹配，跳过后续模式
                }
            }
        }
        
        return attributes;
    }
    
    /**
     * 解析属性值
     */
    private AttributeValue parseValue(String attrName, Matcher matcher) {
        int groupCount = matcher.groupCount();
        
        try {
            if (groupCount == 1) {
                // 单值或百分比
                String valueStr = matcher.group(1);
                if (valueStr == null || valueStr.isEmpty()) {
                    return null;
                }
                
                // 判断是否为百分比属性
                if (isPercentAttribute(attrName)) {
                    double value = Double.parseDouble(valueStr);
                    return AttributeValue.ofPercent(value);
                } else {
                    double value = Double.parseDouble(valueStr);
                    return AttributeValue.of(value);
                }
            } else if (groupCount >= 2) {
                // 范围值
                String minStr = matcher.group(1);
                String maxStr = matcher.group(2);
                
                if (minStr == null || minStr.isEmpty()) {
                    return null;
                }
                
                double min = Double.parseDouble(minStr);
                
                if (maxStr == null || maxStr.isEmpty()) {
                    // 单值
                    return AttributeValue.of(min);
                } else {
                    // 范围值
                    double max = Double.parseDouble(maxStr);
                    return AttributeValue.ofRange(min, max);
                }
            }
        } catch (NumberFormatException e) {
            return null;
        }
        
        return null;
    }
    
    /**
     * 判断是否为百分比属性
     */
    private boolean isPercentAttribute(String attrName) {
        return attrName.contains("几率") ||
               attrName.contains("倍率") ||
               attrName.contains("强度") ||
               attrName.contains("抵抗") ||
               attrName.contains("减免") ||
               attrName.contains("闪避") ||
               attrName.contains("格挡") ||
               attrName.contains("反伤") ||
               attrName.contains("穿透") ||
               attrName.contains("破甲") ||
               attrName.contains("真实伤害") ||
               attrName.contains("冰冻") ||
               attrName.contains("致盲") ||
               attrName.contains("中毒") ||
               attrName.contains("燃烧") ||
               attrName.contains("灼烧") ||
               attrName.contains("韧性") ||
               attrName.contains("攻速") ||
               attrName.contains("移动速度") ||
               attrName.contains("暴击") ||
               attrName.contains("命中") ||
               attrName.contains("招架");
    }
    
    /**
     * 计算属性增量
     */
    public String calculateIncrement(AttributeValue original, AttributeValue after) {
        if (original == null || after == null) {
            return "";
        }
        
        if (original.isRange() && after.isRange()) {
            double minDiff = after.getMin() - original.getMin();
            double maxDiff = after.getMax() - original.getMax();
            return String.format("+%.0f-%.0f", minDiff, maxDiff);
        } else if (original.isPercent() && after.isPercent()) {
            double diff = after.getValue() - original.getValue();
            return String.format("+%.1f%%", diff);
        } else {
            double diff = after.getValue() - original.getValue();
            return String.format("+%.0f", diff);
        }
    }
}
