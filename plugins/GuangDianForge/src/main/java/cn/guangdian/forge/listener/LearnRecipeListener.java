package cn.guangdian.forge.listener;

import cn.guangdian.forge.GuangDianForge;
import cn.guangdian.forge.model.ForgeRecipe;
import cn.guangdian.forge.model.PlayerForgeData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * 图纸学习监听器
 */
public class LearnRecipeListener implements Listener {
    private final GuangDianForge plugin;

    public LearnRecipeListener(GuangDianForge plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && 
            event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        ItemStack item = event.getItem();
        if (item == null) return;
        
        // 支持书本和普通纸张
        if (item.getType() != Material.WRITTEN_BOOK && 
            item.getType() != Material.WRITABLE_BOOK &&
            item.getType() != Material.PAPER) return;
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, "forge_recipe_id");
        String recipeId = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        
        if (recipeId == null) return;
        
        event.setCancelled(true);
        
        Player player = event.getPlayer();
        PlayerForgeData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        ForgeRecipe recipe = plugin.getRecipeManager().getRecipe(recipeId);
        
        if (recipe == null) {
            player.sendMessage(Component.text("图纸无效!", NamedTextColor.RED));
            return;
        }
        
        if (data.hasLearned(recipeId)) {
            player.sendMessage(Component.text("你已经学会了这个图纸!", NamedTextColor.YELLOW));
            return;
        }
        
        if (data.getForgeLevel() < recipe.getRequiredForgeLevel()) {
            player.sendMessage(Component.text("需要锻造等级 " + recipe.getRequiredForgeLevel(), NamedTextColor.RED));
            return;
        }
        
        // 学习图纸
        data.learnRecipe(recipeId);
        item.setAmount(item.getAmount() - 1);
        
        // 使用 LegacyComponentSerializer 解析颜色代码
        String displayName = recipe.getBlueprintDisplay();
        Component displayComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(displayName);
        player.sendMessage(Component.text("成功学会图纸: ", NamedTextColor.GREEN).append(displayComponent));
        plugin.getPlayerDataManager().save(data);
    }

    /**
     * 创建图纸物品（书本或纸张形式）
     * 只设置物品的名称和 lore，右键直接学习
     */
    public static ItemStack createRecipeBook(ForgeRecipe recipe, GuangDianForge plugin) {
        // 检查是否配置为书本形式
        boolean isBook = recipe.isBlueprintBook();
        Material material = isBook ? Material.WRITTEN_BOOK : Material.PAPER;
        
        ItemStack item = new ItemStack(material);
        
        // 获取 lore（如果配置中没有，会自动生成）
        List<String> loreLines = recipe.getBlueprintLore();
        
        ItemMeta meta = item.getItemMeta();
        
        // 设置物品显示名称
        String displayName = recipe.getBlueprintDisplay();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(displayName));
        
        // 设置物品 lore（自动生成的完整描述）
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
        }
        meta.lore(lore);
        
        // 如果是书本，设置必要的书本属性（但不设置书页内容）
        if (isBook && meta instanceof BookMeta bookMeta) {
            bookMeta.title(LegacyComponentSerializer.legacyAmpersand().deserialize(displayName));
            bookMeta.author(Component.text("锻造大师", NamedTextColor.DARK_PURPLE));
        }
        
        // 存储图纸ID到PDC
        NamespacedKey key = new NamespacedKey(plugin, "forge_recipe_id");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, recipe.getId());
        
        item.setItemMeta(meta);
        
        return item;
    }
}