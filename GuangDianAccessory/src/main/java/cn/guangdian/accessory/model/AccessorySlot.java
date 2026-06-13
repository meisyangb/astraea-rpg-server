package cn.guangdian.accessory.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public enum AccessorySlot {
    BADGE("徽章", 0),
    MEDAL("勋章", 1),
    RELIC("圣物", 2);
    
    private static final List<Accessory> allAccessories = new CopyOnWriteArrayList<>();
    
    private final String displayName;
    private final int slotIndex;
    
    AccessorySlot(String displayName, int slotIndex) {
        this.displayName = displayName;
        this.slotIndex = slotIndex;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getSlotIndex() {
        return slotIndex;
    }
    
    public static AccessorySlot fromIndex(int index) {
        for (AccessorySlot slot : values()) {
            if (slot.slotIndex == index) {
                return slot;
            }
        }
        return null;
    }
    
    public static void clearAllAccessories() {
        allAccessories.clear();
    }
    
    public static void addAccessory(Accessory accessory) {
        allAccessories.add(accessory);
    }
    
    public static List<Accessory> getAllAccessories() {
        return Collections.unmodifiableList(new ArrayList<>(allAccessories));
    }
    
    public static Accessory findAccessoryByLoreKeyword(String plainText) {
        for (Accessory accessory : allAccessories) {
            if (plainText.contains(accessory.getLoreKeyword())) {
                return accessory;
            }
        }
        return null;
    }
}
