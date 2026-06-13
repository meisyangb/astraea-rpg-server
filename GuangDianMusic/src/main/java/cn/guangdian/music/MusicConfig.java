package cn.guangdian.music;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicConfig {

    private final GuangDianMusic plugin;
    private FileConfiguration config;
    
    private String musicDirectory;
    private List<String> supportedFormats;
    private float defaultVolume;
    private boolean fadeEnabled;
    private int fadeDuration;
    private int maxConcurrentTracks;
    private boolean loopByDefault;
    
    private Map<String, SceneMusic> scenes;
    private Map<String, Object> messages;
    private Map<String, Integer> cooldowns;

    public MusicConfig(GuangDianMusic plugin) {
        this.plugin = plugin;
    }

    public boolean load() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        loadSettings();
        return true;
    }

    private void loadSettings() {
        musicDirectory = config.getString("music.directory", "plugins/GuangDianMusic/music");
        supportedFormats = config.getStringList("music.supported-formats");
        
        defaultVolume = (float) config.getDouble("settings.default-volume", 0.5);
        fadeEnabled = config.getBoolean("settings.fade-enabled", true);
        fadeDuration = config.getInt("settings.fade-duration", 2000);
        maxConcurrentTracks = config.getInt("settings.max-concurrent-tracks", 3);
        loopByDefault = config.getBoolean("settings.loop-by-default", false);
        
        loadScenes();
        loadMessages();
        loadCooldowns();
    }

    private void loadScenes() {
        scenes = new HashMap<>();
        
        if (config.contains("scenes")) {
            for (String sceneName : config.getConfigurationSection("scenes").getKeys(false)) {
                String path = "scenes." + sceneName;
                String musicId = config.getString(path + ".music-id", "");
                float volume = (float) config.getDouble(path + ".volume", 0.6);
                boolean enabled = config.getBoolean(path + ".enabled", true);
                
                scenes.put(sceneName, new SceneMusic(sceneName, musicId, volume, enabled));
            }
        }
    }

    private void loadMessages() {
        messages = new HashMap<>();
        
        messages.put("prefix", config.getString("messages.prefix", "<yellow>[音乐] <reset>"));
        messages.put("now-playing", config.getString("messages.now-playing", "<green>正在播放: <white>%song%</white></green>"));
        messages.put("stopped", config.getString("messages.stopped", "<yellow>音乐已停止</yellow>"));
        messages.put("no-permission", config.getString("messages.no-permission", "<red>你没有权限使用此命令</red>"));
        messages.put("invalid-song", config.getString("messages.invalid-song", "<red>无效的音乐ID: <white>%song%</white></red>"));
        messages.put("volume-changed", config.getString("messages.volume-changed", "<green>音量已调整为: <white>%volume%%</white></green>"));
        messages.put("cooldown", config.getString("messages.cooldown", "<red>请等待 %time% 秒后再试</red>"));
    }

    private void loadCooldowns() {
        cooldowns = new HashMap<>();
        cooldowns.put("switch", config.getInt("cooldown.switch", 5));
        cooldowns.put("request", config.getInt("cooldown.request", 30));
    }

    public String getMessage(String key) {
        Object msg = messages.get(key);
        return msg != null ? msg.toString() : key;
    }

    public String getMusicDirectory() { return musicDirectory; }
    public List<String> getSupportedFormats() { return supportedFormats; }
    public float getDefaultVolume() { return defaultVolume; }
    public boolean isFadeEnabled() { return fadeEnabled; }
    public int getFadeDuration() { return fadeDuration; }
    public int getMaxConcurrentTracks() { return maxConcurrentTracks; }
    public boolean isLoopByDefault() { return loopByDefault; }
    public Map<String, SceneMusic> getScenes() { return scenes; }
    public Map<String, Integer> getCooldowns() { return cooldowns; }

    public int getCooldown(String type) {
        Integer cd = cooldowns.get(type);
        return cd != null ? cd : 0;
    }

    public SceneMusic getScene(String name) {
        return scenes.get(name);
    }

    public static class SceneMusic {
        private final String name;
        private final String musicId;
        private final float volume;
        private final boolean enabled;

        public SceneMusic(String name, String musicId, float volume, boolean enabled) {
            this.name = name;
            this.musicId = musicId;
            this.volume = volume;
            this.enabled = enabled;
        }

        public String getName() { return name; }
        public String getMusicId() { return musicId; }
        public float getVolume() { return volume; }
        public boolean isEnabled() { return enabled; }
    }
}