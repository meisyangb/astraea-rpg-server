package cn.guangdian.forge.hook;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MythicMobs物品集成Hook
 * 支持材料识别和物品获取
 */
public class MythicMobsHook {
    
    private static final Logger logger = Logger.getLogger("GuangDianForge");
    
    private boolean enabled = false;
    private Object itemManager;
    private Method getItemStackMethod;
    private Plugin mythicMobsPlugin;
    
    /**
     * 初始化Hook
     */
    public void init() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (plugin == null || !plugin.isEnabled()) {
            logger.info("[GuangDianForge] MythicMobs 未安装，跳过物品集成");
            return;
        }
        
        this.mythicMobsPlugin = plugin;
        
        try {
            // 使用反射获取MythicBukkit实例和ItemManager
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Method instMethod = mythicBukkitClass.getMethod("inst");
            Object mythicBukkitInst = instMethod.invoke(null);
            
            Method getItemManagerMethod = mythicBukkitClass.getMethod("getItemManager");
            itemManager = getItemManagerMethod.invoke(mythicBukkitInst);
            
            // 缓存获取物品的方法
            getItemStackMethod = itemManager.getClass().getMethod("getItemStack", String.class);
            
            enabled = true;
            logger.info("[GuangDianForge] MythicMobs 物品集成已启用");
        } catch (Exception e) {
            logger.warning("[GuangDianForge] MythicMobs 物品集成失败: " + e.getMessage());
        }
    }
    
    /**
     * 是否已启用
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 判断ItemStack是否为MythicMobs物品
     * @param item 物品
     * @return 是否为MythicMobs物品
     */
    public boolean isMythicItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey typeKey = new NamespacedKey("mythicmobs", "type");
        return meta.getPersistentDataContainer().has(typeKey, PersistentDataType.STRING);
    }
    
    /**
     * 获取MythicMobs物品ID
     * @param item 物品
     * @return 物品ID，如果不是MythicMobs物品返回null
     */
    public String getMythicItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey typeKey = new NamespacedKey("mythicmobs", "type");
        return meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
    }
    
    /**
     * 判断物品是否为指定的MythicMobs物品
     * @param item 物品
     * @param itemId MythicMobs物品ID
     * @return 是否匹配
     */
    public boolean isMythicItem(ItemStack item, String itemId) {
        if (item == null || !item.hasItemMeta()) return false;
        
        String storedId = getMythicItemId(item);
        return itemId.equals(storedId);
    }
    
    /**
     * 通过ID获取MythicMobs物品
     * @param itemId 物品ID
     * @return 物品，失败返回null
     */
    public ItemStack getMythicItem(String itemId) {
        return getMythicItem(itemId, 1);
    }
    
    /**
     * 通过ID获取MythicMobs物品（指定数量）
     * @param itemId 物品ID
     * @param amount 数量
     * @return 物品，失败返回null
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
            logger.warning("[GuangDianForge] 获取MythicMobs物品失败: " + itemId + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析物品配置字符串
     * 格式: "vanilla:DIAMOND" 或 "mythicmobs:杀戮圣石I"
     * @param configStr 配置字符串
     * @param amount 数量
     * @return 物品，失败返回null
     */
    public ItemStack parseItemConfig(String configStr, int amount) {
        if (configStr == null || configStr.isEmpty()) return null;
        
        String lowerStr = configStr.toLowerCase();
        
        if (lowerStr.startsWith("mythicmobs:") || lowerStr.startsWith("mm:")) {
            // MythicMobs物品
            String itemId = configStr.substring(configStr.indexOf(':') + 1);
            return getMythicItem(itemId, amount);
        } else if (lowerStr.startsWith("vanilla:") || lowerStr.startsWith("minecraft:")) {
            // 原版物品
            String materialName = configStr.substring(configStr.indexOf(':') + 1).toUpperCase();
            try {
                org.bukkit.Material material = org.bukkit.Material.valueOf(materialName);
                return new ItemStack(material, amount);
            } catch (IllegalArgumentException e) {
                logger.warning("[GuangDianForge] 无效的原版材料: " + materialName);
                return null;
            }
        } else {
            // 默认尝试解析为原版材料
            try {
                org.bukkit.Material material = org.bukkit.Material.valueOf(configStr.toUpperCase());
                return new ItemStack(material, amount);
            } catch (IllegalArgumentException e) {
                // 尝试作为MythicMobs物品
                return getMythicItem(configStr, amount);
            }
        }
    }
    
    /**
     * 解析物品配置字符串（不带数量）
     * @param configStr 配置字符串
     * @return 物品，失败返回null
     */
    public ItemStack parseItemConfig(String configStr) {
        return parseItemConfig(configStr, 1);
    }
    
    /**
     * 检查玩家物品是否匹配配置
     * @param playerItem 玩家物品
     * @param configStr 配置字符串
     * @return 是否匹配
     */
    public boolean matchesConfig(ItemStack playerItem, String configStr) {
        if (playerItem == null || configStr == null) return false;
        
        String lowerStr = configStr.toLowerCase();
        
        if (lowerStr.startsWith("mythicmobs:") || lowerStr.startsWith("mm:")) {
            // MythicMobs物品匹配
            String itemId = configStr.substring(configStr.indexOf(':') + 1);
            return isMythicItem(playerItem, itemId);
        } else if (lowerStr.startsWith("vanilla:") || lowerStr.startsWith("minecraft:")) {
            // 原版物品匹配
            String materialName = configStr.substring(configStr.indexOf(':') + 1).toUpperCase();
            try {
                org.bukkit.Material material = org.bukkit.Material.valueOf(materialName);
                return playerItem.getType() == material;
            } catch (IllegalArgumentException e) {
                return false;
            }
        } else {
            // 默认尝试作为原版材料匹配
            try {
                org.bukkit.Material material = org.bukkit.Material.valueOf(configStr.toUpperCase());
                return playerItem.getType() == material;
            } catch (IllegalArgumentException e) {
                // 尝试作为MythicMobs物品匹配
                return isMythicItem(playerItem, configStr);
            }
        }
    }
    
    /**
     * 获取物品显示名称（用于日志和提示）
     * @param configStr 配置字符串
     * @return 显示名称
     */
    public String getItemDisplayName(String configStr) {
        if (configStr == null) return "未知";

        String lowerStr = configStr.toLowerCase();

        if (lowerStr.startsWith("mythicmobs:") || lowerStr.startsWith("mm:")) {
            String itemId = configStr.substring(configStr.indexOf(':') + 1);
            return "<light_purple>" + itemId + " <gray>(MythicMobs)";
        } else if (lowerStr.startsWith("vanilla:") || lowerStr.startsWith("minecraft:")) {
            String materialName = configStr.substring(configStr.indexOf(':') + 1);
            return "<yellow>" + materialName + " <gray>(原版)";
        } else {
            return "<yellow>" + configStr;
        }
    }
}