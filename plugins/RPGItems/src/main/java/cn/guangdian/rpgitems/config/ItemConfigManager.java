package cn.guangdian.rpgitems.config;

import cn.guangdian.rpgitems.RPGItems;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * 物品配置管理器
 * 支持多配置文件加载（包括中文文件名和子文件夹递归）
 */
public class ItemConfigManager {

    private final RPGItems plugin;
    private final List<FileConfiguration> allConfigs = new ArrayList<>();

    public ItemConfigManager(RPGItems plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.saveDefaultConfig();
        allConfigs.clear();

        // 加载 items 文件夹下的所有配置文件（递归子文件夹）
        File itemsFolder = new File(plugin.getDataFolder(), "items");
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs();
            createDefaultItemFiles(itemsFolder);
        }

        // 递归加载所有 .yml 文件
        loadYamlFilesRecursive(itemsFolder, "items");

        plugin.getLogger().info("共加载 " + allConfigs.size() + " 个配置文件");
    }

    /**
     * 递归加载文件夹中的所有 .yml 文件
     */
    private void loadYamlFilesRecursive(File folder, String relativePath) {
        // 加载当前文件夹中的 .yml 文件
        File[] ymlFiles = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".yml");
            }
        });

        if (ymlFiles != null) {
            for (File file : ymlFiles) {
                try {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    if (config != null && !config.getKeys(false).isEmpty()) {
                        allConfigs.add(config);
                        plugin.getLogger().info("已加载物品配置: " + relativePath + "/" + file.getName());
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("加载物品配置失败: " + file.getName() + " - " + e.getMessage());
                }
            }
        }

        // 递归处理子文件夹
        File[] subDirs = folder.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                loadYamlFilesRecursive(subDir, relativePath + "/" + subDir.getName());
            }
        }
    }

    /**
     * 创建默认的物品配置文件示例
     */
    private void createDefaultItemFiles(File itemsFolder) {
        // 创建武器配置文件示例
        File weaponsFile = new File(itemsFolder, "武器.yml");
        if (!weaponsFile.exists()) {
            try {
                weaponsFile.createNewFile();
                YamlConfiguration config = new YamlConfiguration();
                config.set("# 武器配置", "在此文件中配置所有武器类物品");
                config.set("示例武器.Id", "DIAMOND_SWORD");
                config.set("示例武器.Display", "<yellow>示例武器");
                config.set("示例武器.Lore", java.util.Arrays.asList(
                    "<gray>这是一把示例武器",
                    "<gray>请在游戏中使用 /rpgitem 命令管理物品"
                ));
                config.set("示例武器.Options.Unbreakable", true);
                config.save(weaponsFile);
            } catch (Exception e) {
                plugin.getLogger().warning("创建默认武器配置失败: " + e.getMessage());
            }
        }

        // 创建防具配置文件示例
        File armorsFile = new File(itemsFolder, "防具.yml");
        if (!armorsFile.exists()) {
            try {
                armorsFile.createNewFile();
                YamlConfiguration config = new YamlConfiguration();
                config.set("# 防具配置", "在此文件中配置所有防具类物品");
                config.set("示例头盔.Id", "DIAMOND_HELMET");
                config.set("示例头盔.Display", "<yellow>示例头盔");
                config.set("示例头盔.Lore", java.util.Arrays.asList(
                    "<gray>这是一件示例防具"
                ));
                config.set("示例头盔.Options.Unbreakable", true);
                config.save(armorsFile);
            } catch (Exception e) {
                plugin.getLogger().warning("创建默认防具配置失败: " + e.getMessage());
            }
        }

        // 创建材料配置文件示例
        File materialsFile = new File(itemsFolder, "材料.yml");
        if (!materialsFile.exists()) {
            try {
                materialsFile.createNewFile();
                YamlConfiguration config = new YamlConfiguration();
                config.set("# 材料配置", "在此文件中配置所有材料类物品");
                config.set("示例材料.Id", "EMERALD");
                config.set("示例材料.Display", "<aqua>示例材料");
                config.set("示例材料.Lore", java.util.Arrays.asList(
                    "<gray>这是一种示例材料"
                ));
                config.save(materialsFile);
            } catch (Exception e) {
                plugin.getLogger().warning("创建默认材料配置失败: " + e.getMessage());
            }
        }
    }

    /**
     * 获取所有加载的配置文件
     */
    public List<FileConfiguration> getAllConfigs() {
        return new ArrayList<>(allConfigs);
    }

    public void reload() {
        loadAll();
    }
}
