package cn.guangdian.classsystem.gui;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.manager.AttributeManager;
import cn.guangdian.classsystem.model.AttributeType;
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

public class AttributeGUI implements InventoryHolder {
    
    private static final int GUI_SIZE = 54;
    private static final String GUI_TITLE = "属性加点";
    
    private final GuangDianClass plugin;
    private final AttributeManager attributeManager;
    private final Player player;
    private Inventory inventory;
    
    public AttributeGUI(GuangDianClass plugin, AttributeManager attributeManager, Player player) {
        this.plugin = plugin;
        this.attributeManager = attributeManager;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, GUI_SIZE, Component.text(GUI_TITLE));
        setupItems();
    }
    
    private void setupItems() {
        inventory.clear();
        
        int availablePoints = attributeManager.getAvailablePoints(player);
        
        ItemStack infoItem = createInfoItem(availablePoints);
        inventory.setItem(4, infoItem);
        
        int[] attributeSlots = {10, 12, 14, 16, 31};
        AttributeType[] types = AttributeType.values();
        
        for (int i = 0; i < types.length && i < attributeSlots.length; i++) {
            AttributeType type = types[i];
            int allocated = attributeManager.getAllocatedPoints(player, type);
            ItemStack attrItem = createAttributeItem(type, allocated, availablePoints > 0);
            inventory.setItem(attributeSlots[i], attrItem);
        }
        
        ItemStack resetItem = createResetItem();
        inventory.setItem(49, resetItem);
        
        fillEmptySlots();
    }
    
    private ItemStack createInfoItem(int availablePoints) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("属性点信息").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("可用点数: ").color(NamedTextColor.GRAY)
            .append(Component.text(availablePoints).color(NamedTextColor.GREEN)));
        lore.add(Component.text("已分配: ").color(NamedTextColor.GRAY)
            .append(Component.text(attributeManager.getTotalAllocatedPoints(player)).color(NamedTextColor.YELLOW)));
        lore.add(Component.empty());
        
        // 添加魔力值信息
        if (plugin.getManaManager() != null) {
            double currentMana = plugin.getManaManager().getCurrentMana(player);
            double maxMana = plugin.getManaManager().getMaxMana(player);
            lore.add(Component.text("魔力值: ").color(NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(String.format("%.1f", currentMana)).color(NamedTextColor.AQUA))
                .append(Component.text(" / ").color(NamedTextColor.GRAY))
                .append(Component.text(String.format("%.1f", maxMana)).color(NamedTextColor.AQUA)));
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("点击属性图标进行加点").color(NamedTextColor.AQUA));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createAttributeItem(AttributeType type, int allocated, boolean canAllocate) {
        Material material = getMaterialForType(type);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text(type.getDisplayName()).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(type.getDescription()).color(NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("当前点数: ").color(NamedTextColor.GRAY)
            .append(Component.text(allocated).color(NamedTextColor.GREEN)));
        
        AttributeManager.AttributeBonus bonus = calculateBonusForType(type);
        if (bonus.health > 0) {
            lore.add(Component.text("  生命: +" + (int)bonus.health).color(NamedTextColor.RED));
        }
        if (bonus.attack > 0) {
            lore.add(Component.text("  攻击: +" + bonus.attack).color(NamedTextColor.BLUE));
        }
        if (bonus.defense > 0) {
            lore.add(Component.text("  防御: +" + bonus.defense).color(NamedTextColor.GREEN));
        }
        if (bonus.critChance > 0) {
            lore.add(Component.text("  暴击率: +" + bonus.critChance + "%").color(NamedTextColor.GOLD));
        }
        if (bonus.critDamage > 0) {
            lore.add(Component.text("  暴击伤害: +" + bonus.critDamage + "%").color(NamedTextColor.GOLD));
        }
        if (bonus.dodge > 0) {
            lore.add(Component.text("  闪避: +" + bonus.dodge + "%").color(NamedTextColor.AQUA));
        }
        if (bonus.mana > 0) {
            lore.add(Component.text("  魔力: +" + (int)bonus.mana).color(NamedTextColor.LIGHT_PURPLE));
        }
        
        lore.add(Component.empty());
        if (canAllocate) {
            lore.add(Component.text("[左键] +1点").color(NamedTextColor.GREEN));
            lore.add(Component.text("[右键] -1点").color(NamedTextColor.RED));
            lore.add(Component.text("[Shift+左键] +10点").color(NamedTextColor.YELLOW));
        } else {
            lore.add(Component.text("无可用点数").color(NamedTextColor.RED));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createResetItem() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("重置属性点").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("点击重置所有已分配的属性点").color(NamedTextColor.GRAY));
        lore.add(Component.text("所有点数将返还").color(NamedTextColor.YELLOW));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private Material getMaterialForType(AttributeType type) {
        return switch (type) {
            case STRENGTH -> Material.IRON_SWORD;
            case VITALITY -> Material.GOLDEN_APPLE;
            case AGILITY -> Material.FEATHER;
            case INTELLIGENCE -> Material.BOOK;
            case LUCK -> Material.EMERALD;
        };
    }
    
    private AttributeManager.AttributeBonus calculateBonusForType(AttributeType type) {
        AttributeManager.AttributeBonus bonus = new AttributeManager.AttributeBonus();
        int allocated = attributeManager.getAllocatedPoints(player, type);
        if (allocated == 0) return bonus;
        
        AttributeManager.AttributeConfig config = getAttributeConfig(type);
        if (config == null) return bonus;
        
        bonus.health = config.healthPerPoint * allocated;
        bonus.attack = config.attackPerPoint * allocated;
        bonus.defense = config.defensePerPoint * allocated;
        bonus.critChance = config.critChancePerPoint * allocated;
        bonus.critDamage = config.critDamagePerPoint * allocated;
        bonus.dodge = config.dodgePerPoint * allocated;
        bonus.mana = config.manaPerPoint * allocated;
        
        return bonus;
    }
    
    private AttributeManager.AttributeConfig getAttributeConfig(AttributeType type) {
        try {
            var field = AttributeManager.class.getDeclaredField("attributeConfigs");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<AttributeType, AttributeManager.AttributeConfig> configs = 
                (java.util.Map<AttributeType, AttributeManager.AttributeConfig>) field.get(attributeManager);
            return configs.get(type);
        } catch (Exception e) {
            return null;
        }
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
    
    public void handleClick(int slot, boolean isLeftClick, boolean isShiftClick) {
        int[] attributeSlots = {10, 12, 14, 16, 31};
        AttributeType[] types = AttributeType.values();
        
        for (int i = 0; i < attributeSlots.length && i < types.length; i++) {
            if (slot == attributeSlots[i]) {
                AttributeType type = types[i];
                int points = isShiftClick ? 10 : 1;
                
                if (isLeftClick) {
                    if (attributeManager.allocateAttribute(player, type, points)) {
                        player.sendMessage(Component.text("成功分配 " + points + " 点到 " + type.getDisplayName())
                            .color(NamedTextColor.GREEN));
                    } else {
                        player.sendMessage(Component.text("属性点不足！").color(NamedTextColor.RED));
                    }
                } else {
                    if (attributeManager.deallocateAttribute(player, type, points)) {
                        player.sendMessage(Component.text("成功回收 " + points + " 点从 " + type.getDisplayName())
                            .color(NamedTextColor.YELLOW));
                    } else {
                        player.sendMessage(Component.text("该属性点数不足！").color(NamedTextColor.RED));
                    }
                }
                
                setupItems();
                player.openInventory(inventory);
                return;
            }
        }
        
        if (slot == 49) {
            attributeManager.resetAttributes(player);
            player.sendMessage(Component.text("已重置所有属性点！").color(NamedTextColor.GOLD));
            setupItems();
            player.openInventory(inventory);
        }
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    public void open() {
        setupItems();
        player.openInventory(inventory);
    }
}
