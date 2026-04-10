package cn.guangdian.classsystem.data;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.model.AttributeType;
import cn.guangdian.classsystem.model.PlayerClassData;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ClassDataHandler extends AbstractPlayerDataHandler {
    
    private final GuangDianClass plugin;
    private final Map<UUID, PlayerClassData> dataCache;
    private File dataFile;
    private FileConfiguration dataConfig;
    
    public ClassDataHandler(GuangDianClass plugin) {
        super(plugin);
        this.plugin = plugin;
        this.dataCache = new ConcurrentHashMap<>();
        initDataFile();
    }
    
    private void initDataFile() {
        dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建玩家数据文件: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    @Override
    protected void onPlayerLoad(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (rpgCore != null) {
            rpgCore.getScheduler().runAsync(() -> {
                PlayerClassData data = loadPlayerData(playerId);
                rpgCore.getScheduler().runSyncLater(() -> {
                    dataCache.put(playerId, data);
                }, 0L);
            });
        } else {
            PlayerClassData data = loadPlayerData(playerId);
            dataCache.put(playerId, data);
        }
    }
    
    @Override
    protected void onPlayerSave(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerClassData data = dataCache.remove(playerId);
        
        if (data != null) {
            if (rpgCore != null) {
                rpgCore.getScheduler().runAsync(() -> {
                    savePlayerData(playerId, data);
                });
            } else {
                savePlayerData(playerId, data);
            }
        }
    }
    
    private PlayerClassData loadPlayerData(UUID playerId) {
        String path = "players." + playerId.toString();
        
        if (dataConfig.contains(path)) {
            PlayerClassData data = new PlayerClassData(playerId);
            data.setClassId(dataConfig.getString(path + ".classId", plugin.getDefaultClassId()));
            data.setTier(dataConfig.getInt(path + ".tier", 1));
            data.setExp(dataConfig.getLong(path + ".exp", 0));
            data.setAdvancementLevel(dataConfig.getInt(path + ".advancementLevel", 0));
            data.setTotalExp(dataConfig.getLong(path + ".totalExp", 0));
            data.setLastUpdateTime(dataConfig.getLong(path + ".lastUpdateTime", System.currentTimeMillis()));
            
            data.setAttributePoints(dataConfig.getInt(path + ".attributePoints", 0));
            data.setUsedAttributePoints(dataConfig.getInt(path + ".usedAttributePoints", 0));
            
            ConfigurationSection attrSection = dataConfig.getConfigurationSection(path + ".allocatedAttributes");
            if (attrSection != null) {
                for (String key : attrSection.getKeys(false)) {
                    AttributeType type = AttributeType.fromId(key);
                    if (type != null) {
                        data.setAllocatedAttribute(type, attrSection.getInt(key, 0));
                    }
                }
            }
            
            return data;
        }
        
        PlayerClassData newData = new PlayerClassData(playerId);
        newData.setClassId(plugin.getDefaultClassId());
        return newData;
    }
    
    private void savePlayerData(UUID playerId, PlayerClassData data) {
        String path = "players." + playerId.toString();
        
        dataConfig.set(path + ".classId", data.getClassId());
        dataConfig.set(path + ".tier", data.getTier());
        dataConfig.set(path + ".exp", data.getExp());
        dataConfig.set(path + ".advancementLevel", data.getAdvancementLevel());
        dataConfig.set(path + ".totalExp", data.getTotalExp());
        dataConfig.set(path + ".lastUpdateTime", data.getLastUpdateTime());
        
        dataConfig.set(path + ".attributePoints", data.getAttributePoints());
        dataConfig.set(path + ".usedAttributePoints", data.getUsedAttributePoints());
        
        String attrPath = path + ".allocatedAttributes";
        dataConfig.set(attrPath, null);
        for (Map.Entry<AttributeType, Integer> entry : data.getAllocatedAttributes().entrySet()) {
            dataConfig.set(attrPath + "." + entry.getKey().getId(), entry.getValue());
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存玩家数据失败: " + playerId + " - " + e.getMessage());
        }
    }
    
    public PlayerClassData getPlayerData(UUID playerId) {
        return dataCache.get(playerId);
    }
    
    public PlayerClassData getOrCreatePlayerData(UUID playerId) {
        return dataCache.computeIfAbsent(playerId, id -> {
            PlayerClassData data = loadPlayerData(id);
            if (data.getClassId() == null) {
                data.setClassId(plugin.getDefaultClassId());
            }
            return data;
        });
    }
    
    public void saveAll() {
        for (Map.Entry<UUID, PlayerClassData> entry : dataCache.entrySet()) {
            savePlayerData(entry.getKey(), entry.getValue());
        }
    }
    
    public void clearCache() {
        dataCache.clear();
    }
    
    public int getCacheSize() {
        return dataCache.size();
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
    
    @Override
    public String getHandlerName() {
        return "ClassData";
    }
}
