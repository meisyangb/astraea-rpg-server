package cn.guangdian.rpgitems.migration;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MythicMobs 物品迁移工具
 * 将 MythicMobs 的物品配置转换为 RPGItems 格式
 */
public class MythicMobsMigrator {

    // 材质 ID 映射表 (旧版数字ID -> Material名称)
    private static final Map<Integer, String> MATERIAL_ID_MAP = new HashMap<>();

    static {
        // 武器
        MATERIAL_ID_MAP.put(267, "IRON_SWORD");
        MATERIAL_ID_MAP.put(268, "WOODEN_SWORD");
        MATERIAL_ID_MAP.put(272, "STONE_SWORD");
        MATERIAL_ID_MAP.put(276, "DIAMOND_SWORD");
        MATERIAL_ID_MAP.put(283, "GOLDEN_SWORD");

        // 防具
        MATERIAL_ID_MAP.put(298, "LEATHER_HELMET");
        MATERIAL_ID_MAP.put(299, "LEATHER_CHESTPLATE");
        MATERIAL_ID_MAP.put(300, "LEATHER_LEGGINGS");
        MATERIAL_ID_MAP.put(301, "LEATHER_BOOTS");
        MATERIAL_ID_MAP.put(302, "CHAINMAIL_HELMET");
        MATERIAL_ID_MAP.put(303, "CHAINMAIL_CHESTPLATE");
        MATERIAL_ID_MAP.put(304, "CHAINMAIL_LEGGINGS");
        MATERIAL_ID_MAP.put(305, "CHAINMAIL_BOOTS");
        MATERIAL_ID_MAP.put(306, "IRON_HELMET");
        MATERIAL_ID_MAP.put(307, "IRON_CHESTPLATE");
        MATERIAL_ID_MAP.put(308, "IRON_LEGGINGS");
        MATERIAL_ID_MAP.put(309, "IRON_BOOTS");
        MATERIAL_ID_MAP.put(310, "DIAMOND_HELMET");
        MATERIAL_ID_MAP.put(311, "DIAMOND_CHESTPLATE");
        MATERIAL_ID_MAP.put(312, "DIAMOND_LEGGINGS");
        MATERIAL_ID_MAP.put(313, "DIAMOND_BOOTS");
        MATERIAL_ID_MAP.put(314, "GOLDEN_HELMET");
        MATERIAL_ID_MAP.put(315, "GOLDEN_CHESTPLATE");
        MATERIAL_ID_MAP.put(316, "GOLDEN_LEGGINGS");
        MATERIAL_ID_MAP.put(317, "GOLDEN_BOOTS");

        // 材料
        MATERIAL_ID_MAP.put(331, "REDSTONE");
        MATERIAL_ID_MAP.put(388, "EMERALD");
        MATERIAL_ID_MAP.put(339, "BOOK");
        MATERIAL_ID_MAP.put(351, "INK_SAC");
        MATERIAL_ID_MAP.put(263, "CHARCOAL");
        MATERIAL_ID_MAP.put(265, "IRON_INGOT");
        MATERIAL_ID_MAP.put(266, "GOLD_INGOT");
        MATERIAL_ID_MAP.put(371, "GOLD_NUGGET");
        MATERIAL_ID_MAP.put(378, "BLAZE_ROD");
        MATERIAL_ID_MAP.put(399, "NETHER_STAR");
        MATERIAL_ID_MAP.put(405, "NETHER_BRICK");

        // 其他
        MATERIAL_ID_MAP.put(368, "ENDER_PEARL");
        MATERIAL_ID_MAP.put(381, "ENDER_EYE");
        MATERIAL_ID_MAP.put(395, "PLAYER_HEAD");
        MATERIAL_ID_MAP.put(262, "ARROW");
        MATERIAL_ID_MAP.put(322, "GOLDEN_APPLE");
        MATERIAL_ID_MAP.put(373, "POTION");
        MATERIAL_ID_MAP.put(374, "GLASS_BOTTLE");
        MATERIAL_ID_MAP.put(345, "COMPASS");
        MATERIAL_ID_MAP.put(346, "FISHING_ROD");
        MATERIAL_ID_MAP.put(280, "STICK");
        MATERIAL_ID_MAP.put(286, "GOLDEN_AXE");
        MATERIAL_ID_MAP.put(333, "OAK_BOAT");
        MATERIAL_ID_MAP.put(329, "SADDLE");
        MATERIAL_ID_MAP.put(358, "MAP");
        MATERIAL_ID_MAP.put(370, "NETHER_WART");
        MATERIAL_ID_MAP.put(377, "BLAZE_POWDER");
        MATERIAL_ID_MAP.put(384, "EXPERIENCE_BOTTLE");
        MATERIAL_ID_MAP.put(400, "NETHER_BRICK_ITEM");
        MATERIAL_ID_MAP.put(403, "ENCHANTED_BOOK");
        MATERIAL_ID_MAP.put(406, "QUARTZ");
        MATERIAL_ID_MAP.put(409, "GLOWSTONE_DUST");
        MATERIAL_ID_MAP.put(410, "MAGMA_CREAM");
        MATERIAL_ID_MAP.put(418, "DRAGON_BREATH");
        MATERIAL_ID_MAP.put(449, "END_CRYSTAL");
    }

    // 属性解析正则表达式
    private static final Pattern RANGE_PATTERN = Pattern.compile("([\\d.]+)-([\\d.]+)");
    private static final Pattern SINGLE_PATTERN = Pattern.compile("([\\d.]+)");
    private static final Pattern PERCENT_PATTERN = Pattern.compile("([\\d.]+)%");

    /**
     * 执行迁移
     */
    public static void migrate(String mythicMobsPath, String rpgItemsPath) {
        System.out.println("========================================");
        System.out.println("MythicMobs 物品迁移工具");
        System.out.println("========================================");

        Path sourcePath = Paths.get(mythicMobsPath);
        Path targetPath = Paths.get(rpgItemsPath);

        if (!Files.exists(sourcePath)) {
            System.err.println("源路径不存在: " + mythicMobsPath);
            return;
        }

        // 创建目标目录
        try {
            Files.createDirectories(targetPath);
        } catch (IOException e) {
            System.err.println("创建目标目录失败: " + e.getMessage());
            return;
        }

        int totalItems = 0;
        int totalFiles = 0;

        try {
            // 遍历所有 YAML 文件
            Files.walk(sourcePath)
                .filter(path -> path.toString().endsWith(".yml"))
                .forEach(ymlFile -> {
                    try {
                        int count = migrateFile(ymlFile, targetPath);
                        System.out.println("✓ 已转换: " + ymlFile.getFileName() + " (" + count + " 个物品)");
                    } catch (Exception e) {
                        System.err.println("✗ 转换失败: " + ymlFile.getFileName() + " - " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            System.err.println("遍历文件失败: " + e.getMessage());
        }

        System.out.println("========================================");
        System.out.println("迁移完成!");
        System.out.println("========================================");
    }

    /**
     * 转换单个文件
     */
    private static int migrateFile(Path sourceFile, Path targetPath) throws IOException {
        YamlConfiguration source = YamlConfiguration.loadConfiguration(sourceFile.toFile());

        // 创建目标配置
        StringBuilder output = new StringBuilder();
        output.append("# ========================================\n");
        output.append("# 从 MythicMobs 迁移的物品配置\n");
        output.append("# 源文件: ").append(sourceFile.getFileName()).append("\n");
        output.append("# ========================================\n\n");

        int count = 0;
        for (String itemId : source.getKeys(false)) {
            if (source.isConfigurationSection(itemId)) {
                String itemYaml = convertItem(itemId, source.getConfigurationSection(itemId));
                if (itemYaml != null) {
                    output.append(itemYaml).append("\n");
                    count++;
                }
            }
        }

        // 保存到目标文件
        Path targetFile = targetPath.resolve(sourceFile.getFileName());
        try (FileWriter writer = new FileWriter(targetFile.toFile())) {
            writer.write(output.toString());
        }

        return count;
    }

    /**
     * 转换单个物品
     */
    private static String convertItem(String itemId, org.bukkit.configuration.ConfigurationSection section) {
        StringBuilder yaml = new StringBuilder();

        // 物品 ID
        yaml.append(itemId).append(":\n");

        // 材质转换
        String material = convertMaterial(section);
        yaml.append("  Id: ").append(material).append("\n");

        // 显示名
        if (section.contains("Display")) {
            yaml.append("  Display: '").append(section.getString("Display")).append("'\n");
        }

        // Lore
        if (section.contains("Lore")) {
            List<String> lore = section.getStringList("Lore");
            yaml.append("  Lore:\n");
            for (String line : lore) {
                yaml.append("  - '").append(line).append("'\n");
            }
        }

        // Options
        if (section.contains("Options")) {
            yaml.append("  Options:\n");
            org.bukkit.configuration.ConfigurationSection options = section.getConfigurationSection("Options");
            if (options != null) {
                if (options.contains("Unbreakable")) {
                    yaml.append("    Unbreakable: ").append(options.getBoolean("Unbreakable")).append("\n");
                }
                if (options.contains("HideAttributes")) {
                    yaml.append("    HideAttributes: ").append(options.getBoolean("HideAttributes")).append("\n");
                }
                if (options.contains("CustomModelData")) {
                    yaml.append("    CustomModelData: ").append(options.getInt("CustomModelData")).append("\n");
                }
            }
        }

        // 解析 Lore 中的属性并添加到 Attributes
        Map<String, Object> attributes = parseAttributesFromLore(section);
        if (!attributes.isEmpty()) {
            yaml.append("  # RPGItems 属性 (从 Lore 解析)\n");
            yaml.append("  Attributes:\n");
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                yaml.append("    ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        return yaml.toString();
    }

    /**
     * 转换材质 ID
     */
    private static String convertMaterial(org.bukkit.configuration.ConfigurationSection section) {
        // 优先使用 Id 字段
        if (section.contains("Id")) {
            Object idObj = section.get("Id");

            // 如果是数字
            if (idObj instanceof Integer) {
                int id = (Integer) idObj;
                return MATERIAL_ID_MAP.getOrDefault(id, "PAPER");
            }

            // 如果是字符串
            if (idObj instanceof String) {
                String idStr = (String) idObj;
                // 尝试解析为数字
                try {
                    int id = Integer.parseInt(idStr);
                    return MATERIAL_ID_MAP.getOrDefault(id, "PAPER");
                } catch (NumberFormatException e) {
                    // 已经是 Material 名称
                    return idStr;
                }
            }
        }

        return "PAPER";
    }

    /**
     * 从 Lore 解析属性
     */
    private static Map<String, Object> parseAttributesFromLore(org.bukkit.configuration.ConfigurationSection section) {
        Map<String, Object> attributes = new LinkedHashMap<>();

        if (!section.contains("Lore")) {
            return attributes;
        }

        List<String> lore = section.getStringList("Lore");

        for (String line : lore) {
            // 去除颜色代码
            String cleanLine = stripColor(line);

            // 攻击力: 30-255
            if (cleanLine.contains("攻击力")) {
                parseRangeValue(cleanLine, "攻击力", attributes, "AttackMin", "AttackMax");
            }

            // 防御力: 1000-2000
            if (cleanLine.contains("防御力") && !cleanLine.contains("PVP")) {
                parseRangeValue(cleanLine, "防御力", attributes, "DefenseMin", "DefenseMax");
            }

            // 生命上限: +3000
            if (cleanLine.contains("生命上限")) {
                parseSingleValue(cleanLine, "生命上限", attributes, "MaxHealth");
            }

            // 暴击几率: 10%
            if (cleanLine.contains("暴击几率") || cleanLine.contains("暴击率")) {
                parsePercentValue(cleanLine, attributes, "CritChance");
            }

            // 暴击伤害: 100%
            if (cleanLine.contains("暴击伤害")) {
                parsePercentValue(cleanLine, attributes, "CritDamage");
            }

            // 吸血几率: 5%
            if (cleanLine.contains("吸血几率") || cleanLine.contains("生命吸取")) {
                parsePercentValue(cleanLine, attributes, "LifestealChance");
            }

            // 吸血倍率: 20%
            if (cleanLine.contains("吸血倍率")) {
                parsePercentValue(cleanLine, attributes, "LifestealMultiplier");
            }

            // 闪避: 2.5%
            if (cleanLine.contains("闪避")) {
                parsePercentValue(cleanLine, attributes, "DodgeChance");
            }

            // 招架: 5%
            if (cleanLine.contains("招架")) {
                parsePercentValue(cleanLine, attributes, "ParryChance");
            }

            // 移动速度: 10%
            if (cleanLine.contains("移动速度")) {
                parsePercentValue(cleanLine, attributes, "MoveSpeed");
            }

            // 护甲强度: 8%
            if (cleanLine.contains("护甲强度")) {
                parsePercentValue(cleanLine, attributes, "Armor");
            }

            // 每秒回血: 3
            if (cleanLine.contains("每秒回血")) {
                parseSingleValue(cleanLine, "每秒回血", attributes, "HealthRegen");
            }

            // 装备等级: 5
            if (cleanLine.contains("装备等级")) {
                parseLevelValue(cleanLine, attributes);
            }

            // 暴击抵抗: 3%
            if (cleanLine.contains("暴击抵抗")) {
                parsePercentValue(cleanLine, attributes, "CritResist");
            }

            // 暴伤抵抗: 60%
            if (cleanLine.contains("暴伤抵抗")) {
                parsePercentValue(cleanLine, attributes, "CritDamageResist");
            }

            // 反伤比例: 2%
            if (cleanLine.contains("反伤比例")) {
                parsePercentValue(cleanLine, attributes, "ReflectRatio");
            }

            // 吸血抵抗: 5%
            if (cleanLine.contains("吸血抵抗")) {
                parsePercentValue(cleanLine, attributes, "LifestealResist");
            }

            // 【PVP】攻击力: 7000-15000
            if (cleanLine.contains("PVP") && cleanLine.contains("攻击力")) {
                parseRangeValue(cleanLine, "攻击力", attributes, "PvpAttackMin", "PvpAttackMax");
            }

            // 【PVP】防御力: 7000
            if (cleanLine.contains("PVP") && cleanLine.contains("防御力")) {
                parseSingleValue(cleanLine, "防御力", attributes, "PvpDefenseMin");
            }
        }

        return attributes;
    }

    /**
     * 解析范围值 (如: 30-255)
     */
    private static void parseRangeValue(String line, String keyword, Map<String, Object> attributes,
                                       String minKey, String maxKey) {
        Matcher m = RANGE_PATTERN.matcher(line);
        if (m.find()) {
            attributes.put(minKey, Double.parseDouble(m.group(1)));
            attributes.put(maxKey, Double.parseDouble(m.group(2)));
        }
    }

    /**
     * 解析单个值 (如: +3000)
     */
    private static void parseSingleValue(String line, String keyword, Map<String, Object> attributes, String key) {
        Matcher m = SINGLE_PATTERN.matcher(line);
        if (m.find()) {
            attributes.put(key, Double.parseDouble(m.group(1)));
        }
    }

    /**
     * 解析百分比值 (如: 10%)
     */
    private static void parsePercentValue(String line, Map<String, Object> attributes, String key) {
        Matcher m = PERCENT_PATTERN.matcher(line);
        if (m.find()) {
            attributes.put(key, Double.parseDouble(m.group(1)));
        }
    }

    /**
     * 解析装备等级
     */
    private static void parseLevelValue(String line, Map<String, Object> attributes) {
        Matcher m = SINGLE_PATTERN.matcher(line);
        if (m.find()) {
            attributes.put("Level", Integer.parseInt(m.group(1)));
        }
    }

    /**
     * 去除颜色代码
     */
    private static String stripColor(String text) {
        // 去除 <color> 格式
        text = text.replaceAll("<[^>]+>", "");
        // 去除 §x 格式
        text = text.replaceAll("§[0-9a-fA-Fk-oK-orR]", "");
        // 去除 &x 格式
        text = text.replaceAll("&[0-9a-fA-Fk-oK-orR]", "");
        return text.trim();
    }

    /**
     * 主方法 - 直接执行迁移
     */
    public static void main(String[] args) {
        // MythicMobs 物品路径
        String mythicMobsPath = "e:\\RPG\\译梦传说\\plugins\\MythicMobs\\items";

        // RPGItems 目标路径
        String rpgItemsPath = "e:\\RPG\\原创RPG服务端-插件最多提交\\plugins\\RPGItems\\src\\main\\resources\\items";

        migrate(mythicMobsPath, rpgItemsPath);
    }
}
