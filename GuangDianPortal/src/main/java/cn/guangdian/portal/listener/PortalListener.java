package cn.guangdian.portal.listener;

import cn.guangdian.portal.GuangDianPortal;
import cn.guangdian.portal.manager.PortalManager;
import cn.guangdian.portal.model.Portal;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PortalListener implements Listener {

    private final GuangDianPortal plugin;
    private final PortalManager portalManager;
    private final Material selectionTool;
    private final Map<UUID, Long> messageCooldowns;

    public PortalListener(GuangDianPortal plugin) {
        this.plugin = plugin;
        this.portalManager = plugin.getPortalManager();
        this.selectionTool = Material.valueOf(plugin.getConfig().getString("settings.selection-tool", "WOODEN_AXE"));
        this.messageCooldowns = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Location to = event.getTo();

        Portal portal = portalManager.getPortalAt(to);
        if (portal != null) {
            if (!portalManager.canTeleport(player)) {
                return;
            }

            if (portal.getPermission() != null && !player.hasPermission(portal.getPermission())) {
                sendCooldownMessage(player, "<red>你没有权限使用此传送门!");
                return;
            }

            portalManager.teleportPlayer(player, portal);

            plugin.sendMessage(player, "<green>已通过传送门 <yellow>" + portal.getName() + "<green>!");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != selectionTool) return;

        if (!player.hasPermission("guangdian.portal.select")) return;

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            Location loc = event.getClickedBlock().getLocation();
            portalManager.setPlayerSelection(player, 0, loc);

            plugin.sendMessage(player, "<green>已选择第一个点: <yellow>" + formatLocation(loc));

            checkAndCreatePortal(player);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            Location loc = event.getClickedBlock().getLocation();
            portalManager.setPlayerSelection(player, 1, loc);

            plugin.sendMessage(player, "<green>已选择第二个点: <yellow>" + formatLocation(loc));

            checkAndCreatePortal(player);
        }
    }

    private void checkAndCreatePortal(Player player) {
        if (!portalManager.hasCompleteSelection(player)) {
            return;
        }

        Location[] selections = portalManager.getPlayerSelection(player);
        Location loc1 = selections[0];
        Location loc2 = selections[1];

        if (!loc1.getWorld().equals(loc2.getWorld())) {
            plugin.sendMessage(player, "<red>两个选择点必须在同一个世界!");
            portalManager.clearPlayerSelection(player);
            return;
        }

        plugin.sendMessage(player, "<green>已选择传送门区域: <yellow>" + formatBounds(loc1, loc2));
        plugin.sendMessage(player, "<gray>使用 <yellow>/portal create <名称> <gray>创建传送门");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        portalManager.clearPlayerSelection(event.getPlayer());
        messageCooldowns.remove(event.getPlayer().getUniqueId());
    }

    private void sendCooldownMessage(Player player, String message) {
        UUID playerId = player.getUniqueId();
        Long lastMessage = messageCooldowns.get(playerId);

        if (lastMessage == null || System.currentTimeMillis() - lastMessage > 3000) {
            plugin.sendMessage(player, message);
            messageCooldowns.put(playerId, System.currentTimeMillis());
        }
    }

    private String formatLocation(Location loc) {
        World world = loc.getWorld();
        return String.format("[%s] (%d, %d, %d)",
            world != null ? world.getName() : "未知",
            loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private String formatBounds(Location loc1, Location loc2) {
        World world = loc1.getWorld();
        return String.format("[%s] (%d,%d,%d) -> (%d,%d,%d)",
            world != null ? world.getName() : "未知",
            loc1.getBlockX(), loc1.getBlockY(), loc1.getBlockZ(),
            loc2.getBlockX(), loc2.getBlockY(), loc2.getBlockZ());
    }
}
