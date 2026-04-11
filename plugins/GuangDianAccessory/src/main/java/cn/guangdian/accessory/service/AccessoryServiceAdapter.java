package cn.guangdian.accessory.service;

import cn.guangdian.accessory.GuangDianAccessory;
import cn.guangdian.accessory.api.AccessoryService;
import cn.guangdian.accessory.model.Accessory;
import cn.guangdian.accessory.model.AccessorySlot;
import cn.guangdian.accessory.model.PlayerAccessoryData;
import cn.guangdian.rpgcore.RPGCore;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AccessoryServiceAdapter implements AccessoryService {
    
    private final GuangDianAccessory plugin;
    private final Map<UUID, PlayerAccessoryData> dataCache;
    
    public AccessoryServiceAdapter(GuangDianAccessory plugin) {
        this.plugin = plugin;
        this.dataCache = new ConcurrentHashMap<>();
        
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().registerService(AccessoryService.class, this);
        }
    }
    
    public void unregister() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().unregisterService(AccessoryService.class);
        }
    }
    
    public void cachePlayerData(UUID playerId, PlayerAccessoryData data) {
        dataCache.put(playerId, data);
    }
    
    public void removePlayerData(UUID playerId) {
        dataCache.remove(playerId);
    }
    
    @Override
    public Optional<Accessory> getAccessory(String id) {
        return Optional.ofNullable(plugin.getAccessoryManager().getAccessory(id));
    }
    
    @Override
    public Collection<Accessory> getAllAccessories() {
        return plugin.getAccessoryManager().getAllAccessories();
    }
    
    @Override
    public Collection<Accessory> getAccessoriesBySlot(AccessorySlot slot) {
        return plugin.getAccessoryManager().getAccessoriesBySlot(slot);
    }
    
    @Override
    public PlayerAccessoryData getPlayerData(UUID playerId) {
        return dataCache.get(playerId);
    }
    
    @Override
    public boolean equipAccessory(Player player, AccessorySlot slot, String accessoryId) {
        Accessory accessory = getAccessory(accessoryId).orElse(null);
        if (accessory == null) {
            return false;
        }
        if (accessory.getSlot() != slot) {
            return false;
        }
        
        PlayerAccessoryData data = getPlayerData(player.getUniqueId());
        if (data == null) {
            data = new PlayerAccessoryData(player.getUniqueId());
            cachePlayerData(player.getUniqueId(), data);
        }
        
        data.equipAccessory(slot, accessoryId);
        
        plugin.getServer().getPluginManager().callEvent(new cn.guangdian.accessory.event.AccessoryEquipEvent(player, slot, accessoryId, true));
        
        return true;
    }
    
    @Override
    public boolean unequipAccessory(Player player, AccessorySlot slot) {
        PlayerAccessoryData data = getPlayerData(player.getUniqueId());
        if (data == null || data.getEquippedAccessory(slot) == null) {
            return false;
        }
        
        String accessoryId = data.getEquippedAccessory(slot);
        data.unequipAccessory(slot);
        
        plugin.getServer().getPluginManager().callEvent(new cn.guangdian.accessory.event.AccessoryEquipEvent(player, slot, accessoryId, false));
        
        return true;
    }
    
    @Override
    public Optional<Accessory> getEquippedAccessory(Player player, AccessorySlot slot) {
        PlayerAccessoryData data = getPlayerData(player.getUniqueId());
        if (data == null) {
            return Optional.empty();
        }
        
        String accessoryId = data.getEquippedAccessory(slot);
        if (accessoryId == null) {
            return Optional.empty();
        }
        
        return getAccessory(accessoryId);
    }
    
    @Override
    public Map<String, Double> getTotalAttributes(Player player) {
        return new HashMap<>();
    }
    
    @Override
    public boolean giveAccessory(Player player, String accessoryId, int amount) {
        Accessory accessory = getAccessory(accessoryId).orElse(null);
        if (accessory == null) {
            return false;
        }
        
        ItemStack item = createAccessoryItem(accessoryId);
        if (item == null) {
            return false;
        }
        item.setAmount(amount);
        
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        return leftover.isEmpty();
    }
    
    @Override
    public boolean hasAccessory(Player player, String accessoryId) {
        for (ItemStack item : player.getInventory().getContents()) {
            Accessory acc = Accessory.fromItem(item);
            if (acc != null && acc.getId().equals(accessoryId)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public ItemStack createAccessoryItem(String accessoryId) {
        Accessory accessory = getAccessory(accessoryId).orElse(null);
        if (accessory == null) {
            return new ItemStack(org.bukkit.Material.AIR);
        }
        
        return accessory.getMythicItem(1);
    }
    
    public Accessory getAccessoryFromItem(ItemStack item) {
        return Accessory.fromItem(item);
    }
    
    public String getAccessoryIdFromItem(ItemStack item) {
        Accessory accessory = Accessory.fromItem(item);
        return accessory != null ? accessory.getId() : null;
    }
}
