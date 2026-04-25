package cn.guangdian.classsystem.gui;

import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public final class GUIUtils {

    private static final MiniMessageService msg = MiniMessageService.getInstance();

    private GUIUtils() {}

    public static ItemStack createItem(Material material, String name) {
        return createItem(material, name, (List<String>) null);
    }

    public static ItemStack createItem(Material material, String name, String... lore) {
        return createItem(material, name, lore != null ? Arrays.asList(lore) : null);
    }

    public static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(msg.colorize(name)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream().map(msg::colorize).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createBorder() {
        return createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
    }

    public static ItemStack createCloseButton() {
        return createItem(Material.BARRIER, "<red>关闭", "<gray>点击关闭界面");
    }

    public static ItemStack createBackButton(String destination) {
        return createItem(Material.ARROW, "<yellow>返回", "<gray>返回到: " + destination);
    }
}
