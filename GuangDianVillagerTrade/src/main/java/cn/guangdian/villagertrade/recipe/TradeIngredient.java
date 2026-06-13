package cn.guangdian.villagertrade.recipe;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * 兑换材料
 *
 * <p>定义兑换所需的材料要求</p>
 */
public class TradeIngredient {

    private final Material material;
    private final int amount;
    private final String name;
    private final List<String> lore;
    private final int customModelData;
    private final boolean requireExactMatch;
    private final String mythicType;
    private final String rpgItem; // RPGItems 物品 ID

    public TradeIngredient(Material material, int amount, String name, List<String> lore,
                          int customModelData, boolean requireExactMatch, String mythicType, String rpgItem) {
        this.material = material;
        this.amount = amount;
        this.name = name;
        this.lore = lore;
        this.customModelData = customModelData;
        this.requireExactMatch = requireExactMatch;
        this.mythicType = mythicType;
        this.rpgItem = rpgItem;
    }

    /**
     * 获取材料类型
     *
     * @return 材料类型
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * 获取数量
     *
     * @return 数量
     */
    public int getAmount() {
        return amount;
    }

    /**
     * 获取名称要求
     *
     * @return 名称要求，null表示不检查
     */
    public String getName() {
        return name;
    }

    /**
     * 获取Lore要求
     *
     * @return Lore要求
     */
    public List<String> getLore() {
        return lore;
    }

    /**
     * 获取CustomModelData要求
     *
     * @return CustomModelData，0表示不检查
     */
    public int getCustomModelData() {
        return customModelData;
    }

    /**
     * 是否需要精确匹配
     *
     * @return 是否需要精确匹配
     */
    public boolean isRequireExactMatch() {
        return requireExactMatch;
    }

    /**
     * 获取MythicMobs类型
     *
     * @return MythicMobs类型
     */
    public String getMythicType() {
        return mythicType;
    }

    /**
     * 获取RPGItems物品ID
     *
     * @return RPGItems物品ID
     */
    public String getRpgItem() {
        return rpgItem;
    }

    /**
     * 检查物品是否匹配此材料要求
     *
     * @param item 物品
     * @return 是否匹配
     */
    public boolean matches(ItemStack item) {
        if (item == null) {
            return false;
        }

        if (item.getAmount() < amount) {
            return false;
        }

        // 如果不需要精确匹配，只检查数量
        if (!requireExactMatch && name == null && lore.isEmpty() 
            && customModelData == 0 && mythicType == null && rpgItem == null) {
            // 只检查材料类型和数量
            if (material != null && item.getType() != material) {
                return false;
            }
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            // 如果要求精确匹配但没有meta，则不匹配
            return !requireExactMatch && name == null && lore.isEmpty() 
                && customModelData == 0 && mythicType == null && rpgItem == null;
        }

        // 检查RPGItems物品
        if (rpgItem != null) {
            NamespacedKey key = new NamespacedKey("rpgitems", "id");
            String itemRpgId = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (itemRpgId == null || !itemRpgId.equals(rpgItem)) {
                return false;
            }
        } else if (material != null && item.getType() != material) {
            // 如果没有指定RPGItems物品，则检查材料类型
            return false;
        }

        // 检查名称
        if (name != null) {
            String itemName = meta.hasDisplayName() ? meta.getDisplayName() : "";
            if (!itemName.equals(name)) {
                return false;
            }
        }

        // 检查Lore
        if (!lore.isEmpty()) {
            List<String> itemLore = meta.getLore();
            if (itemLore == null) {
                return false;
            }
            for (String requiredLore : lore) {
                boolean found = false;
                for (String actualLore : itemLore) {
                    if (actualLore.contains(requiredLore)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
        }

        // 检查CustomModelData
        if (customModelData > 0) {
            if (!meta.hasCustomModelData() || meta.getCustomModelData() != customModelData) {
                return false;
            }
        }

        // 检查MythicMobs类型
        if (mythicType != null) {
            NamespacedKey key = new NamespacedKey("mythicmobs", "type");
            String itemMythicType = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (itemMythicType == null || !itemMythicType.equals(mythicType)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 创建用于展示的材料物品
     *
     * @return 展示用物品
     */
    public ItemStack createDisplayItem() {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(name);
            }
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }
            if (mythicType != null) {
                NamespacedKey key = new NamespacedKey("mythicmobs", "type");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, mythicType);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 是否有名称要求
     *
     * @return 是否有名称要求
     */
    public boolean hasName() {
        return name != null && !name.isEmpty();
    }

    /**
     * 是否有Lore要求
     *
     * @return 是否有Lore要求
     */
    public boolean hasLore() {
        return !lore.isEmpty();
    }

    /**
     * 是否有CustomModelData要求
     *
     * @return 是否有CustomModelData要求
     */
    public boolean hasCustomModelData() {
        return customModelData > 0;
    }

    /**
     * 是否有MythicMobs类型要求
     *
     * @return 是否有MythicMobs类型要求
     */
    public boolean hasMythicType() {
        return mythicType != null && !mythicType.isEmpty();
    }

    /**
     * 是否有RPGItems物品要求
     *
     * @return 是否有RPGItems物品要求
     */
    public boolean hasRpgItem() {
        return rpgItem != null && !rpgItem.isEmpty();
    }
}
