package cn.guangdian.decompose.hook;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;

public class MythicMobsHook {

    private Object mythicBukkitInstance;
    private Method getItemManagerMethod;
    private static final NamespacedKey MYTHIC_TYPE_KEY = new NamespacedKey("mythicmobs", "type");

    public MythicMobsHook() {
        try {
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            mythicBukkitInstance = mythicBukkitClass.getMethod("inst").invoke(null);
            getItemManagerMethod = mythicBukkitClass.getMethod("getItemManager");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[GuangDianDecompose] MythicMobs not found, using PDC fallback");
        }
    }

    public String getMythicItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        
        String pdcId = meta.getPersistentDataContainer().get(MYTHIC_TYPE_KEY, PersistentDataType.STRING);
        if (pdcId != null) {
            return pdcId;
        }

        return null;
    }

    public ItemStack getMythicItem(String itemId, int amount) {
        if (mythicBukkitInstance == null || getItemManagerMethod == null) {
            return null;
        }

        try {
            Object itemManager = getItemManagerMethod.invoke(mythicBukkitInstance);
            Method getItemStackMethod = itemManager.getClass().getMethod("getItemStack", String.class);
            ItemStack item = (ItemStack) getItemStackMethod.invoke(itemManager, itemId);

            if (item != null) {
                item.setAmount(amount);
            }
            return item;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[GuangDianDecompose] Failed to get MythicMobs item: " + itemId);
            e.printStackTrace();
        }
        return null;
    }

    public boolean isMythicItem(ItemStack item) {
        return getMythicItemId(item) != null;
    }
}
