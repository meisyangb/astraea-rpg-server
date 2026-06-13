package cn.guangdian.raid.config;

import cn.guangdian.raid.GuangDianRaid;
import cn.guangdian.raid.model.Raid;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class RaidConfigManager {

    private final GuangDianRaid plugin;
    private final Map<String, Raid> raids;
    private FileConfiguration mainConfig;
    private File raidsFolder;

    public RaidConfigManager(GuangDianRaid plugin) {
        this.plugin = plugin;
        this.raids = new HashMap<>();
    }

    public void loadConfigs() {
        loadMainConfig();
        loadRaids();
    }

    private void loadMainConfig() {
        plugin.saveDefaultConfig();
        mainConfig = plugin.getConfig();
    }

    private void loadRaids() {
        raidsFolder = new File(plugin.getDataFolder(), "raids");
        if (!raidsFolder.exists()) {
            raidsFolder.mkdirs();
            saveDefaultRaid();
        }

        File[] files = raidsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                for (String key : config.getKeys(false)) {
                    ConfigurationSection section = config.getConfigurationSection(key);
                    if (section != null) {
                        Raid raid = Raid.fromConfig(key, section);
                        raids.put(key, raid);
                        plugin.getLogger().info("已加载副本: " + raid.getName());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "加载副本配置失败: " + file.getName(), e);
            }
        }
    }

    private void saveDefaultRaid() {
        File exampleFile = new File(raidsFolder, "example.yml");
        if (exampleFile.exists()) return;

        YamlConfiguration config = new YamlConfiguration();
        
        ConfigurationSection raid = config.createSection("blacksite_alpha");
        raid.set("name", "黑据点Alpha");
        raid.set("description", Arrays.asList(
            "§7潜入敌方据点，收集情报并撤离",
            "§c警告: 高难度副本"
        ));

        ConfigurationSection players = raid.createSection("players");
        players.set("min", 2);
        players.set("max", 4);

        raid.set("time_limit", 600);
        raid.set("world", "raid_blacksite");

        ConfigurationSection phases = raid.createSection("phases");
        
        ConfigurationSection search = phases.createSection("search");
        search.set("duration", 180);
        List<Map<String, Object>> searchObjectives = new ArrayList<>();
        Map<String, Object> obj1 = new HashMap<>();
        obj1.put("type", "COLLECT_INTEL");
        obj1.put("target", "intel_document");
        obj1.put("amount", 3);
        searchObjectives.add(obj1);
        search.set("objectives", searchObjectives);

        ConfigurationSection combat = phases.createSection("combat");
        combat.set("duration", 300);
        List<Map<String, Object>> combatObjectives = new ArrayList<>();
        Map<String, Object> obj2 = new HashMap<>();
        obj2.put("type", "KILL_MOBS");
        obj2.put("target", "elite_soldier");
        obj2.put("amount", 10);
        combatObjectives.add(obj2);
        Map<String, Object> obj3 = new HashMap<>();
        obj3.put("type", "KILL_BOSS");
        obj3.put("target", "commander_boss");
        combatObjectives.add(obj3);
        combat.set("objectives", combatObjectives);

        ConfigurationSection extract = phases.createSection("extract");
        extract.set("duration", 120);
        List<Map<String, Object>> extractObjectives = new ArrayList<>();
        Map<String, Object> obj4 = new HashMap<>();
        obj4.put("type", "REACH_EXTRACTION");
        obj4.put("target", "extraction_alpha");
        extractObjectives.add(obj4);
        extract.set("objectives", extractObjectives);

        ConfigurationSection extractionPoints = raid.createSection("extraction_points");
        ConfigurationSection extAlpha = extractionPoints.createSection("extraction_alpha");
        ConfigurationSection extLoc = extAlpha.createSection("location");
        extLoc.set("world", "raid_blacksite");
        extLoc.set("x", 100);
        extLoc.set("y", 64);
        extLoc.set("z", 200);
        extAlpha.set("radius", 5.0);
        extAlpha.set("extraction_time", 10);
        extAlpha.set("requires_intel", true);
        extAlpha.set("min_intel", 2);

        ConfigurationSection intelItems = raid.createSection("intel_items");
        ConfigurationSection intelDoc = intelItems.createSection("intel_document");
        intelDoc.set("name", "§e机密文件");
        intelDoc.set("item", "PAPER");
        intelDoc.set("value", 1);
        intelDoc.set("unlock_areas", Collections.singletonList("sector_b"));
        List<Map<String, Object>> spawnLocs = new ArrayList<>();
        Map<String, Object> loc1 = new HashMap<>();
        loc1.put("x", 50);
        loc1.put("y", 64);
        loc1.put("z", 50);
        spawnLocs.add(loc1);
        intelDoc.set("spawn_locations", spawnLocs);

        ConfigurationSection rewards = raid.createSection("rewards");
        rewards.set("base_points", 1000);
        rewards.set("base_exp", 500);
        rewards.set("bonus_per_intel", 100);
        rewards.set("bonus_per_kill", 10);
        List<Map<String, Object>> rewardItems = new ArrayList<>();
        Map<String, Object> item1 = new HashMap<>();
        item1.put("type", "rifle_blueprint");
        item1.put("chance", 0.3);
        rewardItems.add(item1);
        rewards.set("items", rewardItems);

        try {
            config.save(exampleFile);
            plugin.getLogger().info("已创建示例副本配置: " + exampleFile.getName());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "保存示例配置失败", e);
        }
    }

    public Raid getRaid(String raidId) {
        return raids.get(raidId);
    }

    public List<String> getRaidIds() {
        return new ArrayList<>(raids.keySet());
    }

    public Collection<Raid> getAllRaids() {
        return raids.values();
    }

    public void reload() {
        raids.clear();
        loadConfigs();
    }
}
