package cn.guangdian.classsystem.gui;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.manager.ClassManager;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;

import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ClassSelectionGUI implements InventoryHolder {
    
    private static final int GUI_SIZE = 54;
    private static final String GUI_TITLE = "职业选择";
    
    private final GuangDianClass plugin;
    private final ClassManager classManager;
    private final Player player;
    private Inventory inventory;
    
    public ClassSelectionGUI(GuangDianClass plugin, ClassManager classManager, Player player) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, GUI_SIZE, Component.text(GUI_TITLE));
        setupItems();
    }
    
    private void setupItems() {
        inventory.clear();
        
        List<GameClass> baseClasses = classManager.getBaseClasses();
        
        int[] slots = {10, 12, 14, 16, 31, 33, 35, 39, 41, 43};
        
        for (int i = 0; i < baseClasses.size() && i < slots.length; i++) {
            GameClass gameClass = baseClasses.get(i);
            ItemStack item = createClassItem(gameClass);
            inventory.setItem(slots[i], item);
        }
        
        ItemStack infoItem = createInfoItem();
        inventory.setItem(4, infoItem);
        
        fillEmptySlots();
    }
    
    private ItemStack createClassItem(GameClass gameClass) {
        Material material = getMaterialForClass(gameClass);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text(gameClass.getName())
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(gameClass.getDescription()).color(NamedTextColor.GRAY));
        lore.add(Component.empty());
        
        lore.add(Component.text("职业属性:").color(NamedTextColor.YELLOW));
        Map<String, Double> stats = gameClass.getStats();
        Double health = stats.get("health");
        if (health != null) {
            lore.add(Component.text("  生命: " + health.intValue())
                .color(NamedTextColor.RED));
        }
        Double attack = stats.get("attack");
        if (attack != null) {
            lore.add(Component.text("  攻击: " + attack.intValue())
                .color(NamedTextColor.BLUE));
        }
        Double defense = stats.get("defense");
        if (defense != null) {
            lore.add(Component.text("  防御: " + defense.intValue())
                .color(NamedTextColor.GREEN));
        }
        Double mana = stats.get("mana");
        if (mana != null) {
            lore.add(Component.text("  魔力: " + mana.intValue())
                .color(NamedTextColor.LIGHT_PURPLE));
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("[点击选择此职业]").color(NamedTextColor.GREEN));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("职业选择指南")
            .color(NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("选择一个职业开始你的冒险之旅").color(NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("每个职业有独特的:").color(NamedTextColor.AQUA));
        lore.add(Component.text("  - 基础属性加成").color(NamedTextColor.WHITE));
        lore.add(Component.text("  - 专属技能").color(NamedTextColor.WHITE));
        lore.add(Component.text("  - 转职路线").color(NamedTextColor.WHITE));
        lore.add(Component.empty());
        lore.add(Component.text("选择后可通过转职进阶").color(NamedTextColor.GOLD));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private Material getMaterialForClass(GameClass gameClass) {
        String id = gameClass.getId().toLowerCase();
        return switch (id) {
            case "warrior" -> Material.IRON_SWORD;
            case "mage" -> Material.BLAZE_ROD;
            case "archer" -> Material.BOW;
            case "assassin" -> Material.IRON_SWORD;
            case "priest" -> Material.GOLDEN_APPLE;
            default -> Material.NETHER_STAR;
        };
    }
    
    private void fillEmptySlots() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.text(" "));
        filler.setItemMeta(meta);
        
        for (int i = 0; i < GUI_SIZE; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }
    
    public void handleClick(int slot) {
        int[] slots = {10, 12, 14, 16, 31, 33, 35, 39, 41, 43};
        List<GameClass> baseClasses = classManager.getBaseClasses();
        
        for (int i = 0; i < slots.length && i < baseClasses.size(); i++) {
            if (slot == slots[i]) {
                GameClass gameClass = baseClasses.get(i);
                player.closeInventory();
                player.performCommand("class choose " + gameClass.getId());
                return;
            }
        }
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    public void open() {
        player.openInventory(inventory);
    }
}
