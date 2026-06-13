package cn.guangdian.bank.placeholder;

import cn.guangdian.bank.GuangDianBank;
import cn.guangdian.bank.data.BankAccount;
import cn.guangdian.rpgcore.service.data.Loan;
import org.bukkit.entity.Player;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import java.util.List;

public class BankPlaceholder extends PlaceholderExpansion {
    
    private final GuangDianBank plugin;
    
    public BankPlaceholder(GuangDianBank plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getIdentifier() {
        return "gdbank";
    }
    
    @Override
    public String getAuthor() {
        return "Astraea RPG Team";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }
        
        BankAccount account = plugin.getAccount(player.getUniqueId());
        if (account == null) {
            return "0";
        }
        
        switch (identifier.toLowerCase()) {
            case "balance":
                return String.valueOf(account.getBalance());
                
            case "balance_formatted":
                return formatNumber(account.getBalance());
                
            case "credit_score":
                return String.valueOf(account.getCreditScore());
                
            case "total_deposited":
                return String.valueOf(account.getTotalDeposited());
                
            case "total_withdrawn":
                return String.valueOf(account.getTotalWithdrawn());
                
            case "loan_count":
                return String.valueOf(account.getActiveLoans().size());
                
            case "total_loan":
                return String.valueOf(account.getTotalLoanAmount());
                
            case "total_loan_formatted":
                return formatNumber(account.getTotalLoanAmount());
                
            case "max_loan":
                long maxLoan = plugin.getMaxLoanAmount() - account.getTotalLoanAmount();
                return String.valueOf(Math.max(0, maxLoan));
                
            case "can_loan":
                return account.getCreditScore() >= plugin.getMinCreditScoreForLoan() ? "true" : "false";
                
            case "deposit_rate":
                return String.format("%.2f%%", plugin.getDepositInterestRate());
                
            case "loan_rate":
                return String.format("%.2f%%", plugin.getLoanInterestRate());
                
            case "has_loan":
                return account.getActiveLoans().isEmpty() ? "false" : "true";
                
            case "first_loan_amount":
                List<Loan> loans = account.getActiveLoans();
                if (loans.isEmpty()) {
                    return "0";
                }
                return String.valueOf(loans.get(0).getCurrentAmount());
                
            case "first_loan_remaining":
                loans = account.getActiveLoans();
                if (loans.isEmpty()) {
                    return "0";
                }
                long remaining = loans.get(0).getRemainingTime() / (24 * 60 * 60 * 1000L);
                return String.valueOf(remaining) + "天";
                
            default:
                return null;
        }
    }
    
    private String formatNumber(long num) {
        if (num >= 100000000) {
            return String.format("%.2f亿", num / 100000000.0);
        } else if (num >= 10000) {
            return String.format("%.2f万", num / 10000.0);
        }
        return String.format("%,d", num);
    }
}
