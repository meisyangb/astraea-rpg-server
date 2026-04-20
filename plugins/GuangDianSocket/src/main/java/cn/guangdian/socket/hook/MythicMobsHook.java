package cn.guangdian.socket.hook;

import org.bukkit.inventory.ItemStack;

/**
 * MythicMobs 钩子
 * 运行时动态检测，避免编译依赖
 */
public class MythicMobsHook {

    private static MythicMobsHook instance;
    private final boolean enabled;

    public MythicMobsHook() {
        instance = this;
        this.enabled = isMythicMobsAvailable();
    }

    public static MythicMobsHook getInstance() {
        if (instance == null) {
            instance = new MythicMobsHook();
        }
        return instance;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private boolean isMythicMobsAvailable() {
        try {
            Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public ItemStack getMythicItem(String itemId) {
        if (!enabled) return null;
        try {
            Object mythicPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("MythicMobs");
            if (mythicPlugin == null) return null;

            // 使用反射调用 MythicMobs API
            Object itemManager = mythicPlugin.getClass().getMethod("getItemManager").invoke(mythicPlugin);
            Object optional = itemManager.getClass().getMethod("getItem", String.class).invoke(itemManager, itemId);

            if (optional instanceof java.util.Optional && ((java.util.Optional<?>) optional).isPresent()) {
                Object mythicItem = ((java.util.Optional<?>) optional).get();
                Object itemStack = mythicItem.getClass().getMethod("generateItemStack", int.class).invoke(mythicItem, 1);
                // 转换为 Bukkit ItemStack
                return (ItemStack) itemStack;
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return null;
    }

    public boolean isMythicItem(ItemStack item, String itemId) {
        if (!enabled || item == null) return false;
        try {
            Object mythicPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("MythicMobs");
            if (mythicPlugin == null) return false;

            Object itemManager = mythicPlugin.getClass().getMethod("getItemManager").invoke(mythicPlugin);
            String type = (String) itemManager.getClass().getMethod("getMythicTypeFromItem", ItemStack.class).invoke(itemManager, item);
            return itemId.equalsIgnoreCase(type);
        } catch (Exception e) {
            return false;
        }
    }

    public String getMythicType(ItemStack item) {
        if (!enabled || item == null) return null;
        try {
            Object mythicPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("MythicMobs");
            if (mythicPlugin == null) return null;

            Object itemManager = mythicPlugin.getClass().getMethod("getItemManager").invoke(mythicPlugin);
            return (String) itemManager.getClass().getMethod("getMythicTypeFromItem", ItemStack.class).invoke(itemManager, item);
        } catch (Exception e) {
            return null;
        }
    }
}
