package cn.guangdian.accessory.hook;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Level;

public class MythicMobsHook {
    
    private static MythicMobsHook instance;
    private boolean enabled = false;
    private boolean initialized = false;
    private Object itemManager;
    private Method getItemStackMethod;
    
    private MythicMobsHook() {}
    
    public static MythicMobsHook getInstance() {
        if (instance == null) {
            instance = new MythicMobsHook();
        }
        return instance;
    }
    
    public synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        
        Plugin plugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (plugin == null || !plugin.isEnabled()) {
            Bukkit.getLogger().info("[GuangDianAccessory] MythicMobs 未安装，饰品将无法加载");
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
            Bukkit.getLogger().info("[GuangDianAccessory] MythicMobs 物品集成已启用");
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[GuangDianAccessory] MythicMobs 物品集成失败: " + e.getMessage());
        }
    }
    
    public synchronized boolean tryInit() {
        if (!initialized) {
            init();
        }
        return enabled;
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
}
