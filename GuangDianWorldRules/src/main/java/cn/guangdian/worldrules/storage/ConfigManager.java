package cn.guangdian.worldrules.storage;

import cn.guangdian.worldrules.GuangDianWorldRules;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class ConfigManager {

    private final GuangDianWorldRules plugin;
    private File configFile;
    private FileConfiguration config;
    private boolean debug;

    public ConfigManager(GuangDianWorldRules plugin) {
        this.plugin = plugin;
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            saveDefaultConfig();
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        debug = config.getBoolean("debug", false);

        if (debug) {
            plugin.getLogger().info("调试模式已启用");
        }
    }

    public void reload() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        debug = config.getBoolean("debug", false);
    }

    public void save() {
        if (config == null || configFile == null) {
            return;
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存配置文件: " + e.getMessage());
        }
    }

    private void saveDefaultConfig() {
        try (InputStream in = plugin.getResource("config.yml")) {
            if (in != null) {
                Files.copy(in, configFile.toPath());
            } else {
                // 创建默认配置
                config = new YamlConfiguration();
                config.set("default-rules.keep-inventory", false);
                config.set("default-rules.keep-exp", false);
                config.set("default-rules.disable-natural-spawn", false);
                config.set("default-rules.disable-monster-spawn", false);
                config.set("default-rules.disable-animal-spawn", false);
                config.set("default-rules.disable-weather-change", false);
                config.set("default-rules.disable-time-change", false);
                config.set("default-rules.disable-hunger", false);
                config.set("default-rules.disable-fall-damage", false);
                config.set("default-rules.disable-fire-damage", false);
                config.set("default-rules.disable-drowning-damage", false);
                config.set("default-rules.disable-explosion-block-damage", false);
                config.set("default-rules.disable-mob-griefing", false);
                config.set("default-rules.pvp", true);
                config.set("default-rules.disable-item-drop", false);
                config.set("default-rules.disable-item-pickup", false);
                config.set("debug", false);
                save();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存默认配置文件: " + e.getMessage());
        }
    }

    public FileConfiguration getConfig() {
        if (config == null) {
            reload();
        }
        return config;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        config.set("debug", debug);
        save();
    }
}
