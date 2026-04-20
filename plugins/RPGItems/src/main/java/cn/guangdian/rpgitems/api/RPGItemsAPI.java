package cn.guangdian.rpgitems.api;

import cn.guangdian.rpgitems.item.ItemStackBuilder;
import cn.guangdian.rpgitems.service.ItemService;
import cn.guangdian.rpgitems.template.ItemTemplate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * RPGItems 对外 API
 */
public class RPGItemsAPI {

    private final ItemService itemService;

    public RPGItemsAPI(ItemService itemService) {
        this.itemService = itemService;
    }

    /**
     * 获取物品
     *
     * @param itemId 物品ID
     * @return 物品堆
     */
    public Optional<ItemStack> getItem(String itemId) {
        return itemService.createItem(itemId);
    }

    /**
     * 获取物品（指定数量）
     *
     * @param itemId 物品ID
     * @param amount 数量
     * @return 物品堆
     */
    public Optional<ItemStack> getItem(String itemId, int amount) {
        Optional<ItemStack> item = itemService.createItem(itemId);
        item.ifPresent(i -> i.setAmount(amount));
        return item;
    }

    /**
     * 给予玩家物品
     *
     * @param player 玩家
     * @param itemId 物品ID
     * @param amount 数量
     * @return 是否成功
     */
    public boolean giveItem(Player player, String itemId, int amount) {
        return itemService.giveItem(player, itemId, amount);
    }

    /**
     * 获取物品模板
     *
     * @param itemId 物品ID
     * @return 物品模板
     */
    public Optional<ItemTemplate> getTemplate(String itemId) {
        return itemService.getTemplate(itemId);
    }

    /**
     * 检查物品是否是RPG物品
     *
     * @param item 物品
     * @return 是否是RPG物品
     */
    public boolean isRPGItem(ItemStack item) {
        return itemService.isRPGItem(item);
    }

    /**
     * 获取物品的RPG ID
     *
     * @param item 物品
     * @return RPG ID
     */
    public Optional<String> getItemId(ItemStack item) {
        return itemService.getItemId(item);
    }

    /**
     * 创建物品构建器
     *
     * @param itemId 物品ID
     * @return 物品构建器
     */
    public Optional<ItemStackBuilder> createBuilder(String itemId) {
        return itemService.createBuilder(itemId);
    }
}
