package cn.guangdian.world.manager;

import cn.guangdian.rpgcore.event.events.WorldCreatedEvent;
import cn.guangdian.rpgcore.event.events.WorldDeletedEvent;
import cn.guangdian.world.GuangDianWorld;
import cn.guangdian.world.model.GDWorld;
import cn.guangdian.world.storage.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class WorldManager {

    private final GuangDianWorld plugin;
    private final ConfigManager configManager;
    private final Map<String, GDWorld> worlds = new ConcurrentHashMap<>();
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final Object saveLock = new Object();

    public WorldManager(GuangDianWorld plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public void loadAllWorlds() {
        worlds.clear();
        Map<String, GDWorld> loadedWorlds = configManager.loadWorlds();
        
        for (Map.Entry<String, GDWorld> entry : loadedWorlds.entrySet()) {
            GDWorld gdWorld = entry.getValue();
            if (gdWorld.isAutoLoad()) {
                loadWorld(gdWorld);
            }
            worlds.put(entry.getKey(), gdWorld);
            
            if (plugin.getCacheProvider() != null) {
                plugin.getCacheProvider().put("world:" + entry.getKey(), gdWorld);
            }
        }

        importExistingWorlds();
    }

    private void importExistingWorlds() {
        for (World world : Bukkit.getWorlds()) {
            if (!worlds.containsKey(world.getName())) {
                GDWorld gdWorld = new GDWorld(world.getName());
                gdWorld.setBukkitWorld(world);
                gdWorld.setSpawnLocation(world.getSpawnLocation());
                applyWorldSettings(gdWorld, world);
                worlds.put(world.getName(), gdWorld);
                
                saveWorldAsync(gdWorld);
                
                if (plugin.getCacheProvider() != null) {
                    plugin.getCacheProvider().put("world:" + world.getName(), gdWorld);
                }
                
                plugin.getLogger().info("自动导入世界: " + world.getName());
            }
        }
    }

    public void reloadWorlds() {
        for (GDWorld gdWorld : worlds.values()) {
            if (gdWorld.isLoaded()) {
                applyWorldSettings(gdWorld, gdWorld.getBukkitWorld());
            }
        }
    }

    public boolean loadWorld(String worldName) {
        GDWorld gdWorld = worlds.get(worldName);
        if (gdWorld == null) {
            return false;
        }
        return loadWorld(gdWorld);
    }

    public boolean loadWorld(GDWorld gdWorld) {
        String worldName = gdWorld.getName();
        World world = Bukkit.getWorld(worldName);
        
        if (world != null) {
            gdWorld.setBukkitWorld(world);
            applyWorldSettings(gdWorld, world);
            return true;
        }

        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (!worldFolder.exists() || !worldFolder.isDirectory()) {
            return false;
        }

        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(gdWorld.getEnvironment());
        creator.type(WorldType.NORMAL);
        
        String generator = gdWorld.getGenerator();
        if (generator != null && !generator.isEmpty()) {
            ChunkGenerator gen = getGenerator(generator);
            if (gen != null) {
                creator.generator(gen);
            }
        }

        world = creator.createWorld();
        if (world == null) {
            plugin.getLogger().severe("加载世界失败: " + worldName);
            return false;
        }

        gdWorld.setBukkitWorld(world);
        applyWorldSettings(gdWorld, world);

        if (gdWorld.getSpawnLocation() != null) {
            gdWorld.getSpawnLocation().setWorld(world);
            world.setSpawnLocation(gdWorld.getSpawnLocation());
        }

        if (plugin.getCacheProvider() != null) {
            plugin.getCacheProvider().put("world:" + worldName, gdWorld);
        }

        plugin.getLogger().info("已加载世界: " + worldName);
        return true;
    }

    public boolean unloadWorld(String worldName) {
        GDWorld gdWorld = worlds.get(worldName);
        if (gdWorld == null || !gdWorld.isLoaded()) {
            return false;
        }

        World world = gdWorld.getBukkitWorld();
        String defaultWorld = configManager.getDefaultWorld();
        World fallbackWorld = Bukkit.getWorld(defaultWorld);

        for (Player player : world.getPlayers()) {
            if (fallbackWorld != null) {
                player.teleport(fallbackWorld.getSpawnLocation());
            } else {
                player.kick(Component.text("世界已卸载", NamedTextColor.RED));
            }
        }

        boolean success = Bukkit.unloadWorld(world, true);
        if (success) {
            gdWorld.setBukkitWorld(null);
            
            if (plugin.getCacheProvider() != null) {
                plugin.getCacheProvider().invalidate("world:" + worldName);
            }
            
            plugin.getLogger().info("已卸载世界: " + worldName);
        }
        return success;
    }

    public GDWorld createWorld(String name, World.Environment environment) {
        if (worlds.containsKey(name)) {
            return null;
        }

        GDWorld gdWorld = new GDWorld(name);
        gdWorld.setEnvironment(environment);

        WorldCreator creator = new WorldCreator(name);
        creator.environment(environment);
        creator.type(WorldType.NORMAL);
        creator.seed(System.currentTimeMillis());

        World world = creator.createWorld();
        if (world == null) {
            return null;
        }

        gdWorld.setBukkitWorld(world);
        gdWorld.setSpawnLocation(world.getSpawnLocation());
        applyWorldSettings(gdWorld, world);

        worlds.put(name, gdWorld);
        saveWorldAsync(gdWorld);

        if (plugin.getCacheProvider() != null) {
            plugin.getCacheProvider().put("world:" + name, gdWorld);
        }

        if (plugin.getEventBus() != null) {
            plugin.getEventBus().publish(new WorldCreatedEvent(name, environment, null));
        }

        plugin.getLogger().info("已创建世界: " + name);
        return gdWorld;
    }

    public boolean deleteWorld(String worldName) {
        GDWorld gdWorld = worlds.get(worldName);
        if (gdWorld == null) {
            return false;
        }

        if (gdWorld.isLoaded()) {
            if (!unloadWorld(worldName)) {
                return false;
            }
        }

        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (worldFolder.exists()) {
            deleteDirectory(worldFolder);
        }

        worlds.remove(worldName);
        configManager.deleteWorld(worldName);

        if (plugin.getCacheProvider() != null) {
            plugin.getCacheProvider().invalidate("world:" + worldName);
        }

        if (plugin.getEventBus() != null) {
            plugin.getEventBus().publish(new WorldDeletedEvent(worldName));
        }

        plugin.getLogger().info("已删除世界: " + worldName);
        return true;
    }

    public boolean teleportToWorld(Player player, String worldName) {
        GDWorld gdWorld = getWorldCached(worldName);
        if (gdWorld == null) {
            return false;
        }

        if (!gdWorld.isLoaded()) {
            if (!loadWorld(gdWorld)) {
                return false;
            }
        }

        World world = gdWorld.getBukkitWorld();
        Location spawnLoc = gdWorld.getSpawnLocation();
        if (spawnLoc != null) {
            spawnLoc = spawnLoc.clone();
            spawnLoc.setWorld(world);
        } else {
            spawnLoc = world.getSpawnLocation();
        }

        player.teleport(spawnLoc);
        return true;
    }

    private GDWorld getWorldCached(String name) {
        if (plugin.getCacheProvider() != null) {
            GDWorld cached = plugin.getCacheProvider().get("world:" + name, GDWorld.class);
            if (cached != null) {
                return cached;
            }
        }
        return worlds.get(name);
    }

    public void setSpawnPoint(String worldName, Location location) {
        GDWorld gdWorld = worlds.get(worldName);
        if (gdWorld == null) return;

        gdWorld.setSpawnLocation(location.clone());
        if (gdWorld.isLoaded()) {
            gdWorld.getBukkitWorld().setSpawnLocation(location);
        }
        saveWorldAsync(gdWorld);
    }

    private void applyWorldSettings(GDWorld gdWorld, World world) {
        world.setKeepSpawnInMemory(gdWorld.isKeepSpawnInMemory());
        world.setAutoSave(true);
        world.setPVP(gdWorld.isPvp());

        try {
            world.setDifficulty(Difficulty.valueOf(gdWorld.getDifficulty().toUpperCase()));
        } catch (IllegalArgumentException e) {
            // 工业级优化: 难度值无效时使用默认值并记录警告
            plugin.getLogger().warning("Invalid difficulty: " + gdWorld.getDifficulty() + ", using NORMAL");
            world.setDifficulty(Difficulty.NORMAL);
        }

        world.setGameRule(GameRule.KEEP_INVENTORY, gdWorld.isKeepInventory());
        world.setGameRule(GameRule.DO_MOB_SPAWNING, gdWorld.isDoMobSpawning());
        world.setGameRule(GameRule.DO_FIRE_TICK, gdWorld.isDoFireTick());
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, gdWorld.isAllowWeather());
        world.setGameRule(GameRule.NATURAL_REGENERATION, gdWorld.isHunger());
    }

    private ChunkGenerator getGenerator(String name) {
        if ("void".equalsIgnoreCase(name)) {
            return new VoidWorldGenerator();
        }
        return null;
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    public void saveWorldAsync(GDWorld world) {
        if (plugin.getAsyncExecutor() != null) {
            plugin.getAsyncExecutor().execute(() -> {
                synchronized (saveLock) {
                    configManager.saveWorld(world);
                }
            });
        } else {
            configManager.saveWorld(world);
        }
    }

    public void saveAllWorlds() {
        if (plugin.getAsyncExecutor() != null) {
            plugin.getAsyncExecutor().execute(() -> {
                synchronized (saveLock) {
                    for (GDWorld gdWorld : worlds.values()) {
                        if (gdWorld.isLoaded()) {
                            configManager.saveWorld(gdWorld);
                        }
                    }
                }
            });
        } else {
            for (GDWorld gdWorld : worlds.values()) {
                if (gdWorld.isLoaded()) {
                    configManager.saveWorld(gdWorld);
                }
            }
        }
    }

    public GDWorld getWorld(String name) {
        return getWorldCached(name);
    }

    public GDWorld getWorld(World world) {
        return getWorldCached(world.getName());
    }

    public Collection<GDWorld> getAllWorlds() {
        return worlds.values();
    }

    public int getWorldCount() {
        return worlds.size();
    }

    public List<String> getWorldNames() {
        return new ArrayList<>(worlds.keySet());
    }
}
