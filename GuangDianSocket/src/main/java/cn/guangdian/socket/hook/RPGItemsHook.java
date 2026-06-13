package cn.guangdian.socket.hook;

import cn.guangdian.rpgitems.RPGItems;
import cn.guangdian.rpgitems.api.ItemAttributeAPI;
import cn.guangdian.rpgitems.item.ItemFactory;
import cn.guangdian.rpgitems.registry.ItemRegistry;
import cn.guangdian.rpgitems.template.ItemTemplate;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * RPGItems 钩子
 * 直接调用 RPGItems API，无需反射
 * 
 * 修复：延迟初始化，确保 RPGItems 插件完全加载后再初始化
 */
public class RPGItemsHook {

    private static RPGItemsHook instance;
    private boolean enabled = false;
    private boolean initialized = false;
    private ItemRegistry itemRegistry;
    private ItemFactory itemFactory;
    private ItemAttributeAPI attributeAPI;

    public RPGItemsHook() {
        instance = this;
    }

    public static RPGItemsHook getInstance() {
        if (instance == null) {
            instance = new RPGItemsHook();
        }
        return instance;
    }

    public boolean isEnabled() {
        // 延迟初始化：每次调用时检查是否需要初始化
        if (!initialized) {
            init();
        }
        return enabled;
    }

    private void init() {
        if (initialized) return;
        initialized = true;
        
        try {
            // 使用 Bukkit 插件管理器获取 RPGItems 插件
            org.bukkit.plugin.Plugin rpgPlugin = Bukkit.getPluginManager().getPlugin("RPGItems");
            Bukkit.getLogger().info("[GuangDianSocket] init: getPlugin 结果=" + (rpgPlugin != null ? rpgPlugin.getName() : "null"));
            Bukkit.getLogger().info("[GuangDianSocket] init: getPlugin class=" + (rpgPlugin != null ? rpgPlugin.getClass().getName() : "null"));
            Bukkit.getLogger().info("[GuangDianSocket] init: is RPGItems instanceof=" + (rpgPlugin instanceof RPGItems));
            Bukkit.getLogger().info("[GuangDianSocket] init: RPGItems isEnabled=" + (rpgPlugin != null && rpgPlugin.isEnabled()));
            
            if (rpgPlugin == null) {
                Bukkit.getLogger().warning("[GuangDianSocket] RPGItems 插件未找到 (getPlugin返回null)");
                return;
            }
            
            if (!rpgPlugin.isEnabled()) {
                Bukkit.getLogger().warning("[GuangDianSocket] RPGItems 插件未启用");
                return;
            }
            
            if (!(rpgPlugin instanceof RPGItems)) {
                Bukkit.getLogger().warning("[GuangDianSocket] RPGItems 插件类型不匹配: " + rpgPlugin.getClass().getName());
                return;
            }
            
            RPGItems rpgItems = (RPGItems) rpgPlugin;
            
            itemRegistry = rpgItems.getItemRegistry();
            itemFactory = rpgItems.getItemFactory();
            attributeAPI = rpgItems.getAttributeAPI();

            if (itemRegistry != null && itemFactory != null && attributeAPI != null) {
                enabled = true;
                Bukkit.getLogger().info("[GuangDianSocket] RPGItems 钩子初始化成功");
            } else {
                Bukkit.getLogger().warning("[GuangDianSocket] RPGItems 组件为空: registry=" + itemRegistry + ", factory=" + itemFactory + ", api=" + attributeAPI);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[GuangDianSocket] RPGItems 初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从 RPGItems 获取物品
     */
    public ItemStack getRPGItem(String itemId) {
        return getRPGItem(itemId, 1);
    }

    /**
     * 从 RPGItems 获取物品
     */
    public ItemStack getRPGItem(String itemId, int amount) {
        if (!enabled || itemRegistry == null || itemFactory == null) {
            return null;
        }

        try {
            Optional<ItemTemplate> templateOpt = itemRegistry.getItem(itemId);
            if (templateOpt.isPresent()) {
                ItemStack item = itemFactory.createItem(templateOpt.get());
                if (item != null) {
                    item.setAmount(amount);
                }
                return item;
            }
            return null;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[GuangDianSocket] 获取RPGItems物品失败: " + itemId + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 检查物品是否是 RPGItems 物品
     */
    public boolean isRPGItem(ItemStack item, String itemId) {
        if (!enabled || item == null || attributeAPI == null) return false;
        String id = attributeAPI.getItemId(item);
        return itemId.equalsIgnoreCase(id);
    }

    /**
     * 获取物品的 RPGItems ID
     */
    public String getRPGItemId(ItemStack item) {
        if (!enabled || item == null || attributeAPI == null) return null;
        return attributeAPI.getItemId(item);
    }

    /**
     * 检查物品是否是 RPGItems 物品
     */
    public boolean isRPGItem(ItemStack item) {
        if (!enabled || item == null || attributeAPI == null) return false;
        return attributeAPI.isRPGItem(item);
    }

    /**
     * 获取物品属性 API
     */
    public ItemAttributeAPI getAttributeAPI() {
        return attributeAPI;
    }
}
