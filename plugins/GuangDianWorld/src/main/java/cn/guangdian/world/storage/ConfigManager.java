package cn.guangdian.world.storage;

import cn.guangdian.world.GuangDianWorld;
import cn.guangdian.world.model.GDWorld;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final GuangDianWorld plugin;
    private FileConfiguration config;
    private FileConfiguration worldsConfig;
    private File worldsFile;
    private final Map<String, String> messages = new HashMap<>();

    public ConfigManager(GuangDianWorld plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        loadMessages();

        worldsFile = new File(plugin.getDataFolder(), "worlds.yml");
        if (!worldsFile.exists()) {
            plugin.saveResource("worlds.yml", false);
        }
        worldsConfig = YamlConfiguration.loadConfiguration(worldsFile);
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadMessages();
        worldsConfig = YamlConfiguration.loadConfiguration(worldsFile);
    }

    private void loadMessages() {
        messages.clear();
        ConfigurationSection msgSection = config.getConfigurationSection("messages");
        if (msgSection != null) {
            for (String key : msgSection.getKeys(false)) {
                messages.put(key, msgSection.getString(key, ""));
            }
        }
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "");
    }

    public String getMessage(String key, String... placeholders) {
        String msg = getMessage(key);
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return msg;
    }

    public String getPrefix() {
        return config.getString("settings.prefix", "&6[世界] &f");
    }

    public String getLanguage() {
        return config.getString("settings.language", "zh_cn");
    }

    public boolean isDebug() {
        return config.getBoolean("settings.debug", false);
    }

    public int getAutoSaveInterval() {
        return config.getInt("settings.auto-save-interval", 6000);
    }

    public String getDefaultWorld() {
        return config.getString("settings.default-world", "world");
    }

    public String getFirstSpawnWorld() {
        return config.getString("spawn-worlds.first-spawn-world", "spawn");
    }

    public boolean isFirstSpawnOverride() {
        return config.getBoolean("spawn-worlds.first-spawn-override", true);
    }

    public String getJoinDestination() {
        return config.getString("spawn-worlds.join-destination", "");
    }

    public boolean isRespawnInOverworld() {
        return config.getBoolean("spawn-worlds.respawn-in-overworld", true);
    }

    public Map<String, GDWorld> loadWorlds() {
        Map<String, GDWorld> worlds = new HashMap<>();
        ConfigurationSection worldsSection = worldsConfig.getConfigurationSection("worlds");
        if (worldsSection == null) return worlds;

        for (String worldName : worldsSection.getKeys(false)) {
            ConfigurationSection worldSection = worldsSection.getConfigurationSection(worldName);
            if (worldSection == null) continue;

            GDWorld gdWorld = new GDWorld(worldName);
            gdWorld.setAlias(worldSection.getString("alias", ""));
            gdWorld.setEnvironment(parseEnvironment(worldSection.getString("environment", "normal")));
            gdWorld.setDifficulty(worldSection.getString("difficulty", "normal"));
            gdWorld.setGamemode(worldSection.getString("gamemode", "survival"));
            gdWorld.setPvp(worldSection.getBoolean("pvp", true));
            gdWorld.setAllowFlight(worldSection.getBoolean("allow-flight", false));
            gdWorld.setAllowWeather(worldSection.getBoolean("allow-weather", true));
            gdWorld.setHunger(worldSection.getBoolean("hunger", true));
            gdWorld.setKeepSpawnInMemory(worldSection.getBoolean("keep-spawn-in-memory", false));
            gdWorld.setAutoLoad(worldSection.getBoolean("auto-load", true));
            gdWorld.setDoMobSpawning(worldSection.getBoolean("do-mob-spawning", true));
            gdWorld.setDoFireTick(worldSection.getBoolean("do-fire-tick", true));
            gdWorld.setKeepInventory(worldSection.getBoolean("keep-inventory", false));
            gdWorld.setRespawnWorld(worldSection.getString("respawn-world", ""));
            gdWorld.setGenerator(worldSection.getString("generator", ""));

            ConfigurationSection spawnSection = worldSection.getConfigurationSection("spawn-location");
            if (spawnSection != null) {
                gdWorld.setSpawnLocation(new Location(
                    null,
                    spawnSection.getDouble("x", 0),
                    spawnSection.getDouble("y", 64),
                    spawnSection.getDouble("z", 0),
                    (float) spawnSection.getDouble("yaw", 0),
                    (float) spawnSection.getDouble("pitch", 0)
                ));
            }

            worlds.put(worldName, gdWorld);
        }
        return worlds;
    }

    public void saveWorld(GDWorld world) {
        ConfigurationSection worldsSection = worldsConfig.getConfigurationSection("worlds");
        if (worldsSection == null) {
            worldsSection = worldsConfig.createSection("worlds");
        }

        ConfigurationSection worldSection = worldsSection.createSection(world.getName());
        worldSection.set("alias", world.getAlias() != null ? world.getAlias() : "");
        worldSection.set("environment", world.getEnvironment().name().toLowerCase());
        worldSection.set("difficulty", world.getDifficulty());
        worldSection.set("gamemode", world.getGamemode());
        worldSection.set("pvp", world.isPvp());
        worldSection.set("allow-flight", world.isAllowFlight());
        worldSection.set("allow-weather", world.isAllowWeather());
        worldSection.set("hunger", world.isHunger());
        worldSection.set("keep-spawn-in-memory", world.isKeepSpawnInMemory());
        worldSection.set("auto-load", world.isAutoLoad());
        worldSection.set("do-mob-spawning", world.isDoMobSpawning());
        worldSection.set("do-fire-tick", world.isDoFireTick());
        worldSection.set("keep-inventory", world.isKeepInventory());
        worldSection.set("respawn-world", world.getRespawnWorld() != null ? world.getRespawnWorld() : "");
        worldSection.set("generator", world.getGenerator() != null ? world.getGenerator() : "");

        if (world.getSpawnLocation() != null) {
            ConfigurationSection spawnSection = worldSection.createSection("spawn-location");
            Location loc = world.getSpawnLocation();
            spawnSection.set("x", loc.getX());
            spawnSection.set("y", loc.getY());
            spawnSection.set("z", loc.getZ());
            spawnSection.set("yaw", loc.getYaw());
            spawnSection.set("pitch", loc.getPitch());
        }

        saveWorldsConfig();
    }

    public void deleteWorld(String worldName) {
        ConfigurationSection worldsSection = worldsConfig.getConfigurationSection("worlds");
        if (worldsSection != null) {
            worldsSection.set(worldName, null);
            saveWorldsConfig();
        }
    }

    private void saveWorldsConfig() {
        try {
            worldsConfig.save(worldsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存 worlds.yml 失败: " + e.getMessage());
        }
    }

    private World.Environment parseEnvironment(String env) {
        return switch (env.toLowerCase()) {
            case "nether" -> World.Environment.NETHER;
            case "the_end", "end" -> World.Environment.THE_END;
            default -> World.Environment.NORMAL;
        };
    }
}
