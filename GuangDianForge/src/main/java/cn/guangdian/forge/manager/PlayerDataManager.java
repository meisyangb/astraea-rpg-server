package cn.guangdian.forge.manager;

import cn.guangdian.forge.GuangDianForge;
import cn.guangdian.forge.model.PlayerForgeData;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家数据管理器
 */
public class PlayerDataManager {
    private final GuangDianForge plugin;
    private final Map<UUID, PlayerForgeData> cache = new ConcurrentHashMap<>();
    private final File dataFolder;

    public PlayerDataManager(GuangDianForge plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public PlayerForgeData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::load);
    }

    public PlayerForgeData load(UUID uuid) {
        File file = new File(dataFolder, uuid.toString() + ".yml");
        if (!file.exists()) {
            return new PlayerForgeData(uuid);
        }
        
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        PlayerForgeData data = new PlayerForgeData(uuid);
        data.setForgeLevel(cfg.getInt("forgeLevel", 1));
        data.setForgeExp(cfg.getLong("forgeExp", 0));
        data.setLearnedRecipes(new HashSet<>(cfg.getStringList("recipes")));
        data.setTotalForges(cfg.getInt("totalForges", 0));
        data.setSuccessForges(cfg.getInt("successForges", 0));
        return data;
    }

    public void save(PlayerForgeData data) {
        File file = new File(dataFolder, data.getPlayerId().toString() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("forgeLevel", data.getForgeLevel());
        cfg.set("forgeExp", data.getForgeExp());
        cfg.set("recipes", new ArrayList<>(data.getLearnedRecipes()));
        cfg.set("totalForges", data.getTotalForges());
        cfg.set("successForges", data.getSuccessForges());
        
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("保存玩家数据失败: " + data.getPlayerId());
        }
    }

    public void unload(UUID uuid) {
        PlayerForgeData data = cache.remove(uuid);
        if (data != null) {
            save(data);
        }
    }

    public void addExp(PlayerForgeData data, long amount) {
        data.setForgeExp(data.getForgeExp() + amount);
        checkLevelUp(data);
    }

    public boolean checkLevelUp(PlayerForgeData data) {
        int currentLevel = data.getForgeLevel();
        long currentExp = data.getForgeExp();
        
        Map<Integer, Long> thresholds = getLevelThresholds();
        
        for (Map.Entry<Integer, Long> entry : thresholds.entrySet()) {
            if (entry.getKey() > currentLevel && currentExp >= entry.getValue()) {
                data.setForgeLevel(entry.getKey());
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Long> getLevelThresholds() {
        Map<Integer, Long> thresholds = new java.util.TreeMap<>();
        var section = plugin.getConfig().getConfigurationSection("level-thresholds");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    long exp = section.getLong(key);
                    thresholds.put(level, exp);
                } catch (NumberFormatException ignored) {}
            }
        }
        return thresholds;
    }
}