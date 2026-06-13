package cn.guangdian.dungeon.ui;

import cn.guangdian.dungeon.GuangDianDungeon;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractDungeonUI implements InventoryHolder, Listener {

    protected final GuangDianDungeon plugin;
    protected final Player player;
    protected final Inventory inventory;

    public AbstractDungeonUI(GuangDianDungeon plugin, Player player, int size, String title) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, size, plugin.color(title));
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void close() {
        HandlerList.unregisterAll(this);
        player.closeInventory();
    }

    protected abstract void refresh();

    protected abstract void handleClick(int slot);

    protected void playClickSound() {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }

    protected ItemStack createItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.color(name));
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(plugin.color(line));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    protected ItemStack createFillItem() {
        return createFillItem(Material.GRAY_STAINED_GLASS_PANE);
    }

    protected ItemStack createFillItem(Material material) {
        return createItem(material, "<dark_gray>");
    }

    protected ItemStack createBackItem() {
        return createItem(Material.ARROW, "<red>返回", "<gray>点击返回上一页");
    }

    protected ItemStack createCloseItem() {
        return createItem(Material.BARRIER, "<red>关闭", "<gray>点击关闭界面");
    }

    protected void fillRow(int row) {
        int start = row * 9;
        for (int i = start; i < start + 9; i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, createFillItem());
            }
        }
    }

    protected void fillAllEmpty() {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, createFillItem());
            }
        }
    }

    protected void fillRange(int start, int end) {
        for (int i = start; i <= end; i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, createFillItem());
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AbstractDungeonUI)) return;
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        handleClick(slot);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            HandlerList.unregisterAll(this);
        }
    }
}
