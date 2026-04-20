package cn.guangdian.socket.model;

/**
 * 宝石数据模型
 * 
 * 职责：封装单个宝石的所有数据
 */
public class GemData {

    private int slotIndex;          // 镶嵌槽位索引
    private String type;            // 宝石类型 (如: 红宝石, 蓝宝石)
    private String socketType;      // 孔位类型 (如: 攻击, 防御)
    private String itemId;          // 物品ID (MythicMobs ID, RPGItems ID 或 Material 名)
    private int amount;             // 数量
    private boolean isMythicItem;   // 是否是 MythicMobs 物品
    private boolean isRPGItem;      // 是否是 RPGItems 物品

    public GemData() {
        this.slotIndex = -1;
        this.amount = 1;
        this.isMythicItem = false;
        this.isRPGItem = false;
    }

    public GemData(int slotIndex, String type, String socketType, String itemId, int amount) {
        this.slotIndex = slotIndex;
        this.type = type;
        this.socketType = socketType;
        this.itemId = itemId;
        this.amount = amount;
        this.isMythicItem = false;
        this.isRPGItem = false;
    }

    // Getters and Setters
    public int getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSocketType() {
        return socketType;
    }

    public void setSocketType(String socketType) {
        this.socketType = socketType;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public boolean isRPGItem() {
        return isRPGItem;
    }

    public void setRPGItem(boolean RPGItem) {
        isRPGItem = RPGItem;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = Math.max(1, amount);
    }

    public boolean isMythicItem() {
        return isMythicItem;
    }

    /**
     * 验证宝石数据是否有效
     */
    public boolean isValid() {
        return slotIndex >= 0 && 
               type != null && !type.isEmpty() && 
               itemId != null && !itemId.isEmpty();
    }

    /**
     * 创建宝石数据的副本
     */
    public GemData copy() {
        GemData copy = new GemData();
        copy.slotIndex = this.slotIndex;
        copy.type = this.type;
        copy.socketType = this.socketType;
        copy.itemId = this.itemId;
        copy.amount = this.amount;
        copy.isMythicItem = this.isMythicItem;
        copy.isRPGItem = this.isRPGItem;
        return copy;
    }

    @Override
    public String toString() {
        return "GemData{" +
                "slot=" + slotIndex +
                ", type='" + type + '\'' +
                ", socket='" + socketType + '\'' +
                ", id='" + itemId + '\'' +
                ", amount=" + amount +
                '}';
    }
}
