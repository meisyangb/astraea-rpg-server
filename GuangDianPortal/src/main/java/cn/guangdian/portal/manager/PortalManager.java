package cn.guangdian.portal.manager;

import cn.guangdian.portal.GuangDianPortal;
import cn.guangdian.portal.model.Portal;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PortalManager {

    private final GuangDianPortal plugin;
    private final Map<String, Portal> portals;
    private final Map<UUID, Location[]> playerSelections;
    private final Map<UUID, Long> cooldowns;
    private File portalsFile;
    private FileConfiguration portalsConfig;

    private int cooldownMs;
    private Material defaultFrameMaterial;
    private Material portalFillMaterial;

    public PortalManager(GuangDianPortal plugin) {
        this.plugin = plugin;
        this.portals = new ConcurrentHashMap<>();
        this.playerSelections = new ConcurrentHashMap<>();
        this.cooldowns = new ConcurrentHashMap<>();
        this.cooldownMs = plugin.getConfig().getInt("settings.cooldown-ms", 1000);
        this.defaultFrameMaterial = Material.valueOf(plugin.getConfig().getString("settings.default-frame-material", "OBSIDIAN"));
        this.portalFillMaterial = Material.valueOf(plugin.getConfig().getString("settings.portal-fill-material", "END_GATEWAY"));

        initStorage();
    }

    private void initStorage() {
        portalsFile = new File(plugin.getDataFolder(), "portals.yml");
        if (!portalsFile.exists()) {
            portalsFile.getParentFile().mkdirs();
            plugin.saveResource("portals.yml", false);
        }
        portalsConfig = YamlConfiguration.loadConfiguration(portalsFile);
    }

    public void loadPortals() {
        portals.clear();

        ConfigurationSection portalsSection = portalsConfig.getConfigurationSection("portals");
        if (portalsSection == null) return;

        for (String portalName : portalsSection.getKeys(false)) {
            ConfigurationSection portalSection = portalsSection.getConfigurationSection(portalName);
            if (portalSection != null) {
                try {
                    Portal portal = Portal.loadFromConfig(portalName, portalSection);
                    portals.put(portalName.toLowerCase(), portal);
                    plugin.getLogger().info("加载传送门: " + portalName + " -> " + portal.getDestinationString());
                } catch (Exception e) {
                    plugin.getLogger().warning("加载传送门失败: " + portalName + " - " + e.getMessage());
                }
            }
        }

        plugin.getLogger().info("共加载 " + portals.size() + " 个传送门");
    }

    public void savePortals() {
        for (String key : portalsConfig.getKeys(false)) {
            portalsConfig.set(key, null);
        }

        ConfigurationSection portalsSection = portalsConfig.createSection("portals");
        for (Map.Entry<String, Portal> entry : portals.entrySet()) {
            ConfigurationSection portalSection = portalsSection.createSection(entry.getKey());
            entry.getValue().saveToConfig(portalSection);
        }

        try {
            portalsConfig.save(portalsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存传送门配置失败: " + e.getMessage());
        }
    }

    public boolean createPortal(String name, String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {
        return createPortal(name, worldName, x1, y1, z1, x2, y2, z2, defaultFrameMaterial);
    }

    public boolean createPortal(String name, String worldName, int x1, int y1, int z1, int x2, int y2, int z2, Material frameMaterial) {
        String key = name.toLowerCase();
        if (portals.containsKey(key)) {
            return false;
        }

        Portal portal = new Portal(name, worldName, x1, y1, z1, x2, y2, z2, frameMaterial);
        portals.put(key, portal);

        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            portal.createFrame(world);
            portal.fillPortal(world, portalFillMaterial);
        }

        savePortals();
        return true;
    }

    public boolean deletePortal(String name) {
        String key = name.toLowerCase();
        Portal portal = portals.remove(key);
        if (portal == null) return false;

        World world = Bukkit.getWorld(portal.getWorldName());
        if (world != null) {
            portal.clearPortal(world);
        }

        savePortals();
        return true;
    }

    public Portal getPortal(String name) {
        return portals.get(name.toLowerCase());
    }

    public Portal getPortalAt(Location location) {
        for (Portal portal : portals.values()) {
            if (portal.isEnabled() && portal.isInside(location)) {
                return portal;
            }
        }
        return null;
    }

    public Collection<Portal> getAllPortals() {
        return Collections.unmodifiableCollection(portals.values());
    }

    public List<String> getPortalNames() {
        return new ArrayList<>(portals.keySet());
    }

    public void setPlayerSelection(Player player, int corner, Location location) {
        UUID playerId = player.getUniqueId();
        Location[] selections = playerSelections.computeIfAbsent(playerId, k -> new Location[2]);
        selections[corner] = location.clone();
    }

    public Location[] getPlayerSelection(Player player) {
        return playerSelections.get(player.getUniqueId());
    }

    public void clearPlayerSelection(Player player) {
        playerSelections.remove(player.getUniqueId());
    }

    public boolean hasCompleteSelection(Player player) {
        Location[] selections = playerSelections.get(player.getUniqueId());
        return selections != null && selections[0] != null && selections[1] != null;
    }

    public boolean canTeleport(Player player) {
        UUID playerId = player.getUniqueId();
        Long lastTeleport = cooldowns.get(playerId);
        if (lastTeleport == null) return true;

        return System.currentTimeMillis() - lastTeleport >= cooldownMs;
    }

    public void updateCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void teleportPlayer(Player player, Portal portal) {
        if (!canTeleport(player)) {
            return;
        }

        if (portal.getPermission() != null && !player.hasPermission(portal.getPermission())) {
            return;
        }

        Location destination = portal.getDestination();
        if (destination == null && portal.getDestinationPortal() != null) {
            Portal destPortal = getPortal(portal.getDestinationPortal());
            if (destPortal != null) {
                destination = destPortal.getDestination();
            }
        }

        if (destination == null) {
            return;
        }

        World world = destination.getWorld();
        if (world == null) {
            return;
        }

        updateCooldown(player);

        final Location finalDestination = destination;
        int delay = plugin.getTeleportDelay();
        if (delay > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.teleport(finalDestination);
                playTeleportEffects(player);
            }, delay * 20L);
        } else {
            player.teleport(finalDestination);
            playTeleportEffects(player);
        }
    }

    private void playTeleportEffects(Player player) {
        if (!plugin.isTeleportEffectEnabled()) return;

        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        world.spawnParticle(org.bukkit.Particle.PORTAL, loc, 50, 0.5, 1, 0.5, 0.1);
        world.spawnParticle(org.bukkit.Particle.END_ROD, loc, 20, 0.5, 1, 0.5, 0.05);

        if (plugin.isTeleportSoundEnabled()) {
            try {
                player.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            } catch (Exception ignored) {
            }
        }
    }

    public void reload() {
        loadPortals();
        cooldownMs = plugin.getConfig().getInt("settings.cooldown-ms", 1000);
        defaultFrameMaterial = Material.valueOf(plugin.getConfig().getString("settings.default-frame-material", "OBSIDIAN"));
        portalFillMaterial = Material.valueOf(plugin.getConfig().getString("settings.portal-fill-material", "END_GATEWAY"));
    }
}
