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

public class ClassAdvanceGUI implements InventoryHolder {
    
    private static final int GUI_SIZE = 54;
    private static final String GUI_TITLE = "职业转职";
    
    private final GuangDianClass plugin;
    private final ClassManager classManager;
    private final Player player;
    private final PlayerClassData playerData;
    private Inventory inventory;
    
    public ClassAdvanceGUI(GuangDianClass plugin, ClassManager classManager, Player player, PlayerClassData playerData) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.player = player;
        this.playerData = playerData;
        this.inventory = Bukkit.createInventory(this, GUI_SIZE, Component.text(GUI_TITLE));
        setupItems();
    }
    
    private void setupItems() {
        inventory.clear();
        
        GameClass currentClass = classManager.getClass(playerData.getClassId());
        if (currentClass != null) {
            ItemStack currentItem = createCurrentClassItem(currentClass);
            inventory.setItem(4, currentItem);
        }
        
        List<GameClass> availableClasses = classManager.getAvailableClasses(playerData);
        
        int[] slots = {10, 12, 14, 16, 28, 30, 32, 34};
        
        for (int i = 0; i < availableClasses.size() && i < slots.length; i++) {
            GameClass gameClass = availableClasses.get(i);
            ItemStack item = createAdvanceItem(gameClass);
            inventory.setItem(slots[i], item);
        }
        
        if (availableClasses.isEmpty()) {
            ItemStack noAdvance = createNoAdvanceItem();
            inventory.setItem(22, noAdvance);
        }
        
        fillEmptySlots();
    }
    
    private ItemStack createCurrentClassItem(GameClass gameClass) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("当前职业: " + gameClass.getName())
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("阶位: " + playerData.getTier() + "阶").color(NamedTextColor.YELLOW));
        lore.add(Component.text("转职: " + playerData.getAdvancementName()).color(NamedTextColor.AQUA));
        lore.add(Component.empty());
        lore.add(Component.text("经验: " + playerData.getExp()).color(NamedTextColor.GREEN));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createAdvanceItem(GameClass gameClass) {
        Material material = getMaterialForClass(gameClass);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text(gameClass.getName())
            .color(NamedTextColor.LIGHT_PURPLE)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(gameClass.getDescription()).color(NamedTextColor.GRAY));
        lore.add(Component.empty());
        
        lore.add(Component.text("转职等级: " + gameClass.getAdvancementName())
            .color(NamedTextColor.YELLOW));
        
        lore.add(Component.text("需要阶位: " + gameClass.getTier() + "阶")
            .color(NamedTextColor.AQUA));
        
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
        
        if (gameClass.getAttributePoints() > 0) {
            lore.add(Component.empty());
            lore.add(Component.text("属性点奖励: " + gameClass.getAttributePoints() + " 点")
                .color(NamedTextColor.GOLD));
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("[点击进行转职]").color(NamedTextColor.GREEN));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createNoAdvanceItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("无法转职")
            .color(NamedTextColor.RED)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("当前没有可转职的职业").color(NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("可能原因:").color(NamedTextColor.YELLOW));
        lore.add(Component.text("  - 阶位不足").color(NamedTextColor.GRAY));
        lore.add(Component.text("  - 已达最高转职等级").color(NamedTextColor.GRAY));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private Material getMaterialForClass(GameClass gameClass) {
        String id = gameClass.getId().toLowerCase();
        if (id.contains("knight") || id.contains("paladin")) return Material.IRON_CHESTPLATE;
        if (id.contains("mage") || id.contains("elementalist") || id.contains("archmage") || id.contains("sage")) 
            return Material.BLAZE_ROD;
        if (id.contains("archer") || id.contains("ranger") || id.contains("sniper") || id.contains("hunter")) 
            return Material.BOW;
        if (id.contains("assassin") || id.contains("shadow") || id.contains("reaper")) 
            return Material.IRON_SWORD;
        if (id.contains("priest") || id.contains("cleric") || id.contains("saint")) 
            return Material.GOLDEN_APPLE;
        return Material.NETHER_STAR;
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
        int[] slots = {10, 12, 14, 16, 28, 30, 32, 34};
        List<GameClass> availableClasses = classManager.getAvailableClasses(playerData);
        
        for (int i = 0; i < slots.length && i < availableClasses.size(); i++) {
            if (slot == slots[i]) {
                GameClass gameClass = availableClasses.get(i);
                player.closeInventory();
                player.performCommand("class advance " + gameClass.getId());
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
