package cn.guangdian.rpgitems.service;

import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgitems.RPGItems;
import cn.guangdian.rpgitems.item.ItemFactory;
import cn.guangdian.rpgitems.item.ItemStackBuilder;
import cn.guangdian.rpgitems.registry.ItemRegistry;
import cn.guangdian.rpgitems.template.ItemTemplate;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/**
 * 物品服务
 * 核心服务类
 */
public class ItemService {

    private final RPGItems plugin;
    private final ItemRegistry itemRegistry;
    private final ItemFactory itemFactory;
    private final MiniMessageService miniMessage;

    public ItemService(RPGItems plugin, ItemRegistry itemRegistry, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
        this.miniMessage = MiniMessageService.getInstance();
    }

    /**
     * 创建物品
     */
    public Optional<ItemStack> createItem(String itemId) {
        Optional<ItemTemplate> templateOpt = itemRegistry.getItem(itemId);
        if (templateOpt.isEmpty()) {
            return Optional.empty();
        }

        ItemStack item = itemFactory.createItem(templateOpt.get());
        return Optional.of(item);
    }

    /**
     * 给予玩家物品
     */
    public boolean giveItem(Player player, String itemId, int amount) {
        Optional<ItemStack> itemOpt = createItem(itemId);
        if (itemOpt.isEmpty()) {
            player.sendMessage(miniMessage.colorize("<red>物品不存在: " + itemId));
            return false;
        }

        ItemStack item = itemOpt.get();
        item.setAmount(amount);

        // 检查背包空间
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(miniMessage.colorize("<red>背包已满！"));
            return false;
        }

        player.getInventory().addItem(item);
        player.sendMessage(miniMessage.colorize(
                String.format("<green>获得物品: <yellow>%s <gray>x%d", itemId, amount)));
        return true;
    }

    /**
     * 获取物品模板
     */
    public Optional<ItemTemplate> getTemplate(String itemId) {
        return itemRegistry.getItem(itemId);
    }

    /**
     * 检查是否是RPG物品
     */
    public boolean isRPGItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(new NamespacedKey("rpgitems", "id"), PersistentDataType.STRING);
    }

    /**
     * 获取物品的RPG ID
     */
    public Optional<String> getItemId(ItemStack item) {
        if (!isRPGItem(item)) {
            return Optional.empty();
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(new NamespacedKey("rpgitems", "id"), PersistentDataType.STRING);
        return Optional.ofNullable(id);
    }

    /**
     * 创建物品构建器
     */
    public Optional<ItemStackBuilder> createBuilder(String itemId) {
        Optional<ItemTemplate> templateOpt = itemRegistry.getItem(itemId);
        if (templateOpt.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ItemStackBuilder(templateOpt.get(), itemFactory));
    }
}
