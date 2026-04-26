package cn.guangdian.rpgcore.display;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.service.api.TextDisplayService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class TextDisplayServiceImpl implements TextDisplayService {

    private static final String HOLOGRAM_ID_KEY = "hologram_id";

    private final RPGCore plugin;
    private final Logger logger;
    private final Map<String, TextDisplay> holograms = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> visibilityMap = new ConcurrentHashMap<>();
    private boolean available = true;

    public TextDisplayServiceImpl(RPGCore plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        startCleanupTask();
    }

    private void startCleanupTask() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getScheduler().runAsyncRepeating(() -> {
                cleanupInvalidHolograms();
            }, 300000 / 50, 300000 / 50); // 每5分钟清理一次 (tick 转换)
        }
    }

    @Override
    public void createHologram(String id, Location location, Component text) {
        createHologram(id, location, text, TextDisplayOptions.defaults());
    }

    @Override
    public void createHologram(String id, Location location, Component text, TextDisplayOptions options) {
        if (id == null || location == null || location.getWorld() == null) {
            logger.warning("Cannot create hologram: invalid parameters");
            return;
        }

        if (holograms.containsKey(id)) {
            removeHologram(id);
        }

        try {
            World world = location.getWorld();
            TextDisplay display = world.spawn(location, TextDisplay.class);

            display.text(text);
            display.setBillboard(options.isBillboard() ? Display.Billboard.CENTER : Display.Billboard.FIXED);
            display.setShadowed(options.isShadowed());
            display.setSeeThrough(options.isSeeThrough());
            display.setLineWidth((int) options.getLineWidth());
            display.setBackgroundColor(Color.fromARGB(options.getBackgroundColor()));
            display.setViewRange((float) options.getViewRange());
            display.setShadowRadius((float) options.getShadowRadius());
            display.setShadowStrength((float) options.getShadowStrength());

            display.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, HOLOGRAM_ID_KEY),
                org.bukkit.persistence.PersistentDataType.STRING,
                id
            );

            holograms.put(id, display);
            visibilityMap.put(id, new HashSet<>());

            logger.fine("Created hologram: " + id);
        } catch (Exception e) {
            logger.warning("Failed to create hologram " + id + ": " + e.getMessage());
        }
    }

    @Override
    public void updateHologram(String id, Component text) {
        TextDisplay display = holograms.get(id);
        if (display != null && display.isValid()) {
            display.text(text);
        }
    }

    @Override
    public void updateHologramLocation(String id, Location location) {
        TextDisplay display = holograms.get(id);
        if (display != null && display.isValid() && location != null && location.getWorld() != null) {
            display.teleport(location);
        }
    }

    @Override
    public void removeHologram(String id) {
        removeHologram(id, false);
    }

    @Override
    public void removeHologram(String id, boolean playEffect) {
        TextDisplay display = holograms.remove(id);
        visibilityMap.remove(id);

        if (display != null) {
            if (playEffect && display.getLocation().getWorld() != null) {
                display.getLocation().getWorld().spawnParticle(
                    Particle.POOF,
                    display.getLocation(),
                    10,
                    0.2, 0.2, 0.2,
                    0.02
                );
            }
            display.remove();
            logger.fine("Removed hologram: " + id);
        }
    }

    @Override
    public void removeAllHolograms() {
        for (String id : new HashSet<>(holograms.keySet())) {
            removeHologram(id);
        }
    }

    @Override
    public boolean hasHologram(String id) {
        TextDisplay display = holograms.get(id);
        return display != null && display.isValid();
    }

    @Override
    public void showHologramToPlayer(String id, Player player) {
        TextDisplay display = holograms.get(id);
        if (display != null && player != null) {
            player.showEntity(plugin, display);
            visibilityMap.computeIfAbsent(id, k -> new HashSet<>()).add(player.getUniqueId());
        }
    }

    @Override
    public void hideHologramFromPlayer(String id, Player player) {
        TextDisplay display = holograms.get(id);
        if (display != null && player != null) {
            player.hideEntity(plugin, display);
            Set<UUID> viewers = visibilityMap.get(id);
            if (viewers != null) {
                viewers.remove(player.getUniqueId());
            }
        }
    }

    @Override
    public void showHologramToPlayers(String id, Collection<? extends Player> players) {
        for (Player player : players) {
            showHologramToPlayer(id, player);
        }
    }

    @Override
    public void hideHologramFromPlayers(String id, Collection<? extends Player> players) {
        for (Player player : players) {
            hideHologramFromPlayer(id, player);
        }
    }

    @Override
    public void setHologramVisibleToAll(String id, boolean visible) {
        TextDisplay display = holograms.get(id);
        if (display != null) {
            display.setVisibleByDefault(visible);
        }
    }

    @Override
    public UUID getHologramEntityId(String id) {
        TextDisplay display = holograms.get(id);
        return display != null ? display.getUniqueId() : null;
    }

    @Override
    public Location getHologramLocation(String id) {
        TextDisplay display = holograms.get(id);
        return display != null ? display.getLocation().clone() : null;
    }

    @Override
    public int getHologramCount() {
        return holograms.size();
    }

    @Override
    public void clearAll() {
        removeAllHolograms();
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    public void shutdown() {
        available = false;
        removeAllHolograms();
    }

    public void cleanupInvalidHolograms() {
        holograms.entrySet().removeIf(entry -> {
            TextDisplay display = entry.getValue();
            if (display == null || !display.isValid() || display.isDead()) {
                visibilityMap.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
}
