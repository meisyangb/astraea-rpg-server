package cn.guangdian.rpgcore.util;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 冷却时间管理器 - RPGCore 核心工具
 *
 * <p>提供统一的冷却时间管理功能，避免各插件重复实现。</p>
 *
 * <h2>使用示例:</h2>
 * <pre>{@code
 * CooldownManager cooldown = CooldownManager.getInstance();
 *
 * // 设置冷却 (10秒)
 * cooldown.setCooldown(playerUUID, "trade_request", 10000);
 *
 * // 检查是否在冷却中
 * if (cooldown.isOnCooldown(playerUUID, "trade_request")) {
 *     long remaining = cooldown.getRemainingSeconds(playerUUID, "trade_request");
 *     player.sendMessage("还需等待 " + remaining + " 秒");
 *     return;
 * }
 *
 * // 执行操作...
 *
 * // 清除冷却
 * cooldown.clearCooldown(playerUUID, "trade_request");
 * }</pre>
 *
 * @author Astraea RPG Team
 * @since 1.1.0
 */
public final class CooldownManager {

    private static CooldownManager instance;

    /**
     * 冷却数据结构: playerId -> action -> expireTime (毫秒时间戳)
     */
    private final Map<UUID, Map<String, Long>> cooldowns;
    private final SyncScheduler scheduler;

    private CooldownManager() {
        this.cooldowns = new ConcurrentHashMap<>();
        RPGCore rpgCore = RPGCore.getInstance();
        this.scheduler = rpgCore != null ? rpgCore.getScheduler() : null;
    }

    public static synchronized CooldownManager getInstance() {
        if (instance == null) {
            instance = new CooldownManager();
        }
        return instance;
    }

    /**
     * 设置冷却时间
     *
     * @param playerId 玩家 UUID
     * @param action 动作名称 (如 "trade_request", "command_use")
     * @param durationMs 冷却时长 (毫秒)
     */
    public void setCooldown(@NotNull UUID playerId, @NotNull String action, long durationMs) {
        long expireTime = System.currentTimeMillis() + durationMs;
        cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                 .put(action, expireTime);
    }

    /**
     * 检查是否在冷却中
     *
     * @param playerId 玩家 UUID
     * @param action 动作名称
     * @return true 如果在冷却中
     */
    public boolean isOnCooldown(@NotNull UUID playerId, @NotNull String action) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return false;
        }

        Long expireTime = playerCooldowns.get(action);
        if (expireTime == null) {
            return false;
        }

        // 检查是否已过期
        if (System.currentTimeMillis() >= expireTime) {
            playerCooldowns.remove(action);
            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(playerId);
            }
            return false;
        }

        return true;
    }

    /**
     * 获取剩余冷却时间 (毫秒)
     *
     * @param playerId 玩家 UUID
     * @param action 动作名称
     * @return 剩余毫秒数，如果不在冷却中返回 0
     */
    public long getRemainingMillis(@NotNull UUID playerId, @NotNull String action) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return 0;
        }

        Long expireTime = playerCooldowns.get(action);
        if (expireTime == null) {
            return 0;
        }

        long remaining = expireTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * 获取剩余冷却时间 (秒)
     *
     * @param playerId 玩家 UUID
     * @param action 动作名称
     * @return 剩余秒数，如果不在冷却中返回 0
     */
    public long getRemainingSeconds(@NotNull UUID playerId, @NotNull String action) {
        return getRemainingMillis(playerId, action) / 1000;
    }

    /**
     * 清除指定冷却
     *
     * @param playerId 玩家 UUID
     * @param action 动作名称
     */
    public void clearCooldown(@NotNull UUID playerId, @NotNull String action) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns != null) {
            playerCooldowns.remove(action);
            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(playerId);
            }
        }
    }

    /**
     * 清除玩家所有冷却
     *
     * @param playerId 玩家 UUID
     */
    public void clearAllCooldowns(@NotNull UUID playerId) {
        cooldowns.remove(playerId);
    }

    /**
     * 检查是否有任意冷却
     *
     * @param playerId 玩家 UUID
     * @return true 如果有任何冷却
     */
    public boolean hasAnyCooldown(@NotNull UUID playerId) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return false;
        }

        // 清理已过期的冷却
        long now = System.currentTimeMillis();
        playerCooldowns.entrySet().removeIf(entry -> now >= entry.getValue());

        return !playerCooldowns.isEmpty();
    }

    /**
     * 获取玩家所有冷却信息
     *
     * @param playerId 玩家 UUID
     * @return 动作名称 -> 剩余毫秒数
     */
    public @NotNull Map<String, Long> getAllCooldowns(@NotNull UUID playerId) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return new ConcurrentHashMap<>();
        }

        Map<String, Long> result = new ConcurrentHashMap<>();
        long now = System.currentTimeMillis();

        for (Map.Entry<String, Long> entry : playerCooldowns.entrySet()) {
            long remaining = entry.getValue() - now;
            if (remaining > 0) {
                result.put(entry.getKey(), remaining);
            }
        }

        return result;
    }

    /**
     * 启动定期清理任务 (清理过期的冷却数据)
     *
     * @param intervalTicks 清理间隔 (ticks)
     */
    public void startCleanupTask(long intervalTicks) {
        if (scheduler == null) {
            return;
        }

        scheduler.runSyncRepeating(() -> {
            long now = System.currentTimeMillis();
            cooldowns.values().forEach(playerCooldowns ->
                playerCooldowns.entrySet().removeIf(entry -> now >= entry.getValue())
            );
            cooldowns.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        }, intervalTicks, intervalTicks);
    }

    /**
     * 获取统计信息
     */
    public @NotNull String getStats() {
        int totalPlayers = cooldowns.size();
        int totalCooldowns = cooldowns.values().stream()
            .mapToInt(Map::size)
            .sum();
        return String.format("玩家数: %d, 冷却总数: %d", totalPlayers, totalCooldowns);
    }
}
