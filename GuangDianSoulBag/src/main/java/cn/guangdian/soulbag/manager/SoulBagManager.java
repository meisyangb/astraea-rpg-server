package cn.guangdian.soulbag.manager;

import cn.guangdian.soulbag.GuangDianSoulBag;
import cn.guangdian.soulbag.data.SoulBagData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SoulBagManager {
    
    private final GuangDianSoulBag plugin;
    private final Map<UUID, SoulBagData> bags = new ConcurrentHashMap<>();
    
    private File dataFile;
    private YamlConfiguration data;
    
    private int defaultRows;
    
    public SoulBagManager(GuangDianSoulBag plugin) {
        this.plugin = plugin;
        this.defaultRows = 6;
    }
    
    public void loadConfiguration() {
        FileConfiguration config = plugin.getConfig();
        defaultRows = Math.max(1, Math.min(6, config.getInt("settings.rows", 6)));
    }
    
    public void loadData() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建数据文件: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
        
        loadBags();
    }
    
    private void loadBags() {
        if (!data.contains("bags")) {
            return;
        }
        
        for (String key : data.getConfigurationSection("bags").getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                String path = "bags." + key + ".";
                
                int rows = data.getInt(path + "rows", defaultRows);
                int size = rows * 9;
                
                SoulBagData bag = new SoulBagData(playerId, size);
                
                if (data.contains(path + "items")) {
                    for (int i = 0; i < size; i++) {
                        ItemStack item = data.getItemStack(path + "items." + i);
                        if (item != null) {
                            bag.setItem(i, item);
                        }
                    }
                }
                
                bags.put(playerId, bag);
            } catch (Exception e) {
                plugin.getLogger().warning("加载背包数据失败: " + key + " - " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("已加载 " + bags.size() + " 个灵魂背包");
    }
    
    public void saveData() {
        data.set("bags", null);
        
        for (Map.Entry<UUID, SoulBagData> entry : bags.entrySet()) {
            String path = "bags." + entry.getKey().toString() + ".";
            SoulBagData bag = entry.getValue();
            
            data.set(path + "rows", bag.getSize() / 9);
            
            for (int i = 0; i < bag.getSize(); i++) {
                ItemStack item = bag.getItem(i);
                if (item != null) {
                    data.set(path + "items." + i, item);
                }
            }
        }
        
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存数据失败: " + e.getMessage());
        }
    }
    
    public SoulBagData getBag(UUID playerId) {
        return bags.computeIfAbsent(playerId, id -> {
            int size = defaultRows * 9;
            return new SoulBagData(id, size);
        });
    }
    
    public boolean hasBag(UUID playerId) {
        return bags.containsKey(playerId);
    }
    
    public void removeBag(UUID playerId) {
        bags.remove(playerId);
    }
    
    public void clearBag(UUID playerId) {
        SoulBagData bag = bags.get(playerId);
        if (bag != null) {
            bag.clear();
        }
    }
    
    public int getDefaultRows() {
        return defaultRows;
    }
    
    public int getDefaultSize() {
        return defaultRows * 9;
    }
    
    public Map<UUID, SoulBagData> getAllBags() {
        return bags;
    }
}
