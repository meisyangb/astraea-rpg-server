package cn.guangdian.rpgitems.api;

import cn.guangdian.rpgitems.attribute.ItemAttributes;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 物品属性 API 接口
 * 提供统一的接口获取物品属性，供其他插件调用
 */
public interface ItemAttributeAPI {

    /**
     * 获取物品的所有属性
     *
     * @param item 物品
     * @return 属性映射，如果不是 RPGItems 物品则返回空 Map
     */
    Map<String, Double> getAttributes(ItemStack item);

    /**
     * 获取物品的结构化属性对象
     *
     * @param item 物品
     * @return 属性对象，如果不是 RPGItems 物品则返回 null
     */
    ItemAttributes getItemAttributes(ItemStack item);

    /**
     * 获取物品的装备等级
     *
     * @param item 物品
     * @return 装备等级，如果不是 RPGItems 物品则返回 0
     */
    int getLevel(ItemStack item);

    /**
     * 获取物品需要的职业
     *
     * @param item 物品
     * @return 职业名称，如果不是 RPGItems 物品则返回空字符串
     */
    String getRequiredClass(ItemStack item);

    /**
     * 检查是否是 RPGItems 物品
     *
     * @param item 物品
     * @return 是否是 RPGItems 物品
     */
    boolean isRPGItem(ItemStack item);

    /**
     * 获取物品 ID
     *
     * @param item 物品
     * @return 物品 ID，如果不是 RPGItems 物品则返回 null
     */
    String getItemId(ItemStack item);

    /**
     * 检查玩家是否可以装备指定物品
     * 检查物品的职业限制和等级限制
     *
     * @param playerClassId 玩家职业ID
     * @param playerTier    玩家阶位
     * @param item          物品
     * @return 是否可以装备
     */
    boolean canEquip(String playerClassId, int playerTier, ItemStack item);
}
