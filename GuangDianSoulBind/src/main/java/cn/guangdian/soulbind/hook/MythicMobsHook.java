package cn.guangdian.soulbind.hook;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class MythicMobsHook {

    private boolean enabled = false;
    private Object itemManager;
    private Method getItemStackMethod;

    public void init() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (plugin == null || !plugin.isEnabled()) {
            Bukkit.getLogger().info("[GuangDianSoulBind] MythicMobs 未安装，跳过物品集成");
            return;
        }

        try {
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Method instMethod = mythicBukkitClass.getMethod("inst");
            Object mythicBukkitInst = instMethod.invoke(null);

            Method getItemManagerMethod = mythicBukkitClass.getMethod("getItemManager");
            itemManager = getItemManagerMethod.invoke(mythicBukkitInst);

            getItemStackMethod = itemManager.getClass().getMethod("getItemStack", String.class);

            enabled = true;
            Bukkit.getLogger().info("[GuangDianSoulBind] MythicMobs 物品集成已启用");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[GuangDianSoulBind] MythicMobs 物品集成失败: " + e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isMythicItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey typeKey = new NamespacedKey("mythicmobs", "type");
        return meta.getPersistentDataContainer().has(typeKey, PersistentDataType.STRING);
    }

    public String getMythicItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey typeKey = new NamespacedKey("mythicmobs", "type");
        return meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
    }

    public boolean isMythicItem(ItemStack item, String itemId) {
        if (item == null || !item.hasItemMeta()) return false;

        String storedId = getMythicItemId(item);
        return itemId.equals(storedId);
    }

    public ItemStack getMythicItem(String itemId, int amount) {
        if (!enabled || itemManager == null || getItemStackMethod == null) {
            return null;
        }

        try {
            ItemStack item = (ItemStack) getItemStackMethod.invoke(itemManager, itemId);
            if (item != null) {
                item.setAmount(amount);
            }
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    public ItemStack getMythicItem(String itemId) {
        return getMythicItem(itemId, 1);
    }
}
