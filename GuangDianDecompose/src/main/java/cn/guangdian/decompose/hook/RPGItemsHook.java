package cn.guangdian.decompose.hook;

import cn.guangdian.decompose.GuangDianDecompose;
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
    private Method getItemManagerMethod;
    private static final NamespacedKey RPGITEMS_KEY = new NamespacedKey("rpgitems", "id");
    private static final NamespacedKey TIER_KEY = new NamespacedKey("rpgitems", "tier");

    public RPGItemsHook() {
        try {
            rpgItemsPlugin = Bukkit.getPluginManager().getPlugin("RPGItems");
            if (rpgItemsPlugin != null && isDebug()) {
                Bukkit.getLogger().info("[GuangDianDecompose] 已连接到 RPGItems 插件");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[GuangDianDecompose] RPGItems not found, using PDC fallback");
        }
    }

    private boolean isDebug() {
        GuangDianDecompose plugin = GuangDianDecompose.getInstance();
        return plugin != null && plugin.isDebug();
    }

    private void debugLog(String message) {
        if (isDebug()) {
            Bukkit.getLogger().info("[GuangDianDecompose] " + message);
        }
    }

    /**
     * 获取物品的 RPGItems ID
     */
    public String getRPGItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        String pdcId = meta.getPersistentDataContainer().get(RPGITEMS_KEY, PersistentDataType.STRING);
        if (pdcId != null) {
            debugLog("成功读取物品ID: " + pdcId);
            return pdcId;
        }

        Bukkit.getLogger().warning("[GuangDianDecompose] 无法从物品读取 RPGItems ID");
        return null;
    }

    /**
     * 获取 RPGItems 物品
     */
    public ItemStack getRPGItem(String itemId, int amount) {
        debugLog("尝试获取物品: " + itemId + " x" + amount);

        if (rpgItemsPlugin == null) {
            Bukkit.getLogger().warning("[GuangDianDecompose] RPGItems 插件未加载");
            return null;
        }

        try {
            Class<?> rpgItemsClass = rpgItemsPlugin.getClass();
            debugLog("RPGItems类: " + rpgItemsClass.getName());

            Method getItemService = rpgItemsClass.getMethod("getItemService");
            Object itemService = getItemService.invoke(rpgItemsPlugin);
            debugLog("ItemService: " + (itemService != null ? "已获取" : "null"));

            if (itemService != null) {
                Method createItem = itemService.getClass().getMethod("createItem", String.class);
                Object result = createItem.invoke(itemService, itemId);
                debugLog("createItem结果类型: " + (result != null ? result.getClass().getName() : "null"));

                if (result instanceof Optional) {
                    Optional<?> optional = (Optional<?>) result;
                    debugLog("Optional.isPresent: " + optional.isPresent());

                    if (optional.isPresent()) {
                        Object itemObj = optional.get();
                        debugLog("Optional内容类型: " + itemObj.getClass().getName());

                        if (itemObj instanceof ItemStack) {
                            ItemStack item = (ItemStack) itemObj;
                            item.setAmount(amount);
                            debugLog("成功获取物品: " + itemId + " x" + amount);
                            return item;
                        }
                    }
                }
            }

            Bukkit.getLogger().warning("[GuangDianDecompose] 未找到 RPGItems 物品: " + itemId);

        } catch (Exception e) {
            Bukkit.getLogger().warning("[GuangDianDecompose] 获取 RPGItems 物品失败: " + itemId);
            e.printStackTrace();
        }

        return null;
    }

    public boolean isRPGItem(ItemStack item) {
        return getRPGItemId(item) != null;
    }

    /**
     * 获取物品的阶位
     */
    public String getItemTier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        String tier = meta.getPersistentDataContainer().get(TIER_KEY, PersistentDataType.STRING);
        return tier != null && !tier.isEmpty() ? tier : null;
    }
}