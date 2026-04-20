package cn.guangdian.rpgitems.item;

import cn.guangdian.rpgitems.RPGItems;
import cn.guangdian.rpgitems.template.ItemTemplate;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 物品工厂
 * 根据模板创建物品实例
 * Lore驱动模式：Lore直接写入，无需额外生成
 */
public class ItemFactory {

    private final RPGItems plugin;

    public ItemFactory(RPGItems plugin) {
        this.plugin = plugin;
    }

    /**
     * 根据模板创建物品
     */
    public ItemStack createItem(ItemTemplate template) {
        ItemStack item = new ItemStack(template.getMaterial());
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        // 设置显示名称
        meta.displayName(template.getDisplayName());

        // Lore直接写入（使用 Component 格式，Paper 1.21+）
        meta.lore(template.getLore());

        // 设置PDC数据
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey("rpgitems", "id"), PersistentDataType.STRING, template.getId());

        // 绑定技能
        if (!template.getSkillBindings().isEmpty()) {
            String skills = template.getSkillBindings().stream()
                    .map(ItemTemplate.SkillBinding::getSkillId)
                    .collect(Collectors.joining(","));
            pdc.set(new NamespacedKey("rpgitems", "skills"), PersistentDataType.STRING, skills);
        }

        // 应用选项
        ItemTemplate.ItemOptions options = template.getOptions();
        if (options != null) {
            meta.setUnbreakable(options.isUnbreakable());
            if (options.isHideAttributes()) {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            }
            if (options.getCustomModelData() != null) {
                meta.setCustomModelData(options.getCustomModelData());
            }
        }

        // 应用附魔
        for (String enchantStr : template.getEnchantments()) {
            String[] parts = enchantStr.split(":");
            if (parts.length == 2) {
                try {
                    Enchantment enchant = Enchantment.getByName(parts[0].toUpperCase());
                    int level = Integer.parseInt(parts[1]);
                    if (enchant != null) {
                        meta.addEnchant(enchant, level, true);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        item.setItemMeta(meta);
        return item;
    }
}
