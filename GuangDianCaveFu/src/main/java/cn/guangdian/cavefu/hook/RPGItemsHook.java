package cn.guangdian.cavefu.hook;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * RPGItems 物品钩子
 * 用于获取和识别 RPGItems 物品
 */
public class RPGItemsHook {

    private Object rpgItemsPlugin;
    // 使用与 RPGItems ItemFactory 相同的 PDC 键
    private static final NamespacedKey RPGITEMS_KEY = new NamespacedKey("rpgitems", "id");

    public RPGItemsHook() {
        try {
            // RPGItems 插件实例
            rpgItemsPlugin = Bukkit.getPluginManager().getPlugin("RPGItems");
            if (rpgItemsPlugin != null) {
                Bukkit.getLogger().info("[GuangDianCaveFu] 已连接到 RPGItems 插件");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[GuangDianCaveFu] RPGItems not found, using PDC fallback");
        }
    }

    /**
     * 获取物品的 RPGItems ID
     * @param item 物品
     * @return RPGItems 物品ID，如果不是 RPGItems 物品则返回 null
     */
    public String getRPGItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();

        // 通过 PDC 获取 (RPGItems 使用 "rpgitems:id" 键)
        String pdcId = meta.getPersistentDataContainer().get(RPGITEMS_KEY, PersistentDataType.STRING);
        return pdcId;
    }

    /**
     * 获取 RPGItems 物品
     * @param itemId 物品ID
     * @param amount 数量
     * @return 物品堆，如果获取失败返回 null
     */
    public ItemStack getRPGItem(String itemId, int amount) {
        if (rpgItemsPlugin == null) {
            Bukkit.getLogger().warning("[GuangDianCaveFu] RPGItems 插件未加载，无法获取物品: " + itemId);
            return null;
        }

        try {
            // 使用 RPGItems API 获取物品
            Class<?> rpgItemsClass = rpgItemsPlugin.getClass();
            Method getItemService = rpgItemsClass.getMethod("getItemService");
            Object itemService = getItemService.invoke(rpgItemsPlugin);

            if (itemService != null) {
                // 调用 createItem(String itemId) 方法
                Method createItem = itemService.getClass().getMethod("createItem", String.class);
                Object result = createItem.invoke(itemService, itemId);

                if (result instanceof Optional) {
                    Optional<?> optional = (Optional<?>) result;
                    if (optional.isPresent()) {
                        Object itemObj = optional.get();
                        if (itemObj instanceof ItemStack) {
                            ItemStack item = (ItemStack) itemObj;
                            item.setAmount(amount);
                            return item;
                        }
                    }
                }
            }

        } catch (Exception e) {
            Bukkit.getLogger().warning("[GuangDianCaveFu] 获取 RPGItems 物品失败: " + itemId);
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 检查物品是否是 RPGItems 物品
     * @param item 物品
     * @return 是否是 RPGItems 物品
     */
    public boolean isRPGItem(ItemStack item) {
        return getRPGItemId(item) != null;
    }
}
