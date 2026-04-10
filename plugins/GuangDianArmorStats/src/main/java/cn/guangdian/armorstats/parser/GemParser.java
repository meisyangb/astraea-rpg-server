package cn.guangdian.armorstats.parser;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.data.AttributeValue;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GemParser {

    private static final Pattern FALLBACK_SOCKET_PATTERN = Pattern.compile("可镶嵌<([^>]+)>");
    // 已镶嵌的槽位模式
    private static final Pattern INLAID_SOCKET_PATTERN = Pattern.compile("已镶嵌<([^>]+)>");
    private static final Set<String> GEM_TYPES = new LinkedHashSet<>();
    private static final String INLAY_SECTION_TITLE = "镶嵌属性";
    private static final String INLAY_SECTION_BORDER = "================";
    private static final String INLAY_META_VERSION = "v1";
    private static final String INLAY_DATA_PREFIX = "gems:";
    private static Pattern GEM_SOCKET_PATTERN = FALLBACK_SOCKET_PATTERN;

    public static void initialize(Map<String, String> socketPattern, Map<String, String> gemPatterns) {
        if (socketPattern != null && !socketPattern.isEmpty()) {
            String pattern = socketPattern.values().iterator().next();
            GEM_SOCKET_PATTERN = compileNormalizedPattern(pattern, FALLBACK_SOCKET_PATTERN);
        } else {
            GEM_SOCKET_PATTERN = FALLBACK_SOCKET_PATTERN;
        }

        GEM_TYPES.clear();
        if (gemPatterns != null) {
            GEM_TYPES.addAll(gemPatterns.keySet());
        }
    }

    private static Pattern compileNormalizedPattern(String pattern, Pattern fallback) {
        if (pattern == null || pattern.isBlank()) {
            return fallback;
        }
        try {
            String translated = ChatColor.translateAlternateColorCodes('&', pattern);
            String normalized = stripColor(translated);
            return Pattern.compile(normalized);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String stripColor(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[&§][0-9a-fk-or]", "");
    }

    private static String normalizeText(String text) {
        return stripColor(text).replace('：', ':').trim();
    }

    public static boolean isGem(ItemStack item) {
        return getGemType(item) != null && !LoreParser.parse(item).isEmpty();
    }

    public static String getGemType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        List<String> candidates = new ArrayList<>();
        if (meta.hasDisplayName()) {
            candidates.add(normalizeText(meta.getDisplayName()));
        }
        if (meta.hasLore() && meta.getLore() != null) {
            for (String line : meta.getLore()) {
                candidates.add(normalizeText(line));
            }
        }

        for (String candidate : candidates) {
            for (String gemType : GEM_TYPES) {
                if (candidate.contains(gemType)) {
                    return gemType;
                }
            }
        }

        for (String candidate : candidates) {
            if (candidate.endsWith("宝石")) {
                return candidate;
            }
        }

        return null;
    }

    public static boolean isGemCompatible(String requiredType, ItemStack gemItem) {
        String gemType = getGemType(gemItem);
        if (gemType == null) {
            return false;
        }
        return isGemCompatible(requiredType, gemType);
    }

    public static boolean isGemCompatible(String requiredType, String gemType) {
        String normalizedRequired = normalizeText(requiredType);
        String normalizedGemType = normalizeText(gemType);
        if (normalizedRequired.isEmpty()) {
            return true;
        }
        if (normalizedRequired.contains("任意") || normalizedRequired.contains("通用")) {
            return true;
        }
        return normalizedRequired.equals(normalizedGemType)
                || normalizedRequired.contains(normalizedGemType)
                || normalizedGemType.contains(normalizedRequired);
    }

    public static List<String> parseSocketGems(ItemStack item) {
        List<String> gems = new ArrayList<>();

        if (item == null || !item.hasItemMeta()) {
            return gems;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return gems;
        }

        List<String> lore = meta.getLore();
        if (lore == null) {
            return gems;
        }

        for (String line : lore) {
            Matcher matcher = GEM_SOCKET_PATTERN.matcher(normalizeText(line));
            while (matcher.find()) {
                String gemType = matcher.group(1);
                if (gemType != null) {
                    gems.add(normalizeText(gemType));
                }
            }
        }

        return gems;
    }

    public static Map<String, AttributeValue> parseGemAttributes(ItemStack gemItem) {
        Map<String, AttributeValue> attributes = new HashMap<>();

        if (!isGem(gemItem)) {
            return attributes;
        }

        attributes.putAll(LoreParser.parse(gemItem));
        return attributes;
    }

    public static int countSockets(ItemStack item) {
        return parseSocketGems(item).size();
    }

    public static Map<String, AttributeValue> parseSocketGemsFromLore(ItemStack item) {
        Map<String, AttributeValue> attributes = new HashMap<>();

        for (ItemStack gem : getStoredInlaidGems(item)) {
            Map<String, AttributeValue> gemAttributes = parseGemAttributes(gem);
            for (Map.Entry<String, AttributeValue> entry : gemAttributes.entrySet()) {
                attributes.merge(entry.getKey(), entry.getValue(), AttributeValue::merge);
            }
        }
        if (!attributes.isEmpty()) {
            return attributes;
        }

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

        boolean inSocketSection = false;
        for (String line : lore) {
            String plainLine = normalizeText(line);

            if (plainLine.contains(INLAY_SECTION_TITLE)) {
                inSocketSection = true;
                continue;
            }

            if (inSocketSection && plainLine.contains(INLAY_SECTION_BORDER)) {
                inSocketSection = false;
                continue;
            }

            if (inSocketSection) {
                // 跳过包含"已镶嵌"或"可镶嵌"的行，这些行的属性已通过getStoredInlaidGems获取
                if (plainLine.contains("已镶嵌") || plainLine.contains("可镶嵌")) {
                    continue;
                }
                mergeParsedAttribute(attributes, plainLine);
            }
        }

        return attributes;
    }

    public static List<ItemStack> getStoredInlaidGems(ItemStack item) {
        List<ItemStack> gems = new ArrayList<>();
        if (item == null || !item.hasItemMeta()) {
            return gems;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return gems;
        }

        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        NamespacedKey key = getInlayDataKey();
        if (key == null || !dataContainer.has(key, PersistentDataType.STRING)) {
            return gems;
        }

        String encoded = dataContainer.get(key, PersistentDataType.STRING);
        if (encoded == null || encoded.isBlank()) {
            return gems;
        }

        String payload = encoded;
        if (payload.startsWith(INLAY_DATA_PREFIX)) {
            payload = payload.substring(INLAY_DATA_PREFIX.length());
        }
        int versionSeparator = payload.indexOf('|');
        if (versionSeparator > 0) {
            String version = payload.substring(0, versionSeparator);
            if (!INLAY_META_VERSION.equals(version)) {
                return gems;
            }
            payload = payload.substring(versionSeparator + 1);
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(payload);
            try (BukkitObjectInputStream input = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                int count = input.readInt();
                for (int i = 0; i < count; i++) {
                    Object object = input.readObject();
                    if (object instanceof ItemStack stack && stack.getType().isItem()) {
                        gems.add(stack);
                    }
                }
            }
        } catch (Exception e) {
            // 解码失败时返回空列表，记录调试日志
            Bukkit.getLogger().fine("[GuangDianArmorStats] 解码宝石数据失败: " + e.getMessage());
        }

        return gems;
    }

    public static ItemStack applyInlay(ItemStack equipment, List<ItemStack> gems, Map<String, AttributeValue> attributes) {
        if (equipment == null || !equipment.hasItemMeta()) {
            return equipment;
        }

        ItemMeta meta = equipment.getItemMeta();
        if (meta == null) {
            return equipment;
        }

        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();

        // 清除旧的镶嵌属性区域
        lore = stripInlaySection(lore);
        
        // 更新槽位显示：将已镶嵌的槽位从"可镶嵌"改为"已镶嵌"
        lore = updateSocketDisplay(lore, gems);
        
        meta.setLore(lore);

        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        NamespacedKey key = getInlayDataKey();
        if (key != null) {
            String encoded = encodeGems(gems);
            if (encoded == null || encoded.isBlank() || gems == null || gems.isEmpty()) {
                dataContainer.remove(key);
            } else {
                dataContainer.set(key, PersistentDataType.STRING, INLAY_DATA_PREFIX + INLAY_META_VERSION + "|" + encoded);
            }
        }

        equipment.setItemMeta(meta);
        return equipment;
    }

    /**
     * 更新槽位显示：将已镶嵌的槽位从"可镶嵌"改为"已镶嵌"
     * 格式：§4*§3[§7已镶嵌<宝石名>§3] 属性
     */
    private static List<String> updateSocketDisplay(List<String> lore, List<ItemStack> gems) {
        if (lore == null) {
            return new ArrayList<>();
        }
        if (gems == null || gems.isEmpty()) {
            return new ArrayList<>(lore);
        }

        List<String> result = new ArrayList<>();
        int gemIndex = 0;

        for (String line : lore) {
            String plainLine = normalizeText(line);

            // 检查是否是"可镶嵌"或"已镶嵌"的槽位行
            Matcher socketMatcher = GEM_SOCKET_PATTERN.matcher(plainLine);
            Matcher inlaidMatcher = INLAID_SOCKET_PATTERN.matcher(plainLine);
            boolean isSocketLine = socketMatcher.find();
            boolean isInlaidLine = inlaidMatcher.find();

            if ((isSocketLine || isInlaidLine) && gemIndex < gems.size()) {
                // 这是一个槽位行，且还有宝石需要显示
                ItemStack gem = gems.get(gemIndex);
                if (gem != null && gem.hasItemMeta()) {
                    ItemMeta gemMeta = gem.getItemMeta();
                    String gemName = gemMeta.hasDisplayName()
                            ? gemMeta.getDisplayName()
                            : gem.getType().name();

                    // 去掉颜色代码获取纯名
                    String plainGemName = stripColor(gemName);

                    // 获取宝石属性
                    Map<String, AttributeValue> gemAttrs = LoreParser.parse(gem);
                    String attrStr = formatGemAttributes(gemAttrs);

                    // 构建新行：§4*§3[§7已镶嵌<宝石名>§3] 属性
                    // 使用 ChatColor 转换颜色
                    String newLine = ChatColor.DARK_RED + "*" + ChatColor.DARK_AQUA + "[" +
                            ChatColor.GRAY + "已镶嵌<" + plainGemName + ">" + ChatColor.DARK_AQUA + "] " + attrStr;
                    result.add(newLine);
                    gemIndex++;
                    continue;
                }
            }
            result.add(line);
        }

        return result;
    }

    /**
     * 格式化宝石属性为简短字符串（使用 ChatColor）
     */
    private static String formatGemAttributes(Map<String, AttributeValue> attrs) {
        if (attrs == null || attrs.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, AttributeValue> entry : attrs.entrySet()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(ChatColor.AQUA).append(entry.getKey());
            sb.append(": ").append(ChatColor.GREEN).append("+");

            AttributeValue value = entry.getValue();
            if (value instanceof AttributeValue.RangeValue rangeValue) {
                sb.append(formatNumber(rangeValue.getMin()))
                  .append("-")
                  .append(formatNumber(rangeValue.getMax()));
            } else {
                sb.append(formatNumber(value.getValue()));
                if (isPercentAttribute(entry.getKey())) {
                    sb.append("%");
                }
            }
        }
        return sb.toString();
    }

    public static ItemStack clearInlay(ItemStack equipment) {
        return applyInlay(equipment, Collections.emptyList(), Collections.emptyMap());
    }

    public static List<String> stripInlaySection(List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        boolean inSocketSection = false;
        for (String line : lore) {
            String plainLine = normalizeText(line);
            if (plainLine.contains(INLAY_SECTION_TITLE)) {
                inSocketSection = true;
                continue;
            }
            if (inSocketSection && plainLine.contains(INLAY_SECTION_BORDER)) {
                inSocketSection = false;
                continue;
            }
            if (!inSocketSection) {
                result.add(line);
            }
        }
        return result;
    }

    public static List<String> appendInlaySection(List<String> lore, Map<String, AttributeValue> attributes) {
        List<String> result = stripInlaySection(lore);
        if (attributes == null || attributes.isEmpty()) {
            return result;
        }

        result.add(ChatColor.GOLD + "==== " + INLAY_SECTION_TITLE + " ====");
        for (Map.Entry<String, AttributeValue> entry : orderAttributes(attributes).entrySet()) {
            result.add(formatInlayLine(entry.getKey(), entry.getValue()));
        }
        result.add(ChatColor.GOLD + INLAY_SECTION_BORDER);
        return result;
    }

    private static Map<String, AttributeValue> orderAttributes(Map<String, AttributeValue> attributes) {
        List<String> order = Arrays.asList(
                "攻击力", "防御力", "生命上限", "闪避", "暴击几率", "暴击伤害",
                "每秒回血", "PVP攻击力", "PVP防御力"
        );
        Map<String, AttributeValue> ordered = new LinkedHashMap<>();
        for (String key : order) {
            if (attributes.containsKey(key)) {
                ordered.put(key, attributes.get(key));
            }
        }
        attributes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> ordered.putIfAbsent(entry.getKey(), entry.getValue()));
        return ordered;
    }

    public static String formatInlayLine(String attributeName, AttributeValue value) {
        String color = getAttributeColor(attributeName);
        return color + attributeName + ": " + ChatColor.AQUA + "+" + formatAttributeValue(attributeName, value);
    }

    public static String formatAttributeValue(String attributeName, AttributeValue value) {
        if (value instanceof AttributeValue.RangeValue rangeValue) {
            return formatNumber(rangeValue.getMin()) + "-" + formatNumber(rangeValue.getMax());
        }
        return formatNumber(value.getValue()) + (isPercentAttribute(attributeName) ? "%" : "");
    }

    private static boolean isPercentAttribute(String attributeName) {
        return switch (attributeName) {
            case "闪避", "暴击几率", "暴击伤害" -> true;
            default -> false;
        };
    }

    private static String formatNumber(double value) {
        if (Math.floor(value) == value) {
            return String.valueOf((int) value);
        }
        return String.format(Locale.US, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String getAttributeColor(String attrName) {
        return switch (attrName) {
            case "攻击力" -> ChatColor.RED.toString();
            case "生命上限" -> ChatColor.GREEN.toString();
            case "防御力" -> ChatColor.YELLOW.toString();
            case "闪避" -> ChatColor.DARK_PURPLE.toString();
            case "暴击几率", "暴击伤害" -> ChatColor.AQUA.toString();
            default -> ChatColor.WHITE.toString();
        };
    }

    private static void mergeParsedAttribute(Map<String, AttributeValue> attributes, String plainLine) {
        if (!plainLine.contains(":")) {
            return;
        }

        String[] parts = plainLine.split(":", 2);
        if (parts.length < 2) {
            return;
        }

        String attrName = normalizeAttributeName(parts[0]);
        String rawValue = parts[1].trim().replace("+", "");
        if (attrName == null || rawValue.isEmpty()) {
            return;
        }

        boolean percent = rawValue.endsWith("%");
        String numericValue = rawValue.replace("%", "").trim();

        try {
            AttributeValue parsedValue;
            if (numericValue.contains("-")) {
                String[] rangeParts = numericValue.split("-", 2);
                double min = Double.parseDouble(rangeParts[0].trim());
                double max = Double.parseDouble(rangeParts[1].trim());
                parsedValue = AttributeValue.ofRange(min, max);
            } else {
                double value = Double.parseDouble(numericValue);
                parsedValue = percent ? AttributeValue.ofPercent(value) : AttributeValue.of(value);
            }
            attributes.merge(attrName, parsedValue, AttributeValue::merge);
        } catch (NumberFormatException ignored) {
        }
    }

    private static String normalizeAttributeName(String attrName) {
        String normalized = normalizeText(attrName);
        if (normalized.contains("PVP") && normalized.contains("攻击力")) {
            return "PVP攻击力";
        }
        if (normalized.contains("PVP") && normalized.contains("防御力")) {
            return "PVP防御力";
        }
        if (normalized.contains("攻击力")) {
            return "攻击力";
        }
        if (normalized.contains("防御力")) {
            return "防御力";
        }
        if (normalized.contains("生命上限")) {
            return "生命上限";
        }
        if (normalized.contains("闪避")) {
            return "闪避";
        }
        if (normalized.contains("暴击几率")) {
            return "暴击几率";
        }
        if (normalized.contains("暴击伤害") || normalized.contains("暴伤")) {
            return "暴击伤害";
        }
        if (normalized.contains("每秒回血")) {
            return "每秒回血";
        }
        return null;
    }

    private static String encodeGems(List<ItemStack> gems) {
        if (gems == null || gems.isEmpty()) {
            return "";
        }

        List<ItemStack> filtered = new ArrayList<>();
        for (ItemStack gem : gems) {
            if (gem != null && gem.getType().isItem()) {
                ItemStack single = gem.clone();
                single.setAmount(1);
                filtered.add(single);
            }
        }
        if (filtered.isEmpty()) {
            return "";
        }

        try (ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
             BukkitObjectOutputStream output = new BukkitObjectOutputStream(byteOutput)) {
            output.writeInt(filtered.size());
            for (ItemStack gem : filtered) {
                output.writeObject(gem);
            }
            output.flush();
            return Base64.getEncoder().encodeToString(byteOutput.toByteArray());
        } catch (Exception ignored) {
            return "";
        }
    }

    private static NamespacedKey getInlayDataKey() {
        GuangDianArmorStats plugin = GuangDianArmorStats.getInstance();
        if (plugin == null) {
            return null;
        }
        return new NamespacedKey(plugin, "inlaid_gems");
    }
}
