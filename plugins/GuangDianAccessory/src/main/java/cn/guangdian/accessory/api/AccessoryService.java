package cn.guangdian.accessory.api;

import cn.guangdian.accessory.model.Accessory;
import cn.guangdian.accessory.model.AccessorySlot;
import cn.guangdian.accessory.model.PlayerAccessoryData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface AccessoryService {
    
    Optional<Accessory> getAccessory(String id);
    
    Collection<Accessory> getAllAccessories();
    
    Collection<Accessory> getAccessoriesBySlot(AccessorySlot slot);
    
    PlayerAccessoryData getPlayerData(UUID playerId);
    
    boolean equipAccessory(Player player, AccessorySlot slot, String accessoryId);
    
    boolean unequipAccessory(Player player, AccessorySlot slot);
    
    Optional<Accessory> getEquippedAccessory(Player player, AccessorySlot slot);
    
    Map<String, Double> getTotalAttributes(Player player);
    
    boolean giveAccessory(Player player, String accessoryId, int amount);
    
    boolean hasAccessory(Player player, String accessoryId);
    
    ItemStack createAccessoryItem(String accessoryId);
}
