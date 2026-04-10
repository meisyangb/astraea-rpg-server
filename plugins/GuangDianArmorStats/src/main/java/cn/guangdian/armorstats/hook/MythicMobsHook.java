package cn.guangdian.armorstats.hook;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * MythicMobs物品集成Hook (用于GuangDianArmorStats)
 */
public class MythicMobsHook {
    
    private static MythicMobsHook instance;
    private boolean enabled = false;
    private Object itemManager;
    private Method getItemStackMethod;
    
    private MythicMobsHook() {}
    
    public static MythicMobsHook getInstance() {
        if (instance == null) {
            instance = new MythicMobsHook();
            instance.init();
        }
        return instance;
    }
    
    private void init() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (plugin == null || !plugin.isEnabled()) {
            Bukkit.getLogger().info("[GuangDianArmorStats] MythicMobs 未安装，跳过物品集成");
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
            Bukkit.getLogger().info("[GuangDianArmorStats] MythicMobs 物品集成已启用");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[GuangDianArmorStats] MythicMobs 物品集成失败: " + e.getMessage());
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 判断ItemStack是否为MythicMobs物品
     */
    public boolean isMythicItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        // 新版本使用 mythicmobs:type，旧版本使用 mythicmobs:item
        NamespacedKey typeKey = new NamespacedKey("mythicmobs", "type");
        NamespacedKey itemKey = new NamespacedKey("mythicmobs", "item");
        return meta.getPersistentDataContainer().has(typeKey, PersistentDataType.STRING) ||
               meta.getPersistentDataContainer().has(itemKey, PersistentDataType.STRING);
    }
    
    /**
     * 获取MythicMobs物品ID
     */
    public String getMythicItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        
        ItemMeta meta = item.getItemMeta();
        
        // 优先检查 mythicmobs:type（新版本）
        NamespacedKey typeKey = new NamespacedKey("mythicmobs", "type");
        String typeId = meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        if (typeId != null) return typeId;
        
        // 回退到 mythicmobs:item（旧版本）
        NamespacedKey itemKey = new NamespacedKey("mythicmobs", "item");
        return meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
    }
    
    /**
     * 判断物品是否为指定的MythicMobs物品
     */
    public boolean isMythicItem(ItemStack item, String itemId) {
        if (item == null || !item.hasItemMeta()) return false;
        
        String storedId = getMythicItemId(item);
        return itemId.equals(storedId);
    }
    
    /**
     * 通过ID获取MythicMobs物品
     */
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
    
    /**
     * 检查玩家物品是否匹配配置字符串
     */
    public boolean matchesConfig(ItemStack playerItem, String configStr) {
        if (playerItem == null || configStr == null) return false;
        
        String lowerStr = configStr.toLowerCase();
        
        if (lowerStr.startsWith("mythicmobs:") || lowerStr.startsWith("mm:")) {
            String itemId = configStr.substring(configStr.indexOf(':') + 1);
            return isMythicItem(playerItem, itemId);
        }
        
        return false;
    }
    
    /**
     * 解析配置字符串为物品
     */
    public ItemStack parseItemConfig(String configStr, int amount) {
        if (configStr == null || configStr.isEmpty()) return null;
        
        String lowerStr = configStr.toLowerCase();
        
        if (lowerStr.startsWith("mythicmobs:") || lowerStr.startsWith("mm:")) {
            String itemId = configStr.substring(configStr.indexOf(':') + 1);
            return getMythicItem(itemId, amount);
        }
        
        return null;
    }
}