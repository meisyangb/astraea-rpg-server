package cn.guangdian.rpgcore.event.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * 点券交易事件 - Bukkit 原生事件
 *
 * <p>当玩家点券余额变化时触发此事件，其他插件可以通过 @EventHandler 监听此事件。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 监听事件
 * @EventHandler
 * public void onPointsTransaction(PointsTransactionEvent event) {
 *     if (event.getTransactionType() == TransactionType.TRANSFER) {
 *         getLogger().info("玩家 " + event.getPlayerId() + " 转账 " + event.getAmount() + " 点券");
 *     }
 * }
 *
 * // 发布事件
 * Bukkit.getPluginManager().callEvent(new PointsTransactionEvent(
 *     playerId, TransactionType.DEPOSIT, amount, before, after, "充值"
 * ));
 * }</pre>
 *
 * @author GuangDian
 * @since 2.0.0
 */
public class PointsTransactionEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * 交易类型
     */
    public enum TransactionType {
        /** 增加点券 */
        DEPOSIT,
        /** 扣除点券 */
        WITHDRAW,
        /** 转账（作为发起方） */
        TRANSFER_OUT,
        /** 转账（作为接收方） */
        TRANSFER_IN,
        /** 设置余额 */
        SET,
        /** 重置余额 */
        RESET
    }

    private final UUID playerId;
    private final TransactionType transactionType;
    private final long amount;
    private final long balanceBefore;
    private final long balanceAfter;
    private final String reason;
    private final UUID relatedPlayerId; // 用于转账时记录对方玩家

    /**
     * 创建点券交易事件
     *
     * @param playerId 玩家UUID
     * @param transactionType 交易类型
     * @param amount 金额
     * @param balanceBefore 变更前余额
     * @param balanceAfter 变更后余额
     * @param reason 原因
     */
    public PointsTransactionEvent(UUID playerId, TransactionType transactionType,
                                   long amount, long balanceBefore, long balanceAfter,
                                   String reason) {
        this(playerId, transactionType, amount, balanceBefore, balanceAfter, reason, null);
    }

    /**
     * 创建点券交易事件（带关联玩家）
     *
     * @param playerId 玩家UUID
     * @param transactionType 交易类型
     * @param amount 金额
     * @param balanceBefore 变更前余额
     * @param balanceAfter 变更后余额
     * @param reason 原因
     * @param relatedPlayerId 关联玩家UUID（转账时使用）
     */
    public PointsTransactionEvent(UUID playerId, TransactionType transactionType,
                                   long amount, long balanceBefore, long balanceAfter,
                                   String reason, UUID relatedPlayerId) {
        this.playerId = playerId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.reason = reason;
        this.relatedPlayerId = relatedPlayerId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * 获取玩家UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * 获取交易类型
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * 获取金额
     */
    public long getAmount() {
        return amount;
    }

    /**
     * 获取变更前余额
     */
    public long getBalanceBefore() {
        return balanceBefore;
    }

    /**
     * 获取变更后余额
     */
    public long getBalanceAfter() {
        return balanceAfter;
    }

    /**
     * 获取原因
     */
    public String getReason() {
        return reason;
    }

    /**
     * 获取关联玩家UUID（转账时使用）
     */
    public UUID getRelatedPlayerId() {
        return relatedPlayerId;
    }

    /**
     * 检查是否为转账交易
     */
    public boolean isTransfer() {
        return transactionType == TransactionType.TRANSFER_OUT ||
               transactionType == TransactionType.TRANSFER_IN;
    }

    /**
     * 检查是否为增加余额
     */
    public boolean isDeposit() {
        return transactionType == TransactionType.DEPOSIT ||
               transactionType == TransactionType.TRANSFER_IN;
    }

    /**
     * 检查是否为扣除余额
     */
    public boolean isWithdraw() {
        return transactionType == TransactionType.WITHDRAW ||
               transactionType == TransactionType.TRANSFER_OUT;
    }

    @Override
    public String toString() {
        return String.format("PointsTransactionEvent{player=%s, type=%s, amount=%d, before=%d, after=%d, reason='%s'}",
            playerId, transactionType, amount, balanceBefore, balanceAfter, reason);
    }
}
