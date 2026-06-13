package cn.guangdian.soulbind.manager;

import cn.guangdian.soulbind.GuangDianSoulBind;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final GuangDianSoulBind plugin;
    private boolean preventDrop;
    private boolean preventContainer;
    private boolean preventTrade;
    private boolean keepOnDeath;
    private String loreFormat;

    public ConfigManager(GuangDianSoulBind plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();

        preventDrop = config.getBoolean("protection.prevent-drop", true);
        preventContainer = config.getBoolean("protection.prevent-container", true);
        preventTrade = config.getBoolean("protection.prevent-trade", true);
        keepOnDeath = config.getBoolean("protection.keep-on-death", true);

        loreFormat = config.getString("lore-format.format", "<dark_gray>灵魂绑定：<white>%player%");
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
    }

    public boolean isPreventDrop() {
        return preventDrop;
    }

    public boolean isPreventContainer() {
        return preventContainer;
    }

    public boolean isPreventTrade() {
        return preventTrade;
    }

    public boolean isKeepOnDeath() {
        return keepOnDeath;
    }

    public String getLoreFormat() {
        return loreFormat;
    }

    public String getMessage(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }
}
