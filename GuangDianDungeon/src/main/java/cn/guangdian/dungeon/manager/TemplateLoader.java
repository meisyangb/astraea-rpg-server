package cn.guangdian.dungeon.manager;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.*;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TemplateLoader {

    private final GuangDianDungeon plugin;
    private final File dungeonsDir;
    private final Map<String, DungeonTemplate> templates;

    public TemplateLoader(GuangDianDungeon plugin, File dungeonsDir) {
        this.plugin = plugin;
        this.dungeonsDir = dungeonsDir;
        this.templates = new ConcurrentHashMap<>();
    }

    public void loadAll() {
        templates.clear();

        File[] files = dungeonsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                DungeonTemplate template = loadTemplate(file);
                if (template != null) {
                    templates.put(template.getId(), template);
                    plugin.getLogger().info("加载副本模板: " + template.getId());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("加载副本模板失败: " + file.getName() + " - " + e.getMessage());
            }
        }
    }

    public DungeonTemplate loadTemplate(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String id = config.getString("id");
        String name = config.getString("name", id);
        String description = config.getString("description", "");
        String worldTemplate = config.getString("world.template", config.getString("world-template", id));

        DungeonSettings settings = loadSettings(config);
        List<Difficulty> difficulties = loadDifficulties(config);
        Map<String, RewardPool> rewardPools = loadRewardPools(config);
        List<RewardDefinition> firstClearRewards = new ArrayList<>();
        List<ScoreReward> scoreRewards = new ArrayList<>();

        return new DungeonTemplate(id, name, description, worldTemplate, settings,
            difficulties, rewardPools, firstClearRewards, scoreRewards);
    }

    private DungeonSettings loadSettings(YamlConfiguration config) {
        var settingsSection = config.getConfigurationSection("settings");
        if (settingsSection == null) {
            return new DungeonSettings(5, 1, 1800, 3600, 10, 30, 0, 0, null, null, 1);
        }

        return new DungeonSettings(
            settingsSection.getInt("max-players", 5),
            settingsSection.getInt("min-players", 1),
            settingsSection.getInt("time-limit", 1800),
            settingsSection.getInt("cooldown", 3600),
            settingsSection.getInt("max-deaths", 10),
            settingsSection.getInt("revive-cooldown", 30),
            settingsSection.getInt("requirements.min-level", 0),
            settingsSection.getInt("requirements.max-level", 0),
            settingsSection.getString("requirements.permission", null),
            settingsSection.getString("icon-material", null),
            settingsSection.getInt("recommended-level", 1)
        );
    }

    private List<Difficulty> loadDifficulties(YamlConfiguration config) {
        List<Difficulty> difficulties = new ArrayList<>();
        var difficultiesSection = config.getConfigurationSection("difficulties");
        
        if (difficultiesSection == null) {
            difficulties.add(new Difficulty("normal", "普通", 1.0, 1.0, 1.0, 1.0, 1.0, 0, 0));
            return difficulties;
        }

        for (String key : difficultiesSection.getKeys(false)) {
            var diffSection = difficultiesSection.getConfigurationSection(key);
            if (diffSection == null) continue;

            difficulties.add(new Difficulty(
                key,
                diffSection.getString("name", key),
                diffSection.getDouble("health-multiplier", 1.0),
                diffSection.getDouble("damage-multiplier", 1.0),
                diffSection.getDouble("mob-count-multiplier", 1.0),
                diffSection.getDouble("reward-multiplier", 1.0),
                diffSection.getDouble("exp-multiplier", 1.0),
                diffSection.getInt("time-limit-modifier", 0),
                diffSection.getInt("max-deaths-modifier", 0)
            ));
        }

        return difficulties;
    }

    private Map<String, RewardPool> loadRewardPools(YamlConfiguration config) {
        Map<String, RewardPool> pools = new HashMap<>();
        var poolsSection = config.getConfigurationSection("rewards");
        if (poolsSection == null) return pools;

        for (String poolKey : poolsSection.getKeys(false)) {
            var poolSection = poolsSection.getConfigurationSection(poolKey);
            if (poolSection == null) continue;

            List<RewardEntry> entries = new ArrayList<>();
            var items = poolSection.getStringList("items");
            for (String itemStr : items) {
                String[] parts = itemStr.split("\\s+");
                if (parts.length >= 2) {
                    String itemId = parts[0];
                    int amount = 1;
                    double chance = 1.0;
                    try {
                        amount = Integer.parseInt(parts[1]);
                        if (parts.length >= 3) {
                            chance = Double.parseDouble(parts[2].replace("%", "")) / 100.0;
                        }
                    } catch (NumberFormatException ignored) {}
                    entries.add(new RewardEntry(itemId, amount, chance));
                }
            }

            // 解析经验奖励
            int expMin = poolSection.getInt("exp-min", 0);
            int expMax = poolSection.getInt("exp-max", 0);

            // 解析金钱奖励
            double moneyMin = poolSection.getDouble("money-min", 0);
            double moneyMax = poolSection.getDouble("money-max", 0);

            RewardPool pool = new RewardPool(poolKey, entries,
                new RewardPool.RewardRange(expMin, expMax),
                new RewardPool.RewardRange(moneyMin, moneyMax));
            pools.put(poolKey, pool);
        }

        return pools;
    }

    public DungeonTemplate getTemplate(String id) {
        return templates.get(id);
    }

    public boolean hasTemplate(String id) {
        return templates.containsKey(id);
    }

    public Collection<DungeonTemplate> getAllTemplates() {
        return Collections.unmodifiableCollection(templates.values());
    }

    public Set<String> getTemplateIds() {
        return Collections.unmodifiableSet(templates.keySet());
    }

    public int getTemplateCount() {
        return templates.size();
    }
}
