package cn.guangdian.socket.util;

import cn.guangdian.socket.hook.MythicMobsHook;
import cn.guangdian.socket.hook.RPGItemsHook;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * 物品解析器
 * 统一处理 MythicMobs、RPGItems 和原版物品的获取
 */
public class ItemResolver {

    private static final MythicMobsHook mythicMobsHook = MythicMobsHook.getInstance();
    private static final RPGItemsHook rpgItemsHook = RPGItemsHook.getInstance();

    /**
     * 根据物品ID获取物品
     * 支持格式:
     * - mythic:<id> - MythicMobs 物品
     * - rpg:<id>    - RPGItems 物品
     * - <material>  - 原版物品 (如 DIAMOND)
     *
     * @param itemId 物品ID
     * @return 物品堆，如果无法获取则返回 null
     */
    public static ItemStack resolveItem(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }

        // 检查是否是 MythicMobs 物品
        if (itemId.toLowerCase().startsWith("mythic:")) {
            String mythicId = itemId.substring(7);
            return mythicMobsHook.getMythicItem(mythicId);
        }

        // 检查是否是 RPGItems 物品
        if (itemId.toLowerCase().startsWith("rpg:")) {
            String rpgId = itemId.substring(4);
            return rpgItemsHook.getRPGItem(rpgId);
        }

        // 尝试作为原版 Material 解析
        try {
            Material material = Material.valueOf(itemId.toUpperCase());
            return new ItemStack(material);
        } catch (IllegalArgumentException e) {
            // 不是有效的 Material，尝试从各个钩子获取
            ItemStack item = null;

            // 尝试 MythicMobs
            if (mythicMobsHook.isEnabled()) {
                item = mythicMobsHook.getMythicItem(itemId);
                if (item != null) return item;
            }

            // 尝试 RPGItems
            if (rpgItemsHook.isEnabled()) {
                item = rpgItemsHook.getRPGItem(itemId);
                if (item != null) return item;
            }
        }

        return null;
    }

    /**
     * 检查物品是否匹配指定的ID
     *
     * @param item   物品
     * @param itemId 物品ID
     * @return 是否匹配
     */
    public static boolean matches(ItemStack item, String itemId) {
        if (item == null || itemId == null || itemId.isEmpty()) {
            return false;
        }

        // 检查是否是 MythicMobs 物品
        if (itemId.toLowerCase().startsWith("mythic:")) {
            String mythicId = itemId.substring(7);
            return mythicMobsHook.isMythicItem(item, mythicId);
        }

        // 检查是否是 RPGItems 物品
        if (itemId.toLowerCase().startsWith("rpg:")) {
            String rpgId = itemId.substring(4);
            return rpgItemsHook.isRPGItem(item, rpgId);
        }

        // 检查原版 Material
        try {
            Material material = Material.valueOf(itemId.toUpperCase());
            return item.getType() == material;
        } catch (IllegalArgumentException e) {
            // 尝试各个钩子
            if (mythicMobsHook.isEnabled() && mythicMobsHook.isMythicItem(item, itemId)) {
                return true;
            }
            if (rpgItemsHook.isEnabled() && rpgItemsHook.isRPGItem(item, itemId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取物品的类型标识
     *
     * @param item 物品
     * @return 类型标识，如 "mythic:sword", "rpg:gem_ruby", "DIAMOND"
     */
    public static String getItemType(ItemStack item) {
        if (item == null) return null;

        // 检查是否是 MythicMobs 物品
        if (mythicMobsHook.isEnabled()) {
            String mythicType = mythicMobsHook.getMythicType(item);
            if (mythicType != null) {
                return "mythic:" + mythicType;
            }
        }

        // 检查是否是 RPGItems 物品
        if (rpgItemsHook.isEnabled()) {
            String rpgId = rpgItemsHook.getRPGItemId(item);
            if (rpgId != null) {
                return "rpg:" + rpgId;
            }
        }

        // 返回原版 Material
        return item.getType().name();
    }

    /**
     * 检查是否是特殊物品（MythicMobs 或 RPGItems）
     *
     * @param item 物品
     * @return 是否是特殊物品
     */
    public static boolean isSpecialItem(ItemStack item) {
        if (item == null) return false;

        if (mythicMobsHook.isEnabled() && mythicMobsHook.getMythicType(item) != null) {
            return true;
        }

        if (rpgItemsHook.isEnabled() && rpgItemsHook.isRPGItem(item)) {
            return true;
        }

        return false;
    }
}
