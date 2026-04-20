package cn.guangdian.holo.storage;

import cn.guangdian.holo.GuangDianHolo;
import cn.guangdian.holo.model.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final GuangDianHolo plugin;
    private FileConfiguration config;
    private FileConfiguration hologramsConfig;
    private File hologramsFile;
    private final Map<String, String> messages = new HashMap<>();

    public ConfigManager(GuangDianHolo plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        loadMessages();

        hologramsFile = new File(plugin.getDataFolder(), "holograms.yml");
        if (!hologramsFile.exists()) {
            plugin.saveResource("holograms.yml", false);
        }
        hologramsConfig = YamlConfiguration.loadConfiguration(hologramsFile);
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadMessages();
        hologramsConfig = YamlConfiguration.loadConfiguration(hologramsFile);
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
        return config.getString("messages.prefix", "&b[全息] &f");
    }

    public String getLanguage() {
        return config.getString("settings.language", "zh_cn");
    }

    public boolean isDebug() {
        return config.getBoolean("settings.debug", false);
    }

    public int getAutoSaveInterval() {
        return config.getInt("settings.auto-save-interval", 900);
    }

    public int getVisibilityDistance() {
        return config.getInt("settings.visibility-distance", 20);
    }

    public int getUpdateInterval() {
        return config.getInt("settings.update-interval-ticks", 10);
    }

    public double getDefaultLineHeight() {
        return config.getDouble("defaults.line-height", 0.3);
    }

    public long getSpawnDelay() {
        return config.getLong("settings.spawn-delay-on-join-ms", 300) / 50;
    }

    public Map<String, Hologram> loadHolograms() {
        Map<String, Hologram> holograms = new HashMap<>();
        ConfigurationSection holosSection = hologramsConfig.getConfigurationSection("holograms");
        if (holosSection == null) return holograms;

        for (String holoName : holosSection.getKeys(false)) {
            ConfigurationSection holoSection = holosSection.getConfigurationSection(holoName);
            if (holoSection == null) continue;

            String worldName = holoSection.getString("world", "");
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            double x = holoSection.getDouble("x", 0);
            double y = holoSection.getDouble("y", 0);
            double z = holoSection.getDouble("z", 0);
            float yaw = (float) holoSection.getDouble("yaw", 0);
            float pitch = (float) holoSection.getDouble("pitch", 0);

            Location location = new Location(world, x, y, z, yaw, pitch);

            Hologram hologram = new Hologram(holoName, location);
            hologram.setLineHeight(holoSection.getDouble("line-height", 0.3));
            hologram.setViewDistance(holoSection.getInt("view-distance", 20));
            hologram.setPersistent(holoSection.getBoolean("persistent", true));

            List<String> lines = holoSection.getStringList("lines");
            hologram.setLines(lines);

            holograms.put(holoName, hologram);
        }
        return holograms;
    }

    public void saveHologram(Hologram hologram) {
        ConfigurationSection holosSection = hologramsConfig.getConfigurationSection("holograms");
        if (holosSection == null) {
            holosSection = hologramsConfig.createSection("holograms");
        }

        ConfigurationSection holoSection = holosSection.createSection(hologram.getName());
        
        Location loc = hologram.getLocation();
        if (loc != null && loc.getWorld() != null) {
            holoSection.set("world", loc.getWorld().getName());
            holoSection.set("x", loc.getX());
            holoSection.set("y", loc.getY());
            holoSection.set("z", loc.getZ());
            holoSection.set("yaw", loc.getYaw());
            holoSection.set("pitch", loc.getPitch());
        }

        holoSection.set("line-height", hologram.getLineHeight());
        holoSection.set("view-distance", hologram.getViewDistance());
        holoSection.set("persistent", hologram.isPersistent());
        holoSection.set("lines", hologram.getLines());

        saveHologramsConfig();
    }

    public void deleteHologram(String name) {
        ConfigurationSection holosSection = hologramsConfig.getConfigurationSection("holograms");
        if (holosSection != null) {
            holosSection.set(name, null);
            saveHologramsConfig();
        }
    }

    private void saveHologramsConfig() {
        try {
            hologramsConfig.save(hologramsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存 holograms.yml 失败: " + e.getMessage());
        }
    }
}
