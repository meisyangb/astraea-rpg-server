package cn.guangdian.accessory.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class PlayerAccessoryData {
    
    private final UUID playerId;
    private final Map<AccessorySlot, String> equippedAccessories;
    private boolean dirty;
    
    public PlayerAccessoryData(UUID playerId) {
        this.playerId = playerId;
        this.equippedAccessories = new EnumMap<>(AccessorySlot.class);
        this.dirty = false;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getEquippedAccessory(AccessorySlot slot) {
        return equippedAccessories.get(slot);
    }
    
    public void equipAccessory(AccessorySlot slot, String accessoryId) {
        equippedAccessories.put(slot, accessoryId);
        dirty = true;
    }
    
    public void unequipAccessory(AccessorySlot slot) {
        equippedAccessories.remove(slot);
        dirty = true;
    }
    
    public Map<AccessorySlot, String> getAllEquipped() {
        return new EnumMap<>(equippedAccessories);
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    public void markClean() {
        dirty = false;
    }
    
    public Map<String, Object> serialize() {
        Map<String, Object> data = new java.util.HashMap<>();
        for (Map.Entry<AccessorySlot, String> entry : equippedAccessories.entrySet()) {
            data.put(entry.getKey().name(), entry.getValue());
        }
        return data;
    }
    
    public static PlayerAccessoryData deserialize(UUID playerId, Map<String, Object> data) {
        PlayerAccessoryData playerData = new PlayerAccessoryData(playerId);
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            try {
                AccessorySlot slot = AccessorySlot.valueOf(entry.getKey());
                playerData.equippedAccessories.put(slot, (String) entry.getValue());
            } catch (IllegalArgumentException ignored) {
            }
        }
        playerData.dirty = false;
        return playerData;
    }
}
