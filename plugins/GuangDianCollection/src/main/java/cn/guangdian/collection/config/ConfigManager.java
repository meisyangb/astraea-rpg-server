package cn.guangdian.collection.config;

import cn.guangdian.collection.GuangDianCollection;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    
    private final GuangDianCollection plugin;
    
    private boolean notifyPlayer;
    private String notifySound;
    private double notifyVolume;
    private double notifyPitch;
    private int saveInterval;
    
    private String prefix;
    
    public ConfigManager(GuangDianCollection plugin) {
        this.plugin = plugin;
        load();
    }
    
    public void load() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        
        notifyPlayer = config.getBoolean("settings.notify-player", true);
        notifySound = config.getString("settings.notify-sound", "ENTITY_PLAYER_LEVELUP");
        notifyVolume = config.getDouble("settings.notify-volume", 1.0);
        notifyPitch = config.getDouble("settings.notify-pitch", 1.0);
        saveInterval = config.getInt("settings.save-interval", 300);
        
        prefix = config.getString("messages.prefix", "§6[图鉴] §r");
    }
    
    public void reload() {
        plugin.reloadConfig();
        load();
    }
    
    public boolean isNotifyPlayer() { return notifyPlayer; }
    public String getNotifySound() { return notifySound; }
    public double getNotifyVolume() { return notifyVolume; }
    public double getNotifyPitch() { return notifyPitch; }
    public int getSaveInterval() { return saveInterval; }
    public String getPrefix() { return prefix; }
    
    public String getMessage(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }
}
