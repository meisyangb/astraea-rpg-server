package cn.guangdian.decompose.hook;

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
    // 使用与 RPGItems ItemFactory 相同的 PDC 键
    private static final NamespacedKey RPGITEMS_KEY = new NamespacedKey("rpgitems", "id");
    private static final NamespacedKey TIER_KEY = new NamespacedKey("rpgitems", "tier");

    public RPGItemsHook() {
        try {
            // RPGItems 插件实例
            rpgItemsPlugin = Bukkit.getPluginManager().getPlugin("RPGItems");
            if (rpgItemsPlugin != null) {
                Bukkit.getLogger().info("[GuangDianDecompose] 已连接到 RPGItems 插件");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[GuangDianDecompose] RPGItems not found, using PDC fallback");
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
        if (pdcId != null) {
            Bukkit.getLogger().info("[GuangDianDecompose] 成功读取物品ID: " + pdcId);
            return pdcId;
        }

        Bukkit.getLogger().warning("[GuangDianDecompose] 无法从物品读取 RPGItems ID，PDC键: " + RPGITEMS_KEY);
        return null;
    }

    /**
     * 获取 RPGItems 物品
     * @param itemId 物品ID
     * @param amount 数量
     * @return 物品堆，如果获取失败返回 null
     */
    public ItemStack getRPGItem(String itemId, int amount) {
        Bukkit.getLogger().info("[GuangDianDecompose] 尝试获取物品: " + itemId + " x" + amount);
        
        if (rpgItemsPlugin == null) {
            Bukkit.getLogger().warning("[GuangDianDecompose] RPGItems 插件未加载，无法获取物品: " + itemId);
            return null;
        }

        try {
            // 使用 RPGItems API 获取物品
            // 方式1: 通过 ItemService 获取
            Class<?> rpgItemsClass = rpgItemsPlugin.getClass();
            Bukkit.getLogger().info("[GuangDianDecompose] RPGItems类: " + rpgItemsClass.getName());
            
            Method getItemService = rpgItemsClass.getMethod("getItemService");
            Object itemService = getItemService.invoke(rpgItemsPlugin);
            Bukkit.getLogger().info("[GuangDianDecompose] ItemService: " + (itemService != null ? "已获取" : "null"));

            if (itemService != null) {
                // 调用 createItem(String itemId) 方法
                Method createItem = itemService.getClass().getMethod("createItem", String.class);
                Object result = createItem.invoke(itemService, itemId);
                Bukkit.getLogger().info("[GuangDianDecompose] createItem结果类型: " + (result != null ? result.getClass().getName() : "null"));

                if (result instanceof Optional) {
                    Optional<?> optional = (Optional<?>) result;
                    Bukkit.getLogger().info("[GuangDianDecompose] Optional.isPresent: " + optional.isPresent());
                    
                    if (optional.isPresent()) {
                        Object itemObj = optional.get();
                        Bukkit.getLogger().info("[GuangDianDecompose] Optional内容类型: " + itemObj.getClass().getName());
                        
                        if (itemObj instanceof ItemStack) {
                            ItemStack item = (ItemStack) itemObj;
                            item.setAmount(amount);
                            Bukkit.getLogger().info("[GuangDianDecompose] 成功获取物品: " + itemId + " x" + amount);
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

    /**
     * 检查物品是否是 RPGItems 物品
     * @param item 物品
     * @return 是否是 RPGItems 物品
     */
    public boolean isRPGItem(ItemStack item) {
        return getRPGItemId(item) != null;
    }

    /**
     * 获取物品的阶位
     * @param item 物品
     * @return 阶位字符串 (如: 一阶装备, 二阶装备, 魔王, 天族 等)，如果没有阶位则返回 null
     */
    public String getItemTier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();

        // 通过 PDC 获取阶位
        String tier = meta.getPersistentDataContainer().get(TIER_KEY, PersistentDataType.STRING);
        if (tier != null && !tier.isEmpty()) {
            return tier;
        }

        return null;
    }
}
