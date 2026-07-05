package cn.guangdian.custommodels.registry;

import cn.guangdian.custommodels.config.CustomModelsConfig;
import cn.guangdian.custommodels.texture.TextureManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.Set;

/**
 * 物品注册表
 * 管理所有自定义物品的定义和注册
 */
public class CustomItemRegistry {

    private final JavaPlugin plugin;
    private final CustomModelsConfig config;
    private final Map<String, CustomItemDefinition> itemDefinitions = new LinkedHashMap<>();
    private int currentModelData;

    public CustomItemRegistry(JavaPlugin plugin, CustomModelsConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.currentModelData = config.getCustomModelDataStart();
    }

    /**
     * 从配置文件加载物品定义
     */
    public void loadItemDefinitions() {
        File itemsFile = new File(plugin.getDataFolder(), "items.yml");

        if (!itemsFile.exists()) {
            plugin.saveResource("items.yml", false);
            plugin.getLogger().info("items.yml 已创建");
        }

        FileConfiguration itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);

        itemDefinitions.clear();

        // 读取物品定义
        if (itemsConfig.contains("weapons")) {
            for (String itemId : itemsConfig.getConfigurationSection("weapons").getKeys(false)) {
                CustomItemDefinition definition = loadItemDefinition(itemsConfig, "weapons." + itemId);
                if (definition != null) {
                    itemDefinitions.put(itemId, definition);
                    plugin.getLogger().info("已加载物品: " + itemId);
                }
            }
        }

        plugin.getLogger().info("物品定义加载完成，共 " + itemDefinitions.size() + " 个");
    }

    /**
     * 从配置加载单个物品定义
     */
    private CustomItemDefinition loadItemDefinition(FileConfiguration config, String path) {
        CustomItemDefinition definition = new CustomItemDefinition();

        definition.setId(config.getString(path + ".id", ""));
        definition.setDisplayName(config.getString(path + ".display_name", ""));
        definition.setMaterial(config.getString(path + ".material", "DIAMOND_SWORD"));
        definition.setCustomModelData(config.getInt(path + ".custom_model_data", currentModelData++));
        definition.setTexture(config.getString(path + ".texture", ""));
        definition.setModelTemplate(config.getString(path + ".model_template", "sword"));

        // Lore
        List<String> lore = config.getStringList(path + ".lore");
        definition.setLore(lore);

        // 属性
        if (config.contains(path + ".attributes")) {
            Map<String, Object> attributes = new HashMap<>();
            for (String attrKey : config.getConfigurationSection(path + ".attributes").getKeys(false)) {
                attributes.put(attrKey, config.get(path + ".attributes." + attrKey));
            }
            definition.setAttributes(attributes);
        }

        return definition;
    }

    /**
     * 自动为贴图生成物品定义
     */
    public void generateItemDefinitionsFromTextures(TextureManager textureManager) {
        Map<String, TextureManager.TextureInfo> textures = textureManager.getAllTextures();

        plugin.getLogger().info("开始为贴图生成物品定义...");

        // ★ 需要跳过的分类（非武器，不生成物品定义）
        Set<String> skipCategories = new HashSet<>();
        skipCategories.add("称号");
        skipCategories.add("魂环");

        int skipped = 0;
        for (TextureManager.TextureInfo texture : textures.values()) {
            String category = texture.getCategory();

            // ★ 跳过非武器贴图
            if (skipCategories.contains(category)) {
                skipped++;
                continue;
            }

            // 贴图ID已由TextureManager净化（去除非法字符+小写化+去重）
            String itemId = texture.getId();

            CustomItemDefinition definition = new CustomItemDefinition();
            definition.setId(itemId);
            // 使用 ChatColor 替代 MiniMessage 标签
            definition.setDisplayName(ChatColor.GOLD + itemId);
            // ★ 使用净化后的文件名而非原始文件名
            definition.setTexture(texture.getSanitizedFileName());

            // 根据分类设置材质 — 与 ResourcePackGenerator 的 baseMaterials 映射完全一致
            Material material = getMaterialByCategory(category);
            definition.setMaterial(material.name());

            // 分配 CustomModelData
            definition.setCustomModelData(currentModelData++);

            // 设置模板
            definition.setModelTemplate(getTemplateByCategory(category));

            // 设置基础属性
            definition.setAttributes(getDefaultAttributes(category));

            // 设置基础Lore
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "------------");
            lore.add(ChatColor.GRAY + "攻击力: " + ChatColor.RED + "+50");
            lore.add(ChatColor.GRAY + "等级: " + ChatColor.GREEN + "4阶");
            lore.add(ChatColor.YELLOW + "------------");
            definition.setLore(lore);

            itemDefinitions.put(itemId, definition);
        }

        if (skipped > 0) {
            plugin.getLogger().info("跳过非武器贴图 " + skipped + " 个（称号/魂环等）");
        }
        plugin.getLogger().info("物品定义生成完成，共 " + itemDefinitions.size() + " 个");
    }

    /**
     * 根据分类获取材质 — 与 ResourcePackGenerator 保持一致
     */
    private Material getMaterialByCategory(String category) {
        if (category.contains("SD") || category.contains("职业武器-武士")) {
            return Material.DIAMOND_SWORD;
        } else if (category.contains("AXE") || category.contains("职业武器-狂战")) {
            return Material.DIAMOND_AXE;
        } else if (category.contains("BOW")) {
            return Material.BOW;
        } else if (category.contains("SPR") || category.contains("职业武器-冰法")) {
            return Material.TRIDENT;  // 矛使用三叉戟，与资源包overrides保持一致
        } else if (category.contains("STF") || category.contains("职业武器-法师")) {
            return Material.BLAZE_ROD;
        } else if (category.contains("DGR")) {
            return Material.DIAMOND_SWORD;  // 匕首使用钻石剑
        }

        return Material.DIAMOND_SWORD;
    }

    /**
     * 根据分类获取模板
     */
    private String getTemplateByCategory(String category) {
        if (category.contains("SD")) return "sword";
        if (category.contains("AXE")) return "axe";
        if (category.contains("BOW")) return "bow";
        if (category.contains("SPR")) return "spear";
        if (category.contains("STF")) return "staff";
        if (category.contains("DGR")) return "dagger";

        return "sword";
    }

    /**
     * 根据分类获取默认属性
     */
    private Map<String, Object> getDefaultAttributes(String category) {
        Map<String, Object> attributes = new HashMap<>();

        // 基础属性
        attributes.put("attack_damage", 50);
        attributes.put("attack_speed", 1.6);
        attributes.put("durability", 2000);

        return attributes;
    }

    // Getter 方法
    public int getItemCount() {
        return itemDefinitions.size();
    }

    public CustomItemDefinition getDefinition(String id) {
        return itemDefinitions.get(id);
    }

    public Map<String, CustomItemDefinition> getAllDefinitions() {
        return new LinkedHashMap<>(itemDefinitions);
    }

    /**
     * 物品定义类
     */
    public static class CustomItemDefinition {
        private String id;
        private String displayName;
        private String material;
        private int customModelData;
        private String texture;
        private String modelTemplate;
        private List<String> lore;
        private Map<String, Object> attributes;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getMaterial() {
            return material;
        }

        public void setMaterial(String material) {
            this.material = material;
        }

        public int getCustomModelData() {
            return customModelData;
        }

        public void setCustomModelData(int customModelData) {
            this.customModelData = customModelData;
        }

        public String getTexture() {
            return texture;
        }

        public void setTexture(String texture) {
            this.texture = texture;
        }

        public String getModelTemplate() {
            return modelTemplate;
        }

        public void setModelTemplate(String modelTemplate) {
            this.modelTemplate = modelTemplate;
        }

        public List<String> getLore() {
            return lore;
        }

        public void setLore(List<String> lore) {
            this.lore = lore;
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
        }
    }
}