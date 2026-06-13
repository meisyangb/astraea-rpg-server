package cn.guangdian.devour.data;

import java.util.*;

/**
 * 吞噬数据模型
 * 只存储必要信息：已吞噬的物品ID（去重）和槽位占用状态
 * 
 * @author Astraea RPG Team
 * @since 1.0.0
 */
public class DevourData {
    
    /** 物品唯一ID (用于PDC存储) */
    private final UUID itemUUID;
    
    /** MythicMobs 物品类型 */
    private final String mythicType;
    
    /** 最大吞噬槽位数 */
    private final int maxSlots;
    
    /** 已吞噬的物品ID集合 (用于去重，存储Display名称) */
    private final Set<String> devouredItemIds;
    
    /** 槽位占用状态 (槽位编号 -> 物品名称) */
    private final Map<Integer, String> slotOccupied;
    
    public DevourData(UUID itemUUID, String mythicType, int maxSlots) {
        this.itemUUID = itemUUID;
        this.mythicType = mythicType;
        this.maxSlots = maxSlots;
        this.devouredItemIds = new HashSet<>();
        this.slotOccupied = new HashMap<>();
    }
    
    /**
     * 获取物品UUID
     */
    public UUID getItemUUID() {
        return itemUUID;
    }
    
    /**
     * 获取MythicMobs类型
     */
    public String getMythicType() {
        return mythicType;
    }
    
    /**
     * 获取最大槽位数
     */
    public int getMaxSlots() {
        return maxSlots;
    }
    
    /**
     * 检查物品是否已被吞噬
     * @param itemId 物品ID（Display名称）
     */
    public boolean isDevoured(String itemId) {
        return devouredItemIds.contains(itemId);
    }
    
    /**
     * 添加已吞噬的物品
     * @param slotIndex 槽位编号
     * @param itemId 物品ID（Display名称）
     * @param itemName 物品显示名称
     */
    public void addDevoured(int slotIndex, String itemId, String itemName) {
        devouredItemIds.add(itemId);
        slotOccupied.put(slotIndex, itemName);
    }
    
    /**
     * 移除已吞噬的物品
     * @param slotIndex 槽位编号
     * @param itemId 物品ID
     */
    public void removeDevoured(int slotIndex, String itemId) {
        devouredItemIds.remove(itemId);
        slotOccupied.remove(slotIndex);
    }
    
    /**
     * 检查槽位是否已占用
     */
    public boolean isSlotOccupied(int slotIndex) {
        return slotOccupied.containsKey(slotIndex);
    }
    
    /**
     * 获取槽位占用的物品名称
     */
    public String getSlotItemName(int slotIndex) {
        return slotOccupied.get(slotIndex);
    }
    
    /**
     * 获取已占用的槽位数
     */
    public int getOccupiedSlotCount() {
        return slotOccupied.size();
    }
    
    /**
     * 获取第一个空槽位
     * @return 槽位编号，如果没有空槽位返回-1
     */
    public int getFirstEmptySlot() {
        for (int i = 1; i <= maxSlots; i++) {
            if (!slotOccupied.containsKey(i)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 是否有空槽位
     */
    public boolean hasEmptySlot() {
        return getFirstEmptySlot() != -1;
    }
    
    /**
     * 序列化为JSON字符串
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"uuid\":\"").append(itemUUID).append("\",");
        sb.append("\"mythicType\":\"").append(mythicType).append("\",");
        sb.append("\"maxSlots\":").append(maxSlots).append(",");
        
        // 序列化已吞噬物品ID
        sb.append("\"devouredIds\":[");
        boolean first = true;
        for (String id : devouredItemIds) {
            if (!first) sb.append(",");
            sb.append("\"").append(id.replace("\"", "\\\"")).append("\"");
            first = false;
        }
        sb.append("],");
        
        // 序列化槽位占用
        sb.append("\"slots\":{");
        first = true;
        for (Map.Entry<Integer, String> entry : slotOccupied.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append("\"").append(entry.getValue().replace("\"", "\\\"")).append("\"");
            first = false;
        }
        sb.append("}}");
        
        return sb.toString();
    }
    
    /**
     * 从JSON字符串反序列化
     */
    public static DevourData fromJson(String json) {
        try {
            // 提取UUID
            String uuidStr = extractValue(json, "uuid");
            UUID uuid = UUID.fromString(uuidStr);
            
            // 提取mythicType
            String mythicType = extractValue(json, "mythicType");
            
            // 提取maxSlots
            int maxSlots = Integer.parseInt(extractValue(json, "maxSlots"));
            
            DevourData data = new DevourData(uuid, mythicType, maxSlots);
            
            // 提取已吞噬物品ID
            String devouredIdsStr = extractArray(json, "devouredIds");
            if (!devouredIdsStr.isEmpty()) {
                for (String id : devouredIdsStr.split(",")) {
                    id = id.trim().replace("\"", "");
                    if (!id.isEmpty()) {
                        data.devouredItemIds.add(id);
                    }
                }
            }
            
            // 提取槽位占用（简化处理）
            
            return data;
        } catch (Exception e) {
            return null;
        }
    }
    
    private static String extractValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int start = json.indexOf(searchKey);
        if (start == -1) return "";
        
        start += searchKey.length();
        
        // 跳过引号
        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf("\"", start);
            return json.substring(start, end).replace("\\\"", "\"");
        } else if (json.charAt(start) == '[' || json.charAt(start) == '{') {
            // 数组或对象，返回空
            return "";
        } else {
            // 数字
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
                end++;
            }
            return json.substring(start, end);
        }
    }
    
    private static String extractArray(String json, String key) {
        String searchKey = "\"" + key + "\":[";
        int start = json.indexOf(searchKey);
        if (start == -1) return "";
        
        start += searchKey.length();
        int end = json.indexOf("]", start);
        if (end == -1) return "";
        
        return json.substring(start, end);
    }
}
