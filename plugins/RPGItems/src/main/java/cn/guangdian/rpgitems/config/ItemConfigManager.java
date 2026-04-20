package cn.guangdian.rpgitems.config;

import cn.guangdian.rpgitems.RPGItems;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * 物品配置管理器
 */
public class ItemConfigManager {

    private final RPGItems plugin;
    private FileConfiguration itemConfig;

    public ItemConfigManager(RPGItems plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        // 保存默认配置
        plugin.saveDefaultConfig();

        // 加载物品配置
        File itemFile = new File(plugin.getDataFolder(), "items.yml");
        if (!itemFile.exists()) {
            plugin.saveResource("items.yml", false);
        }
        itemConfig = YamlConfiguration.loadConfiguration(itemFile);
    }

    public FileConfiguration getItemConfig() {
        return itemConfig;
    }

    public void reload() {
        loadAll();
    }
}
