package cn.guangdian.socket.parser;

import cn.guangdian.socket.model.AttributeValue;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 宝石镶嵌解析器
 * 适配 GuangDian Items 系统 - 支持基于装备类型的槽位匹配
 */
public class SocketParser {

    private static final String INLAY_SECTION_TITLE = "镶嵌属性";
    private static final String INLAY_SECTION_BORDER = "═══════════════";
    private static final String GEM_DATA_KEY = "inlaid_gems";

    private static List<Pattern> socketPatterns = new ArrayList<>();
    private static Map<String, String> gemTypes = new HashMap<>();
    private static Map<String, Map<String, AttributeValue>> gemAttributesCache = new HashMap<>();

    public static void initialize(ConfigurationSection socketSection, ConfigurationSection gemTypeSection) {
        // 初始化宝石孔位匹配模式
        if (socketSection != null) {
            for (String key : socketSection.getKeys(false)) {
                String pattern = socketSection.getString(key);
                if (pattern != null) {
                    socketPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
                }
            }
        }

        // 初始化宝石类型
        if (gemTypeSection != null) {
            for (String key : gemTypeSection.getKeys(false)) {
                gemTypes.put(key.toLowerCase(), gemTypeSection.getString(key, key));
            }
        }
    }

    /**
     * 初始化宝石属性缓存
     */
    public static void initializeGemAttributes(ConfigurationSection gemsSection) {
        if (gemsSection == null) return;
        
        for (String gemId : gemsSection.getKeys(false)) {
            ConfigurationSection gemSection = gemsSection.getConfigurationSection(gemId);
            if (gemSection == null) continue;
            
            Map<String, AttributeValue> attrs = new HashMap<>();
            ConfigurationSection attrsSection = gemSection.getConfigurationSection("attributes");
            if (attrsSection != null) {
                for (String attrName : attrsSection.getKeys(false)) {
                    String valueStr = attrsSection.getString(attrName);
                    if (valueStr != null) {
                        try {
                            if (valueStr.contains("-")) {
                                String[] parts = valueStr.split("-");
                                double min = Double.parseDouble(parts[0]);
                                double max = Double.parseDouble(parts[1]);
                                attrs.put(attrName, AttributeValue.range(min, max));
                            } else {
                                double value = Double.parseDouble(valueStr);
                                attrs.put(attrName, AttributeValue.of(value));
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            gemAttributesCache.put(gemId.toLowerCase(), attrs);
        }
    }

    /**
     * 根据宝石ID获取属性
     */
    public static Map<String, AttributeValue> parseGemAttributesById(String gemId) {
        if (gemId == null || gemId.isEmpty()) {
            return new HashMap<>();
        }
        return gemAttributesCache.getOrDefault(gemId.toLowerCase(), new HashMap<>());
    }

    /**
     * 解析装备的宝石孔位
     * 从装备 Lore 中解析实际定义的槽位
     */
    public static List<String> parseSocketGems(ItemStack item) {
        List<String> sockets = new ArrayList<>();
        if (item == null || !item.hasItemMeta()) return sockets;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return sockets;

        List<String> lore = meta.getLore();
        if (lore == null) return sockets;

        // 从 Lore 中解析槽位定义
        for (String line : lore) {
            String plain = stripColor(line);
            for (Pattern pattern : socketPatterns) {
                Matcher matcher = pattern.matcher(plain);
                if (matcher.find()) {
                    String socketType = matcher.group(1);
                    sockets.add(socketType != null ? socketType : "通用");
                    break;
                }
            }
        }

        return sockets;
    }

    /**
     * 判断物品是否为宝石
     */
    public static boolean isGem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;

        List<String> lore = meta.getLore();
        if (lore == null) return false;

        for (String line : lore) {
            String plain = stripColor(line);
            if (plain.contains("宝石") || gemTypes.values().stream().anyMatch(plain::contains)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取宝石类型
     */
    public static String getGemType(ItemStack item) {
        if (!isGem(item)) return "未知";
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return "未知";

        List<String> lore = meta.getLore();
        if (lore == null) return "未知";

        for (String line : lore) {
            String plain = stripColor(line);
            for (Map.Entry<String, String> entry : gemTypes.entrySet()) {
                if (plain.contains(entry.getValue())) {
                    return entry.getValue();
                }
            }
        }
        return "通用";
    }

    /**
     * 检查宝石是否兼容孔位
     */
    public static boolean isGemCompatible(String socketType, String gemType) {
        if (socketType == null || gemType == null) return false;
        // 通用孔位兼容所有宝石
        if (socketType.equals("通用") || socketType.equalsIgnoreCase("any")) return true;
        // 精确匹配
        return socketType.equalsIgnoreCase(gemType) || gemType.contains(socketType) || socketType.contains(gemType);
    }

    /**
     * 解析宝石属性
     */
    public static Map<String, AttributeValue> parseGemAttributes(ItemStack gem) {
        Map<String, AttributeValue> attrs = new HashMap<>();
        if (gem == null || !gem.hasItemMeta()) return attrs;

        ItemMeta meta = gem.getItemMeta();
        if (meta == null || !meta.hasLore()) return attrs;

        List<String> lore = meta.getLore();
        if (lore == null) return attrs;

        Pattern attrPattern = Pattern.compile("([\\u4e00-\\u9fa5]+)\\s*[:：]\\s*\\+?([\\d.]+)(?:-([\\d.]+))?");

        for (String line : lore) {
            String plain = stripColor(line);
            Matcher matcher = attrPattern.matcher(plain);
            if (matcher.find()) {
                String attrName = matcher.group(1);
                try {
                    if (matcher.group(3) != null) {
                        double min = Double.parseDouble(matcher.group(2));
                        double max = Double.parseDouble(matcher.group(3));
                        attrs.put(attrName, AttributeValue.range(min, max));
                    } else {
                        double value = Double.parseDouble(matcher.group(2));
                        attrs.put(attrName, AttributeValue.of(value));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return attrs;
    }

    /**
     * 应用镶嵌到装备
     * 将"可镶嵌"槽位替换为"已镶嵌"状态并显示属性
     * 只修改镶嵌行，保持其他 lore 完全不变
     */
    public static void applyInlay(ItemStack item, List<ItemStack> gems, Map<String, AttributeValue> attributes) {
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // 获取现有的 Component Lore
        List<Component> existingLore = meta.lore();
        if (existingLore == null) {
            existingLore = new ArrayList<>();
        }

        // 直接操作 Component 列表，只修改需要改变的行
        MiniMessage miniMessage = MiniMessage.miniMessage();
        List<Component> newLore = new ArrayList<>();
        int gemIndex = 0;

        for (Component lineComponent : existingLore) {
            // 将 Component 序列化为字符串用于匹配
            String line = miniMessage.serialize(lineComponent);
            String plain = stripColor(line);
            boolean matched = false;

            // 检查是否匹配"可镶嵌"槽位格式
            for (Pattern pattern : socketPatterns) {
                Matcher matcher = pattern.matcher(plain);
                if (matcher.find()) {
                    String gemType = matcher.group(1);
                    if (gemType != null && gemIndex < gems.size()) {
                        ItemStack gem = gems.get(gemIndex);
                        String gemName = getGemDisplayName(gem);

                        // 构建新的已镶嵌行 Component
                        String inlayLine = buildInlaidLine(gemName, attributes);
                        newLore.add(miniMessage.deserialize(inlayLine));

                        gemIndex++;
                        matched = true;
                        break;
                    }
                }
            }

            if (!matched) {
                // 保持原行完全不变
                newLore.add(lineComponent);
            }
        }

        meta.lore(newLore);

        // 保存宝石数据到 PDC
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String gemData = serializeGems(gems);
        pdc.set(new NamespacedKey(JavaPlugin.getProvidingPlugin(SocketParser.class), GEM_DATA_KEY), PersistentDataType.STRING, gemData);

        item.setItemMeta(meta);
    }

    /**
     * 获取已存储的镶嵌宝石
     */
    public static List<ItemStack> getStoredInlaidGems(ItemStack item) {
        List<ItemStack> gems = new ArrayList<>();
        if (item == null || !item.hasItemMeta()) return gems;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return gems;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String gemData = pdc.get(new NamespacedKey(JavaPlugin.getProvidingPlugin(SocketParser.class), GEM_DATA_KEY), PersistentDataType.STRING);
        if (gemData != null) {
            // 反序列化宝石数据
            // 简化实现：实际应该存储宝石ID和数量
        }
        return gems;
    }

    /**
     * 清除镶嵌
     * 将"已镶嵌"槽位恢复为"可镶嵌"状态
     * 只修改镶嵌行，保持其他 lore 完全不变
     */
    public static ItemStack clearInlay(ItemStack item) {
        if (item == null) return null;
        ItemStack result = item.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return result;

        // 清除 PDC
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.remove(new NamespacedKey(JavaPlugin.getProvidingPlugin(SocketParser.class), GEM_DATA_KEY));

        // 恢复 Lore 中的槽位为"可镶嵌"状态
        List<Component> existingLore = meta.lore();
        if (existingLore != null) {
            MiniMessage miniMessage = MiniMessage.miniMessage();
            List<Component> newLore = new ArrayList<>();

            for (Component lineComponent : existingLore) {
                // 将 Component 序列化为字符串用于匹配
                String line = miniMessage.serialize(lineComponent);
                String plain = stripColor(line);
                boolean isInlaid = false;

                // 检查是否匹配"已镶嵌"槽位格式
                for (Pattern pattern : socketPatterns) {
                    Matcher matcher = pattern.matcher(plain);
                    if (matcher.find()) {
                        String gemType = matcher.group(1);
                        if (gemType != null && plain.contains("已镶嵌")) {
                            // 恢复为"可镶嵌"状态
                            String restoredLine = restoreSocketLine(line, gemType);
                            newLore.add(miniMessage.deserialize(restoredLine));
                            isInlaid = true;
                            break;
                        }
                    }
                }

                if (!isInlaid) {
                    // 保持原行完全不变
                    newLore.add(lineComponent);
                }
            }

            meta.lore(newLore);
        }

        result.setItemMeta(meta);
        return result;
    }

    /**
     * 恢复槽位为"可镶嵌"状态
     * 格式: [已镶嵌红宝石] 属性+数值 -> [可镶嵌红宝石]
     */
    private static List<String> restoreSocketSlots(List<String> lore) {
        List<String> result = new ArrayList<>();

        for (String line : lore) {
            String plain = stripColor(line);

            // 检查是否匹配"已镶嵌"槽位格式
            boolean isInlaid = false;
            for (Pattern pattern : socketPatterns) {
                Matcher matcher = pattern.matcher(plain);
                if (matcher.find()) {
                    String gemType = matcher.group(1);
                    if (gemType != null && plain.contains("已镶嵌")) {
                        // 恢复为"可镶嵌"状态
                        String restoredLine = restoreSocketLine(line, gemType);
                        result.add(restoredLine);
                        isInlaid = true;
                        break;
                    }
                }
            }

            if (!isInlaid) {
                result.add(line);
            }
        }

        return result;
    }

    /**
     * 恢复槽位行
     * 将"已镶嵌"替换为"可镶嵌"并移除属性
     * 返回带 MiniMessage 格式的字符串
     */
    private static String restoreSocketLine(String originalLine, String gemType) {
        // 移除颜色代码获取纯文本
        String plain = stripColor(originalLine);

        // 替换"已镶嵌"为"可镶嵌"
        String newPlain = plain.replace("已镶嵌", "可镶嵌");

        // 移除属性部分（从第一个空格后开始）
        int spaceIndex = newPlain.indexOf(' ', newPlain.indexOf(']'));
        if (spaceIndex > 0) {
            newPlain = newPlain.substring(0, spaceIndex);
        }

        // 添加 MiniMessage 颜色格式 - 与原装备槽位格式一致
        return "<dark_aqua>[<gray>" + newPlain.substring(1, newPlain.length() - 1) + "<dark_aqua>]";
    }

    /**
     * 将"可镶嵌"槽位替换为"已镶嵌"状态
     * 格式: [可镶嵌红宝石] -> [已镶嵌 幻域杀戮宝石] 属性+数值
     */
    public static List<String> replaceSocketWithInlay(List<String> lore, List<ItemStack> gems, Map<String, AttributeValue> attributes) {
        if (lore == null) return new ArrayList<>();

        List<String> result = new ArrayList<>();
        int gemIndex = 0;

        for (String line : lore) {
            String plain = stripColor(line);
            boolean matched = false;

            // 检查是否匹配"可镶嵌"槽位格式
            for (Pattern pattern : socketPatterns) {
                Matcher matcher = pattern.matcher(plain);
                if (matcher.find()) {
                    String gemType = matcher.group(1);
                    if (gemType != null) {
                        // 找到对应的宝石和属性
                        if (gemIndex < gems.size()) {
                            ItemStack gem = gems.get(gemIndex);
                            String gemName = getGemDisplayName(gem);

                            // 构建已镶嵌行 - 显示宝石名字和属性
                            String inlayLine = buildInlaidLine(gemName, attributes);
                            result.add(inlayLine);

                            gemIndex++;
                            matched = true;
                            break;
                        }
                    }
                }
            }

            if (!matched) {
                result.add(line);
            }
        }

        return result;
    }

    /**
     * 获取宝石的显示名称
     */
    private static String getGemDisplayName(ItemStack gem) {
        if (gem == null || !gem.hasItemMeta()) return "未知宝石";
        ItemMeta meta = gem.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            // 如果没有显示名称，返回物品类型名称
            return gem.getType().name().replace("_", " ").toLowerCase();
        }
        // 移除颜色代码返回纯名称
        return stripColor(meta.getDisplayName());
    }

    /**
     * 构建已镶嵌的 Lore 行
     * 格式: <dark_aqua>[<green>已镶嵌<gray> <red>宝石名<dark_aqua>] <yellow>属性名<gray>: <aqua>+数值
     */
    private static String buildInlaidLine(String gemName, Map<String, AttributeValue> attributes) {
        // 构建已镶嵌行 - 使用 MiniMessage 彩色格式
        StringBuilder sb = new StringBuilder();
        sb.append("<dark_aqua>[<green>已镶嵌<gray> <red>").append(gemName).append("<dark_aqua>]");

        // 如果有属性，追加到行尾
        if (attributes != null && !attributes.isEmpty()) {
            // 找到该宝石的属性（简化：使用第一个属性）
            for (Map.Entry<String, AttributeValue> entry : attributes.entrySet()) {
                String attrName = entry.getKey();
                AttributeValue attrValue = entry.getValue();
                sb.append(" <yellow>").append(attrName).append("<gray>: <aqua>").append(formatAttributeValue(attrValue));
                break; // 只显示第一个属性，避免过长
            }
        }

        return sb.toString();
    }

    /**
     * 格式化属性值
     */
    private static String formatAttributeValue(AttributeValue value) {
        if (value == null) return "";
        if (value instanceof AttributeValue.RangeValue range) {
            return "+" + String.format("%.0f", range.getMin()) + "-" + String.format("%.0f", range.getMax());
        }
        return "+" + String.format("%.0f", value.getValue());
    }

    /**
     * 移除 Lore 中的镶嵌区域
     */
    private static List<String> removeInlaySection(List<String> lore) {
        List<String> result = new ArrayList<>();
        boolean inInlaySection = false;

        for (String line : lore) {
            String plain = stripColor(line);
            if (plain.contains(INLAY_SECTION_TITLE)) {
                inInlaySection = true;
                continue;
            }
            if (plain.contains(INLAY_SECTION_BORDER)) {
                inInlaySection = false;
                continue;
            }
            if (!inInlaySection) {
                result.add(line);
            }
        }
        return result;
    }

    /**
     * 格式化镶嵌属性行
     */
    public static String formatInlayLine(String attributeName, AttributeValue value) {
        String color = getAttributeColor(attributeName);
        return color + attributeName + ": <aqua>+" + formatAttributeValue(attributeName, value);
    }

    private static String getAttributeColor(String attrName) {
        return switch (attrName) {
            case "攻击力" -> "<red>";
            case "生命上限" -> "<green>";
            case "防御力" -> "<yellow>";
            case "闪避" -> "<dark_purple>";
            case "暴击几率", "暴击伤害" -> "<aqua>";
            default -> "<white>";
        };
    }

    private static String formatAttributeValue(String attrName, AttributeValue value) {
        if (value instanceof AttributeValue.RangeValue range) {
            return formatNumber(range.getMin()) + "-" + formatNumber(range.getMax());
        }
        String result = formatNumber(value.getValue());
        if (isPercentAttribute(attrName)) {
            result += "%";
        }
        return result;
    }

    private static String formatNumber(double num) {
        if (num == (long) num) {
            return String.valueOf((long) num);
        }
        return String.valueOf(num);
    }

    private static boolean isPercentAttribute(String attrName) {
        return attrName.contains("几率") || attrName.contains("概率") || attrName.contains("率");
    }

    /**
     * 移除颜色代码，保留纯文本
     * 支持传统颜色字符（& 和 §）和 MiniMessage 标签
     */
    private static String stripColor(String text) {
        if (text == null) return "";
        
        String result = text;
        
        // 1. 移除传统 § 颜色代码
        result = result.replaceAll("§[0-9a-fk-or]", "");
        
        // 2. 移除传统 & 颜色代码
        result = result.replaceAll("&[0-9a-fk-or]", "");
        
        // 3. 移除 MiniMessage 颜色标签（但保留其他标签内容）
        // 颜色标签列表
        String[] colorTags = {
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
            "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white",
            "reset", "bold", "italic", "underlined", "strikethrough", "obfuscated"
        };
        for (String tag : colorTags) {
            result = result.replaceAll("</?" + tag + ">", "");
        }
        
        // 4. 移除十六进制颜色 <#RRGGBB>
        result = result.replaceAll("<#[0-9a-fA-F]{6}>", "");
        
        return result;
    }

    private static String serializeGems(List<ItemStack> gems) {
        // 简化实现
        StringBuilder sb = new StringBuilder();
        for (ItemStack gem : gems) {
            if (sb.length() > 0) sb.append(";");
            sb.append(gem.getType().name()).append(":").append(gem.getAmount());
        }
        return sb.toString();
    }
}
