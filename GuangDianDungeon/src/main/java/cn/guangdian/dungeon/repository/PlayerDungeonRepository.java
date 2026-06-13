package cn.guangdian.dungeon.repository;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.PlayerDungeonData;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDungeonRepository {

    private final GuangDianDungeon plugin;
    private final File dataDir;
    private final Map<UUID, PlayerDungeonData> playerData;

    public PlayerDungeonRepository(GuangDianDungeon plugin, File dataDir) {
        this.plugin = plugin;
        this.dataDir = dataDir;
        this.playerData = new ConcurrentHashMap<>();

        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    public PlayerDungeonData getPlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, this::loadPlayerData);
    }

    public PlayerDungeonData loadPlayerData(UUID playerId) {
        File file = new File(dataDir, playerId.toString() + ".yml");
        
        if (file.exists()) {
            try {
                org.bukkit.configuration.file.YamlConfiguration config = 
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
                
                PlayerDungeonData data = new PlayerDungeonData(playerId);
                
                var clearsSection = config.getConfigurationSection("clears");
                if (clearsSection != null) {
                    for (String key : clearsSection.getKeys(false)) {
                        long time = clearsSection.getLong(key + ".time", 0);
                        int count = clearsSection.getInt(key + ".count", 0);
                        long bestTime = clearsSection.getLong(key + ".bestTime", 0);
                        int bestScore = clearsSection.getInt(key + ".bestScore", 0);
                        data.setClearRecord(key, time, count, bestTime, bestScore);
                    }
                }
                
                var cooldownsSection = config.getConfigurationSection("cooldowns");
                if (cooldownsSection != null) {
                    for (String key : cooldownsSection.getKeys(false)) {
                        long endTime = cooldownsSection.getLong(key);
                        data.setCooldownEnd(key, endTime);
                    }
                }
                
                return data;
            } catch (Exception e) {
                plugin.getLogger().warning("加载玩家数据失败: " + playerId + " - " + e.getMessage());
            }
        }
        
        return new PlayerDungeonData(playerId);
    }

    public void savePlayerData(UUID playerId) {
        PlayerDungeonData data = playerData.get(playerId);
        if (data == null) return;

        File file = new File(dataDir, playerId.toString() + ".yml");

        try {
            org.bukkit.configuration.file.YamlConfiguration config = 
                new org.bukkit.configuration.file.YamlConfiguration();

            for (Map.Entry<String, PlayerDungeonData.ClearRecord> entry : 
                 data.getClearRecords().entrySet()) {
                String path = "clears." + entry.getKey();
                PlayerDungeonData.ClearRecord record = entry.getValue();
                config.set(path + ".time", record.firstClearTime);
                config.set(path + ".count", record.clearCount);
                config.set(path + ".bestTime", record.bestTime);
                config.set(path + ".bestScore", record.bestScore);
            }

            for (Map.Entry<String, Long> entry : data.getCooldowns().entrySet()) {
                config.set("cooldowns." + entry.getKey(), entry.getValue());
            }

            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("保存玩家数据失败: " + playerId + " - " + e.getMessage());
        }
    }

    public void saveAll() {
        for (UUID playerId : playerData.keySet()) {
            savePlayerData(playerId);
        }
    }

    public void unloadPlayerData(UUID playerId) {
        savePlayerData(playerId);
        playerData.remove(playerId);
    }

    public void clear() {
        playerData.clear();
    }
}
