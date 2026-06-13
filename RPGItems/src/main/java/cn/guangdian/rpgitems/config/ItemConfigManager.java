package cn.guangdian.rpgitems.config;

import cn.guangdian.rpgitems.RPGItems;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * 物品配置管理器
 * 支持从 items/ 文件夹加载多个 .yml 文件
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

        // 创建统一的配置对象
        itemConfig = new YamlConfiguration();

        // 1. 优先加载 items/ 文件夹中的所有 .yml 文件
        File itemsFolder = new File(plugin.getDataFolder(), "items");
        if (itemsFolder.exists() && itemsFolder.isDirectory()) {
            loadItemsFromFolder(itemsFolder);
        }

        // 2. 向后兼容：如果 items.yml 存在也加载
        File itemFile = new File(plugin.getDataFolder(), "items.yml");
        if (itemFile.exists()) {
            FileConfiguration singleConfig = YamlConfiguration.loadConfiguration(itemFile);
            mergeConfiguration(singleConfig);
            plugin.getLogger().info("已加载 items.yml");
        }

        // 3. 如果 items/ 文件夹不存在且 items.yml 也不存在，创建默认的 items.yml
        if (!itemsFolder.exists() && !itemFile.exists()) {
            plugin.saveResource("items.yml", false);
            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(itemFile);
            mergeConfiguration(defaultConfig);
            plugin.getLogger().info("已创建默认 items.yml");
        }
    }

    /**
     * 从文件夹加载所有 .yml 文件 (支持子文件夹)
     */
    private void loadItemsFromFolder(File folder) {
        loadItemsFromFolder(folder, "");
    }

    /**
     * 从文件夹加载所有 .yml 文件 (递归)
     * @param folder 文件夹
     * @param relativePath 相对路径 (用于日志)
     */
    private void loadItemsFromFolder(File folder, String relativePath) {
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        // 按文件名排序，确保加载顺序一致
        Arrays.sort(files);

        for (File file : files) {
            if (file.isDirectory()) {
                // 递归加载子文件夹
                String subPath = relativePath.isEmpty() ? file.getName() : relativePath + "/" + file.getName();
                loadItemsFromFolder(file, subPath);
            } else if (file.getName().toLowerCase().endsWith(".yml")) {
                // 加载 YAML 文件
                try {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    int itemCount = config.getKeys(false).size();
                    mergeConfiguration(config);
                    String filePath = relativePath.isEmpty() ? file.getName() : relativePath + "/" + file.getName();
                    plugin.getLogger().info("已加载 items/" + filePath + " (" + itemCount + " 个物品)");
                } catch (Exception e) {
                    String filePath = relativePath.isEmpty() ? file.getName() : relativePath + "/" + file.getName();
                    plugin.getLogger().log(Level.SEVERE, "加载 items/" + filePath + " 失败: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 合并配置到主配置
     */
    private void mergeConfiguration(FileConfiguration source) {
        for (String key : source.getKeys(false)) {
            itemConfig.set(key, source.get(key));
        }
    }

    public FileConfiguration getItemConfig() {
        return itemConfig;
    }

    public void reload() {
        loadAll();
    }
}
