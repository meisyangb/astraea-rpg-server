package cn.guangdian.villagertrade.mythic;

import cn.guangdian.villagertrade.GuangDianVillagerTrade;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * MythicMobs物品管理器
 *
 * <p>提供从MythicMobs获取物品的功能（使用反射API）</p>
 */
public class MythicItemManager {

    private final GuangDianVillagerTrade plugin;
    private boolean mythicMobsEnabled = false;
    private Object itemManager;
    private Method getItemStackMethod;
    private Method getItemMethod;

    public MythicItemManager(GuangDianVillagerTrade plugin) {
        this.plugin = plugin;
        checkMythicMobs();
    }

    /**
     * 检查MythicMobs是否可用
     */
    private void checkMythicMobs() {
        try {
            if (plugin.getServer().getPluginManager().getPlugin("MythicMobs") != null) {
                // 使用反射获取 MythicBukkit 实例
                Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
                Method instMethod = mythicBukkitClass.getMethod("inst");
                Object mythicBukkitInst = instMethod.invoke(null);

                // 获取 ItemManager
                Method getItemManagerMethod = mythicBukkitClass.getMethod("getItemManager");
                itemManager = getItemManagerMethod.invoke(mythicBukkitInst);

                // 缓存方法
                getItemStackMethod = itemManager.getClass().getMethod("getItemStack", String.class);
                getItemMethod = itemManager.getClass().getMethod("getItem", String.class);

                mythicMobsEnabled = true;
                plugin.getLogger().info("MythicMobs 已集成，支持Mythic物品兑换");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("MythicMobs 集成失败: " + e.getMessage());
            mythicMobsEnabled = false;
        }
    }

    /**
     * 获取MythicMobs物品
     *
     * @param itemId Mythic物品ID
     * @param amount 数量
     * @return 物品堆，如果找不到则返回null
     */
    public ItemStack getMythicItem(String itemId, int amount) {
        if (!mythicMobsEnabled || itemId == null || itemManager == null || getItemStackMethod == null) {
            return null;
        }

        try {
            // 使用反射调用 getItemStack(String) 方法
            ItemStack itemStack = (ItemStack) getItemStackMethod.invoke(itemManager, itemId);
            if (itemStack != null) {
                itemStack.setAmount(amount);
                return itemStack;
            }
            plugin.getLogger().warning("无法加载Mythic物品: " + itemId);
        } catch (Exception e) {
            plugin.getLogger().warning("获取Mythic物品失败: " + itemId + " - " + e.getMessage());
        }

        return null;
    }

    /**
     * 检查是否为有效的Mythic物品
     *
     * @param itemId Mythic物品ID
     * @return 是否有效
     */
    public boolean isValidMythicItem(String itemId) {
        if (!mythicMobsEnabled || itemId == null || itemManager == null || getItemMethod == null) {
            return false;
        }

        try {
            Object optional = getItemMethod.invoke(itemManager, itemId);
            if (optional == null) {
                return false;
            }
            // 调用 Optional.isPresent()
            Method isPresentMethod = optional.getClass().getMethod("isPresent");
            return (Boolean) isPresentMethod.invoke(optional);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取物品的Mythic类型
     *
     * @param item 物品
     * @return Mythic类型，如果不是Mythic物品则返回null
     */
    public String getMythicType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        // 从NBT标签读取
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey("mythicmobs", "type");
        String type = item.getItemMeta().getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);

        return type;
    }

    /**
     * 检查物品是否匹配指定的Mythic类型
     *
     * @param item 物品
     * @param mythicType Mythic类型
     * @return 是否匹配
     */
    public boolean matchesMythicType(ItemStack item, String mythicType) {
        if (item == null || mythicType == null) {
            return false;
        }

        String itemType = getMythicType(item);
        return mythicType.equals(itemType);
    }

    /**
     * MythicMobs是否可用
     *
     * @return 是否可用
     */
    public boolean isMythicMobsEnabled() {
        return mythicMobsEnabled;
    }
}
