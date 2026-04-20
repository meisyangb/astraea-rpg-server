package cn.guangdian.forge.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 图纸数据模型
 * 支持 MythicMobs 和 RPGItems 自定义物品作为锻造结果
 * 使用 MiniMessage 格式
 */
public class ForgeRecipe {
    private final String id;
    private final String displayName;
    private final int requiredForgeLevel;
    private final Map<String, Integer> ingredients; // 格式: "mm:物品ID" 或 "mythicmobs:物品ID" 或 "rpg:物品ID"
    private final String resultMythicMobsItem; // MythicMobs结果物品ID
    private final String resultRPGItem; // RPGItems结果物品ID
    private final double baseSuccessRate;

    // 图纸信息
    private final String blueprintDisplay;
    private final List<String> blueprintLore;
    private final boolean isBlueprintBook; // 是否显示为书本

    public ForgeRecipe(String id, String displayName, int requiredForgeLevel,
                       Map<String, Integer> ingredients, String resultMythicMobsItem, String resultRPGItem,
                       double baseSuccessRate,
                       String blueprintDisplay, List<String> blueprintLore, boolean isBlueprintBook) {
        this.id = id;
        this.displayName = displayName;
        this.requiredForgeLevel = requiredForgeLevel;
        this.ingredients = ingredients;
        this.resultMythicMobsItem = resultMythicMobsItem;
        this.resultRPGItem = resultRPGItem;
        this.baseSuccessRate = baseSuccessRate;
        this.blueprintDisplay = blueprintDisplay;
        this.blueprintLore = blueprintLore;
        this.isBlueprintBook = isBlueprintBook;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getRequiredForgeLevel() { return requiredForgeLevel; }
    public Map<String, Integer> getIngredients() { return ingredients; }
    public String getResultMythicMobsItem() { return resultMythicMobsItem; }
    public String getResultRPGItem() { return resultRPGItem; }
    public double getBaseSuccessRate() { return baseSuccessRate; }

    /**
     * 获取结果物品类型
     * @return "mythicmobs" 或 "rpgitems" 或 null
     */
    public String getResultType() {
        if (resultMythicMobsItem != null && !resultMythicMobsItem.isEmpty()) {
            return "mythicmobs";
        }
        if (resultRPGItem != null && !resultRPGItem.isEmpty()) {
            return "rpgitems";
        }
        return null;
    }

    // 图纸相关方法
    public String getBlueprintDisplay() {
        return blueprintDisplay != null ? blueprintDisplay : displayName;
    }

    /**
     * 获取图纸 lore，如果未配置则自动生成
     */
    public List<String> getBlueprintLore() {
        if (blueprintLore != null && !blueprintLore.isEmpty()) {
            return blueprintLore;
        }
        return generateDefaultBlueprintLore();
    }

    public boolean isBlueprintBook() { return isBlueprintBook; }

    /**
     * 自动生成默认的图纸 lore
     * 使用 MiniMessage 格式
     */
    public List<String> generateDefaultBlueprintLore() {
        List<String> lore = new ArrayList<>();

        lore.add("<dark_gray>═══════════════════════");
        lore.add(getBlueprintDisplay());
        lore.add("<dark_gray>═══════════════════════");
        lore.add("");
        lore.add("<gray>所需材料:");

        for (Map.Entry<String, Integer> entry : ingredients.entrySet()) {
            String materialName = getIngredientDisplayName(entry.getKey());
            lore.add("<yellow>◆ " + stripColorCodes(materialName) + " <white>x" + entry.getValue());
        }

        lore.add("");
        lore.add("<gray>锻造等级要求: <gold>" + requiredForgeLevel + "级");
        lore.add("<gray>成功率: <green>" + (int)(baseSuccessRate * 100) + "%");
        lore.add("");
        lore.add("<dark_gray>═══════════════════════");
        lore.add("<green>右键使用学习此图纸");
        lore.add("<dark_gray>═══════════════════════");

        return lore;
    }

    /**
     * 获取材料显示名称（用于Lore）
     * 使用 MiniMessage 格式
     */
    public String getIngredientDisplayName(String ingredientKey) {
        if (ingredientKey == null) return "未知";

        String lowerStr = ingredientKey.toLowerCase();

        if (lowerStr.startsWith("mythicmobs:") || lowerStr.startsWith("mm:")) {
            String itemId = ingredientKey.substring(ingredientKey.indexOf(':') + 1);
            return "<light_purple>" + itemId;
        } else if (lowerStr.startsWith("rpg:") || lowerStr.startsWith("rpgitems:")) {
            String itemId = ingredientKey.substring(ingredientKey.indexOf(':') + 1);
            return "<aqua>" + itemId;
        } else if (lowerStr.startsWith("vanilla:") || lowerStr.startsWith("minecraft:")) {
            String materialName = ingredientKey.substring(ingredientKey.indexOf(':') + 1);
            return "<yellow>" + formatMaterialName(materialName);
        } else {
            // 默认作为 MythicMobs 物品
            return "<light_purple>" + ingredientKey;
        }
    }

    /**
     * 移除字符串中的旧版颜色代码
     */
    private String stripColorCodes(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
    }

    /**
     * 格式化材料名称
     */
    private String formatMaterialName(String name) {
        String[] parts = name.replace("_", " ").toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
