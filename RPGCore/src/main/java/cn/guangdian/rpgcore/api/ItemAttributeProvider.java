package cn.guangdian.rpgcore.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 物品属性提供者接口
 * 
 * 职责：为 RPGItems 提供额外的物品属性
 * 
 * 实现者：
 * - GuangDianSocket: 提供宝石镶嵌属性
 * - GuangDianForge: 提供锻造强化属性
 * - 其他插件: 提供附魔、精炼等属性
 */
public interface ItemAttributeProvider {

    /**
     * 获取提供者名称
     */
    String getProviderName();

    /**
     * 获取物品额外属性
     * 
     * @param item 物品
     * @param player 持有者（可能为null）
     * @return 属性映射 <属性名, 属性值>
     */
    Map<String, Double> getItemAttributes(ItemStack item, Player player);

    /**
     * 获取物品属性描述（用于Lore显示）
     * 
     * @param item 物品
     * @return 描述列表
     */
    java.util.List<String> getAttributeLore(ItemStack item);

    /**
     * 检查物品是否有该提供者管理的属性
     */
    boolean hasAttributes(ItemStack item);
}
