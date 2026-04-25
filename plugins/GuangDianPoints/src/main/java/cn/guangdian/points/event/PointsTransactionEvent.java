package cn.guangdian.points.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 点券交易事件
 *
 * <p>当玩家点券余额变化时触发此事件。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class PointsTransactionEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public enum TransactionType {
        DEPOSIT,
        WITHDRAW,
        TRANSFER_OUT,
        TRANSFER_IN,
        SET,
        RESET
    }

    private final UUID playerId;
    private final TransactionType transactionType;
    private final long amount;
    private final long balanceBefore;
    private final long balanceAfter;
    private final String reason;
    private final UUID relatedPlayerId;

    public PointsTransactionEvent(UUID playerId, TransactionType transactionType,
                                   long amount, long balanceBefore, long balanceAfter,
                                   String reason) {
        this(playerId, transactionType, amount, balanceBefore, balanceAfter, reason, null);
    }

    public PointsTransactionEvent(UUID playerId, TransactionType transactionType,
                                   long amount, long balanceBefore, long balanceAfter,
                                   String reason, UUID relatedPlayerId) {
        super(!Bukkit.isPrimaryThread());
        this.playerId = playerId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.reason = reason;
        this.relatedPlayerId = relatedPlayerId;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public long getAmount() {
        return amount;
    }

    public long getBalanceBefore() {
        return balanceBefore;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    public String getReason() {
        return reason;
    }

    public UUID getRelatedPlayerId() {
        return relatedPlayerId;
    }

    public boolean isTransfer() {
        return transactionType == TransactionType.TRANSFER_OUT ||
               transactionType == TransactionType.TRANSFER_IN;
    }

    public boolean isDeposit() {
        return transactionType == TransactionType.DEPOSIT ||
               transactionType == TransactionType.TRANSFER_IN;
    }

    public boolean isWithdraw() {
        return transactionType == TransactionType.WITHDRAW ||
               transactionType == TransactionType.TRANSFER_OUT;
    }
}
