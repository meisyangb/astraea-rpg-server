package cn.guangdian.armorstats.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BossBar优化器
 * 
 * 职责:
 * - 按需更新BossBar，减少网络开销
 * - 管理战斗状态，支持不同更新频率
 * - 跟踪上次显示的血量，避免无意义更新
 * 
 * 优化策略:
 * 1. 血量变化小于阈值时跳过更新
 * 2. 战斗状态下使用高频更新
 * 3. 非战斗状态下使用低频更新
 * 4. 血量满时隐藏BossBar
 */
public class BossBarOptimizer {
    
    // 上次显示的血量
    private final Map<UUID, Double> lastHealthDisplay;
    
    // 战斗状态 (玩家UUID -> 进入战斗的时间戳)
    private final Map<UUID, Long> combatState;
    
    // 上次更新时间
    private final Map<UUID, Long> lastUpdateTime;
    
    // 配置参数
    private final double minHealthChange;      // 最小血量变化阈值 (0.5 = 半颗心)
    private final long combatDuration;         // 战斗状态持续时间 (毫秒)
    private final long combatUpdateInterval;   // 战斗中更新间隔 (毫秒)
    private final long normalUpdateInterval;   // 非战斗更新间隔 (毫秒)
    
    /**
     * 构造函数
     * 
     * @param minHealthChange 最小血量变化阈值 (默认0.5)
     * @param combatDuration 战斗状态持续时间 (默认5000ms)
     * @param combatUpdateInterval 战斗中更新间隔 (默认100ms)
     * @param normalUpdateInterval 非战斗更新间隔 (默认1000ms)
     */
    public BossBarOptimizer(
            double minHealthChange,
            long combatDuration,
            long combatUpdateInterval,
            long normalUpdateInterval) {
        this.lastHealthDisplay = new ConcurrentHashMap<>();
        this.combatState = new ConcurrentHashMap<>();
        this.lastUpdateTime = new ConcurrentHashMap<>();
        this.minHealthChange = minHealthChange;
        this.combatDuration = combatDuration;
        this.combatUpdateInterval = combatUpdateInterval;
        this.normalUpdateInterval = normalUpdateInterval;
    }
    
    /**
     * 检查是否需要更新BossBar
     * 
     * 判断逻辑:
     * 1. 如果血量变化小于阈值，跳过更新
     * 2. 如果在战斗中，检查是否超过战斗更新间隔
     * 3. 如果非战斗，检查是否超过普通更新间隔
     * 
     * @param playerUuid 玩家UUID
     * @param currentHealth 当前血量
     * @return true表示需要更新，false表示跳过
     */
    public boolean shouldUpdate(UUID playerUuid, double currentHealth) {
        // 获取上次显示的血量
        Double lastHealth = lastHealthDisplay.get(playerUuid);
        
        // 首次更新，必须显示
        if (lastHealth == null) {
            return true;
        }
        
        // 计算血量变化
        double healthChange = Math.abs(currentHealth - lastHealth);
        
        // 血量变化小于阈值，跳过更新
        if (healthChange < minHealthChange) {
            return false;
        }
        
        // 检查更新间隔
        long now = System.currentTimeMillis();
        Long lastUpdate = lastUpdateTime.get(playerUuid);
        
        if (lastUpdate == null) {
            return true;
        }
        
        long timeSinceLastUpdate = now - lastUpdate;
        
        // 根据战斗状态选择更新间隔
        if (isInCombat(playerUuid)) {
            return timeSinceLastUpdate >= combatUpdateInterval;
        } else {
            return timeSinceLastUpdate >= normalUpdateInterval;
        }
    }
    
    /**
     * 记录更新
     * 
     * @param playerUuid 玩家UUID
     * @param health 当前血量
     */
    public void recordUpdate(UUID playerUuid, double health) {
        lastHealthDisplay.put(playerUuid, health);
        lastUpdateTime.put(playerUuid, System.currentTimeMillis());
    }
    
    /**
     * 进入战斗状态
     * 
     * @param playerUuid 玩家UUID
     */
    public void enterCombat(UUID playerUuid) {
        combatState.put(playerUuid, System.currentTimeMillis());
    }
    
    /**
     * 检查是否在战斗中
     * 
     * @param playerUuid 玩家UUID
     * @return true表示在战斗中，false表示非战斗
     */
    public boolean isInCombat(UUID playerUuid) {
        Long combatStartTime = combatState.get(playerUuid);
        
        if (combatStartTime == null) {
            return false;
        }
        
        long now = System.currentTimeMillis();
        long timeSinceCombat = now - combatStartTime;
        
        // 超过战斗持续时间，退出战斗状态
        if (timeSinceCombat > combatDuration) {
            combatState.remove(playerUuid);
            return false;
        }
        
        return true;
    }
    
    /**
     * 清理玩家数据
     * 
     * @param playerUuid 玩家UUID
     */
    public void cleanup(UUID playerUuid) {
        lastHealthDisplay.remove(playerUuid);
        combatState.remove(playerUuid);
        lastUpdateTime.remove(playerUuid);
    }
    
    /**
     * 获取统计信息
     * 
     * @return 统计信息字符串
     */
    public String getStats() {
        return String.format(
            "BossBarOptimizer Stats - Tracked Players: %d, In Combat: %d",
            lastHealthDisplay.size(),
            combatState.size()
        );
    }
}
