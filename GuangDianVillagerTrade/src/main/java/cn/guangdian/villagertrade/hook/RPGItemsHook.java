package cn.guangdian.villagertrade.hook;

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
            Bukkit.getLogger().info("[GuangDianVillagerTrade] RPGItems 未安装，跳过物品集成");
            return;
        }
        
        try {
            // 直接获取 RPGItems 实例和服务
            cn.guangdian.rpgitems.RPGItems rpgItems = cn.guangdian.rpgitems.RPGItems.getInstance();
            
            // 获取 ItemService
            itemService = rpgItems.getItemService();
            
            // 获取 ItemAttributeAPI
            itemAttributeAPI = rpgItems.getAttributeAPI();
            
            enabled = true;
            Bukkit.getLogger().info("[GuangDianVillagerTrade] RPGItems 物品集成已启用");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[GuangDianVillagerTrade] RPGItems 物品集成失败: " + e.getMessage());
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
            Bukkit.getLogger().warning("[GuangDianVillagerTrade] 获取 RPGItems 物品失败: " + itemId + " - " + e.getMessage());
            return null;
        }
    }
}
