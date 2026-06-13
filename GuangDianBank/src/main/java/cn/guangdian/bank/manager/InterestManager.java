package cn.guangdian.bank.manager;

import cn.guangdian.bank.GuangDianBank;
import cn.guangdian.bank.data.BankAccount;
import cn.guangdian.rpgcore.service.data.Loan;
import cn.guangdian.rpgcore.service.data.TransactionRecord;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class InterestManager {
    
    private final GuangDianBank plugin;
    private boolean running = true;
    
    public InterestManager(GuangDianBank plugin) {
        this.plugin = plugin;
    }
    
    public void processAllInterest() {
        if (!running) return;
        
        for (Map.Entry<UUID, BankAccount> entry : plugin.getAccounts().entrySet()) {
            try {
                processInterest(entry.getKey());
            } catch (Exception e) {
                plugin.getLogger().warning("处理玩家 " + entry.getKey() + " 的利息时出错: " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("已完成所有账户的利息结算");
    }
    
    public long processInterest(UUID playerId) {
        BankAccount account = plugin.getAccount(playerId);
        if (account == null) return 0;
        
        long balance = account.getBalance();
        if (balance <= 0) return 0;
        
        long currentTime = System.currentTimeMillis();
        long lastInterestTime = account.getLastInterestTime();
        long timeDiff = currentTime - lastInterestTime;
        
        long days = timeDiff / (24 * 60 * 60 * 1000L);
        if (days < 1) return 0;
        
        double rate = plugin.getDepositInterestRate() / 100.0;
        long interest = (long) (balance * rate * days);
        
        if (interest <= 0) return 0;
        
        long before = account.getBalance();
        account.setBalance(before + interest);
        account.setLastInterestTime(currentTime);
        
        TransactionRecord record = new TransactionRecord(
            "INT-" + System.currentTimeMillis(),
            playerId,
            TransactionRecord.TransactionType.INTEREST_EARNED,
            interest,
            before,
            account.getBalance(),
            "存款利息 (" + days + "天)",
            null
        );
        account.addTransaction(record);
        
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            plugin.sendSuccess(player, "§a你收到了 §e" + interest + " §a存款利息！");
        }
        
        return interest;
    }
    
    public void processLoanInterest(Loan loan) {
        if (loan.getStatus() != Loan.LoanStatus.ACTIVE && 
            loan.getStatus() != Loan.LoanStatus.OVERDUE) {
            return;
        }
        
        BankAccount account = plugin.getAccount(loan.getPlayerId());
        if (account == null) return;
        
        long days = (System.currentTimeMillis() - loan.getStartTime()) / (24 * 60 * 60 * 1000L);
        double interestAmount = loan.getCurrentAmount() * loan.getInterestRate() * days / 100.0;
        
        TransactionRecord record = new TransactionRecord(
            "LOAN-INT-" + System.currentTimeMillis(),
            loan.getPlayerId(),
            TransactionRecord.TransactionType.INTEREST_PAID,
            (long) interestAmount,
            account.getBalance(),
            account.getBalance(),
            "贷款利息: " + loan.getLoanId(),
            null
        );
        account.addTransaction(record);
    }
    
    public void shutdown() {
        running = false;
    }
}
