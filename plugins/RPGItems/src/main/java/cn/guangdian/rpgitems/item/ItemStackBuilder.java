package cn.guangdian.rpgitems.item;

import cn.guangdian.rpgitems.template.ItemTemplate;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * 物品构建器
 * 用于自定义物品生成
 */
public class ItemStackBuilder {

    private final ItemTemplate template;
    private final ItemFactory factory;
    private Consumer<ItemStack> modifier;

    public ItemStackBuilder(ItemTemplate template, ItemFactory factory) {
        this.template = template;
        this.factory = factory;
    }

    /**
     * 添加自定义修改器
     */
    public ItemStackBuilder modify(Consumer<ItemStack> modifier) {
        this.modifier = modifier;
        return this;
    }

    /**
     * 构建物品
     */
    public ItemStack build() {
        ItemStack item = factory.createItem(template);

        if (modifier != null) {
            modifier.accept(item);
        }

        return item;
    }

    /**
     * 构建指定数量的物品
     */
    public ItemStack build(int amount) {
        ItemStack item = build();
        item.setAmount(amount);
        return item;
    }
}
