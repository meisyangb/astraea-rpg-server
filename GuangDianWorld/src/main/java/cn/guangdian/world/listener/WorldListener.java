package cn.guangdian.world.listener;

import cn.guangdian.world.GuangDianWorld;
import cn.guangdian.world.model.GDWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.concurrent.ConcurrentHashMap;

public class WorldListener implements Listener {

    private final GuangDianWorld plugin;
    private final ConcurrentHashMap<String, Location> deathLocations = new ConcurrentHashMap<>();

    public WorldListener(GuangDianWorld plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location loc = player.getLocation();
        deathLocations.put(player.getUniqueId().toString(), loc);
        plugin.getLogger().info("[DEBUG] 玩家 " + player.getName() + " 死亡位置已记录: " + loc.getWorld().getName() + " at " + loc.getX() + "," + loc.getY() + "," + loc.getZ());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getLogger().info("[DEBUG] 玩家加入事件触发: " + player.getName());
        
        if (plugin.getConfigManager().isFirstSpawnOverride() && !player.hasPlayedBefore()) {
            String firstSpawnWorld = plugin.getConfigManager().getFirstSpawnWorld();
            GDWorld world = plugin.getWorldManager().getWorld(firstSpawnWorld);
            
            if (world != null) {
                plugin.getWorldManager().teleportToWorld(player, firstSpawnWorld);
            }
        }

        String joinDestination = plugin.getConfigManager().getJoinDestination();
        if (joinDestination != null && !joinDestination.isEmpty()) {
            GDWorld world = plugin.getWorldManager().getWorld(joinDestination);
            if (world != null) {
                plugin.getWorldManager().teleportToWorld(player, joinDestination);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World toWorld = player.getWorld();
        
        GDWorld gdWorld = plugin.getWorldManager().getWorld(toWorld);
        if (gdWorld == null) return;

        applyWorldGamemode(player, gdWorld);

        if (!gdWorld.isAllowFlight()) {
            player.setAllowFlight(false);
            player.setFlying(false);
        } else if (player.hasPermission("guangdian.world.bypass")) {
            player.setAllowFlight(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location deathLoc = deathLocations.remove(player.getUniqueId().toString());
        World deathWorld = (deathLoc != null) ? deathLoc.getWorld() : player.getWorld();
        plugin.getLogger().info("[DEBUG] 玩家 " + player.getName() + " 重生世界: " + deathWorld.getName() + ", 事件重生点: " + event.getRespawnLocation().getWorld().getName());

        if (deathWorld == null) return;

        GDWorld gdWorld = plugin.getWorldManager().getWorld(deathWorld);
        if (gdWorld == null) return;

        String respawnWorldName = gdWorld.getRespawnWorld();
        if (respawnWorldName != null && !respawnWorldName.isEmpty()) {
            GDWorld respawnWorld = plugin.getWorldManager().getWorld(respawnWorldName);
            if (respawnWorld != null && respawnWorld.isLoaded()) {
                Location targetSpawnLoc = respawnWorld.getSpawnLocation();
                if (targetSpawnLoc != null) {
                    targetSpawnLoc = targetSpawnLoc.clone();
                    targetSpawnLoc.setWorld(respawnWorld.getBukkitWorld());
                    event.setRespawnLocation(targetSpawnLoc);
                    return;
                }
            }
        }

        Location spawnLoc = gdWorld.getSpawnLocation();
        if (spawnLoc != null) {
            spawnLoc = spawnLoc.clone();
            spawnLoc.setWorld(deathWorld);
            event.setRespawnLocation(spawnLoc);
            return;
        }

        if (plugin.getConfigManager().isRespawnInOverworld()) {
            if (deathWorld.getEnvironment() != World.Environment.NORMAL) {
                String defaultWorld = plugin.getConfigManager().getDefaultWorld();
                GDWorld overworld = plugin.getWorldManager().getWorld(defaultWorld);
                if (overworld != null && overworld.isLoaded()) {
                    Location respawnLoc = overworld.getSpawnLocation();
                    if (respawnLoc != null) {
                        respawnLoc = respawnLoc.clone();
                        respawnLoc.setWorld(overworld.getBukkitWorld());
                        event.setRespawnLocation(respawnLoc);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null || event.getTo().getWorld() == null) return;

        Player player = event.getPlayer();
        World toWorld = event.getTo().getWorld();
        GDWorld gdWorld = plugin.getWorldManager().getWorld(toWorld);

        if (gdWorld == null) return;

        // 检查权限：允许 guangdian.world.tp 或 guangdian.world.tp.<世界名> 或 guangdian.world.bypass
        if (!player.hasPermission("guangdian.world.tp")
            && !player.hasPermission("guangdian.world.tp." + toWorld.getName()) 
            && !player.hasPermission("guangdian.world.bypass")) {
            event.setCancelled(true);
            player.sendMessage(Component.text("你没有权限进入这个世界!", NamedTextColor.RED));
        }
    }

    private void applyWorldGamemode(Player player, GDWorld gdWorld) {
        if (player.hasPermission("guangdian.world.bypass.gamemode")) return;

        GameMode targetMode = parseGamemode(gdWorld.getGamemode());
        if (targetMode != null && player.getGameMode() != targetMode) {
            player.setGameMode(targetMode);
        }
    }

    private GameMode parseGamemode(String mode) {
        return switch (mode.toLowerCase()) {
            case "survival", "0" -> GameMode.SURVIVAL;
            case "creative", "1" -> GameMode.CREATIVE;
            case "adventure", "2" -> GameMode.ADVENTURE;
            case "spectator", "3" -> GameMode.SPECTATOR;
            default -> null;
        };
    }
}
