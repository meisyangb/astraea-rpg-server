package cn.guangdian.quest.repository;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerQuestRepository {

    private final GuangDianQuest plugin;
    private final File dataDir;
    private final Map<UUID, PlayerQuestData> playerData;

    public PlayerQuestRepository(GuangDianQuest plugin, File dataDir) {
        this.plugin = plugin;
        this.dataDir = dataDir;
        this.playerData = new ConcurrentHashMap<>();
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    public PlayerQuestData getPlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, this::loadPlayerData);
    }

    @SuppressWarnings("unchecked")
    private PlayerQuestData loadPlayerData(UUID playerId) {
        File file = new File(dataDir, playerId.toString() + ".yml");
        if (!file.exists()) {
            return new PlayerQuestData(playerId);
        }

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            // 注意：不能用 getValues(true)，因为它会把嵌套 Map 展平
            // 例如 completedQuests.f1_01=xxx 而不是 completedQuests={f1_01=xxx}
            // 这会导致 fromMap() 中 map.get("completedQuests") 返回 null
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            for (String key : config.getKeys(false)) {
                map.put(key, config.get(key));
            }
            return PlayerQuestData.fromMap(map);
        } catch (Exception e) {
            plugin.getLogger().warning("加载玩家数据失败: " + playerId + " - " + e.getMessage());
            e.printStackTrace();
            return new PlayerQuestData(playerId);
        }
    }

    public void savePlayerData(UUID playerId) {
        PlayerQuestData data = playerData.get(playerId);
        if (data == null) return;

        File file = new File(dataDir, playerId.toString() + ".yml");
        try {
            FileConfiguration config = new YamlConfiguration();
            Map<String, Object> map = data.toMap();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                config.set(entry.getKey(), entry.getValue());
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

    public void removePlayerData(UUID playerId) {
        PlayerQuestData data = playerData.remove(playerId);
        if (data != null) {
            savePlayerData(playerId, data);
        }
    }
    
    private void savePlayerData(UUID playerId, PlayerQuestData data) {
        File file = new File(dataDir, playerId.toString() + ".yml");
        try {
            FileConfiguration config = new YamlConfiguration();
            Map<String, Object> map = data.toMap();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                config.set(entry.getKey(), entry.getValue());
            }
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("保存玩家数据失败: " + playerId + " - " + e.getMessage());
        }
    }

    public boolean isLoaded(UUID playerId) {
        return playerData.containsKey(playerId);
    }
}
