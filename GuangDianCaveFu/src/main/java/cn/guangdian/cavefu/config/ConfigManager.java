package cn.guangdian.cavefu.config;

import cn.guangdian.cavefu.GuangDianCaveFu;
import cn.guangdian.cavefu.cave.CaveLevel;
import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置管理器
 */
public class ConfigManager {
    private final GuangDianCaveFu plugin;
    private FileConfiguration config;
    private FileConfiguration levelsConfig;
    private final File levelsFile;

    private final Map<Integer, CaveLevel> caveLevels = new HashMap<>();

    public ConfigManager(GuangDianCaveFu plugin) {
        this.plugin = plugin;
        this.levelsFile = new File(plugin.getDataFolder(), "levels.yml");
    }

    public void load() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();

        if (!levelsFile.exists()) {
            plugin.saveResource("levels.yml", false);
        }
        levelsConfig = YamlConfiguration.loadConfiguration(levelsFile);

        loadLevels();
    }

    private void loadLevels() {
        caveLevels.clear();

        plugin.getLogger().info("开始加载等级配置...");

        // 使用getConfigurationSection获取嵌套配置
        var levelsSection = levelsConfig.getConfigurationSection("levels");
        
        if (levelsSection == null) {
            plugin.getLogger().warning("找不到levels配置节！");
            return;
        }
        
        plugin.getLogger().info("levelsSection存在，键数量: " + levelsSection.getKeys(false).size());

        for (String key : levelsSection.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                
                var levelSection = levelsSection.getConfigurationSection(key);
                if (levelSection == null) {
                    plugin.getLogger().warning("等级 " + key + " 配置无效！");
                    continue;
                }
                
                String name = levelSection.getString("name", "等级" + level);
                int size = levelSection.getInt("size", 4);
                int height = levelSection.getInt("height", 6);
                java.util.List<String> upgradeCost = levelSection.getStringList("upgrade-cost");
                
                caveLevels.put(level, new CaveLevel(level, name, size, height, upgradeCost));
                plugin.getLogger().info("成功加载等级: " + level + " - " + name);
                
            } catch (Exception e) {
                plugin.getLogger().warning("加载等级配置失败: " + key + " - " + e.getMessage());
            }
        }

        plugin.getLogger().info("已加载 " + caveLevels.size() + " 个洞府等级");
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        levelsConfig = YamlConfiguration.loadConfiguration(levelsFile);
        loadLevels();
    }

    public CaveLevel getLevel(int level) {
        return caveLevels.get(level);
    }

    public CaveLevel getNextLevel(int currentLevel) {
        return caveLevels.get(currentLevel + 1);
    }

    public int getMaxLevel() {
        return caveLevels.keySet().stream().max(Integer::compare).orElse(1);
    }

    // 世界配置
    public String getWorldName() {
        return config.getString("world.name", "CaveFuWorld");
    }

    public int getViewDistance() {
        return config.getInt("world.view-distance", 2);
    }

    public int getGridSize() {
        return config.getInt("world.grid-size", 48);
    }

    public int getBaseY() {
        return config.getInt("world.base-y", 64);
    }

    public String getPlatformBlock() {
        return config.getString("world.platform-block", "BEDROCK");
    }

    public String getSurfaceBlock() {
        return config.getString("world.surface-block", "GRASS_BLOCK");
    }

    // 设置
    public int getDefaultLevel() {
        return config.getInt("settings.default-level", 1);
    }

    public int getMaxMembers() {
        return config.getInt("settings.max-members", 10);
    }

    public boolean isAllowVisitor() {
        return config.getBoolean("settings.allow-visitor", true);
    }

    public String getCreateCost() {
        return config.getString("settings.create-cost", "");
    }

    // 消息
    public String getMessage(String key) {
        String prefix = config.getString("messages.prefix", "<gold>[洞府] <white>");
        String msg = config.getString("messages." + key, "");
        // 返回原始字符串，由调用方使用 MiniMessageService 解析
        return prefix + msg;
    }

    public String getMessage(String key, String... placeholders) {
        String msg = getMessage(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return msg;
    }

    public String getRawMessage(String key) {
        return config.getString("messages." + key, "");
    }
}