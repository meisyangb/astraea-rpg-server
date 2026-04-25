package cn.guangdian.classsystem.gui;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.api.ClassService;
import cn.guangdian.classsystem.manager.ClassManager;
import cn.guangdian.classsystem.model.AttributeType;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import cn.guangdian.rpgcore.gui.GUI;
import cn.guangdian.rpgcore.gui.GUIBuilder;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ClassAttributeGUI extends ClassGUI {

    private final ClassService classService;
    private final ClassManager classManager;

    public ClassAttributeGUI(GuangDianClass plugin, ClassService classService, ClassManager classManager) {
        super(plugin, null);
        this.classService = classService;
        this.classManager = classManager;
    }

    public void open(Player player) {
        this.player = player;
        build();
    }

    @Override
    public void open() {
        throw new UnsupportedOperationException("Use open(Player) instead");
    }

    @Override
    protected void build() {
        PlayerClassData data = classService.getPlayerData(player);
        if (data == null) {
            playErrorSound();
            player.sendMessage(msg.red("无法获取玩家数据！"));
            return;
        }

        GameClass gameClass = classManager.getClass(data.getClassId());
        if (gameClass == null) {
            playErrorSound();
            player.sendMessage(msg.red("无法获取职业数据！"));
            return;
        }

        GUIBuilder builder = GUIBuilder.create("<dark_gray>★ <gold>属性加点 <dark_gray>★", 6);

        applyBorder(builder, 6);

        builder.setItem(4, createInfoItem(data, gameClass));

        List<AttributeType> availableAttributes = gameClass.getAvailableAttributes();
        int[] slots = {20, 22, 24, 30, 32, 34};

        for (int i = 0; i < availableAttributes.size() && i < slots.length; i++) {
            AttributeType type = availableAttributes.get(i);
            int slot = slots[i];

            builder.setItem(slot, createAttributeItem(type, data, gameClass), (event) -> {
                handleClick(type, data, gameClass, event.getClick());
            });
        }

        builder.setItem(49, createBackItem("职业系统"), (event) -> {
            playClickSound();
            plugin.openMainGUI(player);
        });

        GUI gui = builder.build();
        gui.open(player);
    }

    private void handleClick(AttributeType type, PlayerClassData data, GameClass gameClass, ClickType clickType) {
        int amount = 1;
        boolean allocate = true;

        switch (clickType) {
            case LEFT -> {
                amount = 1;
                allocate = true;
            }
            case RIGHT -> {
                amount = 1;
                allocate = false;
            }
            case SHIFT_LEFT -> {
                amount = 10;
                allocate = true;
            }
            case SHIFT_RIGHT -> {
                amount = 10;
                allocate = false;
            }
            default -> {
                return;
            }
        }

        if (allocate) {
            int available = data.getAvailableAttributePoints();
            if (available <= 0) {
                playErrorSound();
                player.sendMessage(msg.red("没有可用的属性点！"));
                return;
            }
            amount = Math.min(amount, available);

            if (classService.allocateAttribute(player, type, amount)) {
                playSuccessSound();
                player.sendMessage(msg.green("成功分配 ").append(msg.white(String.valueOf(amount))).append(msg.green(" 点 ")).append(msg.white(type.getDisplayName())));
            } else {
                playErrorSound();
                player.sendMessage(msg.red("属性点分配失败！"));
            }
        } else {
            int allocated = data.getAllocatedAttribute(type);
            if (allocated <= 0) {
                playErrorSound();
                player.sendMessage(msg.red("该属性没有已分配的点数！"));
                return;
            }
            amount = Math.min(amount, allocated);

            if (classService.deallocateAttribute(player, type, amount)) {
                playSuccessSound();
                player.sendMessage(msg.yellow("成功回收 ").append(msg.white(String.valueOf(amount))).append(msg.yellow(" 点 ")).append(msg.white(type.getDisplayName())));
            } else {
                playErrorSound();
                player.sendMessage(msg.red("属性点回收失败！"));
            }
        }

        refresh();
    }

    private ItemStack createInfoItem(PlayerClassData data, GameClass gameClass) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(msg.gold("属性点分配")
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(msg.gray("可用点数: ").append(msg.green(String.valueOf(data.getAvailableAttributePoints()))));
        lore.add(msg.gray("已分配: ").append(msg.white(String.valueOf(data.getUsedAttributePoints()))));
        lore.add(Component.empty());
        lore.add(msg.yellow("操作说明:"));
        lore.add(msg.gray("左键: +1点"));
        lore.add(msg.gray("右键: -1点"));
        lore.add(msg.gray("Shift+左键: +10点"));
        lore.add(msg.gray("Shift+右键: -10点"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAttributeItem(AttributeType type, PlayerClassData data, GameClass gameClass) {
        Material material = type.getIcon();

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(msg.aqua(type.getDisplayName())
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        int allocated = data.getAllocatedAttribute(type);
        lore.add(msg.gray("已分配: ").append(msg.green(String.valueOf(allocated))));
        lore.add(Component.empty());

        var effect = gameClass.getAttributeEffect(type);
        if (effect != null) {
            lore.add(msg.gold("属性效果:"));
            for (String desc : effect.getEffectDescriptions(allocated)) {
                lore.add(msg.white("  " + desc));
            }
        }

        lore.add(Component.empty());
        lore.add(msg.yellow("左键: +1  右键: -1"));
        lore.add(msg.yellow("Shift+左键: +10  Shift+右键: -10"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
