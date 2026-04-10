package cn.guangdian.decompose.hook;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

public class MythicMobsHook {

    private Object mythicBukkitInstance;
    private Method getItemManagerMethod;
    private Method getItemStackMethod;

    public MythicMobsHook() {
        try {
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            mythicBukkitInstance = mythicBukkitClass.getMethod("inst").invoke(null);
            getItemManagerMethod = mythicBukkitClass.getMethod("getItemManager");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getMythicItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        try {
            Object itemManager = getItemManagerMethod.invoke(mythicBukkitInstance);
            Method getItemMethod = itemManager.getClass().getMethod("getItem", ItemStack.class);
            Object optionalItem = getItemMethod.invoke(itemManager, item);

            if (optionalItem != null) {
                Method isPresentMethod = optionalItem.getClass().getMethod("isPresent");
                boolean isPresent = (boolean) isPresentMethod.invoke(optionalItem);

                if (isPresent) {
                    Method getMethod = optionalItem.getClass().getMethod("get");
                    Object mythicItem = getMethod.invoke(optionalItem);
                    Method getInternalNameMethod = mythicItem.getClass().getMethod("getInternalName");
                    return (String) getInternalNameMethod.invoke(mythicItem);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public ItemStack getMythicItem(String itemId, int amount) {
        try {
            Object itemManager = getItemManagerMethod.invoke(mythicBukkitInstance);
            Method getItemStackMethod = itemManager.getClass().getMethod("getItemStack", String.class);
            ItemStack item = (ItemStack) getItemStackMethod.invoke(itemManager, itemId);

            if (item != null) {
                item.setAmount(amount);
            }
            return item;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isMythicItem(ItemStack item) {
        return getMythicItemId(item) != null;
    }
}
