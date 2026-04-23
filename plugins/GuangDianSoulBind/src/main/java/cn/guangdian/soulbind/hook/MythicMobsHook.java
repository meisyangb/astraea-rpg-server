package cn.guangdian.soulbind.hook;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;
import java.util.logging.Logger;

public class MythicMobsHook {

    private static final Logger logger = Logger.getLogger("GuangDianSoulBind");
    private Object mythicBukkitInstance;
    private Method getItemManagerMethod;
    private static final NamespacedKey MYTHIC_TYPE_KEY = new NamespacedKey("mythicmobs", "type");
    private boolean enabled = false;
    private boolean initialized = false;

    public void init() {
        if (initialized) return;
        initialized = true;
        try {
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            mythicBukkitInstance = mythicBukkitClass.getMethod("inst").invoke(null);
            getItemManagerMethod = mythicBukkitClass.getMethod("getItemManager");
            enabled = true;
        } catch (Exception e) {
            logger.warning("[GuangDianSoulBind] MythicMobs not found, using PDC fallback");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public MythicMobsHook() {
        try {
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            mythicBukkitInstance = mythicBukkitClass.getMethod("inst").invoke(null);
            getItemManagerMethod = mythicBukkitClass.getMethod("getItemManager");
            enabled = true;
        } catch (Exception e) {
            logger.warning("[GuangDianSoulBind] MythicMobs not found, using PDC fallback");
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
            logger.warning("[GuangDianSoulBind] Failed to get MythicMobs item: " + itemId);
            e.printStackTrace();
        }
        return null;
    }

    public boolean isMythicItem(ItemStack item) {
        return getMythicItemId(item) != null;
    }
}
