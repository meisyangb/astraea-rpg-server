package cn.guangdian.sect;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家门派数据类
 */
public class SectPlayer {
    private final String playerId;
    private String sectId;
    private String rankId;
    private long joinTime;
    private int contribution;
    private long lastLeaveTime;
    
    public SectPlayer(String playerId) {
        this.playerId = playerId;
        this.joinTime = 0;
        this.contribution = 0;
        this.lastLeaveTime = 0;
    }
    
    // Getters and Setters
    public String getPlayerId() { return playerId; }
    public String getSectId() { return sectId; }
    public void setSectId(String sectId) { this.sectId = sectId; }
    public String getRankId() { return rankId; }
    public void setRankId(String rankId) { this.rankId = rankId; }
    public long getJoinTime() { return joinTime; }
    public void setJoinTime(long joinTime) { this.joinTime = joinTime; }
    public int getContribution() { return contribution; }
    public void setContribution(int contribution) { this.contribution = contribution; }
    public long getLastLeaveTime() { return lastLeaveTime; }
    public void setLastLeaveTime(long lastLeaveTime) { this.lastLeaveTime = lastLeaveTime; }
    
    public void addContribution(int amount) {
        this.contribution += amount;
    }
    
    // 序列化
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("playerId", playerId);
        if (sectId != null) map.put("sectId", sectId);
        if (rankId != null) map.put("rankId", rankId);
        map.put("joinTime", joinTime);
        map.put("contribution", contribution);
        map.put("lastLeaveTime", lastLeaveTime);
        return map;
    }
    
    public static SectPlayer deserialize(Map<String, Object> map) {
        SectPlayer player = new SectPlayer((String) map.get("playerId"));
        player.setSectId((String) map.get("sectId"));
        player.setRankId((String) map.get("rankId"));
        if (map.containsKey("joinTime")) {
            player.setJoinTime(((Number) map.get("joinTime")).longValue());
        }
        if (map.containsKey("contribution")) {
            player.setContribution(((Number) map.get("contribution")).intValue());
        }
        if (map.containsKey("lastLeaveTime")) {
            player.setLastLeaveTime(((Number) map.get("lastLeaveTime")).longValue());
        }
        return player;
    }
}