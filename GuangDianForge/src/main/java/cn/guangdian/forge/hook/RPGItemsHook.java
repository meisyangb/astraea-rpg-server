package cn.guangdian.forge.hook;

import cn.guangdian.rpgitems.api.ItemAttributeAPI;
import cn.guangdian.rpgitems.service.ItemService;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.logging.Level;

/**
 * RPGItems 物品集成 Hook
 * 支持材料识别和物品获取
 */
public class RPGItemsHook {
    
    private boolean enabled = false;
    private ItemService itemService;
    private ItemAttributeAPI itemAttributeAPI;
    
    /**
     * 初始化 Hook
     */
    public void init() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("RPGItems");
        if (plugin == null || !plugin.isEnabled()) {
            Bukkit.getLogger().info("[GuangDianForge] RPGItems 未安装，跳过物品集成");
            return;
        }
        
        try {
            // 获取 RPGItems 的服务
            // 通过反射获取 RPGItems 实例
            Class<?> rpgItemsClass = Class.forName("cn.guangdian.rpgitems.RPGItems");
            Object rpgItemsInstance = rpgItemsClass.getMethod("getInstance").invoke(null);
            
            // 获取 ItemService
            itemService = (ItemService) rpgItemsClass.getMethod("getItemService").invoke(rpgItemsInstance);
            
            // 获取 ItemAttributeAPI
            itemAttributeAPI = (ItemAttributeAPI) rpgItemsClass.getMethod("getAttributeAPI").invoke(rpgItemsInstance);
            
            enabled = true;
            Bukkit.getLogger().info("[GuangDianForge] RPGItems 物品集成已启用");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[GuangDianForge] RPGItems 物品集成失败: " + e.getMessage());
        }
    }
    
    /**
     * 是否已启用
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 判断 ItemStack 是否为 RPGItems 物品
     * @param item 物品
     * @return 是否为 RPGItems 物品
     */
    public boolean isRPGItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        if (itemAttributeAPI != null) {
            return itemAttributeAPI.isRPGItem(item);
        }
        
        // 降级检查 PDC
        ItemMeta meta = item.getItemMeta();
        NamespacedKey idKey = new NamespacedKey("rpgitems", "id");
        return meta.getPersistentDataContainer().has(idKey, PersistentDataType.STRING);
    }
    
    /**
     * 获取 RPGItems 物品 ID
     * @param item 物品
     * @return 物品 ID，如果不是 RPGItems 物品返回 null
     */
    public String getRPGItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        
        if (itemAttributeAPI != null) {
            return itemAttributeAPI.getItemId(item);
        }
        
        // 降级检查 PDC
        ItemMeta meta = item.getItemMeta();
        NamespacedKey idKey = new NamespacedKey("rpgitems", "id");
        return meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }
    
    /**
     * 判断物品是否为指定的 RPGItems 物品
     * @param item 物品
     * @param itemId RPGItems 物品 ID
     * @return 是否匹配
     */
    public boolean isRPGItem(ItemStack item, String itemId) {
        if (item == null || !item.hasItemMeta()) return false;
        
        String storedId = getRPGItemId(item);
        return itemId.equals(storedId);
    }
    
    /**
     * 通过 ID 获取 RPGItems 物品
     * @param itemId 物品 ID
     * @return 物品，失败返回 null
     */
    public ItemStack getRPGItem(String itemId) {
        return getRPGItem(itemId, 1);
    }
    
    /**
     * 通过 ID 获取 RPGItems 物品（指定数量）
     * @param itemId 物品 ID
     * @param amount 数量
     * @return 物品，失败返回 null
     */
    public ItemStack getRPGItem(String itemId, int amount) {
        if (!enabled || itemService == null) {
            return null;
        }
        
        try {
            Optional<ItemStack> itemOpt = itemService.createItem(itemId);
            if (itemOpt.isPresent()) {
                ItemStack item = itemOpt.get();
                item.setAmount(amount);
                return item;
            }
            return null;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[GuangDianForge] 获取 RPGItems 物品失败: " + itemId + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析物品配置字符串
     * 格式: "vanilla:DIAMOND" 或 "rpgitems:杀戮圣石I" 或 "rpg:杀戮圣石I"
     * @param configStr 配置字符串
     * @param amount 数量
     * @return 物品，失败返回 null
     */
    public ItemStack parseItemConfig(String configStr, int amount) {
        if (configStr == null || configStr.isEmpty()) return null;
        
        String lowerStr = configStr.toLowerCase();
        
        if (lowerStr.startsWith("rpgitems:") || lowerStr.startsWith("rpg:")) {
            // RPGItems 物品
            String itemId = configStr.substring(configStr.indexOf(':') + 1);
            return getRPGItem(itemId, amount);
        } else if (lowerStr.startsWith("vanilla:") || lowerStr.startsWith("minecraft:")) {
            // 原版物品
            String materialName = configStr.substring(configStr.indexOf(':') + 1).toUpperCase();
            try {
                org.bukkit.Material material = org.bukkit.Material.valueOf(materialName);
                return new ItemStack(material, amount);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("[GuangDianForge] 无效的原版材料: " + materialName);
                return null;
            }
        } else {
            // 默认尝试解析为原版材料
            try {
                org.bukkit.Material material = org.bukkit.Material.valueOf(configStr.toUpperCase());
                return new ItemStack(material, amount);
            } catch (IllegalArgumentException e) {
                // 尝试作为 RPGItems 物品
                return getRPGItem(configStr, amount);
            }
        }
    }
    
    /**
     * 解析物品配置字符串（不带数量）
     * @param configStr 配置字符串
     * @return 物品，失败返回 null
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
        
        if (lowerStr.startsWith("rpgitems:") || lowerStr.startsWith("rpg:")) {
            // RPGItems 物品匹配
            String itemId = configStr.substring(configStr.indexOf(':') + 1);
            return isRPGItem(playerItem, itemId);
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
                // 尝试作为 RPGItems 物品匹配
                return isRPGItem(playerItem, configStr);
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

        if (lowerStr.startsWith("rpgitems:") || lowerStr.startsWith("rpg:")) {
            String itemId = configStr.substring(configStr.indexOf(':') + 1);
            return "<light_purple>" + itemId + " <gray>(RPGItems)";
        } else if (lowerStr.startsWith("vanilla:") || lowerStr.startsWith("minecraft:")) {
            String materialName = configStr.substring(configStr.indexOf(':') + 1);
            return "<yellow>" + materialName + " <gray>(原版)";
        } else {
            return "<yellow>" + configStr;
        }
    }

    /**
     * 按倍率更新 RPGItems 物品的所有数值属性（PDC）
     * 委托给 RPGItems 的 ItemAttributeAPI
     *
     * @param item       RPGItems 物品
     * @param multiplier 属性倍率
     */
    public void updateItemAttributes(ItemStack item, double multiplier) {
        if (!enabled || itemAttributeAPI == null) return;
        itemAttributeAPI.updateAttributes(item, multiplier);
    }

    /**
     * 获取 ItemAttributeAPI（供其他模块使用）
     */
    public ItemAttributeAPI getAttributeAPI() {
        return itemAttributeAPI;
    }
}
