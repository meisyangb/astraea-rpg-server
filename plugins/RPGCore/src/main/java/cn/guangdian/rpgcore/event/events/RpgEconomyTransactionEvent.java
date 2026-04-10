package cn.guangdian.rpgcore.event.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 经济交易事件
 * 
 * <p>当玩家进行经济交易时触发（点券、金币等）。</p>
 * 
 * <p>使用示例：</p>
 * <pre>
 * &#64;EventHandler
 * public void onTransaction(RpgEconomyTransactionEvent event) {
 *     Player player = event.getPlayer();
 *     double amount = event.getAmount();
 *     String currency = event.getCurrency();
 *     // 记录交易日志
 * }
 * </pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class RpgEconomyTransactionEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public enum TransactionType {
        DEPOSIT,
        WITHDRAW,
        TRANSFER,
        SET
    }

    public enum CurrencyType {
        POINTS,
        MONEY,
        COINS,
        CUSTOM
    }

    private final Player player;
    private final TransactionType transactionType;
    private final CurrencyType currencyType;
    private final double amount;
    private final double balanceBefore;
    private final double balanceAfter;
    private final String reason;

    public RpgEconomyTransactionEvent(Player player, TransactionType transactionType, 
                                       CurrencyType currencyType, double amount,
                                       double balanceBefore, double balanceAfter, String reason) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.transactionType = transactionType;
        this.currencyType = currencyType;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.reason = reason;
    }

    public Player getPlayer() {
        return player;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public CurrencyType getCurrencyType() {
        return currencyType;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalanceBefore() {
        return balanceBefore;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
