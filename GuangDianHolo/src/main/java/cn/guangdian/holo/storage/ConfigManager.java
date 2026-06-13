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
    
    // 缓存所有全息图数据，用于延迟加载
    private final Map<String, HologramData> hologramDataCache = new HashMap<>();

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
        
        // 预加载全息图数据到缓存
        preloadHologramData();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadMessages();
        hologramsConfig = YamlConfiguration.loadConfiguration(hologramsFile);
        hologramDataCache.clear();
        preloadHologramData();
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
        return config.getString("messages.prefix", "<aqua>[全息] <white>");
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

    public boolean isPlayerProximityEnabled() {
        return config.getBoolean("settings.player-proximity.enabled", true);
    }

    public double getPlayerProximityDistance() {
        return config.getDouble("settings.player-proximity.distance", 50.0);
    }
    
    /**
     * 预加载全息图数据到缓存（不创建实体）
     */
    private void preloadHologramData() {
        hologramDataCache.clear();
        ConfigurationSection holosSection = hologramsConfig.getConfigurationSection("holograms");
        if (holosSection == null) return;

        for (String holoName : holosSection.getKeys(false)) {
            ConfigurationSection holoSection = holosSection.getConfigurationSection(holoName);
            if (holoSection == null) continue;

            String worldName = holoSection.getString("world", "");
            double x = holoSection.getDouble("x", 0);
            double y = holoSection.getDouble("y", 0);
            double z = holoSection.getDouble("z", 0);
            float yaw = (float) holoSection.getDouble("yaw", 0);
            float pitch = (float) holoSection.getDouble("pitch", 0);
            double lineHeight = holoSection.getDouble("line-height", 0.3);
            int viewDistance = holoSection.getInt("view-distance", 20);
            boolean persistent = holoSection.getBoolean("persistent", true);
            List<String> lines = holoSection.getStringList("lines");

            HologramData data = new HologramData(
                holoName, worldName, x, y, z, yaw, pitch,
                lineHeight, viewDistance, persistent, lines
            );
            hologramDataCache.put(holoName, data);
        }
    }
    
    /**
     * 获取所有已缓存的全息图数据
     */
    public Map<String, HologramData> getHologramDataCache() {
        return new HashMap<>(hologramDataCache);
    }
    
    /**
     * 获取指定世界的全息图数据
     */
    public Map<String, HologramData> getHologramDataForWorld(String worldName) {
        Map<String, HologramData> result = new HashMap<>();
        for (Map.Entry<String, HologramData> entry : hologramDataCache.entrySet()) {
            if (entry.getValue().worldName.equalsIgnoreCase(worldName)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public Map<String, Hologram> loadHolograms() {
        Map<String, Hologram> holograms = new HashMap<>();
        
        // 尝试加载所有已缓存的全息图数据
        for (HologramData data : hologramDataCache.values()) {
            Hologram hologram = createHologramFromData(data);
            if (hologram != null) {
                holograms.put(data.name, hologram);
            }
        }
        
        return holograms;
    }
    
    /**
     * 从数据创建全息图（如果世界已加载）
     */
    public Hologram createHologramFromData(HologramData data) {
        World world = Bukkit.getWorld(data.worldName);
        if (world == null) return null;

        Location location = new Location(world, data.x, data.y, data.z, data.yaw, data.pitch);

        Hologram hologram = new Hologram(data.name, location);
        hologram.setLineHeight(data.lineHeight);
        hologram.setViewDistance(data.viewDistance);
        hologram.setPersistent(data.persistent);
        hologram.setLines(data.lines);

        return hologram;
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
        hologramDataCache.remove(name);
    }

    private void saveHologramsConfig() {
        try {
            hologramsConfig.save(hologramsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存 holograms.yml 失败: " + e.getMessage());
        }
    }
    
    /**
     * 全息图数据类 - 用于延迟加载
     */
    public static class HologramData {
        public final String name;
        public final String worldName;
        public final double x, y, z;
        public final float yaw, pitch;
        public final double lineHeight;
        public final int viewDistance;
        public final boolean persistent;
        public final List<String> lines;

        public HologramData(String name, String worldName, double x, double y, double z,
                           float yaw, float pitch, double lineHeight, int viewDistance,
                           boolean persistent, List<String> lines) {
            this.name = name;
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.lineHeight = lineHeight;
            this.viewDistance = viewDistance;
            this.persistent = persistent;
            this.lines = new ArrayList<>(lines);
        }
    }
}
