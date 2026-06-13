import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * MythicMobs 物品迁移工具 - 独立版本
 * 直接运行此脚本执行转换
 */
public class MigrateItems {

    // 材质 ID 映射表
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

    private static final Pattern RANGE_PATTERN = Pattern.compile("([\\d.]+)-([\\d.]+)");
    private static final Pattern SINGLE_PATTERN = Pattern.compile("([\\d.]+)");
    private static final Pattern PERCENT_PATTERN = Pattern.compile("([\\d.]+)%");

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("MythicMobs 物品迁移工具");
        System.out.println("========================================");

        String sourcePath = "e:\\RPG\\译梦传说\\plugins\\MythicMobs\\items";
        String targetPath = "e:\\RPG\\原创RPG服务端-插件最多提交\\plugins\\RPGItems\\src\\main\\resources\\items";

        migrate(sourcePath, targetPath);
    }

    public static void migrate(String sourcePath, String targetPath) {
        Path source = Paths.get(sourcePath);
        Path target = Paths.get(targetPath);

        if (!Files.exists(source)) {
            System.err.println("源路径不存在: " + sourcePath);
            return;
        }

        try {
            Files.createDirectories(target);
        } catch (IOException e) {
            System.err.println("创建目标目录失败: " + e.getMessage());
            return;
        }

        int[] totalItems = {0};
        int[] totalFiles = {0};

        try {
            Files.walk(source)
                .filter(path -> path.toString().endsWith(".yml"))
                .forEach(ymlFile -> {
                    try {
                        // 计算相对路径，保持文件夹结构
                        Path relativePath = source.relativize(ymlFile);
                        Path targetFile = target.resolve(relativePath);

                        // 创建目标文件的父目录
                        Files.createDirectories(targetFile.getParent());

                        int count = migrateFile(ymlFile, targetFile);
                        totalItems[0] += count;
                        totalFiles[0]++;
                        System.out.println("✓ 已转换: " + relativePath + " (" + count + " 个物品)");
                    } catch (Exception e) {
                        System.err.println("✗ 转换失败: " + ymlFile.getFileName() + " - " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            System.err.println("遍历文件失败: " + e.getMessage());
        }

        System.out.println("========================================");
        System.out.println("迁移完成!");
        System.out.println("转换文件数: " + totalFiles[0]);
        System.out.println("转换物品数: " + totalItems[0]);
        System.out.println("目标路径: " + targetPath);
        System.out.println("========================================");
    }

    private static int migrateFile(Path sourceFile, Path targetFile) throws IOException {
        List<String> lines = Files.readAllLines(sourceFile);
        StringBuilder output = new StringBuilder();

        output.append("# ========================================\n");
        output.append("# 从 MythicMobs 迁移的物品配置\n");
        output.append("# 源文件: ").append(sourceFile.getFileName()).append("\n");
        output.append("# ========================================\n\n");

        int count = 0;
        String currentItem = null;
        List<String> itemLines = new ArrayList<>();

        for (String line : lines) {
            // 检测物品开始 (不包含空格且以冒号结尾)
            if (!line.isEmpty() && !line.startsWith(" ") && !line.startsWith("#") && line.contains(":")) {
                // 保存上一个物品
                if (currentItem != null && !itemLines.isEmpty()) {
                    String converted = convertItem(currentItem, itemLines);
                    if (converted != null) {
                        output.append(converted).append("\n");
                        count++;
                    }
                }

                // 开始新物品
                currentItem = line.substring(0, line.indexOf(":")).trim();
                itemLines = new ArrayList<>();
            } else if (currentItem != null) {
                itemLines.add(line);
            }
        }

        // 保存最后一个物品
        if (currentItem != null && !itemLines.isEmpty()) {
            String converted = convertItem(currentItem, itemLines);
            if (converted != null) {
                output.append(converted).append("\n");
                count++;
            }
        }

        // 保存到目标文件 (UTF-8 编码)
        Files.write(targetFile, output.toString().getBytes("UTF-8"));

        return count;
    }

    private static String convertItem(String itemId, List<String> lines) {
        StringBuilder yaml = new StringBuilder();
        yaml.append(itemId).append(":\n");

        String material = "PAPER";
        String display = null;
        List<String> lore = new ArrayList<>();
        Map<String, String> options = new LinkedHashMap<>();
        Map<String, Object> attributes = new LinkedHashMap<>();

        boolean inLore = false;
        boolean inOptions = false;

        for (String line : lines) {
            String trimmed = line.trim();

            // Id 字段
            if (trimmed.startsWith("Id:")) {
                String idValue = trimmed.substring(3).trim();
                material = convertMaterial(idValue);
                inLore = false;
                inOptions = false;
            }
            // Display 字段
            else if (trimmed.startsWith("Display:")) {
                display = trimmed.substring(8).trim();
                // 去除已有的引号
                if (display.startsWith("'") && display.endsWith("'")) {
                    display = display.substring(1, display.length() - 1);
                }
                inLore = false;
                inOptions = false;
            }
            // Lore 开始
            else if (trimmed.startsWith("Lore:")) {
                inLore = true;
                inOptions = false;
            }
            // Options 开始
            else if (trimmed.startsWith("Options:")) {
                inOptions = true;
                inLore = false;
            }
            // Lore 行
            else if (inLore && trimmed.startsWith("-")) {
                String loreLine = trimmed.substring(1).trim();
                // 去除已有的引号
                if (loreLine.startsWith("'") && loreLine.endsWith("'")) {
                    loreLine = loreLine.substring(1, loreLine.length() - 1);
                }
                lore.add(loreLine);
                parseAttributeFromLore(loreLine, attributes);
            }
            // Options 行
            else if (inOptions && trimmed.contains(":")) {
                String[] parts = trimmed.split(":", 2);
                options.put(parts[0].trim(), parts[1].trim());
            }
        }

        // 生成 YAML
        yaml.append("  Id: ").append(material).append("\n");

        if (display != null) {
            yaml.append("  Display: '").append(display).append("'\n");
        }

        if (!lore.isEmpty()) {
            yaml.append("  Lore:\n");
            for (String line : lore) {
                yaml.append("  - '").append(line).append("'\n");
            }
        }

        if (!options.isEmpty()) {
            yaml.append("  Options:\n");
            for (Map.Entry<String, String> entry : options.entrySet()) {
                yaml.append("    ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        if (!attributes.isEmpty()) {
            yaml.append("  # RPGItems 属性 (从 Lore 解析)\n");
            yaml.append("  Attributes:\n");
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                yaml.append("    ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        return yaml.toString();
    }

    private static String convertMaterial(String idValue) {
        try {
            int id = Integer.parseInt(idValue);
            return MATERIAL_ID_MAP.getOrDefault(id, "PAPER");
        } catch (NumberFormatException e) {
            return idValue; // 已经是 Material 名称
        }
    }

    private static void parseAttributeFromLore(String line, Map<String, Object> attributes) {
        String cleanLine = stripColor(line);

        // 攻击力
        if (cleanLine.contains("攻击力")) {
            Matcher m = RANGE_PATTERN.matcher(cleanLine);
            if (m.find()) {
                attributes.put("AttackMin", Double.parseDouble(m.group(1)));
                attributes.put("AttackMax", Double.parseDouble(m.group(2)));
            }
        }

        // 防御力 (非PVP)
        if (cleanLine.contains("防御力") && !cleanLine.contains("PVP")) {
            Matcher m = RANGE_PATTERN.matcher(cleanLine);
            if (m.find()) {
                attributes.put("DefenseMin", Double.parseDouble(m.group(1)));
                attributes.put("DefenseMax", Double.parseDouble(m.group(2)));
            } else {
                m = SINGLE_PATTERN.matcher(cleanLine);
                if (m.find()) {
                    double value = Double.parseDouble(m.group(1));
                    attributes.put("DefenseMin", value);
                    attributes.put("DefenseMax", value);
                }
            }
        }

        // 生命上限
        if (cleanLine.contains("生命上限")) {
            Matcher m = SINGLE_PATTERN.matcher(cleanLine);
            if (m.find()) {
                attributes.put("MaxHealth", Double.parseDouble(m.group(1)));
            }
        }

        // 暴击几率/暴击率
        if (cleanLine.contains("暴击几率") || cleanLine.contains("暴击率")) {
            Matcher m = PERCENT_PATTERN.matcher(cleanLine);
            if (m.find()) {
                attributes.put("CritChance", Double.parseDouble(m.group(1)));
            }
        }

        // 暴击伤害
        if (cleanLine.contains("暴击伤害")) {
            Matcher m = PERCENT_PATTERN.matcher(cleanLine);
            if (m.find()) {
                attributes.put("CritDamage", Double.parseDouble(m.group(1)));
            }
        }

        // 吸血几率/生命吸取
        if (cleanLine.contains("吸血几率") || cleanLine.contains("生命吸取")) {
            Matcher m = PERCENT_PATTERN.matcher(cleanLine);
            if (m.find()) {
                attributes.put("LifestealChance", Double.parseDouble(m.group(1)));
            }
        }

        // 吸血倍率
        if (cleanLine.contains("吸血倍率")) {
            Matcher m = PERCENT_PATTERN.matcher(cleanLine);
            if (m.find()) {
                attributes.put("LifestealMultiplier", Double.parseDouble(m.group(1)));
            }
        }

        // 闪避
        if (cleanLine.contains("闪避")) {
            Matcher m = PERCENT_PATTERN.matcher(cleanLine);
            if (m.find()) {
                attributes.put("DodgeChance", Double.parseDouble(m.group(1)));
            }
        }

        // 招架
        if (cleanLine.contains("招架")) {
            Matcher m = PERCENT_PATTERN.matcher(cleanLine);
            if (m.find()) {
                attributes.put("ParryChance", Double.parseDouble(m.group(1)));
            }
        }

        // 移动速度
        if (cleanLine.contains("移动速度")) {
            Matcher m = PERCENT_PATTERN.matcher(cleanLine);
            if (m.find()) {
                attributes.put("MoveSpeed", Double.parseDouble(m.group(1)));
            }
        }

        // 护甲强度
        if (cleanLine.contains("护甲强度")) {
            Matcher m = PERCENT_PATTERN.matcher(cleanLine);
            if (m.find()) {
                attributes.put("Armor", Double.parseDouble(m.group(1)));
            }
        }

        // 每秒回血
        if (cleanLine.contains("每秒回血")) {
            Matcher m = SINGLE_PATTERN.matcher(cleanLine);
            if (m.find()) {
                attributes.put("HealthRegen", Double.parseDouble(m.group(1)));
            }
        }

        // 装备等级
        if (cleanLine.contains("装备等级")) {
            Matcher m = SINGLE_PATTERN.matcher(cleanLine);
            if (m.find()) {
                attributes.put("Level", Integer.parseInt(m.group(1)));
            }
        }

        // 暴击抵抗
        if (cleanLine.contains("暴击抵抗")) {
            Matcher m = PERCENT_PATTERN.matcher(cleanLine);
            if (m.find()) {
                attributes.put("CritResist", Double.parseDouble(m.group(1)));
            }
        }

        // 暴伤抵抗
        if (cleanLine.contains("暴伤抵抗")) {
            Matcher m = PERCENT_PATTERN.matcher(cleanLine);
            if (m.find()) {
                attributes.put("CritDamageResist", Double.parseDouble(m.group(1)));
            }
        }

        // 反伤比例
        if (cleanLine.contains("反伤比例")) {
            Matcher m = PERCENT_PATTERN.matcher(cleanLine);
            if (m.find()) {
                attributes.put("ReflectRatio", Double.parseDouble(m.group(1)));
            }
        }

        // 吸血抵抗
        if (cleanLine.contains("吸血抵抗")) {
            Matcher m = PERCENT_PATTERN.matcher(cleanLine);
            if (m.find()) {
                attributes.put("LifestealResist", Double.parseDouble(m.group(1)));
            }
        }

        // PVP 攻击力
        if (cleanLine.contains("PVP") && cleanLine.contains("攻击力")) {
            Matcher m = RANGE_PATTERN.matcher(cleanLine);
            if (m.find()) {
                attributes.put("PvpAttackMin", Double.parseDouble(m.group(1)));
                attributes.put("PvpAttackMax", Double.parseDouble(m.group(2)));
            }
        }

        // PVP 防御力
        if (cleanLine.contains("PVP") && cleanLine.contains("防御力")) {
            Matcher m = SINGLE_PATTERN.matcher(cleanLine);
            if (m.find()) {
                double value = Double.parseDouble(m.group(1));
                attributes.put("PvpDefenseMin", value);
                attributes.put("PvpDefenseMax", value);
            }
        }
    }

    private static String stripColor(String text) {
        text = text.replaceAll("<[^>]+>", "");
        text = text.replaceAll("§[0-9a-fA-Fk-oK-orR]", "");
        text = text.replaceAll("&[0-9a-fA-Fk-oK-orR]", "");
        return text.trim();
    }
}
