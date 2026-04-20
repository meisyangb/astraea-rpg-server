package cn.guangdian.classsystem.gui;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.api.ClassService;
import cn.guangdian.classsystem.model.AttributeEffect;
import cn.guangdian.classsystem.model.AttributeType;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import cn.guangdian.rpgcore.gui.GUI;
import cn.guangdian.rpgcore.gui.GUIBuilder;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ClassAttributeGUI {
    
    private final GuangDianClass plugin;
    private final ClassService classService;
    private final MiniMessageService msg;
    
    public ClassAttributeGUI(GuangDianClass plugin, ClassService classService) {
        this.plugin = plugin;
        this.classService = classService;
        this.msg = MiniMessageService.getInstance();
    }
    
    public void open(Player player) {
        PlayerClassData data = classService.getPlayerData(player);
        if (data == null) {
            player.sendMessage(msg.red("无法获取玩家数据！"));
            return;
        }
        
        GameClass gameClass = classService.getClass(data.getClassId());
        if (gameClass == null) {
            player.sendMessage(msg.red("无法获取职业数据！"));
            return;
        }
        
        List<AttributeType> availableAttrs = gameClass.getAvailableAttributes();
        if (availableAttrs.isEmpty()) {
            player.sendMessage(msg.red("当前职业没有可分配的属性！"));
            return;
        }
        
        int availablePoints = data.getAvailableAttributePoints();
        int usedPoints = data.getUsedAttributePoints();
        
        String title = gameClass.getName() + " - 属性加点";
        GUIBuilder builder = GUIBuilder.create(title, 6);
        
        builder.setItem(4, createInfoItem(gameClass, availablePoints, usedPoints));
        
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        
        for (int i = 0; i < availableAttrs.size() && i < slots.length; i++) {
            AttributeType type = availableAttrs.get(i);
            int allocated = data.getAllocatedAttribute(type);
            AttributeEffect effect = gameClass.getAttributeEffect(type);
            
            ItemStack item = createAttributeItem(type, allocated, effect, availablePoints > 0);
            int slot = slots[i];
            
            builder.setItem(slot, item, (event) -> handleClick(event, player, type));
        }
        
        builder.setItem(49, createResetItem(), (event) -> {
            classService.resetAttributes(player);
            player.sendMessage(msg.gold("已重置所有属性点！"));
            open(player);
        });
        
        ItemStack filler = createFiller();
        builder.setFillerItem(filler);
        
        GUI gui = builder.build();
        gui.open(player);
    }
    
    private void handleClick(InventoryClickEvent event, Player player, AttributeType type) {
        boolean isLeftClick = event.isLeftClick();
        boolean isShiftClick = event.isShiftClick();
        int points = isShiftClick ? 10 : 1;
        
        if (isLeftClick) {
            if (classService.allocateAttribute(player, type, points)) {
                player.sendMessage(msg.green("成功分配 " + points + " 点到 " + type.getDisplayName()));
            } else {
                player.sendMessage(msg.red("属性点不足或该属性不可用！"));
            }
        } else {
            if (classService.deallocateAttribute(player, type, points)) {
                player.sendMessage(msg.yellow("成功回收 " + points + " 点从 " + type.getDisplayName()));
            } else {
                player.sendMessage(msg.red("该属性点数不足！"));
            }
        }
        
        open(player);
    }
    
    private ItemStack createInfoItem(GameClass gameClass, int availablePoints, int usedPoints) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text(gameClass.getName() + " 属性点信息")
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("职业类型: ").color(NamedTextColor.GRAY)
            .append(Component.text(gameClass.getClassType()).color(NamedTextColor.YELLOW)));
        lore.add(Component.text("可用点数: ").color(NamedTextColor.GRAY)
            .append(Component.text(availablePoints).color(NamedTextColor.GREEN)));
        lore.add(Component.text("已分配: ").color(NamedTextColor.GRAY)
            .append(Component.text(usedPoints).color(NamedTextColor.YELLOW)));
        lore.add(Component.empty());
        lore.add(Component.text("左键 +1点 | 右键 -1点").color(NamedTextColor.AQUA));
        lore.add(Component.text("Shift+左键 +10点 | Shift+右键 -10点").color(NamedTextColor.AQUA));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createAttributeItem(AttributeType type, int allocated, 
                                          AttributeEffect effect, boolean canAllocate) {
        ItemStack item = new ItemStack(type.getIcon());
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text(type.getDisplayName())
            .color(NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(type.getDescription()).color(NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("当前点数: ").color(NamedTextColor.GRAY)
            .append(Component.text(allocated).color(NamedTextColor.GREEN)));
        
        if (effect != null && allocated > 0) {
            lore.add(Component.empty());
            lore.add(Component.text("当前加成:").color(NamedTextColor.GOLD));
            for (String desc : effect.getEffectDescriptions(allocated)) {
                lore.add(Component.text("  " + desc).color(NamedTextColor.WHITE));
            }
        }
        
        if (effect != null && allocated == 0) {
            lore.add(Component.empty());
            lore.add(Component.text("每点加成:").color(NamedTextColor.GOLD));
            for (String desc : effect.getEffectDescriptions(1)) {
                lore.add(Component.text("  " + desc).color(NamedTextColor.WHITE));
            }
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
        
        meta.displayName(Component.text("重置属性点")
            .color(NamedTextColor.RED)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("点击重置所有已分配的属性点").color(NamedTextColor.GRAY));
        lore.add(Component.text("所有点数将返还").color(NamedTextColor.YELLOW));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createFiller() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.text(" "));
        filler.setItemMeta(meta);
        return filler;
    }
}
