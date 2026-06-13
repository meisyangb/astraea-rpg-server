package cn.guangdian.dungeon.ui;

import cn.guangdian.dungeon.GuangDianDungeon;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.function.Consumer;

/**
 * 铁砧输入界面 - 让玩家通过铁砧输入文本
 * 用法：new AnvilInputUI(plugin, player, "输入玩家名", name -> { ... })
 */
public class AnvilInputUI implements InventoryHolder, Listener {

    private final GuangDianDungeon plugin;
    private final Player player;
    private final Inventory inventory;
    private final Consumer<String> callback;
    private boolean closed = false;

    public AnvilInputUI(GuangDianDungeon plugin, Player player, String prompt, Consumer<String> callback) {
        this.plugin = plugin;
        this.player = player;
        this.callback = callback;

        this.inventory = Bukkit.createInventory(this, InventoryType.ANVIL, plugin.color("<dark_gray>" + prompt));

        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        meta.displayName(plugin.color("<white>"));
        paper.setItemMeta(meta);

        inventory.setItem(0, paper);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void close() {
        if (!closed) {
            closed = true;
            HandlerList.unregisterAll(this);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AnvilInputUI)) return;
        if (event.getInventory().getHolder() != this) return;

        InventoryView view = event.getView();
        int rawSlot = event.getRawSlot();

        // 铁砧输出格 (slot 2)
        if (rawSlot == view.convertSlot(rawSlot) && event.getSlotType() == InventoryType.SlotType.RESULT) {
            ItemStack result = event.getCurrentItem();
            if (result != null && result.hasItemMeta()) {
                ItemMeta meta = result.getItemMeta();
                if (meta.hasDisplayName()) {
                    String text = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                        .legacySection().serialize(meta.displayName());
                    if (text != null && !text.isBlank()) {
                        event.setCancelled(true);
                        close();
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.closeInventory();
                            callback.accept(text.trim());
                        });
                        return;
                    }
                }
            }
        }

        // 阻止物品被拿走
        if (rawSlot <= 2) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            close();
        }
    }
}
