package cn.guangdian.raid.listener;

import cn.guangdian.raid.GuangDianRaid;
import cn.guangdian.raid.instance.RaidInstance;
import cn.guangdian.raid.model.RaidPlayerState;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class RaidListener implements Listener {

    private final GuangDianRaid plugin;

    public RaidListener(GuangDianRaid plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        Optional<RaidInstance> instanceOpt = plugin.getInstanceManager().getPlayerInstance(player.getUniqueId());
        if (instanceOpt.isEmpty()) return;

        RaidInstance instance = instanceOpt.get();
        instance.fail("玩家断开连接");
        plugin.getInstanceManager().removePlayer(player.getUniqueId());
        plugin.getRaidBoard().removeBoard(player);
        plugin.getExtractionManager().clearPlayerProgress(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        Optional<RaidInstance> instanceOpt = plugin.getInstanceManager().getPlayerInstance(player.getUniqueId());
        if (instanceOpt.isEmpty()) return;

        RaidInstance instance = instanceOpt.get();
        instance.onPlayerDeath(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Optional<RaidInstance> instanceOpt = plugin.getInstanceManager().getPlayerInstance(player.getUniqueId());
        if (instanceOpt.isEmpty()) return;

        RaidInstance instance = instanceOpt.get();
        var rp = instance.getTeam().getMember(player.getUniqueId());
        if (rp != null && rp.getState() == RaidPlayerState.DEAD) {
            event.setRespawnLocation(player.getWorld().getSpawnLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) return;

        plugin.getSpawnManager().onMobDeath(entity);

        Player killer = entity.getKiller();
        if (killer == null) return;

        Optional<RaidInstance> instanceOpt = plugin.getInstanceManager().getPlayerInstance(killer.getUniqueId());
        if (instanceOpt.isEmpty()) return;

        RaidInstance instance = instanceOpt.get();
        instance.onMobKill(killer, entity);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        Optional<RaidInstance> instanceOpt = plugin.getInstanceManager().getPlayerInstance(player.getUniqueId());
        if (instanceOpt.isEmpty()) return;

        if (plugin.getIntelManager().isIntelItem(item)) {
            event.setCancelled(true);
            plugin.getIntelManager().handleIntelCollect(player, null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Item droppedItem = event.getItemDrop();

        Optional<RaidInstance> instanceOpt = plugin.getInstanceManager().getPlayerInstance(player.getUniqueId());
        if (instanceOpt.isEmpty()) return;

        RaidInstance instance = instanceOpt.get();
        if (instance.getDroppedItems().contains(droppedItem.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
