package cn.guangdian.forge.gui;

import cn.guangdian.forge.GuangDianForge;
import cn.guangdian.forge.model.ForgeRecipe;
import cn.guangdian.forge.model.PlayerForgeData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 图纸选择界面 - 27格（3行）
 * 展示玩家已解锁的图纸列表
 */
public class RecipeSelectGUI implements InventoryHolder {
    private final GuangDianForge plugin;
    private final Player player;
    private final Inventory inventory;
    
    private static final int SIZE = 27;
    private int pageIndex = 0;
    
    public RecipeSelectGUI(GuangDianForge plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, SIZE, 
            Component.text("锻造图纸", NamedTextColor.GOLD));
    }
    
    public void open() {
        fillBackground();
        loadRecipes();
        setNavigation();
        player.openInventory(inventory);
    }
    
    private void fillBackground() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.empty());
        glass.setItemMeta(meta);
        
        // 边框填充（第1行和第3行）
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, glass);
            inventory.setItem(18 + i, glass);
        }
        inventory.setItem(9, glass);
        inventory.setItem(17, glass);
    }
    
    private void loadRecipes() {
        PlayerForgeData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        List<ForgeRecipe> learned = new ArrayList<>();
        
        for (ForgeRecipe recipe : plugin.getRecipeManager().getAllRecipes()) {
            if (data.hasLearned(recipe.getId())) {
                learned.add(recipe);
            }
        }
        
        // 图纸槽位：10-16（7个）
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        int start = pageIndex * slots.length;
        
        for (int i = 0; i < slots.length; i++) {
            inventory.setItem(slots[i], new ItemStack(Material.AIR));
        }
        
        for (int i = 0; i < slots.length && (start + i) < learned.size(); i++) {
            ForgeRecipe recipe = learned.get(start + i);
            inventory.setItem(slots[i], createRecipeItem(recipe));
        }
    }
    
    private ItemStack createRecipeItem(ForgeRecipe recipe) {
        ItemStack item;
        
        // 所有图纸结果都是 MythicMobs 物品
        cn.guangdian.forge.hook.MythicMobsHook hook = plugin.getMythicMobsHook();
        if (hook != null && hook.isEnabled()) {
            item = hook.getMythicItem(recipe.getResultMythicMobsItem());
            if (item == null) {
                // 获取失败，使用默认材质
                item = new ItemStack(Material.PAPER);
            }
        } else {
            item = new ItemStack(Material.PAPER);
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // 使用Legacy序列化器处理&颜色代码
        meta.displayName(LegacyComponentSerializer.legacyAmpersand()
            .deserialize(recipe.getDisplayName()));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("等级要求: " + recipe.getRequiredForgeLevel(), NamedTextColor.GRAY));
        lore.add(Component.text("基础成功率: " + (int)(recipe.getBaseSuccessRate() * 100) + "%", NamedTextColor.YELLOW));
        lore.add(Component.empty());
        lore.add(Component.text("材料:", NamedTextColor.AQUA));
        
        recipe.getIngredients().forEach((key, amt) -> {
            String displayName = recipe.getIngredientDisplayName(key);
            // 使用Legacy序列化器处理§颜色代码
            lore.add(LegacyComponentSerializer.legacySection().deserialize("  " + displayName + " x" + amt));
        });
        
        lore.add(Component.empty());
        lore.add(Component.text("点击选择", NamedTextColor.GREEN));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private void setNavigation() {
        // 上一页按钮
        ItemStack prevBtn = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prevBtn.getItemMeta();
        prevMeta.displayName(Component.text("上一页", NamedTextColor.YELLOW));
        prevBtn.setItemMeta(prevMeta);
        inventory.setItem(9, prevBtn);
        
        // 下一页按钮
        ItemStack nextBtn = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = nextBtn.getItemMeta();
        nextMeta.displayName(Component.text("下一页", NamedTextColor.YELLOW));
        nextBtn.setItemMeta(nextMeta);
        inventory.setItem(17, nextBtn);
        
        // 页码显示
        ItemStack pageItem = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageItem.getItemMeta();
        pageMeta.displayName(Component.text("第 " + (pageIndex + 1) + " 页", NamedTextColor.WHITE));
        pageItem.setItemMeta(pageMeta);
        inventory.setItem(22, pageItem);
    }
    
    public ForgeRecipe getRecipeAtSlot(int slot) {
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        int start = pageIndex * slots.length;
        
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                PlayerForgeData data = plugin.getPlayerDataManager().get(player.getUniqueId());
                List<ForgeRecipe> learned = new ArrayList<>();
                for (ForgeRecipe recipe : plugin.getRecipeManager().getAllRecipes()) {
                    if (data.hasLearned(recipe.getId())) {
                        learned.add(recipe);
                    }
                }
                if (start + i < learned.size()) {
                    return learned.get(start + i);
                }
            }
        }
        return null;
    }
    
    public void prevPage() {
        if (pageIndex > 0) {
            pageIndex--;
            open();
        }
    }
    
    public void nextPage() {
        PlayerForgeData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        int count = 0;
        for (ForgeRecipe recipe : plugin.getRecipeManager().getAllRecipes()) {
            if (data.hasLearned(recipe.getId())) count++;
        }
        int maxPage = (count - 1) / 7;
        if (pageIndex < maxPage) {
            pageIndex++;
            open();
        }
    }
    
    public boolean isRecipeSlot(int slot) {
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        for (int s : slots) if (s == slot) return true;
        return false;
    }
    
    public boolean isPrevButton(int slot) { return slot == 9; }
    public boolean isNextButton(int slot) { return slot == 17; }
    
    @Override
    public Inventory getInventory() { return inventory; }
    public Player getPlayer() { return player; }
    public GuangDianForge getPlugin() { return plugin; }
}