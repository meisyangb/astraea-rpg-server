package cn.guangdian.villagertrade.recipe;

import cn.guangdian.villagertrade.GuangDianVillagerTrade;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * 配方管理器
 *
 * <p>管理所有兑换配方的加载和查询</p>
 */
public class RecipeManager {

    private final GuangDianVillagerTrade plugin;
    private final Map<String, TradeRecipe> recipes = new HashMap<>();
    private final Map<String, RecipeGroup> recipeGroups = new HashMap<>();
    
    public RecipeManager(GuangDianVillagerTrade plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 加载所有配方
     */
    public void loadRecipes() {
        recipes.clear();
        recipeGroups.clear();
        
        File recipesFile = new File(plugin.getDataFolder(), "recipes.yml");
        if (!recipesFile.exists()) {
            plugin.saveResource("recipes.yml", false);
        }
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(recipesFile);
            
            // 加载单个配方
            ConfigurationSection recipesSection = config.getConfigurationSection("recipes");
            if (recipesSection != null) {
                for (String recipeName : recipesSection.getKeys(false)) {
                    ConfigurationSection recipeSection = recipesSection.getConfigurationSection(recipeName);
                    if (recipeSection != null) {
                        TradeRecipe recipe = loadRecipe(recipeName, recipeSection);
                        if (recipe != null) {
                            recipes.put(recipeName.toLowerCase(), recipe);
                            plugin.getLogger().fine("加载配方: " + recipeName);
                        }
                    }
                }
            }
            
            // 加载配方组 - 优先从单独文件加载
            loadRecipeGroupsFromSeparateFile();
            
            // 如果没有单独文件，则从recipes.yml加载
            if (recipeGroups.isEmpty()) {
                ConfigurationSection groupsSection = config.getConfigurationSection("recipe-groups");
                if (groupsSection != null) {
                    for (String groupName : groupsSection.getKeys(false)) {
                        ConfigurationSection groupSection = groupsSection.getConfigurationSection(groupName);
                        if (groupSection != null) {
                            RecipeGroup group = loadRecipeGroup(groupName, groupSection);
                            if (group != null) {
                                recipeGroups.put(groupName.toLowerCase(), group);
                                plugin.getLogger().fine("加载配方组: " + groupName);
                            }
                        }
                    }
                }
            }
            
            plugin.getLogger().info("成功加载 " + recipes.size() + " 个兑换配方, " + recipeGroups.size() + " 个配方组");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "加载配方文件失败", e);
        }
    }
    
    /**
     * 加载单个配方
     *
     * @param name 配方名称
     * @param section 配置节
     * @return 配方对象
     */
    private TradeRecipe loadRecipe(String name, ConfigurationSection section) {
        try {
            // 加载显示信息
            String displayName = section.getString("display.name", name);
            String displayMaterial = section.getString("display.material", "PAPER");
            List<String> displayLore = section.getStringList("display.lore");
            
            // 加载输入物品
            List<TradeIngredient> inputs = new ArrayList<>();
            ConfigurationSection inputSection = section.getConfigurationSection("input");
            if (inputSection != null) {
                for (String key : inputSection.getKeys(false)) {
                    ConfigurationSection ingredientSection = inputSection.getConfigurationSection(key);
                    if (ingredientSection != null) {
                        TradeIngredient ingredient = loadIngredient(ingredientSection);
                        if (ingredient != null) {
                            inputs.add(ingredient);
                        }
                    }
                }
            }
            
            // 加载输出物品
            ConfigurationSection outputSection = section.getConfigurationSection("output");
            ItemStack output = null;
            if (outputSection != null) {
                output = loadItemStack(outputSection);
            }
            
            if (output == null) {
                plugin.getLogger().warning("配方 " + name + " 没有设置输出物品");
                return null;
            }
            
            // 加载限制条件
            String permission = section.getString("limit.permission");
            int dailyLimit = section.getInt("limit.daily", -1);
            int maxUses = section.getInt("limit.max-uses", Integer.MAX_VALUE);
            
            // 加载其他设置
            boolean giveExperience = section.getBoolean("settings.give-experience", true);
            int experienceAmount = section.getInt("settings.experience-amount", 0);
            
            return new TradeRecipe(
                name,
                displayName,
                displayMaterial,
                displayLore,
                inputs,
                output,
                permission,
                dailyLimit,
                maxUses,
                giveExperience,
                experienceAmount
            );
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "加载配方 " + name + " 失败", e);
            return null;
        }
    }
    
    /**
     * 加载配方组
     *
     * @param name 配方组名称
     * @param section 配置节
     * @return 配方组对象
     */
    private RecipeGroup loadRecipeGroup(String name, ConfigurationSection section) {
        try {
            // 加载显示信息
            String displayName = section.getString("display.name", name);
            String displayMaterial = section.getString("display.material", "BOOK");
            List<String> displayLore = section.getStringList("display.lore");
            
            // 加载权限
            String permission = section.getString("permission");
            
            // 加载子配方列表
            List<TradeRecipe> groupRecipes = new ArrayList<>();
            List<String> recipeNames = section.getStringList("recipes");
            
            for (String recipeName : recipeNames) {
                TradeRecipe recipe = recipes.get(recipeName.toLowerCase());
                if (recipe != null) {
                    groupRecipes.add(recipe);
                } else {
                    plugin.getLogger().warning("配方组 " + name + " 引用了不存在的配方: " + recipeName);
                }
            }
            
            if (groupRecipes.isEmpty()) {
                plugin.getLogger().warning("配方组 " + name + " 没有有效的子配方");
                return null;
            }
            
            return new RecipeGroup(
                name,
                displayName,
                displayMaterial,
                displayLore,
                groupRecipes,
                permission
            );
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "加载配方组 " + name + " 失败", e);
            return null;
        }
    }
    
    /**
     * 加载配方材料
     *
     * @param section 配置节
     * @return 材料对象
     */
    private TradeIngredient loadIngredient(ConfigurationSection section) {
        // 优先检查是否为RPGItems物品
        String rpgItemId = section.getString("rpg-item");
        
        String materialName = section.getString("material");
        Material material = null;
        if (materialName != null) {
            try {
                material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的材料类型: " + materialName);
            }
        }
        
        // 如果既没有material也没有rpg-item，返回null
        if (material == null && rpgItemId == null) {
            plugin.getLogger().warning("配方材料缺少 material 或 rpg-item 配置");
            return null;
        }
        
        int amount = section.getInt("amount", 1);
        String name = section.getString("name");
        List<String> lore = section.getStringList("lore");
        int customModelData = section.getInt("custom-model-data", 0);
        boolean requireExactMatch = section.getBoolean("exact-match", false);
        String mythicType = section.getString("mythic-type");
        
        return new TradeIngredient(material, amount, name, lore, customModelData, requireExactMatch, mythicType, rpgItemId);
    }
    
    /**
     * 加载物品堆
     *
     * @param section 配置节
     * @return 物品堆
     */
    private ItemStack loadItemStack(ConfigurationSection section) {
        // 优先检查是否为RPGItems物品
        String rpgItemId = section.getString("rpg-item");
        if (rpgItemId != null && plugin.getRPGItemsHook() != null && plugin.getRPGItemsHook().isEnabled()) {
            int amount = section.getInt("amount", 1);
            ItemStack rpgItem = plugin.getRPGItemsHook().getRPGItem(rpgItemId, amount);
            if (rpgItem != null) {
                return rpgItem;
            }
            plugin.getLogger().warning("无法加载RPGItems物品: " + rpgItemId + "，将使用默认配置");
        }
        
        // 检查是否为MythicMobs物品（支持mythic-item和mythic-type两种配置方式）
        String mythicItemId = section.getString("mythic-item");
        if (mythicItemId == null) {
            mythicItemId = section.getString("mythic-type");
        }
        if (mythicItemId != null && plugin.getMythicItemManager().isMythicMobsEnabled()) {
            int amount = section.getInt("amount", 1);
            ItemStack mythicItem = plugin.getMythicItemManager().getMythicItem(mythicItemId, amount);
            if (mythicItem != null) {
                return mythicItem;
            }
            plugin.getLogger().warning("无法加载Mythic物品: " + mythicItemId + "，将使用默认配置");
        }
        
        String materialName = section.getString("material");
        if (materialName == null) {
            return null;
        }
        
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的材料类型: " + materialName);
            return null;
        }
        
        int amount = section.getInt("amount", 1);
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // 设置名称
            String name = section.getString("name");
            if (name != null) {
                meta.setDisplayName(plugin.legacyColorize(name));
            }
            
            // 设置Lore
            List<String> lore = section.getStringList("lore");
            if (!lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(plugin.legacyColorize(line));
                }
                meta.setLore(coloredLore);
            }
            
            // 设置CustomModelData
            int customModelData = section.getInt("custom-model-data", 0);
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }
            
            // 设置MythicMobs类型
            String mythicType = section.getString("mythic-type");
            if (mythicType != null) {
                NamespacedKey key = new NamespacedKey("mythicmobs", "type");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, mythicType);
            }
            
            // 设置不可破坏
            if (section.getBoolean("unbreakable", false)) {
                meta.setUnbreakable(true);
            }
            
            // 隐藏属性
            if (section.getBoolean("hide-attributes", false)) {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * 获取配方
     *
     * @param name 配方名称
     * @return 配方对象
     */
    public TradeRecipe getRecipe(String name) {
        return recipes.get(name.toLowerCase());
    }
    
    /**
     * 获取配方组
     *
     * @param name 配方组名称
     * @return 配方组对象
     */
    public RecipeGroup getRecipeGroup(String name) {
        return recipeGroups.get(name.toLowerCase());
    }
    
    /**
     * 检查是否存在配方
     *
     * @param name 配方名称
     * @return 是否存在
     */
    public boolean hasRecipe(String name) {
        return recipes.containsKey(name.toLowerCase());
    }
    
    /**
     * 检查是否存在配方组
     *
     * @param name 配方组名称
     * @return 是否存在
     */
    public boolean hasRecipeGroup(String name) {
        return recipeGroups.containsKey(name.toLowerCase());
    }
    
    /**
     * 获取所有配方名称
     *
     * @return 配方名称列表
     */
    public Set<String> getRecipeNames() {
        return new HashSet<>(recipes.keySet());
    }
    
    /**
     * 获取所有配方组名称
     *
     * @return 配方组名称列表
     */
    public Set<String> getRecipeGroupNames() {
        return new HashSet<>(recipeGroups.keySet());
    }
    
    /**
     * 获取配方数量
     *
     * @return 配方数量
     */
    public int getRecipeCount() {
        return recipes.size();
    }
    
    /**
     * 获取配方组数量
     *
     * @return 配方组数量
     */
    public int getRecipeGroupCount() {
        return recipeGroups.size();
    }
    
    /**
     * 获取配方权限
     *
     * @param name 配方名称
     * @return 权限字符串
     */
    public String getRecipePermission(String name) {
        TradeRecipe recipe = recipes.get(name.toLowerCase());
        return recipe != null ? recipe.getPermission() : null;
    }
    
    /**
     * 从单独文件加载配方组
     */
    private void loadRecipeGroupsFromSeparateFile() {
        File groupsFile = new File(plugin.getDataFolder(), "recipe-groups.yml");
        if (!groupsFile.exists()) {
            return; // 如果没有单独文件，直接返回
        }
        
        try {
            FileConfiguration groupsConfig = YamlConfiguration.loadConfiguration(groupsFile);
            ConfigurationSection groupsSection = groupsConfig.getConfigurationSection("recipe-groups");
            
            if (groupsSection != null) {
                int loadedCount = 0;
                for (String groupName : groupsSection.getKeys(false)) {
                    ConfigurationSection groupSection = groupsSection.getConfigurationSection(groupName);
                    if (groupSection != null) {
                        RecipeGroup group = loadRecipeGroup(groupName, groupSection);
                        if (group != null) {
                            recipeGroups.put(groupName.toLowerCase(), group);
                            loadedCount++;
                            plugin.getLogger().fine("从单独文件加载配方组: " + groupName);
                        }
                    }
                }
                if (loadedCount > 0) {
                    plugin.getLogger().info("从 recipe-groups.yml 加载了 " + loadedCount + " 个配方组");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "从单独文件加载配方组失败", e);
        }
    }
    
    /**
     * 重新加载配方
     */
    public void reload() {
        loadRecipes();
    }
}
