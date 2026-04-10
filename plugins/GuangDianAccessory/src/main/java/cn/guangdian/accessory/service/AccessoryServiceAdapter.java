package cn.guangdian.accessory.service;

import cn.guangdian.accessory.GuangDianAccessory;
import cn.guangdian.accessory.api.AccessoryService;
import cn.guangdian.accessory.model.Accessory;
import cn.guangdian.accessory.model.AccessorySlot;
import cn.guangdian.accessory.model.PlayerAccessoryData;
import cn.guangdian.rpgcore.RPGCore;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AccessoryServiceAdapter implements AccessoryService {
    
    private final GuangDianAccessory plugin;
    private final Map<UUID, PlayerAccessoryData> dataCache;
    private final NamespacedKey accessoryKey;
    
    public AccessoryServiceAdapter(GuangDianAccessory plugin) {
        this.plugin = plugin;
        this.dataCache = new ConcurrentHashMap<>();
        this.accessoryKey = new NamespacedKey(plugin, "accessory_id");
        
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
        if (accessory == null || accessory.getSlot() != slot) {
            return false;
        }
        
        PlayerAccessoryData data = getPlayerData(player.getUniqueId());
        if (data == null) {
            return false;
        }
        
        data.equipAccessory(slot, accessoryId);
        return true;
    }
    
    @Override
    public boolean unequipAccessory(Player player, AccessorySlot slot) {
        PlayerAccessoryData data = getPlayerData(player.getUniqueId());
        if (data == null || data.getEquippedAccessory(slot) == null) {
            return false;
        }
        
        data.unequipAccessory(slot);
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
        Map<String, Double> total = new HashMap<>();
        
        for (AccessorySlot slot : AccessorySlot.values()) {
            getEquippedAccessory(player, slot).ifPresent(accessory -> {
                for (Map.Entry<String, Double> attr : accessory.getAttributes().entrySet()) {
                    total.merge(attr.getKey(), attr.getValue(), Double::sum);
                }
            });
        }
        
        return total;
    }
    
    @Override
    public boolean giveAccessory(Player player, String accessoryId, int amount) {
        Accessory accessory = getAccessory(accessoryId).orElse(null);
        if (accessory == null) {
            return false;
        }
        
        ItemStack item = createAccessoryItem(accessoryId);
        item.setAmount(amount);
        
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        return leftover.isEmpty();
    }
    
    @Override
    public boolean hasAccessory(Player player, String accessoryId) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isAccessoryItem(item, accessoryId)) {
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
        
        ItemStack item = accessory.getItem();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(accessoryKey, PersistentDataType.STRING, accessoryId);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    public boolean isAccessoryItem(ItemStack item, String accessoryId) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        String id = meta.getPersistentDataContainer().get(accessoryKey, PersistentDataType.STRING);
        return accessoryId.equals(id);
    }
    
    public String getAccessoryIdFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(accessoryKey, PersistentDataType.STRING);
    }
}
