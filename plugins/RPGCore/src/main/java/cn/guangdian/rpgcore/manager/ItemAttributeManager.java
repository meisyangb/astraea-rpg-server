package cn.guangdian.rpgcore.manager;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ItemAttributeProvider;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 物品属性管理器
 * 
 * 职责：
 * 1. 管理所有 ItemAttributeProvider
 * 2. 聚合所有插件提供的物品属性
 * 3. 为 RPGItems 提供统一的属性查询接口
 */
public class ItemAttributeManager {

    private final RPGCore plugin;
    private final List<ItemAttributeProvider> providers = new ArrayList<>();

    public ItemAttributeManager(RPGCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 注册属性提供者
     */
    public void registerProvider(ItemAttributeProvider provider) {
        providers.removeIf(p -> p.getProviderName().equals(provider.getProviderName()));
        providers.add(provider);
        plugin.getLogger().info("[ItemAttributeManager] 注册属性提供者: " + provider.getProviderName());
    }

    /**
     * 注销属性提供者
     */
    public void unregisterProvider(String providerName) {
        providers.removeIf(p -> p.getProviderName().equals(providerName));
        plugin.getLogger().info("[ItemAttributeManager] 注销属性提供者: " + providerName);
    }

    /**
     * 获取物品的所有聚合属性
     * 
     * @param item 物品
     * @param player 持有者
     * @return 聚合后的属性映射
     */
    public Map<String, Double> getAllAttributes(ItemStack item, Player player) {
        Map<String, Double> allAttributes = new HashMap<>();

        for (ItemAttributeProvider provider : providers) {
            try {
                Map<String, Double> attrs = provider.getItemAttributes(item, player);
                if (attrs != null) {
                    // 合并属性（相同属性累加）
                    attrs.forEach((key, value) -> 
                        allAttributes.merge(key, value, Double::sum)
                    );
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[ItemAttributeManager] 提供者 " + provider.getProviderName() + " 获取属性失败: " + e.getMessage());
            }
        }

        return allAttributes;
    }

    /**
     * 获取物品的所有属性描述（用于Lore）
     */
    public List<String> getAllAttributeLore(ItemStack item) {
        List<String> allLore = new ArrayList<>();

        for (ItemAttributeProvider provider : providers) {
            try {
                if (provider.hasAttributes(item)) {
                    List<String> lore = provider.getAttributeLore(item);
                    if (lore != null && !lore.isEmpty()) {
                        allLore.addAll(lore);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[ItemAttributeManager] 提供者 " + provider.getProviderName() + " 获取Lore失败: " + e.getMessage());
            }
        }

        return allLore;
    }

    /**
     * 获取所有已注册的提供者
     */
    public List<ItemAttributeProvider> getProviders() {
        return new ArrayList<>(providers);
    }
}
