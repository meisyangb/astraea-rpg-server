package cn.guangdian.accessory.manager;

import cn.guangdian.accessory.GuangDianAccessory;
import cn.guangdian.accessory.model.Accessory;
import cn.guangdian.accessory.model.AccessorySlot;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AccessoryManager {
    
    private final GuangDianAccessory plugin;
    private final Map<String, Accessory> accessories;
    private final Map<AccessorySlot, List<Accessory>> accessoriesBySlot;
    
    public AccessoryManager(GuangDianAccessory plugin) {
        this.plugin = plugin;
        this.accessories = new ConcurrentHashMap<>();
        this.accessoriesBySlot = new ConcurrentHashMap<>();
        for (AccessorySlot slot : AccessorySlot.values()) {
            accessoriesBySlot.put(slot, new ArrayList<>());
        }
    }
    
    public void loadAccessories() {
        File accessoriesFile = new File(plugin.getDataFolder(), "accessories.yml");
        if (!accessoriesFile.exists()) {
            plugin.saveResource("accessories.yml", false);
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(accessoriesFile);
        
        InputStream defaultStream = plugin.getResource("accessories.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            );
            config.setDefaults(defaultConfig);
        }
        
        ConfigurationSection accessoriesSection = config.getConfigurationSection("accessories");
        if (accessoriesSection == null) {
            plugin.getLogger().warning("accessories.yml 中没有找到饰品配置");
            return;
        }
        
        accessories.clear();
        for (AccessorySlot slot : AccessorySlot.values()) {
            accessoriesBySlot.get(slot).clear();
        }
        
        for (String id : accessoriesSection.getKeys(false)) {
            ConfigurationSection accessoryConfig = accessoriesSection.getConfigurationSection(id);
            if (accessoryConfig == null) {
                continue;
            }
            
            try {
                Accessory accessory = Accessory.fromConfig(id, accessoryConfig);
                registerAccessory(accessory);
            } catch (Exception e) {
                plugin.getLogger().warning("加载饰品 " + id + " 失败: " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("已加载 " + accessories.size() + " 个饰品");
    }
    
    public void registerAccessory(Accessory accessory) {
        accessories.put(accessory.getId(), accessory);
        accessoriesBySlot.get(accessory.getSlot()).add(accessory);
    }
    
    public Accessory getAccessory(String id) {
        return accessories.get(id);
    }
    
    public Collection<Accessory> getAllAccessories() {
        return Collections.unmodifiableCollection(accessories.values());
    }
    
    public List<Accessory> getAccessoriesBySlot(AccessorySlot slot) {
        return Collections.unmodifiableList(accessoriesBySlot.get(slot));
    }
    
    public void reloadAccessories() {
        loadAccessories();
    }
}
