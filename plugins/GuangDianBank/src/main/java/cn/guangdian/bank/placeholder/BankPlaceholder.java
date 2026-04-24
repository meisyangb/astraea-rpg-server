package cn.guangdian.bank.placeholder;

import cn.guangdian.bank.GuangDianBank;
import cn.guangdian.bank.data.BankAccount;
import cn.guangdian.rpgcore.integration.PlaceholderService;
import cn.guangdian.rpgcore.service.data.Loan;
import org.bukkit.entity.Player;

import java.util.List;

public class BankPlaceholder {
    
    private final GuangDianBank plugin;
    
    public BankPlaceholder(GuangDianBank plugin) {
        this.plugin = plugin;
    }
    
    public void register() {
        PlaceholderService service = PlaceholderService.getInstance();
        if (service == null) return;
        
        service.register("gdbank", (player, params) -> {
            if (player == null) return "";
            
            BankAccount account = plugin.getAccount(player.getUniqueId());
            if (account == null) return "0";
            
            return switch (params.toLowerCase()) {
                case "balance" -> String.valueOf(account.getBalance());
                case "balance_formatted" -> formatNumber(account.getBalance());
                case "credit_score" -> String.valueOf(account.getCreditScore());
                case "total_deposited" -> String.valueOf(account.getTotalDeposited());
                case "total_withdrawn" -> String.valueOf(account.getTotalWithdrawn());
                case "loan_count" -> String.valueOf(account.getActiveLoans().size());
                case "total_loan" -> String.valueOf(account.getTotalLoanAmount());
                case "total_loan_formatted" -> formatNumber(account.getTotalLoanAmount());
                case "max_loan" -> {
                    long maxLoan = plugin.getMaxLoanAmount() - account.getTotalLoanAmount();
                    yield String.valueOf(Math.max(0, maxLoan));
                }
                case "can_loan" -> account.getCreditScore() >= plugin.getMinCreditScoreForLoan() ? "true" : "false";
                case "deposit_rate" -> String.format("%.2f%%", plugin.getDepositInterestRate());
                case "loan_rate" -> String.format("%.2f%%", plugin.getLoanInterestRate());
                case "has_loan" -> account.getActiveLoans().isEmpty() ? "false" : "true";
                case "first_loan_amount" -> {
                    List<Loan> loans = account.getActiveLoans();
                    if (loans.isEmpty()) {
                        yield "0";
                    }
                    yield String.valueOf(loans.get(0).getCurrentAmount());
                }
                case "first_loan_remaining" -> {
                    List<Loan> loans = account.getActiveLoans();
                    if (loans.isEmpty()) {
                        yield "0";
                    }
                    long remaining = loans.get(0).getRemainingTime() / (24 * 60 * 60 * 1000L);
                    yield String.valueOf(remaining) + "天";
                }
                default -> null;
            };
        });
    }
    
    public void unregister() {
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
