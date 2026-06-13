package cn.guangdian.socket.util;

import cn.guangdian.socket.hook.RPGItemsHook;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * 物品解析器
 * 统一处理 RPGItems 和原版物品的获取
 */
public class ItemResolver {

    private static final RPGItemsHook rpgItemsHook = RPGItemsHook.getInstance();

    /**
     * 根据物品ID获取物品
     * 支持格式:
     * - rpgitem:<id> - RPGItems 物品
     * - <material>  - 原版物品 (如 DIAMOND)
     *
     * @param itemId 物品ID
     * @return 物品堆，如果无法获取则返回 null
     */
    public static ItemStack resolveItem(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }

        // 支持 rpgitem: 前缀
        if (itemId.toLowerCase().startsWith("rpgitem:")) {
            String rpgItemId = itemId.substring(8);
            return rpgItemsHook.getRPGItem(rpgItemId);
        }

        // 尝试作为原版物品
        try {
            Material material = Material.valueOf(itemId.toUpperCase());
            return new ItemStack(material);
        } catch (IllegalArgumentException e) {
            // 不是原版物品，尝试作为 RPGItems 物品
            if (rpgItemsHook.isEnabled()) {
                return rpgItemsHook.getRPGItem(itemId);
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

        // 支持 rpgitem: 前缀
        if (itemId.toLowerCase().startsWith("rpgitem:")) {
            String rpgItemId = itemId.substring(8);
            return rpgItemsHook.isRPGItem(item, rpgItemId);
        }

        // 尝试作为原版物品
        try {
            Material material = Material.valueOf(itemId.toUpperCase());
            return item.getType() == material;
        } catch (IllegalArgumentException e) {
            // 不是原版物品，检查是否是 RPGItems 物品
            return rpgItemsHook.isEnabled() && rpgItemsHook.isRPGItem(item, itemId);
        }
    }

    /**
     * 获取物品的类型标识
     *
     * @param item 物品
     * @return 类型标识，如 "rpgitem:sword", "DIAMOND"
     */
    public static String getItemType(ItemStack item) {
        if (item == null) return null;

        // 检查是否是 RPGItems 物品
        if (rpgItemsHook.isEnabled()) {
            String rpgItemId = rpgItemsHook.getRPGItemId(item);
            if (rpgItemId != null) {
                return "rpgitem:" + rpgItemId;
            }
        }

        return item.getType().name();
    }

    /**
     * 检查是否是特殊物品（RPGItems）
     *
     * @param item 物品
     * @return 是否是特殊物品
     */
    public static boolean isSpecialItem(ItemStack item) {
        if (item == null) return false;
        return rpgItemsHook.isEnabled() && rpgItemsHook.isRPGItem(item);
    }
}
