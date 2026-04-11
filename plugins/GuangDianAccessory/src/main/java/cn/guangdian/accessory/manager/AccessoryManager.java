package cn.guangdian.accessory.manager;

import cn.guangdian.accessory.GuangDianAccessory;
import cn.guangdian.accessory.model.Accessory;
import cn.guangdian.accessory.model.AccessorySlot;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
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
        AccessorySlot.clearAllAccessories();
        
        File accessoriesFile = new File(plugin.getDataFolder(), "accessories.yml");
        if (!accessoriesFile.exists()) {
            plugin.saveResource("accessories.yml", false);
        }
        
        org.bukkit.configuration.file.YamlConfiguration config = 
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(accessoriesFile);
        
        ConfigurationSection accessoriesSection = config.getConfigurationSection("accessories");
        if (accessoriesSection == null) {
            plugin.getLogger().warning("accessories.yml 中没有找到饰品配置");
            return;
        }
        
        accessories.clear();
        for (AccessorySlot slot : AccessorySlot.values()) {
            accessoriesBySlot.get(slot).clear();
        }
        
        int loadedCount = 0;
        int failedCount = 0;
        
        for (String id : accessoriesSection.getKeys(false)) {
            ConfigurationSection accessoryConfig = accessoriesSection.getConfigurationSection(id);
            if (accessoryConfig == null) {
                continue;
            }
            
            try {
                Accessory accessory = Accessory.fromConfig(id, accessoryConfig);
                registerAccessory(accessory);
                AccessorySlot.addAccessory(accessory);
                loadedCount++;
                
                plugin.getLogger().info("加载饰品: " + accessory.getName() + 
                    " [" + accessory.getSlot().name() + "] 关键词: " + accessory.getLoreKeyword());
            } catch (Exception e) {
                plugin.getLogger().warning("加载饰品 " + id + " 失败: " + e.getMessage());
                failedCount++;
            }
        }
        
        plugin.getLogger().info("饰品加载完成: 成功 " + loadedCount + " 个, 失败 " + failedCount + " 个");
    }
    
    public void registerAccessory(Accessory accessory) {
        accessories.put(accessory.getId(), accessory);
        accessoriesBySlot.get(accessory.getSlot()).add(accessory);
    }
    
    public Accessory getAccessory(String id) {
        return accessories.get(id);
    }
    
    public Accessory getAccessoryByItem(org.bukkit.inventory.ItemStack item) {
        return Accessory.fromItem(item);
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
