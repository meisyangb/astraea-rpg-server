package cn.guangdian.soulbag.gui;

import cn.guangdian.soulbag.GuangDianSoulBag;
import cn.guangdian.soulbag.data.SoulBagData;
import cn.guangdian.rpgcore.gui.GUI;
import cn.guangdian.rpgcore.gui.GUIBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SoulBagGUI implements Listener {
    
    private final GuangDianSoulBag plugin;
    private final Map<UUID, UUID> openInventories = new HashMap<>();
    
    public SoulBagGUI(GuangDianSoulBag plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    public void openBag(Player player) {
        openBag(player, player.getUniqueId());
    }
    
    public void openBag(Player viewer, UUID ownerId) {
        SoulBagData bag = plugin.getBagManager().getBag(ownerId);
        
        String title = plugin.getConfig().getString("messages.gui-title", "&8灵魂背包");
        title = title.replace("&", "§");
        
        int rows = bag.getSize() / 9;
        GUI gui = GUIBuilder.create(title, rows).build();
        
        for (int i = 0; i < bag.getSize(); i++) {
            ItemStack item = bag.getItem(i);
            if (item != null) {
                gui.setItem(i, item);
            }
        }
        
        openInventories.put(viewer.getUniqueId(), ownerId);
        gui.open(viewer);
        
        if (viewer.getUniqueId().equals(ownerId)) {
            String openMsg = plugin.getConfig().getString("messages.open", "&7正在打开灵魂背包...");
            viewer.sendMessage(Component.text(openMsg.replace("&", "§")));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID viewerId = player.getUniqueId();
        
        if (!openInventories.containsKey(viewerId)) {
            return;
        }
        
        UUID ownerId = openInventories.get(viewerId);
        SoulBagData bag = plugin.getBagManager().getBag(ownerId);
        
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return;
        }
        
        int rawSlot = event.getRawSlot();
        
        if (rawSlot >= 0 && rawSlot < bag.getSize()) {
            if (!viewerId.equals(ownerId)) {
                if (!plugin.getConfig().getBoolean("settings.allow-others-view", false)) {
                    event.setCancelled(true);
                    return;
                }
            }
            
            event.setCancelled(false);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Inventory topInventory = player.getOpenInventory().getTopInventory();
                for (int i = 0; i < bag.getSize(); i++) {
                    ItemStack item = topInventory.getItem(i);
                    bag.setItem(i, item);
                }
            }, 1L);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        UUID viewerId = player.getUniqueId();
        
        if (!openInventories.containsKey(viewerId)) {
            return;
        }
        
        UUID ownerId = openInventories.remove(viewerId);
        SoulBagData bag = plugin.getBagManager().getBag(ownerId);
        
        Inventory topInventory = event.getInventory();
        for (int i = 0; i < bag.getSize(); i++) {
            ItemStack item = topInventory.getItem(i);
            bag.setItem(i, item);
        }
        
        if (viewerId.equals(ownerId)) {
            String closeMsg = plugin.getConfig().getString("messages.close", "&7灵魂背包已关闭");
            player.sendMessage(Component.text(closeMsg.replace("&", "§")));
        }
    }
    
    public boolean hasOpenBag(UUID playerId) {
        return openInventories.containsKey(playerId);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID viewerId = event.getPlayer().getUniqueId();
        if (openInventories.containsKey(viewerId)) {
            UUID ownerId = openInventories.remove(viewerId);
            SoulBagData bag = plugin.getBagManager().getBag(ownerId);
            // 保存背包数据
            Player player = event.getPlayer();
            Inventory topInventory = player.getOpenInventory().getTopInventory();
            for (int i = 0; i < bag.getSize(); i++) {
                ItemStack item = topInventory.getItem(i);
                bag.setItem(i, item);
            }
        }
    }

    public void closeAllBags() {
        for (UUID viewerId : openInventories.keySet()) {
            Player player = Bukkit.getPlayer(viewerId);
            if (player != null) {
                player.closeInventory();
            }
        }
        openInventories.clear();
    }
}
