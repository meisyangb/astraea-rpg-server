package cn.guangdian.accessory.lifecycle;

import cn.guangdian.accessory.GuangDianAccessory;
import cn.guangdian.accessory.model.PlayerAccessoryData;
import cn.guangdian.accessory.service.AccessoryServiceAdapter;
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class AccessoryDataHandler extends AbstractPlayerDataHandler {
    
    private final GuangDianAccessory plugin;
    private final File dataFolder;
    
    public AccessoryDataHandler(GuangDianAccessory plugin) {
        super(plugin);
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }
    
    @Override
    protected void onPlayerLoad(Player player) {
        UUID playerId = player.getUniqueId();
        File dataFile = new File(dataFolder, playerId.toString() + ".yml");
        
        PlayerAccessoryData data = new PlayerAccessoryData(playerId);
        
        if (dataFile.exists()) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
                var section = config.getConfigurationSection("accessories");
                if (section != null) {
                    Map<String, Object> serialized = section.getValues(false);
                    data = PlayerAccessoryData.deserialize(playerId, serialized);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("加载玩家 " + player.getName() + " 的饰品数据失败: " + e.getMessage());
            }
        }
        
        AccessoryServiceAdapter service = (AccessoryServiceAdapter) plugin.getAccessoryService();
        if (service != null) {
            service.cachePlayerData(playerId, data);
        }
    }
    
    @Override
    protected void onPlayerSave(Player player) {
        UUID playerId = player.getUniqueId();
        AccessoryServiceAdapter service = (AccessoryServiceAdapter) plugin.getAccessoryService();
        PlayerAccessoryData data = service.getPlayerData(playerId);
        
        if (data == null) {
            return;
        }
        
        if (data.isDirty()) {
            File dataFile = new File(dataFolder, playerId.toString() + ".yml");
            YamlConfiguration config = new YamlConfiguration();
            config.set("accessories", data.serialize());
            
            try {
                config.save(dataFile);
                data.markClean();
            } catch (IOException e) {
                plugin.getLogger().warning("无法保存玩家 " + player.getName() + " 的饰品数据: " + e.getMessage());
            }
        }
    }
    
    @Override
    public int getPriority() {
        return 200;
    }
    
    @Override
    public String getHandlerName() {
        return "Accessory";
    }
}
