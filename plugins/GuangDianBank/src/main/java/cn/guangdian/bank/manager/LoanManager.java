package cn.guangdian.bank.manager;

import cn.guangdian.bank.GuangDianBank;
import cn.guangdian.bank.data.BankAccount;
import cn.guangdian.rpgcore.service.data.Loan;
import cn.guangdian.rpgcore.service.data.TransactionRecord;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LoanManager {
    
    private final GuangDianBank plugin;
    private final Map<String, Loan> loans = new ConcurrentHashMap<>();
    private boolean running = true;
    
    public LoanManager(GuangDianBank plugin) {
        this.plugin = plugin;
    }
    
    public Optional<Loan> createLoan(UUID playerId, long amount, int durationDays) {
        BankAccount account = plugin.getAccount(playerId);
        if (account == null) {
            return Optional.empty();
        }
        
        if (!plugin.getAccount(playerId).getActiveLoans().isEmpty()) {
            long totalLoans = account.getTotalLoanAmount();
            if (totalLoans + amount > plugin.getMaxLoanAmount()) {
                return Optional.empty();
            }
        }
        
        String loanId = "LOAN-" + System.currentTimeMillis() + "-" + playerId.toString().substring(0, 8);
        
        Loan loan = new Loan(loanId, playerId, amount, plugin.getLoanInterestRate(), durationDays);
        
        account.addLoan(loan);
        loans.put(loanId, loan);
        
        long before = account.getBalance();
        account.setBalance(before + amount);
        
        TransactionRecord record = new TransactionRecord(
            loanId,
            playerId,
            TransactionRecord.TransactionType.LOAN_TAKE,
            amount,
            before,
            account.getBalance(),
            "贷款: " + durationDays + "天",
            null
        );
        account.addTransaction(record);
        
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            plugin.sendSuccess(player, "§a你成功贷款 §e" + amount + " §a，期限 " + durationDays + " 天");
        }
        
        return Optional.of(loan);
    }
    
    public boolean repayLoan(UUID playerId, String loanId, long amount) {
        Loan loan = loans.get(loanId);
        if (loan == null) {
            return false;
        }
        
        if (!loan.getPlayerId().equals(playerId)) {
            return false;
        }
        
        BankAccount account = plugin.getAccount(playerId);
        if (account == null) {
            return false;
        }
        
        if (account.getBalance() < amount) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                plugin.sendError(player, "§c余额不足，无法还款！");
            }
            return false;
        }
        
        long before = account.getBalance();
        account.setBalance(before - amount);
        
        long remaining = loan.getCurrentAmount() - amount;
        if (remaining <= 0) {
            loan.setStatus(Loan.LoanStatus.PAID);
            account.removeLoan(loan);
            loans.remove(loanId);
            
            account.addCreditScore(5);
            
            TransactionRecord record = new TransactionRecord(
                loanId + "-REPAY",
                playerId,
                TransactionRecord.TransactionType.LOAN_REPAY,
                amount,
                before,
                account.getBalance(),
                "贷款还清: " + loanId,
                null
            );
            account.addTransaction(record);
            
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                plugin.sendSuccess(player, "§a你已还清贷款！");
                plugin.sendSuccess(player, "§a信用评分 +5");
            }
        } else {
            loan.setCurrentAmount(remaining);
            
            TransactionRecord record = new TransactionRecord(
                loanId + "-REPAY",
                playerId,
                TransactionRecord.TransactionType.LOAN_REPAY,
                amount,
                before,
                account.getBalance(),
                "贷款还款: " + loanId + " (剩余: " + remaining + ")",
                null
            );
            account.addTransaction(record);
            
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                plugin.sendSuccess(player, "§a还款成功！剩余: §e" + remaining);
            }
        }
        
        return true;
    }
    
    public Optional<Loan> getLoan(String loanId) {
        return Optional.ofNullable(loans.get(loanId));
    }
    
    public void checkOverdueLoans() {
        if (!running) return;
        
        for (Loan loan : loans.values()) {
            if (loan.getStatus() == Loan.LoanStatus.ACTIVE && loan.isOverdue()) {
                loan.setStatus(Loan.LoanStatus.OVERDUE);
                
                BankAccount account = plugin.getAccount(loan.getPlayerId());
                if (account != null) {
                    account.addCreditScore(-10);
                    
                    Player player = Bukkit.getPlayer(loan.getPlayerId());
                    if (player != null && player.isOnline()) {
                        plugin.sendError(player, "§c你的贷款已逾期！信用评分 -10");
                    }
                }
            }
        }
    }
    
    public void shutdown() {
        running = false;
    }
}
