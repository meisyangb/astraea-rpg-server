package cn.guangdian.rpgcore.service.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 点券服务接口
 * 
 * <p>提供玩家点券/积分的管理功能。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 获取服务
 * PointsService points = serviceRegistry.getService(PointsService.class);
 * 
 * // 查询余额
 * long balance = points.getBalance(playerId);
 * 
 * // 扣除点券
 * boolean success = points.removeBalance(playerId, 100, "购买物品");
 * 
 * // 异步操作
 * points.getBalanceAsync(playerId).thenAccept(bal -> {
 *     player.sendMessage("你的点券: " + bal);
 * });
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface PointsService {

    // ==================== 同步操作 ====================

    /**
     * 获取玩家余额
     * 
     * @param playerId 玩家UUID
     * @return 余额
     */
    long getBalance(UUID playerId);

    /**
     * 设置玩家余额
     * 
     * @param playerId 玩家UUID
     * @param amount 金额
     * @param reason 原因（用于日志记录）
     */
    void setBalance(UUID playerId, long amount, String reason);

    /**
     * 增加玩家余额
     * 
     * @param playerId 玩家UUID
     * @param amount 金额
     * @param reason 原因
     */
    void addBalance(UUID playerId, long amount, String reason);

    /**
     * 扣除玩家余额
     * 
     * @param playerId 玩家UUID
     * @param amount 金额
     * @param reason 原因
     * @return 如果扣除成功返回 true（余额不足返回 false）
     */
    boolean removeBalance(UUID playerId, long amount, String reason);

    /**
     * 转账
     * 
     * @param from 付款方UUID
     * @param to 收款方UUID
     * @param amount 金额
     * @param reason 原因
     * @return 如果转账成功返回 true
     */
    boolean transfer(UUID from, UUID to, long amount, String reason);

    /**
     * 检查是否有足够余额
     * 
     * @param playerId 玩家UUID
     * @param amount 金额
     * @return 如果余额足够返回 true
     */
    boolean hasBalance(UUID playerId, long amount);

    // ==================== 异步操作 ====================

    /**
     * 异步获取玩家余额
     * 
     * @param playerId 玩家UUID
     * @return 包含余额的 CompletableFuture
     */
    CompletableFuture<Long> getBalanceAsync(UUID playerId);

    /**
     * 异步设置玩家余额
     * 
     * @param playerId 玩家UUID
     * @param amount 金额
     * @param reason 原因
     * @return 操作完成的 CompletableFuture
     */
    CompletableFuture<Void> setBalanceAsync(UUID playerId, long amount, String reason);

    /**
     * 异步增加玩家余额
     * 
     * @param playerId 玩家UUID
     * @param amount 金额
     * @param reason 原因
     * @return 操作完成的 CompletableFuture
     */
    CompletableFuture<Void> addBalanceAsync(UUID playerId, long amount, String reason);

    /**
     * 异步扣除玩家余额
     * 
     * @param playerId 玩家UUID
     * @param amount 金额
     * @param reason 原因
     * @return 包含操作结果的 CompletableFuture
     */
    CompletableFuture<Boolean> removeBalanceAsync(UUID playerId, long amount, String reason);

    /**
     * 异步转账
     * 
     * @param from 付款方UUID
     * @param to 收款方UUID
     * @param amount 金额
     * @param reason 原因
     * @return 包含操作结果的 CompletableFuture
     */
    CompletableFuture<Boolean> transferAsync(UUID from, UUID to, long amount, String reason);

    // ==================== 管理操作 ====================

    /**
     * 管理员给予点券
     * 
     * @param playerId 玩家UUID
     * @param amount 金额
     * @param admin 操作管理员UUID
     * @param reason 原因
     */
    void adminGive(UUID playerId, long amount, UUID admin, String reason);

    /**
     * 管理员扣除点券
     * 
     * @param playerId 玩家UUID
     * @param amount 金额
     * @param admin 操作管理员UUID
     * @param reason 原因
     * @return 如果扣除成功返回 true
     */
    boolean adminTake(UUID playerId, long amount, UUID admin, String reason);

    /**
     * 重置玩家余额
     * 
     * @param playerId 玩家UUID
     * @param reason 原因
     */
    void resetBalance(UUID playerId, String reason);

    /**
     * 获取默认余额
     * 
     * @return 默认余额
     */
    long getDefaultBalance();

    /**
     * 获取在线玩家总数
     * 
     * @return 在线玩家总数
     */
    int getOnlinePlayerCount();

    /**
     * 获取总点券流通量
     * 
     * @return 总流通量
     */
    long getTotalCirculation();
}