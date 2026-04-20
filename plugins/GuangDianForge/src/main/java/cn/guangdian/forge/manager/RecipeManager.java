package cn.guangdian.forge.manager;

import cn.guangdian.forge.GuangDianForge;
import cn.guangdian.forge.model.ForgeRecipe;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * 图纸管理器
 * 只支持 MythicMobs 自定义物品作为锻造结果
 */
public class RecipeManager {
    private final GuangDianForge plugin;
    private final Map<String, ForgeRecipe> recipes = new HashMap<>();

    public RecipeManager(GuangDianForge plugin) {
        this.plugin = plugin;
    }

    public void loadRecipes() {
        recipes.clear();
        
        File file = new File(plugin.getDataFolder(), "recipes.yml");
        if (!file.exists()) {
            plugin.saveResource("recipes.yml", false);
        }
        
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("recipes");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection rs = section.getConfigurationSection(id);
            if (rs == null) continue;
            
            try {
                ForgeRecipe recipe = parseRecipe(id, rs);
                recipes.put(id, recipe);
                plugin.getLogger().info("加载图纸: " + id + " - " + recipe.getDisplayName());
            } catch (Exception e) {
                plugin.getLogger().warning("加载图纸失败: " + id + " - " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        plugin.getLogger().info("共加载 " + recipes.size() + " 个图纸");
    }

    private ForgeRecipe parseRecipe(String id, ConfigurationSection rs) {
        String name = rs.getString("name", id);
        int requiredLevel = rs.getInt("required-forge-level", 1);
        double baseRate = rs.getDouble("base-success-rate", 0.5);
        
        // 解析材料 - 格式 "mm:物品ID" 或 "mythicmobs:物品ID"
        Map<String, Integer> ingredients = new HashMap<>();
        ConfigurationSection ingSection = rs.getConfigurationSection("ingredients");
        boolean debug = plugin.getConfig().getBoolean("debug", false);
        
        if (ingSection != null) {
            for (String key : ingSection.getKeys(false)) {
                int amount = ingSection.getInt(key, 1);
                ingredients.put(key, amount);
                if (debug) {
                    plugin.getLogger().info("  材料: " + key + " x" + amount);
                }
            }
        } else {
            plugin.getLogger().warning("图纸 " + id + " 没有配置材料!");
        }
        
        // 解析结果 - 支持 MythicMobs 或 RPGItems 物品
        ConfigurationSection resultSection = rs.getConfigurationSection("result");
        String resultMythicMobsItem = null;
        String resultRPGItem = null;

        if (resultSection != null) {
            resultMythicMobsItem = resultSection.getString("mythicmobs-item");
            resultRPGItem = resultSection.getString("rpg-item");

            // 尝试旧的配置格式
            if (resultMythicMobsItem == null || resultMythicMobsItem.isEmpty()) {
                resultMythicMobsItem = resultSection.getString("item");
            }
        }

        if ((resultMythicMobsItem == null || resultMythicMobsItem.isEmpty()) &&
            (resultRPGItem == null || resultRPGItem.isEmpty())) {
            throw new IllegalArgumentException("图纸 " + id + " 必须配置 result.mythicmobs-item 或 result.rpg-item");
        }
        
        // 解析图纸信息
        ConfigurationSection blueprintSection = rs.getConfigurationSection("blueprint");
        String blueprintDisplay = null;
        List<String> blueprintLore = new ArrayList<>();
        boolean isBlueprintBook = true;
        
        if (blueprintSection != null) {
            blueprintDisplay = blueprintSection.getString("display", name);
            blueprintLore = blueprintSection.getStringList("lore");
            isBlueprintBook = blueprintSection.getBoolean("is-book", true);
        }
        
        return new ForgeRecipe(id, name, requiredLevel, ingredients, resultMythicMobsItem, resultRPGItem,
                              baseRate, blueprintDisplay, blueprintLore, isBlueprintBook);
    }

    public ForgeRecipe getRecipe(String id) { return recipes.get(id); }
    public Collection<ForgeRecipe> getAllRecipes() { return recipes.values(); }
}