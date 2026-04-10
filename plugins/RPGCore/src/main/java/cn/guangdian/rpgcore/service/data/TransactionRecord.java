package cn.guangdian.rpgcore.service.data;

import java.util.UUID;

public class TransactionRecord {
    
    private final String transactionId;
    private final UUID playerId;
    private final TransactionType type;
    private final long amount;
    private final long balanceBefore;
    private final long balanceAfter;
    private final long timestamp;
    private final String description;
    private final UUID relatedPlayer;
    
    public TransactionRecord(String transactionId, UUID playerId, TransactionType type,
                            long amount, long balanceBefore, long balanceAfter,
                            String description, UUID relatedPlayer) {
        this.transactionId = transactionId;
        this.playerId = playerId;
        this.type = type;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.timestamp = System.currentTimeMillis();
        this.description = description;
        this.relatedPlayer = relatedPlayer;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public TransactionType getType() {
        return type;
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
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getDescription() {
        return description;
    }
    
    public UUID getRelatedPlayer() {
        return relatedPlayer;
    }
    
    public enum TransactionType {
        DEPOSIT,
        WITHDRAW,
        TRANSFER_IN,
        TRANSFER_OUT,
        LOAN_TAKE,
        LOAN_REPAY,
        INTEREST_EARNED,
        INTEREST_PAID,
        PENALTY,
        ADMIN_SET,
        ADMIN_GIVE,
        ADMIN_TAKE
    }
}
