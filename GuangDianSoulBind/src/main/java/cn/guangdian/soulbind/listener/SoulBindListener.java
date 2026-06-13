package cn.guangdian.soulbind.listener;

import cn.guangdian.soulbind.GuangDianSoulBind;
import cn.guangdian.soulbind.api.SoulBindService;
import cn.guangdian.soulbind.hook.MythicMobsHook;
import cn.guangdian.soulbind.manager.ConfigManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SoulBindListener implements Listener {

    private final GuangDianSoulBind plugin;
    private final SoulBindService service;
    private final ConfigManager config;
    private final MythicMobsHook mythicMobsHook;

    public SoulBindListener(GuangDianSoulBind plugin) {
        this.plugin = plugin;
        this.service = plugin.getService();
        this.config = plugin.getConfigManager();
        this.mythicMobsHook = plugin.getMythicMobsHook();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!config.isPreventDrop()) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();

        if (service.isBound(item) && !service.isBoundTo(item, player.getUniqueId())) {
            event.setCancelled(true);
            sendMessage(player, "bound-by-other");
            return;
        }

        if (service.isBound(item)) {
            event.setCancelled(true);
            sendMessage(player, "cannot-drop");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        Inventory inventory = event.getClickedInventory();

        if (inventory == null) return;

        if (clickedItem != null && service.isBound(clickedItem)) {
            if (!service.isBoundTo(clickedItem, player.getUniqueId())) {
                event.setCancelled(true);
                sendMessage(player, "bound-by-other");
                return;
            }

            if (config.isPreventContainer() && isContainerInventory(inventory, event.getSlot())) {
                event.setCancelled(true);
                sendMessage(player, "cannot-container");
                return;
            }
        }

        if (cursorItem != null && service.isBound(cursorItem)) {
            if (config.isPreventContainer() && isContainerInventory(inventory, event.getSlot())) {
                event.setCancelled(true);
                sendMessage(player, "cannot-container");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        for (ItemStack item : event.getNewItems().values()) {
            if (service.isBound(item)) {
                if (!service.isBoundTo(item, player.getUniqueId())) {
                    event.setCancelled(true);
                    sendMessage(player, "bound-by-other");
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!config.isKeepOnDeath()) return;

        Player player = event.getEntity();
        List<ItemStack> toKeep = new ArrayList<>();
        Iterator<ItemStack> iterator = event.getDrops().iterator();

        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (service.isBound(item) && service.isBoundTo(item, player.getUniqueId())) {
                toKeep.add(item.clone());
                iterator.remove();
            }
        }

        for (ItemStack item : toKeep) {
            player.getInventory().addItem(item);
        }
    }

    private boolean isContainerInventory(Inventory inventory, int slot) {
        InventoryType type = inventory.getType();
        return type == InventoryType.CHEST ||
               type == InventoryType.BARREL ||
               type == InventoryType.SHULKER_BOX ||
               type == InventoryType.ENDER_CHEST ||
               type == InventoryType.HOPPER ||
               type == InventoryType.DISPENSER ||
               type == InventoryType.DROPPER ||
               (type == InventoryType.PLAYER && slot >= 0 && slot < 36 && !isPlayerInventorySlot(inventory, slot));
    }

    private boolean isPlayerInventorySlot(Inventory inventory, int slot) {
        return inventory.getHolder() instanceof Player;
    }

    private void sendMessage(Player player, String messageKey) {
        String message = config.getMessage(messageKey);
        if (message != null && !message.isEmpty()) {
            Component component = plugin.getMiniMessage().colorize(message);
            player.sendMessage(component);
        }
    }
}
