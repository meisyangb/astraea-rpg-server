package cn.guangdian.points.transaction;

import java.util.UUID;

/**
 * 未完成的事务数据类
 * 用于崩溃恢复时记录需要处理的事务
 */
public class UnfinishedTransaction {

    private final String transactionId;
    private final UUID playerUuid;
    private final TransactionLogger.TransactionType type;
    private final long amount;
    private final long timestamp;
    private final long balanceBefore;

    /**
     * 创建未完成事务记录
     */
    public UnfinishedTransaction(String transactionId, UUID playerUuid,
                                 TransactionLogger.TransactionType type,
                                 long amount, long timestamp, long balanceBefore) {
        this.transactionId = transactionId;
        this.playerUuid = playerUuid;
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
        this.balanceBefore = balanceBefore;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public TransactionLogger.TransactionType getType() {
        return type;
    }

    public long getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getBalanceBefore() {
        return balanceBefore;
    }

    /**
     * 判断是否为转出操作
     */
    public boolean isTransferOut() {
        return type == TransactionLogger.TransactionType.TRANSFER_OUT ||
               type == TransactionLogger.TransactionType.SPEND ||
               type == TransactionLogger.TransactionType.ADMIN_TAKE;
    }

    /**
     * 判断是否为转入操作
     */
    public boolean isTransferIn() {
        return type == TransactionLogger.TransactionType.TRANSFER_IN ||
               type == TransactionLogger.TransactionType.EARN ||
               type == TransactionLogger.TransactionType.ADMIN_GIVE;
    }

    @Override
    public String toString() {
        return "UnfinishedTransaction{" +
            "transactionId='" + transactionId + '\'' +
            ", playerUuid=" + playerUuid +
            ", type=" + type +
            ", amount=" + amount +
            ", balanceBefore=" + balanceBefore +
            '}';
    }
}