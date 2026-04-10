package cn.guangdian.accessory.model;

public enum AccessorySlot {
    BADGE("徽章", 0),
    MEDAL("勋章", 1),
    RELIC("圣物", 2);
    
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
}
