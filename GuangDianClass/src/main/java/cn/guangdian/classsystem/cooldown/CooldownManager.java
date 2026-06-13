package cn.guangdian.classsystem.cooldown;

import cn.guangdian.classsystem.GuangDianClass;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {
    
    private final GuangDianClass plugin;
    private final Map<UUID, Map<String, Long>> cooldowns;
    private long advancementCooldownMillis;
    private long resetCooldownMillis;
    
    public CooldownManager(GuangDianClass plugin) {
        this.plugin = plugin;
        this.cooldowns = new ConcurrentHashMap<>();
        
        loadConfig();
    }
    
    private void loadConfig() {
        advancementCooldownMillis = plugin.getConfig().getLong("cooldowns.advancement", 0) * 1000;
        resetCooldownMillis = plugin.getConfig().getLong("cooldowns.reset", 300) * 1000;
    }
    
    public void setCooldown(UUID playerId, String type, long durationMillis) {
        Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(playerId, id -> new ConcurrentHashMap<>());
        playerCooldowns.put(type, System.currentTimeMillis() + durationMillis);
    }
    
    public boolean isOnCooldown(UUID playerId, String type) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return false;
        
        Long endTime = playerCooldowns.get(type);
        if (endTime == null) return false;
        
        return System.currentTimeMillis() < endTime;
    }
    
    public long getRemainingCooldown(UUID playerId, String type) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return 0;
        
        Long endTime = playerCooldowns.get(type);
        if (endTime == null) return 0;
        
        long remaining = endTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    public String getRemainingCooldownFormatted(UUID playerId, String type) {
        long remaining = getRemainingCooldown(playerId, type);
        if (remaining <= 0) return "0秒";
        
        long seconds = remaining / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return hours + "小时" + (minutes % 60) + "分钟";
        } else if (minutes > 0) {
            return minutes + "分钟" + (seconds % 60) + "秒";
        } else {
            return seconds + "秒";
        }
    }
    
    public void clearCooldown(UUID playerId, String type) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns != null) {
            playerCooldowns.remove(type);
        }
    }
    
    public void clearAllCooldowns(UUID playerId) {
        cooldowns.remove(playerId);
    }
    
    public boolean canAdvance(Player player) {
        return !isOnCooldown(player.getUniqueId(), "advancement");
    }
    
    public void setAdvancementCooldown(Player player) {
        if (advancementCooldownMillis > 0) {
            setCooldown(player.getUniqueId(), "advancement", advancementCooldownMillis);
        }
    }
    
    public boolean canReset(Player player) {
        return !isOnCooldown(player.getUniqueId(), "reset");
    }
    
    public void setResetCooldown(Player player) {
        if (resetCooldownMillis > 0) {
            setCooldown(player.getUniqueId(), "reset", resetCooldownMillis);
        }
    }
    
    public void reload() {
        loadConfig();
    }
    
    public void cleanup() {
        long now = System.currentTimeMillis();
        
        for (Map.Entry<UUID, Map<String, Long>> playerEntry : cooldowns.entrySet()) {
            Map<String, Long> playerCooldowns = playerEntry.getValue();
            playerCooldowns.entrySet().removeIf(entry -> entry.getValue() < now);
        }
        
        cooldowns.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
