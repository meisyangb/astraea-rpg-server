package cn.guangdian.realm;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家修炼数据类
 */
public class CultivationPlayer {
    private final String playerId;
    private long cultivation;        // 当前修为
    private String currentRealmId;   // 当前境界ID
    private long totalGained;        // 累计获得修为
    private long lastBreakthroughTime; // 上次突破时间
    
    public CultivationPlayer(String playerId) {
        this.playerId = playerId;
        this.cultivation = 0;
        this.currentRealmId = null;
        this.totalGained = 0;
        this.lastBreakthroughTime = 0;
    }
    
    // Getters and Setters
    public String getPlayerId() { return playerId; }
    public long getCultivation() { return cultivation; }
    public void setCultivation(long cultivation) { this.cultivation = cultivation; }
    public String getCurrentRealmId() { return currentRealmId; }
    public void setCurrentRealmId(String currentRealmId) { this.currentRealmId = currentRealmId; }
    public long getTotalGained() { return totalGained; }
    public long getLastBreakthroughTime() { return lastBreakthroughTime; }
    public void setLastBreakthroughTime(long time) { this.lastBreakthroughTime = time; }
    
    /**
     * 增加修为
     */
    public void addCultivation(long amount) {
        this.cultivation += amount;
        this.totalGained += amount;
    }
    
    /**
     * 扣除修为
     */
    public void subtractCultivation(long amount) {
        this.cultivation = Math.max(0, this.cultivation - amount);
    }
    
    // 序列化
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("playerId", playerId);
        map.put("cultivation", cultivation);
        if (currentRealmId != null) map.put("currentRealmId", currentRealmId);
        map.put("totalGained", totalGained);
        map.put("lastBreakthroughTime", lastBreakthroughTime);
        return map;
    }
    
    public static CultivationPlayer deserialize(Map<String, Object> map) {
        CultivationPlayer player = new CultivationPlayer((String) map.get("playerId"));
        player.setCultivation(map.containsKey("cultivation") ? ((Number) map.get("cultivation")).longValue() : 0);
        player.setCurrentRealmId((String) map.get("currentRealmId"));
        player.totalGained = map.containsKey("totalGained") ? ((Number) map.get("totalGained")).longValue() : 0;
        player.lastBreakthroughTime = map.containsKey("lastBreakthroughTime") ? ((Number) map.get("lastBreakthroughTime")).longValue() : 0;
        return player;
    }
}