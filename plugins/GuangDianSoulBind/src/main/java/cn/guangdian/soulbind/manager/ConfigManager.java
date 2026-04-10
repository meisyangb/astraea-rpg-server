package cn.guangdian.soulbind.manager;

import cn.guangdian.soulbind.GuangDianSoulBind;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {

    private final GuangDianSoulBind plugin;
    private boolean autoBindOnPickup;
    private boolean mythicMobsOnly;
    private List<String> mythicItemIds;
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

        autoBindOnPickup = config.getBoolean("binding.auto-bind-on-pickup", true);
        mythicMobsOnly = config.getBoolean("binding.mythic-mobs-only", true);
        mythicItemIds = config.getStringList("binding.mythic-item-ids");

        preventDrop = config.getBoolean("protection.prevent-drop", true);
        preventContainer = config.getBoolean("protection.prevent-container", true);
        preventTrade = config.getBoolean("protection.prevent-trade", true);
        keepOnDeath = config.getBoolean("protection.keep-on-death", true);

        loreFormat = config.getString("lore-format.format", "&8灵魂绑定：&f%player%");
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
    }

    public boolean isAutoBindOnPickup() {
        return autoBindOnPickup;
    }

    public boolean isMythicMobsOnly() {
        return mythicMobsOnly;
    }

    public List<String> getMythicItemIds() {
        return mythicItemIds;
    }

    public boolean shouldBindItem(String mythicItemId) {
        if (!autoBindOnPickup) return false;
        if (mythicItemIds.isEmpty()) return true;
        return mythicItemIds.contains(mythicItemId);
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
