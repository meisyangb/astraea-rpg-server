package cn.guangdian.classsystem.gui;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.gui.GUI;
import cn.guangdian.rpgcore.gui.GUIBuilder;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public abstract class ClassGUI {

    protected final GuangDianClass plugin;
    protected Player player;
    protected final MiniMessageService msg;
    protected GUI gui;

    protected static final Material BORDER_MATERIAL = Material.BLACK_STAINED_GLASS_PANE;
    protected static final Material CLOSE_MATERIAL = Material.BARRIER;
    protected static final Material BACK_MATERIAL = Material.ARROW;

    public ClassGUI(GuangDianClass plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.msg = MiniMessageService.getInstance();
    }

    public abstract void open();

    protected abstract void build();

    public void refresh() {
        close();
        open();
    }

    public void close() {
        if (gui != null) {
            gui.close(player);
        }
    }

    protected void applyBorder(GUIBuilder builder, int rows) {
        ItemStack border = createItem(BORDER_MATERIAL, " ");
        for (int i = 0; i < 9; i++) {
            builder.setItem(i, border);
            builder.setItem((rows - 1) * 9 + i, border);
        }
        for (int i = 1; i < rows - 1; i++) {
            builder.setItem(i * 9, border);
            builder.setItem(i * 9 + 8, border);
        }
    }

    protected ItemStack createItem(Material material, String name, String... lore) {
        return createItem(material, name, lore != null ? Arrays.asList(lore) : null);
    }

    protected ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(msg.colorize(name)
                .decoration(TextDecoration.ITALIC, false));
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream().map(msg::colorize).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack createCloseItem() {
        return createItem(CLOSE_MATERIAL, "<red>关闭", "<gray>点击关闭界面");
    }

    protected ItemStack createBackItem(String destination) {
        return createItem(BACK_MATERIAL, "<yellow>返回", "<gray>返回到: " + destination);
    }

    protected Material getClassMaterial(String classType) {
        return switch (classType.toUpperCase()) {
            case "WARRIOR" -> Material.IRON_SWORD;
            case "MAGE" -> Material.BLAZE_ROD;
            case "ARCHER" -> Material.BOW;
            case "ASSASSIN" -> Material.NETHERITE_SWORD;
            case "PRIEST" -> Material.TOTEM_OF_UNDYING;
            default -> Material.BOOK;
        };
    }

    protected String getClassTypeName(String classType) {
        return switch (classType.toUpperCase()) {
            case "WARRIOR" -> "战士系";
            case "MAGE" -> "法师系";
            case "ARCHER" -> "弓箭手系";
            case "ASSASSIN" -> "刺客系";
            case "PRIEST" -> "牧师系";
            default -> "未知";
        };
    }

    protected void playClickSound() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getSoundService().playSound(player, "CLICK", 0.5f, 1.0f);
        }
    }

    protected void playSuccessSound() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getSoundService().playSound(player, "SUCCESS", 0.5f, 1.0f);
        }
    }

    protected void playErrorSound() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getSoundService().playSound(player, "ERROR", 0.5f, 1.0f);
        }
    }
}
