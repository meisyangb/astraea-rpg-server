package cn.guangdian.holo.listener;

import cn.guangdian.holo.GuangDianHolo;
import cn.guangdian.holo.model.Hologram;
import cn.guangdian.rpgcore.RPGCore;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class HologramListener implements Listener {

    private final GuangDianHolo plugin;

    public HologramListener(GuangDianHolo plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        runTaskLaterSafe(() -> updateVisibilityForPlayer(player), plugin.getConfigManager().getSpawnDelay());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        updateVisibilityForPlayer(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to != null && to.getWorld() != null) {
            runTaskLaterSafe(() -> updateVisibilityForPlayer(player), 1L);
        }
    }
    
    private void runTaskLaterSafe(Runnable task, long delay) {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getScheduler().runSyncLater(task, delay);
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    private void updateVisibilityForPlayer(Player player) {
        Location playerLoc = player.getLocation();
        int viewDistance = plugin.getConfigManager().getVisibilityDistance();

        for (Hologram holo : plugin.getHologramManager().getAllHolograms()) {
            Location holoLoc = holo.getLocation();
            if (holoLoc == null || holoLoc.getWorld() == null) continue;

            if (!holoLoc.getWorld().equals(playerLoc.getWorld())) continue;

            double distance = playerLoc.distanceSquared(holoLoc);
            boolean shouldSee = distance <= viewDistance * viewDistance;
        }
    }
}
