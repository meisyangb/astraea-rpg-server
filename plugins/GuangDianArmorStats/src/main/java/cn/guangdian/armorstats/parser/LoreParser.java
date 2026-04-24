package cn.guangdian.armorstats.parser;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.data.AttributeValue;
import cn.guangdian.rpgcore.util.TextStripper;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lore解析器 - 高性能优化版
 * 
 * 优化特性:
 * 1. 合并正则模式 - 单次匹配所有属性，避免逐个正则遍历
 * 2. 快速颜色剥离 - 字符级处理，避免正则开销
 * 3. 属性名索引 - 快速定位属性类型
 * 4. 缓存友好的解析结果
 * 
 * @author GuangDian
 * @since 2.0.0
 */
public class LoreParser {

    private static final Map<String, Pattern> ATTRIBUTE_PATTERNS = new HashMap<>();
    private static boolean initialized = false;

    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("(?:<[a-z_]+>)*");
    private static final Pattern ATTRIBUTE_LINE_PATTERN = Pattern.compile("<dark_gray>([^:]+):\\s*(?:<[a-z_]+>)*([\\d.]+%|[\\d]+-[\\d]+|[\\d]+)");

    private static final Map<String, Pattern> DIRECT_PATTERNS = new LinkedHashMap<>();  // 性能优化: 使用LinkedHashMap保持顺序
    private static final Map<String, Pattern> SKILL_PATTERNS = new HashMap<>();

    // 性能优化: 属性名快速查找表（用于提前判断是否需要解析）
    private static final Set<String> ATTRIBUTE_KEYWORDS = new HashSet<>();

    // ==================== 高性能优化: Trie 属性树 ====================

    /**
     * 属性名 Trie 树 - O(n) 时间复杂度匹配
     */
    private static AttributeTrie attributeTrie;

    // ==================== 高性能优化: 合并正则模式 ====================
    
    /**
     * 合并的属性匹配正则
     * 模式: (属性名1|属性名2|...)[：:]\\s*(\\+?[\\d.]+%?|\\+?[\\d]+-[\\d]+)
     */
    private static Pattern COMBINED_ATTRIBUTE_PATTERN;
    
    /**
     * 属性名到值解析器的映射
     */
    private static final Map<String, Function<Matcher, AttributeValue>> VALUE_PARSERS = new HashMap<>();
    
    /**
     * 快速属性名集合（用于合并正则）
     */
    private static final List<String> ORDERED_ATTR_NAMES = new ArrayList<>();

    static {
        // 修复: 正则表达式用于匹配已去除颜色代码的文本
        // stripColor 在匹配前调用，因此正则中不需要颜色代码匹配部分
        
        // 防御类属性 (范围格式) - 支持中英文冒号
        DIRECT_PATTERNS.put("防御力", Pattern.compile("防御力[：:][\\s]*\\+?([\\d]+)(?:-([\\d]+))?"));
        DIRECT_PATTERNS.put("护甲强度", Pattern.compile("护甲强度[：:][\\s]*\\+?([\\d.]+)%?"));
        DIRECT_PATTERNS.put("护甲值", Pattern.compile("护甲值[：:][\\s]*\\+?([\\d.]+)%?"));

        // 生命类属性
        DIRECT_PATTERNS.put("生命上限", Pattern.compile("生命上限[：:][\\s]*\\+?([\\d]+)"));
        DIRECT_PATTERNS.put("每秒回血", Pattern.compile("每秒回血[：:][\\s]*\\+?([\\d]+)"));
        DIRECT_PATTERNS.put("生命回复", Pattern.compile("生命回复[：:][\\s]*\\+?([\\d]+)"));

        // 攻击类属性 (范围格式)
        DIRECT_PATTERNS.put("攻击力", Pattern.compile("攻击力[：:][\\s]*\\+?([\\d]+)(?:-([\\d]+))?"));
        DIRECT_PATTERNS.put("PVP攻击力", Pattern.compile("【PVP】攻击力[：:][\\s]*\\+?([\\d]+)(?:-([\\d]+))?"));
        DIRECT_PATTERNS.put("PVP防御力", Pattern.compile("【PVP】防御力[：:][\\s]*\\+?([\\d]+)(?:-([\\d]+))?"));

        // 暴击类属性 - 支持中英文冒号，百分号可选
        DIRECT_PATTERNS.put("暴击几率", Pattern.compile("暴击几率[：:][\\s]*\\+?([\\d.]+)%?"));
        DIRECT_PATTERNS.put("暴击伤害", Pattern.compile("暴击伤害[：:][\\s]*\\+?([\\d.]+)%?"));
        DIRECT_PATTERNS.put("暴击抵抗", Pattern.compile("暴击抵抗[：:][\\s]*\\+?([\\d.]+)%?"));
        DIRECT_PATTERNS.put("暴伤抵抗", Pattern.compile("暴伤抵抗[：:][\\s]*\\+?([\\d.]+)%?"));

        // 战斗属性
        DIRECT_PATTERNS.put("招架", Pattern.compile("招架[：:][\\s]*\\+?([\\d.]+)%?"));
        DIRECT_PATTERNS.put("闪避", Pattern.compile("闪避[：:][\\s]*\\+?([\\d.]+)%?"));
        DIRECT_PATTERNS.put("吸血几率", Pattern.compile("吸血几率[：:][\\s]*\\+?([\\d.]+)%?"));
        DIRECT_PATTERNS.put("吸血倍率", Pattern.compile("吸血倍率[：:][\\s]*\\+?([\\d.]+)%?"));
        DIRECT_PATTERNS.put("吸血抵抗", Pattern.compile("吸血抵抗[：:][\\s]*\\+?([\\d.]+)%?"));
        DIRECT_PATTERNS.put("伤害反弹", Pattern.compile("伤害反弹[：:][\\s]*\\+?([\\d.]+)%?"));
        DIRECT_PATTERNS.put("反伤比例", Pattern.compile("反伤比例[：:][\\s]*\\+?([\\d.]+)%?"));

        // 移动属性
        DIRECT_PATTERNS.put("移动速度", Pattern.compile("移动速度[：:][\\s]*\\+?([\\d.]+)%?"));

        // 状态效果属性
        DIRECT_PATTERNS.put("中毒", Pattern.compile("中毒[：:][\\s]*\\+?([\\d.]+)%?"));
        DIRECT_PATTERNS.put("冰冻", Pattern.compile("冰冻[：:][\\s]*\\+?([\\d.]+)%?"));
        DIRECT_PATTERNS.put("致盲", Pattern.compile("致盲[：:][\\s]*\\+?([\\d.]+)%?"));
        DIRECT_PATTERNS.put("燃烧", Pattern.compile("燃烧[：:][\\s]*\\+?([\\d.]+)%?"));
        DIRECT_PATTERNS.put("灼烧", Pattern.compile("灼烧[：:][\\s]*\\+?([\\d.]+)%?"));

        // 其他属性
        DIRECT_PATTERNS.put("经验加成", Pattern.compile("经验加成[：:][\\s]*\\+?([\\d.]+)%?"));

        // 穿透属性
        DIRECT_PATTERNS.put("护甲穿透", Pattern.compile("护甲穿透[：:][\\s]*\\+?([\\d.]+)%?"));
        DIRECT_PATTERNS.put("防御穿透", Pattern.compile("防御穿透[：:][\\s]*\\+?([\\d.]+)%?"));

        // 躲避反伤属性（拆分为触发概率和反弹比例）
        DIRECT_PATTERNS.put("躲避反伤", Pattern.compile("躲避反伤[：:][\\s]*\\+?([\\d.]+)%?"));
        DIRECT_PATTERNS.put("躲避反弹比例", Pattern.compile("躲避反弹比例[：:][\\s]*\\+?([\\d.]+)%?"));

        // 生命恢复属性（百分比回复）
        DIRECT_PATTERNS.put("生命恢复", Pattern.compile("生命恢复[：:][\\s]*\\+?([\\d.]+)%?"));

        // 技能解析模式 - 技能名不需要去除颜色，但匹配strippedLine
        SKILL_PATTERNS.put("主动技能", Pattern.compile("主动技能[：:][\\s]*([^&]+)"));
        SKILL_PATTERNS.put("被动技能", Pattern.compile("被动技能[：:][\\s]*([^&]+)"));
        SKILL_PATTERNS.put("技能", Pattern.compile("技能[：:][\\s]*([^&]+)"));

        // 性能优化: 初始化属性关键词集合（用于快速判断）
        ATTRIBUTE_KEYWORDS.addAll(DIRECT_PATTERNS.keySet());

        // ==================== 高性能优化: 初始化 Trie 属性树 ====================
        attributeTrie = AttributeTrie.createDefault();

        // ==================== 高性能优化: 构建合并正则 ====================
        buildCombinedPattern();
    }
    
    /**
     * 构建合并正则模式
     * 将所有属性名合并为单个正则，一次匹配所有属性
     */
    private static void buildCombinedPattern() {
        // 收集所有属性名（保持顺序）
        ORDERED_ATTR_NAMES.addAll(DIRECT_PATTERNS.keySet());
        
        // 构建值匹配模式
        // 支持格式: +数字、数字-数字、数字%、数字.% 
        String valuePattern = "(\\+?[\\d]+-[\\d]+|\\+?[\\d.]+%?|\\+?[\\d]+)";
        
        // 构建合并正则
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < ORDERED_ATTR_NAMES.size(); i++) {
            if (i > 0) sb.append("|");
            // 转义特殊字符（如【PVP】中的中括号）
            String attrName = ORDERED_ATTR_NAMES.get(i);
            sb.append(Pattern.quote(attrName));
        }
        sb.append(")[：:][\\s]*");
        sb.append(valuePattern);
        
        COMBINED_ATTRIBUTE_PATTERN = Pattern.compile(sb.toString());
        
        GuangDianArmorStats.getInstance().getLogger().info(
            "[LoreParser] 合并正则模式已构建，共 " + ORDERED_ATTR_NAMES.size() + " 个属性");
    }

    public static void initializePatterns(Map<String, String> configPatterns) {
        ATTRIBUTE_PATTERNS.clear();
        for (Map.Entry<String, String> entry : configPatterns.entrySet()) {
            String patternStr = entry.getValue();
            // 剥离传统颜色代码（& 和 §），用于MythicMobs物品Lore解析
            // 注意：属性配置文件通常使用传统颜色格式
            patternStr = TextStripper.stripLegacy(patternStr);
            ATTRIBUTE_PATTERNS.put(entry.getKey(), Pattern.compile(patternStr));
        }
        initialized = true;
        GuangDianArmorStats.getInstance().getLogger().info("=== LoreParser Config ===");
        GuangDianArmorStats.getInstance().getLogger().info("Patterns loaded: " + ATTRIBUTE_PATTERNS.size());
        for (String key : ATTRIBUTE_PATTERNS.keySet()) {
            GuangDianArmorStats.getInstance().getLogger().info("  " + key + " -> " + ATTRIBUTE_PATTERNS.get(key).pattern());
        }
        GuangDianArmorStats.getInstance().getLogger().info("========================");
    }

    /**
     * 快速检测物品是否有RPG属性
     * 用于区分原版物品和自定义物品
     */
    public static boolean hasRpgAttributes(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return false;
        }

        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) {
            return false;
        }

        for (String line : lore) {
            if (mightContainAttributeFast(line)) {
                // 剥离传统颜色代码（& 和 §），用于MythicMobs物品Lore解析
                String strippedLine = TextStripper.stripLegacy(line);
                Matcher matcher = COMBINED_ATTRIBUTE_PATTERN.matcher(strippedLine);
                if (matcher.find()) {
                    return true;
                }
            }
        }

        return false;
    }

    public static Map<String, AttributeValue> parse(ItemStack item) {
        Map<String, AttributeValue> attributes = new HashMap<>();

        if (item == null || !item.hasItemMeta()) {
            return attributes;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return attributes;
        }

        List<String> lore = meta.getLore();
        if (lore == null) {
            return attributes;
        }

        // ==================== 高性能优化: 使用合并正则模式 ====================
        // 单次遍历Lore，使用合并正则一次匹配所有属性
        for (String line : lore) {
            // 性能优化: 快速跳过不含属性的行
            if (!mightContainAttributeFast(line)) {
                continue;
            }

            // 剥离传统颜色代码（& 和 §）
            // 注意：物品Lore通常使用传统颜色代码，不使用 MiniMessage
            String strippedLine = TextStripper.stripLegacy(line);



            // 使用合并正则单次匹配
            Matcher matcher = COMBINED_ATTRIBUTE_PATTERN.matcher(strippedLine);
            if (matcher.find()) {
                String attrName = matcher.group(1);  // 属性名
                String valueStr = matcher.group(2);  // 值

                AttributeValue value = parseValueFast(attrName, valueStr);
                if (value != null) {
                    attributes.merge(attrName, value, AttributeValue::merge);
                }
                continue;  // 已匹配，跳过后续处理
            }

            // 如果合并正则未匹配，尝试配置模式匹配
            if (initialized) {
                for (Map.Entry<String, Pattern> entry : ATTRIBUTE_PATTERNS.entrySet()) {
                    String attrName = entry.getKey();
                    Pattern pattern = entry.getValue();

                    Matcher configMatcher = pattern.matcher(strippedLine);
                    if (configMatcher.find()) {
                        AttributeValue value = parseValue(attrName, configMatcher);
                        if (value != null) {
                            attributes.merge(attrName, value, AttributeValue::merge);
                        }
                    }
                }
            }
        }

        return attributes;
    }
    
    /**
     * 高性能解析值
     * 根据值字符串格式判断是范围值还是单值
     */
    private static AttributeValue parseValueFast(String attrName, String valueStr) {
        if (valueStr == null || valueStr.isEmpty()) {
            return null;
        }
        
        try {
            // 移除可能的+号
            valueStr = valueStr.replace("+", "");
            
            // 判断是否是范围值 (min-max格式)
            if (valueStr.contains("-") && !valueStr.startsWith("-")) {
                String[] parts = valueStr.split("-");
                if (parts.length == 2) {
                    double min = Double.parseDouble(parts[0]);
                    double max = Double.parseDouble(parts[1]);
                    return AttributeValue.ofRange(min, max);
                }
            }
            
            // 判断是否是百分比值
            if (valueStr.endsWith("%")) {
                double val = Double.parseDouble(valueStr.substring(0, valueStr.length() - 1));
                return AttributeValue.ofPercent(val);
            }
            
            // 普通数值
            double val = Double.parseDouble(valueStr);
            return AttributeValue.of(val);
            
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 高性能: 快速判断行是否可能包含属性
     * 优化: 先检查冒号，再检查关键词
     */
    private static boolean mightContainAttributeFast(String line) {
        if (line == null || line.isEmpty()) {
            return false;
        }
        
        // 快速检查: 属性行通常包含冒号（中英文）
        // 这是最快的检查，因为大多属性行都有冒号
        int len = line.length();
        for (int i = 0; i < len; i++) {
            char c = line.charAt(i);
            if (c == ':' || c == '：') {
                return true;
            }
        }
        
        // 检查是否包含任何属性关键词
        for (String keyword : ATTRIBUTE_KEYWORDS) {
            if (line.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 性能优化: 快速判断行是否可能包含属性
     * 通过关键词匹配避免对每行都进行正则匹配
     *
     * @param line Lore行
     * @return 是否可能包含属性
     */
    private static boolean mightContainAttribute(String line) {
        if (line == null || line.isEmpty()) {
            return false;
        }
        // 快速检查: 属性行通常包含冒号或关键属性名
        if (line.contains(":")) {
            return true;
        }
        // 检查是否包含任何属性关键词
        for (String keyword : ATTRIBUTE_KEYWORDS) {
            if (line.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 剥离颜色代码（兼容旧方法，已废弃）
     * @deprecated 使用 {@link TextStripper#stripLegacy(String)} 替代
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public static String stripColorStatic(String input) {
        return TextStripper.stripLegacy(input);
    }

    public static int getPatternCount() {
        return initialized ? ATTRIBUTE_PATTERNS.size() : 0;
    }

    private static AttributeValue parseValue(String attrName, Matcher matcher) {
        int groupCount = matcher.groupCount();

        if (groupCount == 1) {
            String valueStr = matcher.group(1);
            try {
                if (valueStr.endsWith("%")) {
                    double val = Double.parseDouble(valueStr.substring(0, valueStr.length() - 1));
                    return AttributeValue.ofPercent(val);
                } else {
                    double val = Double.parseDouble(valueStr);
                    return AttributeValue.of(val);
                }
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (groupCount == 2) {
            String minStr = matcher.group(1);
            String maxStr = matcher.group(2);
            try {
                double min = Double.parseDouble(minStr);
                if (maxStr == null || maxStr.isEmpty()) {
                    return AttributeValue.of(min);
                }
                double max = Double.parseDouble(maxStr);
                return AttributeValue.ofRange(min, max);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    public static Map<String, Double> parseFromConfig(Map<String, Object> config) {
        Map<String, Double> result = new HashMap<>();
        if (config == null) {
            return result;
        }

        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (entry.getValue() instanceof Number) {
                result.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
            }
        }
        return result;
    }

    public static List<String> parseSkills(ItemStack item) {
        List<String> skills = new ArrayList<>();

        if (item == null || !item.hasItemMeta()) {
            return skills;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return skills;
        }

        List<String> lore = meta.getLore();
        if (lore == null) {
            return skills;
        }

        for (String line : lore) {
            // 剥离传统颜色代码（& 和 §），用于MythicMobs物品Lore解析
            String strippedLine = TextStripper.stripLegacy(line);

            for (Map.Entry<String, Pattern> entry : SKILL_PATTERNS.entrySet()) {
                Pattern pattern = entry.getValue();
                Matcher matcher = pattern.matcher(strippedLine);
                if (matcher.find()) {
                    String skillName = matcher.group(1).trim();
                    if (!skillName.isEmpty()) {
                        skills.add(skillName);
                    }
                    break;
                }
            }
        }

        return skills;
    }
}
