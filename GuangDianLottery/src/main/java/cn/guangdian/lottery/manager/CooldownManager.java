package cn.guangdian.lottery.manager;

import cn.guangdian.lottery.GuangDianLottery;
import cn.guangdian.lottery.model.LotteryPool;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final GuangDianLottery plugin;
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    
    public CooldownManager(GuangDianLottery plugin) {
        this.plugin = plugin;
    }
    
    public boolean isOnCooldown(UUID playerId, String poolId) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return false;
        
        Long lastDraw = playerCooldowns.get(poolId);
        if (lastDraw == null) return false;
        
        LotteryPool pool = plugin.getPools().get(poolId);
        if (pool == null) return false;
        
        long cooldownMs = pool.getCooldownSeconds() * 1000L;
        return System.currentTimeMillis() - lastDraw < cooldownMs;
    }
    
    public long getRemainingCooldown(UUID playerId, String poolId) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return 0;
        
        Long lastDraw = playerCooldowns.get(poolId);
        if (lastDraw == null) return 0;
        
        LotteryPool pool = plugin.getPools().get(poolId);
        if (pool == null) return 0;
        
        long cooldownMs = pool.getCooldownSeconds() * 1000L;
        long remaining = cooldownMs - (System.currentTimeMillis() - lastDraw);
        return Math.max(0, remaining);
    }
    
    public void setCooldown(UUID playerId, String poolId) {
        cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .put(poolId, System.currentTimeMillis());
    }
    
    public void clearCooldown(UUID playerId, String poolId) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns != null) {
            playerCooldowns.remove(poolId);
        }
    }
    
    public void clearAllCooldowns(UUID playerId) {
        cooldowns.remove(playerId);
    }
    
    public void cleanup(UUID playerId) {
        cooldowns.remove(playerId);
    }
}
