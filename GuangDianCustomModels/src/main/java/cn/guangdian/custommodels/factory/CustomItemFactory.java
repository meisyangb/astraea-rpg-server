package cn.guangdian.custommodels.factory;

import cn.guangdian.custommodels.registry.CustomItemRegistry;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 物品工厂
 * 根据物品定义创建ItemStack
 */
public class CustomItemFactory {

    private final JavaPlugin plugin;
    private final CustomItemRegistry registry;

    public CustomItemFactory(JavaPlugin plugin, CustomItemRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    /**
     * 创建物品
     */
    public ItemStack createItem(String itemId) {
        CustomItemRegistry.CustomItemDefinition definition = registry.getDefinition(itemId);

        if (definition == null) {
            plugin.getLogger().warning("物品定义不存在: " + itemId);
            return null;
        }

        try {
            // 创建基础物品
            Material material = Material.valueOf(definition.getMaterial());
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                // 设置显示名称 — 将 MiniMessage 标签转换为 ChatColor
                String displayName = convertMiniMessageToChatColor(definition.getDisplayName());
                meta.setDisplayName(displayName);

                // 设置Lore — 同样转换 MiniMessage 标签
                List<String> lore = definition.getLore();
                if (lore != null && !lore.isEmpty()) {
                    List<String> convertedLore = new ArrayList<>();
                    for (String line : lore) {
                        convertedLore.add(convertMiniMessageToChatColor(line));
                    }
                    meta.setLore(convertedLore);
                }

                // 设置 CustomModelData
                meta.setCustomModelData(definition.getCustomModelData());

                // 保存物品ID到PDC
                org.bukkit.NamespacedKey idKey = new org.bukkit.NamespacedKey(plugin, "custom_item_id");
                meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, itemId);

                // 保存属性到PDC
                if (definition.getAttributes() != null) {
                    for (Map.Entry<String, Object> entry : definition.getAttributes().entrySet()) {
                        String attrKey = entry.getKey();
                        Object attrValue = entry.getValue();

                        org.bukkit.NamespacedKey attrNamespacedKey = new org.bukkit.NamespacedKey(plugin, attrKey);
                        if (attrValue instanceof Integer) {
                            meta.getPersistentDataContainer().set(attrNamespacedKey, PersistentDataType.INTEGER, (Integer) attrValue);
                        } else if (attrValue instanceof Double) {
                            meta.getPersistentDataContainer().set(attrNamespacedKey, PersistentDataType.DOUBLE, (Double) attrValue);
                        } else if (attrValue instanceof String) {
                            meta.getPersistentDataContainer().set(attrNamespacedKey, PersistentDataType.STRING, (String) attrValue);
                        }
                    }
                }

                item.setItemMeta(meta);
            }

            return item;

        } catch (Exception e) {
            plugin.getLogger().severe("创建物品失败: " + itemId + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 创建物品（带数量）
     */
    public ItemStack createItem(String itemId, int amount) {
        ItemStack item = createItem(itemId);
        if (item != null) {
            item.setAmount(amount);
        }
        return item;
    }

    /**
     * 检查物品是否为自定义物品
     */
    public boolean isCustomItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        org.bukkit.NamespacedKey idKey = new org.bukkit.NamespacedKey(plugin, "custom_item_id");

        return meta.getPersistentDataContainer().has(idKey, PersistentDataType.STRING);
    }

    /**
     * 获取物品ID
     */
    public String getItemId(ItemStack item) {
        if (!isCustomItem(item)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        org.bukkit.NamespacedKey idKey = new org.bukkit.NamespacedKey(plugin, "custom_item_id");

        return meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }

    /**
     * 获取物品属性
     */
    public Object getAttribute(ItemStack item, String attributeKey) {
        if (!isCustomItem(item)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        org.bukkit.NamespacedKey attrNamespacedKey = new org.bukkit.NamespacedKey(plugin, attributeKey);

        if (meta.getPersistentDataContainer().has(attrNamespacedKey, PersistentDataType.INTEGER)) {
            return meta.getPersistentDataContainer().get(attrNamespacedKey, PersistentDataType.INTEGER);
        } else if (meta.getPersistentDataContainer().has(attrNamespacedKey, PersistentDataType.DOUBLE)) {
            return meta.getPersistentDataContainer().get(attrNamespacedKey, PersistentDataType.DOUBLE);
        } else if (meta.getPersistentDataContainer().has(attrNamespacedKey, PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(attrNamespacedKey, PersistentDataType.STRING);
        }

        return null;
    }

    /**
     * 将 MiniMessage 格式标签转换为 Bukkit ChatColor
     * 支持 <gold>, <red>, <green>, <aqua>, <yellow>, <gray>, <white>, <dark_red>, <dark_blue>, <dark_green>, <dark_aqua>, <dark_gray>, <blue>, <light_purple>, <dark_purple>, <bold>, <italic>, <underlined>, <strikethrough>, <obfuscated>
     */
    private String convertMiniMessageToChatColor(String input) {
        if (input == null) return "";
        String result = input;
        result = result.replace("<gold>", "§6");
        result = result.replace("<red>", "§c");
        result = result.replace("<green>", "§a");
        result = result.replace("<aqua>", "§b");
        result = result.replace("<yellow>", "§e");
        result = result.replace("<gray>", "§7");
        result = result.replace("<white>", "§f");
        result = result.replace("<dark_red>", "§4");
        result = result.replace("<dark_blue>", "§1");
        result = result.replace("<dark_green>", "§2");
        result = result.replace("<dark_aqua>", "§3");
        result = result.replace("<dark_gray>", "§8");
        result = result.replace("<blue>", "§9");
        result = result.replace("<light_purple>", "§d");
        result = result.replace("<dark_purple>", "§5");
        result = result.replace("<black>", "§0");
        result = result.replace("<bold>", "§l");
        result = result.replace("<italic>", "§o");
        result = result.replace("<underlined>", "§n");
        result = result.replace("<strikethrough>", "§m");
        result = result.replace("<obfuscated>", "§k");
        result = result.replace("<reset>", "§r");
        return result;
    }
}