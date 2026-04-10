package cn.guangdian.forge.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 图纸数据模型
 * 只支持 MythicMobs 自定义物品作为锻造结果
 */
public class ForgeRecipe {
    private final String id;
    private final String displayName;
    private final int requiredForgeLevel;
    private final Map<String, Integer> ingredients; // 格式: "mm:物品ID" 或 "mythicmobs:物品ID"
    private final String resultMythicMobsItem; // MythicMobs结果物品ID（必须配置）
    private final double baseSuccessRate;
    
    // 图纸信息
    private final String blueprintDisplay;
    private final List<String> blueprintLore;
    private final boolean isBlueprintBook; // 是否显示为书本

    public ForgeRecipe(String id, String displayName, int requiredForgeLevel,
                       Map<String, Integer> ingredients, String resultMythicMobsItem,
                       double baseSuccessRate,
                       String blueprintDisplay, List<String> blueprintLore, boolean isBlueprintBook) {
        this.id = id;
        this.displayName = displayName;
        this.requiredForgeLevel = requiredForgeLevel;
        this.ingredients = ingredients;
        this.resultMythicMobsItem = resultMythicMobsItem;
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
    public double getBaseSuccessRate() { return baseSuccessRate; }
    
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
     */
    public List<String> generateDefaultBlueprintLore() {
        List<String> lore = new ArrayList<>();
        
        lore.add("§8═══════════════════════");
        lore.add(getBlueprintDisplay());
        lore.add("§8═══════════════════════");
        lore.add("");
        lore.add("§7所需材料:");
        
        for (Map.Entry<String, Integer> entry : ingredients.entrySet()) {
            String materialName = getIngredientDisplayName(entry.getKey());
            lore.add("§e◆ " + materialName.replace("§d", "").replace("§e", "") + " §fx" + entry.getValue());
        }
        
        lore.add("");
        lore.add("§7锻造等级要求: §6" + requiredForgeLevel + "级");
        lore.add("§7成功率: §a" + (int)(baseSuccessRate * 100) + "%");
        lore.add("");
        lore.add("§8═══════════════════════");
        lore.add("§a右键使用学习此图纸");
        lore.add("§8═══════════════════════");
        
        return lore;
    }
    
    /**
     * 获取材料显示名称（用于Lore）
     */
    public String getIngredientDisplayName(String ingredientKey) {
        if (ingredientKey == null) return "未知";
        
        String lowerStr = ingredientKey.toLowerCase();
        
        if (lowerStr.startsWith("mythicmobs:") || lowerStr.startsWith("mm:")) {
            String itemId = ingredientKey.substring(ingredientKey.indexOf(':') + 1);
            return "§d" + itemId;
        } else if (lowerStr.startsWith("vanilla:") || lowerStr.startsWith("minecraft:")) {
            String materialName = ingredientKey.substring(ingredientKey.indexOf(':') + 1);
            return "§e" + formatMaterialName(materialName);
        } else {
            // 默认作为 MythicMobs 物品
            return "§d" + ingredientKey;
        }
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