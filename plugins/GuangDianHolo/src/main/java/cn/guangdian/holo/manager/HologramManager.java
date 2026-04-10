package cn.guangdian.holo.manager;

import cn.guangdian.rpgcore.event.events.HologramCreatedEvent;
import cn.guangdian.rpgcore.event.events.HologramDeletedEvent;
import cn.guangdian.holo.GuangDianHolo;
import cn.guangdian.holo.model.Hologram;
import cn.guangdian.holo.storage.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class HologramManager {

    private final GuangDianHolo plugin;
    private final ConfigManager configManager;
    private final Map<String, Hologram> holograms = new ConcurrentHashMap<>();
    private BukkitTask updateTask;
    private final Object saveLock = new Object();
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public HologramManager(GuangDianHolo plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public void loadHolograms() {
        holograms.clear();
        Map<String, Hologram> loaded = configManager.loadHolograms();
        for (Hologram holo : loaded.values()) {
            holograms.put(holo.getName(), holo);
            spawnHologram(holo);
            
            if (plugin.getCacheProvider() != null) {
                plugin.getCacheProvider().put("holo:" + holo.getName(), holo);
            }
        }
    }

    public void reloadHolograms() {
        for (Hologram holo : holograms.values()) {
            despawnHologram(holo);
        }
        loadHolograms();
    }

    public void saveHolograms() {
        if (plugin.getAsyncExecutor() != null) {
            plugin.getAsyncExecutor().execute(() -> {
                synchronized (saveLock) {
                    for (Hologram holo : holograms.values()) {
                        if (holo.isPersistent()) {
                            configManager.saveHologram(holo);
                        }
                    }
                }
            });
        } else {
            synchronized (saveLock) {
                for (Hologram holo : holograms.values()) {
                    if (holo.isPersistent()) {
                        configManager.saveHologram(holo);
                    }
                }
            }
        }
    }

    public void startUpdateTask() {
        int interval = configManager.getUpdateInterval();
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateHolograms, interval, interval);
    }

    public void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void updateHolograms() {
        for (Hologram holo : holograms.values()) {
            if (!holo.isVisible()) continue;
            updateHologramDisplay(holo);
        }
    }

    public Hologram createHologram(String name, Location location) {
        if (holograms.containsKey(name)) {
            return null;
        }

        Hologram holo = new Hologram(name, location);
        holo.setLineHeight(configManager.getDefaultLineHeight());
        holo.setViewDistance(configManager.getVisibilityDistance());

        holograms.put(name, holo);
        spawnHologram(holo);
        saveHologramAsync(holo);

        if (plugin.getCacheProvider() != null) {
            plugin.getCacheProvider().put("holo:" + name, holo);
        }

        if (plugin.getEventBus() != null) {
            plugin.getEventBus().publish(new HologramCreatedEvent(name, location, null));
        }

        return holo;
    }

    public boolean deleteHologram(String name) {
        Hologram holo = holograms.remove(name);
        if (holo == null) {
            return false;
        }

        despawnHologram(holo);
        
        if (plugin.getAsyncExecutor() != null) {
            plugin.getAsyncExecutor().execute(() -> configManager.deleteHologram(name));
        } else {
            configManager.deleteHologram(name);
        }

        if (plugin.getCacheProvider() != null) {
            plugin.getCacheProvider().invalidate("holo:" + name);
        }

        if (plugin.getEventBus() != null) {
            plugin.getEventBus().publish(new HologramDeletedEvent(name));
        }

        return true;
    }

    public void spawnHologram(Hologram holo) {
        World world = holo.getWorld();
        if (world == null) return;

        despawnHologram(holo);

        List<Integer> entityIds = new ArrayList<>();
        List<String> lines = holo.getLines();

        for (int i = 0; i < lines.size(); i++) {
            Location lineLoc = holo.getLineLocation(i);
            if (lineLoc == null) continue;

            TextDisplay display = (TextDisplay) world.spawnEntity(lineLoc, EntityType.TEXT_DISPLAY);
            
            display.setCustomNameVisible(false);
            display.setPersistent(false);
            display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            display.setSeeThrough(true);
            display.setShadowed(false);
            
            String line = lines.get(i);
            Component text = parseText(line);
            display.text(text);

            entityIds.add(display.getEntityId());
        }

        holo.setEntityIds(entityIds);
    }

    public void despawnHologram(Hologram holo) {
        World world = holo.getWorld();
        if (world == null) return;

        for (int entityId : holo.getEntityIds()) {
            Entity entity = world.getEntities().stream()
                .filter(e -> e.getEntityId() == entityId)
                .findFirst()
                .orElse(null);
            if (entity != null) {
                entity.remove();
            }
        }
        holo.setEntityIds(new ArrayList<>());
    }

    public void updateHologramDisplay(Hologram holo) {
        World world = holo.getWorld();
        if (world == null) return;

        List<String> lines = holo.getLines();
        List<Integer> currentIds = holo.getEntityIds();

        if (lines.size() != currentIds.size()) {
            respawnHologram(holo);
            return;
        }

        for (int i = 0; i < lines.size(); i++) {
            int entityId = currentIds.get(i);
            Entity entity = world.getEntities().stream()
                .filter(e -> e.getEntityId() == entityId)
                .findFirst()
                .orElse(null);
            
            if (entity instanceof TextDisplay display) {
                String line = lines.get(i);
                Component text = parseText(line);
                display.text(text);
            }
        }
    }

    public void respawnHologram(Hologram holo) {
        despawnHologram(holo);
        spawnHologram(holo);
    }

    private Component parseText(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        if (text.contains("<") && text.contains(">")) {
            try {
                return MiniMessage.miniMessage().deserialize(text);
            } catch (Exception e) {
                // 工业级优化: MiniMessage解析失败时记录警告并降级处理
                plugin.getLogger().fine("MiniMessage parse failed, using legacy: " + e.getMessage());
            }
        }

        if (text.contains("&") || text.contains("§")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        }

        return Component.text(text);
    }

    public void saveHologramAsync(Hologram holo) {
        if (!holo.isPersistent()) return;
        
        if (plugin.getAsyncExecutor() != null) {
            plugin.getAsyncExecutor().execute(() -> {
                synchronized (saveLock) {
                    configManager.saveHologram(holo);
                }
            });
        } else {
            configManager.saveHologram(holo);
        }
    }

    public void removeAllHolograms() {
        for (Hologram holo : holograms.values()) {
            despawnHologram(holo);
        }
    }

    public Hologram getHologram(String name) {
        if (plugin.getCacheProvider() != null) {
            Hologram cached = plugin.getCacheProvider().get("holo:" + name, Hologram.class);
            if (cached != null) {
                return cached;
            }
        }
        return holograms.get(name);
    }

    public Collection<Hologram> getAllHolograms() {
        return holograms.values();
    }

    public int getHologramCount() {
        return holograms.size();
    }

    public List<String> getHologramNames() {
        return new ArrayList<>(holograms.keySet());
    }

    public List<Hologram> getHologramsNear(Location location, double radius) {
        List<Hologram> result = new ArrayList<>();
        for (Hologram holo : holograms.values()) {
            if (holo.getLocation() != null && 
                holo.getLocation().getWorld() != null &&
                holo.getLocation().getWorld().equals(location.getWorld()) &&
                holo.getLocation().distanceSquared(location) <= radius * radius) {
                result.add(holo);
            }
        }
        return result;
    }
}
