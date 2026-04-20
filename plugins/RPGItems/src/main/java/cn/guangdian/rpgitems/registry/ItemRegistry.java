package cn.guangdian.rpgitems.registry;

import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgitems.template.ItemTemplate;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * 物品注册表
 * 管理所有物品模板 - 复用 MythicMobs 格式
 * Lore驱动模式：Lore直接写，程序解析
 */
public class ItemRegistry {

    private final Map<String, ItemTemplate> items = new HashMap<>();

    /**
     * 从配置加载物品 - 兼容 MythicMobs 格式
     */
    public void loadFromConfig(ConfigurationSection config) {
        if (config == null) return;

        for (String itemId : config.getKeys(false)) {
            // 跳过注释行
            if (itemId.startsWith("#")) continue;

            ConfigurationSection itemConfig = config.getConfigurationSection(itemId);
            if (itemConfig == null) continue;

            ItemTemplate item = parseItemTemplate(itemId, itemConfig);
            if (item != null) {
                items.put(itemId, item);
            }
        }
    }

    /**
     * 从多个配置文件加载物品
     */
    public void loadFromConfigs(List<org.bukkit.configuration.file.FileConfiguration> configs) {
        items.clear();
        int totalItems = 0;

        for (org.bukkit.configuration.file.FileConfiguration config : configs) {
            if (config == null) continue;

            for (String itemId : config.getKeys(false)) {
                // 跳过注释行
                if (itemId.startsWith("#")) continue;

                ConfigurationSection itemConfig = config.getConfigurationSection(itemId);
                if (itemConfig == null) continue;

                ItemTemplate item = parseItemTemplate(itemId, itemConfig);
                if (item != null) {
                    items.put(itemId, item);
                    totalItems++;
                }
            }
        }
    }

    private ItemTemplate parseItemTemplate(String id, ConfigurationSection config) {
        try {
            // 基础信息 - 兼容 Id 字段（MythicMobs 格式）
            String materialStr = config.getString("Id", "STONE");
            Material material = parseMaterial(materialStr);

            // 显示名称 - 使用 MiniMessage 解析
            String displayNameStr = config.getString("Display", id);
            Component displayName = MiniMessageService.getInstance().colorize(displayNameStr);

            // Lore - 列表格式，每行使用 MiniMessage
            List<Component> lore = new ArrayList<>();
            List<String> loreStrings = config.getStringList("Lore");
            for (String line : loreStrings) {
                lore.add(MiniMessageService.getInstance().colorize(line));
            }

            // 选项解析
            ItemTemplate.ItemOptions options = parseOptions(config.getConfigurationSection("Options"));

            // 技能绑定
            List<ItemTemplate.SkillBinding> skillBindings = parseSkillBindings(config.getConfigurationSection("Skills"));

            // 附魔
            List<String> enchantments = config.getStringList("Enchantments");

            return new ItemTemplate(id, material, displayName, lore, options, skillBindings, enchantments);

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析材质 - 支持数字ID和英文名称
     */
    private Material parseMaterial(String materialStr) {
        if (materialStr == null || materialStr.isEmpty()) {
            return Material.STONE;
        }

        // 尝试直接解析英文名称
        try {
            return Material.valueOf(materialStr.toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }

        // 尝试解析数字ID
        try {
            int id = Integer.parseInt(materialStr);
            return getMaterialFromId(id);
        } catch (NumberFormatException ignored) {
        }

        return Material.STONE;
    }

    /**
     * 数字ID转 Material (常用物品)
     */
    private Material getMaterialFromId(int id) {
        return switch (id) {
            case 267 -> Material.IRON_SWORD;
            case 268 -> Material.WOODEN_SWORD;
            case 272 -> Material.STONE_SWORD;
            case 276 -> Material.DIAMOND_SWORD;
            case 283 -> Material.GOLDEN_SWORD;
            case 298 -> Material.LEATHER_HELMET;
            case 299 -> Material.LEATHER_CHESTPLATE;
            case 300 -> Material.LEATHER_LEGGINGS;
            case 301 -> Material.LEATHER_BOOTS;
            case 302 -> Material.CHAINMAIL_HELMET;
            case 303 -> Material.CHAINMAIL_CHESTPLATE;
            case 304 -> Material.CHAINMAIL_LEGGINGS;
            case 305 -> Material.CHAINMAIL_BOOTS;
            case 306 -> Material.IRON_HELMET;
            case 307 -> Material.IRON_CHESTPLATE;
            case 308 -> Material.IRON_LEGGINGS;
            case 309 -> Material.IRON_BOOTS;
            case 310 -> Material.DIAMOND_HELMET;
            case 311 -> Material.DIAMOND_CHESTPLATE;
            case 312 -> Material.DIAMOND_LEGGINGS;
            case 313 -> Material.DIAMOND_BOOTS;
            case 314 -> Material.GOLDEN_HELMET;
            case 315 -> Material.GOLDEN_CHESTPLATE;
            case 316 -> Material.GOLDEN_LEGGINGS;
            case 317 -> Material.GOLDEN_BOOTS;
            case 331 -> Material.REDSTONE;
            case 338 -> Material.SUGAR_CANE;
            case 339 -> Material.PAPER;
            case 388 -> Material.EMERALD;
            default -> Material.STONE;
        };
    }

    private ItemTemplate.ItemOptions parseOptions(ConfigurationSection section) {
        if (section == null) {
            return new ItemTemplate.ItemOptions(false, false, null);
        }

        boolean unbreakable = section.getBoolean("Unbreakable", false);
        boolean hideAttributes = section.getBoolean("HideAttributes", false);
        Integer customModelData = section.contains("CustomModelData")
                ? section.getInt("CustomModelData")
                : null;

        return new ItemTemplate.ItemOptions(unbreakable, hideAttributes, customModelData);
    }

    private List<ItemTemplate.SkillBinding> parseSkillBindings(ConfigurationSection section) {
        List<ItemTemplate.SkillBinding> bindings = new ArrayList<>();
        if (section == null) return bindings;

        // 支持列表格式
        List<Map<?, ?>> skillList = section.getMapList("");
        for (Map<?, ?> skillMap : skillList) {
            String skillId = String.valueOf(skillMap.get("id"));
            Object triggerObj = skillMap.get("trigger");
            String trigger = triggerObj != null ? String.valueOf(triggerObj) : "RIGHT_CLICK";
            int cooldown = skillMap.containsKey("cooldown") ? ((Number) skillMap.get("cooldown")).intValue() : -1;
            int chance = skillMap.containsKey("chance") ? ((Number) skillMap.get("chance")).intValue() : -1;

            bindings.add(new ItemTemplate.SkillBinding(skillId, trigger, cooldown, chance));
        }

        return bindings;
    }

    public Optional<ItemTemplate> getItem(String id) {
        return Optional.ofNullable(items.get(id));
    }

    public Map<String, ItemTemplate> getAllItems() {
        return new HashMap<>(items);
    }

    public int getItemCount() {
        return items.size();
    }

    public void clear() {
        items.clear();
    }
}
 