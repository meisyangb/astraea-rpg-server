package cn.guangdian.sect;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

/**
 * 玩家数据管理器
 */
public class PlayerDataManager {
    private final GuangDianSect plugin;
    private final File dataFile;
    private final YamlConfiguration dataConfig;
    
    public PlayerDataManager(GuangDianSect plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建玩家数据文件: " + e.getMessage());
            }
        }
        
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    public SectPlayer load(String playerId) {
        ConfigurationSection section = dataConfig.getConfigurationSection(playerId);
        if (section == null) return null;
        
        Map<String, Object> map = new HashMap<>();
        for (String key : section.getKeys(false)) {
            map.put(key, section.get(key));
        }
        map.put("playerId", playerId);
        
        return SectPlayer.deserialize(map);
    }
    
    public void loadAll() {
        // 数据已在内存中，此方法用于启动时加载数据
    }
    
    public void save(SectPlayer player) {
        String playerId = player.getPlayerId();
        Map<String, Object> data = player.serialize();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!entry.getKey().equals("playerId")) {
                dataConfig.set(playerId + "." + entry.getKey(), entry.getValue());
            }
        }
        
        saveToFile();
    }
    
    public void saveAll() {
        saveToFile();
    }
    
    private void saveToFile() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存玩家数据: " + e.getMessage());
        }
    }
}