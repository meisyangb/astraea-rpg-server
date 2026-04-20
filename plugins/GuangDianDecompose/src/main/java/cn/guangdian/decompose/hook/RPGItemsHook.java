package cn.guangdian.decompose.hook;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

/**
 * RPGItems 钩子
 * 运行时动态检测，避免编译依赖
 */
public class RPGItemsHook {

    private static RPGItemsHook instance;
    private final boolean enabled;

    public RPGItemsHook() {
        instance = this;
        this.enabled = isRPGItemsAvailable();
    }

    public static RPGItemsHook getInstance() {
        if (instance == null) {
            instance = new RPGItemsHook();
        }
        return instance;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private boolean isRPGItemsAvailable() {
        try {
            Class.forName("cn.guangdian.rpgitems.RPGItems");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * 获取 RPGItems 物品
     *
     * @param itemId 物品ID
     * @return 物品堆
     */
    public ItemStack getRPGItem(String itemId) {
        if (!enabled) return null;
        try {
            Object rpgItemsPlugin = Bukkit.getPluginManager().getPlugin("RPGItems");
            if (rpgItemsPlugin == null) return null;

            Object api = rpgItemsPlugin.getClass().getMethod("getAPI").invoke(rpgItemsPlugin);
            Object optional = api.getClass().getMethod("getItem", String.class).invoke(api, itemId);

            if (optional instanceof java.util.Optional && ((java.util.Optional<?>) optional).isPresent()) {
                return (ItemStack) ((java.util.Optional<?>) optional).get();
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return null;
    }

    /**
     * 获取 RPGItems 物品（指定数量）
     *
     * @param itemId 物品ID
     * @param amount 数量
     * @return 物品堆
     */
    public ItemStack getRPGItem(String itemId, int amount) {
        ItemStack item = getRPGItem(itemId);
        if (item != null) {
            item.setAmount(amount);
        }
        return item;
    }

    /**
     * 检查物品是否是 RPGItems 物品
     *
     * @param item 物品
     * @param itemId 物品ID
     * @return 是否匹配
     */
    public boolean isRPGItem(ItemStack item, String itemId) {
        if (!enabled || item == null) return false;
        try {
            Object rpgItemsPlugin = Bukkit.getPluginManager().getPlugin("RPGItems");
            if (rpgItemsPlugin == null) return false;

            Object api = rpgItemsPlugin.getClass().getMethod("getAPI").invoke(rpgItemsPlugin);
            Object optional = api.getClass().getMethod("getItemId", ItemStack.class).invoke(api, item);

            if (optional instanceof java.util.Optional && ((java.util.Optional<?>) optional).isPresent()) {
                String id = (String) ((java.util.Optional<?>) optional).get();
                return itemId.equalsIgnoreCase(id);
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /**
     * 获取 RPGItems 物品的 ID
     *
     * @param item 物品
     * @return 物品ID
     */
    public String getRPGItemId(ItemStack item) {
        if (!enabled || item == null) return null;
        try {
            Object rpgItemsPlugin = Bukkit.getPluginManager().getPlugin("RPGItems");
            if (rpgItemsPlugin == null) return null;

            Object api = rpgItemsPlugin.getClass().getMethod("getAPI").invoke(rpgItemsPlugin);
            Object optional = api.getClass().getMethod("getItemId", ItemStack.class).invoke(api, item);

            if (optional instanceof java.util.Optional && ((java.util.Optional<?>) optional).isPresent()) {
                return (String) ((java.util.Optional<?>) optional).get();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * 检查物品是否是 RPGItems 物品（不检查具体ID）
     *
     * @param item 物品
     * @return 是否是RPG物品
     */
    public boolean isRPGItem(ItemStack item) {
        if (!enabled || item == null) return false;
        try {
            Object rpgItemsPlugin = Bukkit.getPluginManager().getPlugin("RPGItems");
            if (rpgItemsPlugin == null) return false;

            Object api = rpgItemsPlugin.getClass().getMethod("getAPI").invoke(rpgItemsPlugin);
            Boolean result = (Boolean) api.getClass().getMethod("isRPGItem", ItemStack.class).invoke(api, item);
            return result != null && result;
        } catch (Exception e) {
            return false;
        }
    }
}
