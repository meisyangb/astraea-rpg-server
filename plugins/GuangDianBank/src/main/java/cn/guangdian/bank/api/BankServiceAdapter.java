package cn.guangdian.bank.api;

import cn.guangdian.bank.GuangDianBank;
import cn.guangdian.bank.data.BankAccount;
import cn.guangdian.rpgcore.service.data.Loan;
import cn.guangdian.rpgcore.service.data.TransactionRecord;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.service.api.BankService;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class BankServiceAdapter implements BankService {
    
    private final GuangDianBank plugin;
    private final boolean useRPGCore;
    
    public BankServiceAdapter(GuangDianBank plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        
        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                registry.registerService(BankService.class, this);
                plugin.getLogger().info("已注册到 RPGCore 服务注册表: BankService");
            } catch (Exception e) {
                plugin.getLogger().warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }
    
    @Override
    public long getBalance(UUID playerId) {
        BankAccount account = plugin.getAccount(playerId);
        return account != null ? account.getBalance() : 0;
    }
    
    @Override
    public boolean hasAccount(UUID playerId) {
        return plugin.hasAccount(playerId);
    }
    
    @Override
    public boolean createAccount(UUID playerId) {
        if (hasAccount(playerId)) {
            return false;
        }
        plugin.getAccount(playerId);
        return true;
    }
    
    @Override
    public int getCreditScore(UUID playerId) {
        BankAccount account = plugin.getAccount(playerId);
        return account != null ? account.getCreditScore() : 100;
    }
    
    @Override
    public void setCreditScore(UUID playerId, int score, String reason) {
        BankAccount account = plugin.getAccount(playerId);
        if (account != null) {
            account.setCreditScore(score);
        }
    }
    
    @Override
    public boolean deposit(UUID playerId, long amount, String reason) {
        if (amount <= 0) return false;
        
        BankAccount account = plugin.getAccount(playerId);
        if (account == null) return false;
        
        long before = account.getBalance();
        account.setBalance(before + amount);
        account.addDeposit(amount);
        
        TransactionRecord record = new TransactionRecord(
            generateTransactionId(),
            playerId,
            TransactionRecord.TransactionType.DEPOSIT,
            amount,
            before,
            account.getBalance(),
            reason,
            null
        );
        account.addTransaction(record);
        
        return true;
    }
    
    @Override
    public boolean withdraw(UUID playerId, long amount, String reason) {
        if (amount <= 0) return false;
        
        BankAccount account = plugin.getAccount(playerId);
        if (account == null) return false;
        
        if (account.getBalance() < amount) return false;
        
        long before = account.getBalance();
        account.setBalance(before - amount);
        account.addWithdrawal(amount);
        
        TransactionRecord record = new TransactionRecord(
            generateTransactionId(),
            playerId,
            TransactionRecord.TransactionType.WITHDRAW,
            amount,
            before,
            account.getBalance(),
            reason,
            null
        );
        account.addTransaction(record);
        
        return true;
    }
    
    @Override
    public boolean transfer(UUID from, UUID to, long amount, String reason) {
        if (amount <= 0) return false;
        if (from.equals(to)) return false;
        
        BankAccount fromAccount = plugin.getAccount(from);
        BankAccount toAccount = plugin.getAccount(to);
        
        if (fromAccount == null || toAccount == null) return false;
        if (fromAccount.getBalance() < amount) return false;
        
        long fromBefore = fromAccount.getBalance();
        long toBefore = toAccount.getBalance();
        
        fromAccount.setBalance(fromBefore - amount);
        toAccount.setBalance(toBefore + amount);
        
        TransactionRecord fromRecord = new TransactionRecord(
            generateTransactionId(),
            from,
            TransactionRecord.TransactionType.TRANSFER_OUT,
            amount,
            fromBefore,
            fromAccount.getBalance(),
            reason,
            to
        );
        fromAccount.addTransaction(fromRecord);
        
        TransactionRecord toRecord = new TransactionRecord(
            generateTransactionId(),
            to,
            TransactionRecord.TransactionType.TRANSFER_IN,
            amount,
            toBefore,
            toAccount.getBalance(),
            reason,
            from
        );
        toAccount.addTransaction(toRecord);
        
        return true;
    }
    
    @Override
    public boolean hasBalance(UUID playerId, long amount) {
        return getBalance(playerId) >= amount;
    }
    
    @Override
    public Optional<Loan> applyForLoan(UUID playerId, long amount, int durationDays) {
        if (!canApplyForLoan(playerId, amount)) {
            return Optional.empty();
        }
        
        return plugin.getLoanManager().createLoan(playerId, amount, durationDays);
    }
    
    @Override
    public boolean repayLoan(UUID playerId, String loanId, long amount) {
        return plugin.getLoanManager().repayLoan(playerId, loanId, amount);
    }
    
    @Override
    public List<Loan> getActiveLoans(UUID playerId) {
        BankAccount account = plugin.getAccount(playerId);
        return account != null ? new ArrayList<>(account.getActiveLoans()) : new ArrayList<>();
    }
    
    @Override
    public Optional<Loan> getLoan(String loanId) {
        return plugin.getLoanManager().getLoan(loanId);
    }
    
    @Override
    public boolean canApplyForLoan(UUID playerId, long amount) {
        BankAccount account = plugin.getAccount(playerId);
        if (account == null) return false;
        
        if (account.getCreditScore() < plugin.getMinCreditScoreForLoan()) return false;
        if (amount > plugin.getMaxLoanAmount()) return false;
        if (account.getTotalLoanAmount() + amount > plugin.getMaxLoanAmount()) return false;
        
        return true;
    }
    
    @Override
    public long getMaxLoanAmount(UUID playerId) {
        BankAccount account = plugin.getAccount(playerId);
        if (account == null) return 0;
        
        if (account.getCreditScore() < plugin.getMinCreditScoreForLoan()) return 0;
        
        long available = plugin.getMaxLoanAmount() - account.getTotalLoanAmount();
        return Math.max(0, available);
    }
    
    @Override
    public long calculateAndPayInterest(UUID playerId) {
        return plugin.getInterestManager().processInterest(playerId);
    }
    
    @Override
    public double getDepositInterestRate() {
        return plugin.getDepositInterestRate();
    }
    
    @Override
    public double getLoanInterestRate() {
        return plugin.getLoanInterestRate();
    }
    
    @Override
    public void setDepositInterestRate(double rate) {
        plugin.getConfig().set("settings.deposit-interest-rate", rate);
        plugin.saveConfig();
    }
    
    @Override
    public void setLoanInterestRate(double rate) {
        plugin.getConfig().set("settings.loan-interest-rate", rate);
        plugin.saveConfig();
    }
    
    @Override
    public List<TransactionRecord> getTransactionHistory(UUID playerId, int limit) {
        BankAccount account = plugin.getAccount(playerId);
        if (account == null) return new ArrayList<>();
        
        List<TransactionRecord> history = account.getTransactionHistory();
        int start = Math.max(0, history.size() - limit);
        return new ArrayList<>(history.subList(start, history.size()));
    }
    
    @Override
    public List<TransactionRecord> getAllTransactions(UUID playerId) {
        BankAccount account = plugin.getAccount(playerId);
        return account != null ? new ArrayList<>(account.getTransactionHistory()) : new ArrayList<>();
    }
    
    @Override
    public void adminSetBalance(UUID playerId, long amount, UUID admin, String reason) {
        BankAccount account = plugin.getAccount(playerId);
        if (account == null) return;
        
        long before = account.getBalance();
        account.setBalance(amount);
        
        TransactionRecord record = new TransactionRecord(
            generateTransactionId(),
            playerId,
            TransactionRecord.TransactionType.ADMIN_SET,
            amount,
            before,
            amount,
            "Admin: " + (admin != null ? admin.toString() : "Console") + " - " + reason,
            admin
        );
        account.addTransaction(record);
    }
    
    @Override
    public void adminGive(UUID playerId, long amount, UUID admin, String reason) {
        BankAccount account = plugin.getAccount(playerId);
        if (account == null) return;
        
        long before = account.getBalance();
        account.setBalance(before + amount);
        
        TransactionRecord record = new TransactionRecord(
            generateTransactionId(),
            playerId,
            TransactionRecord.TransactionType.ADMIN_GIVE,
            amount,
            before,
            account.getBalance(),
            "Admin: " + (admin != null ? admin.toString() : "Console") + " - " + reason,
            admin
        );
        account.addTransaction(record);
    }
    
    @Override
    public boolean adminTake(UUID playerId, long amount, UUID admin, String reason) {
        BankAccount account = plugin.getAccount(playerId);
        if (account == null) return false;
        
        if (account.getBalance() < amount) return false;
        
        long before = account.getBalance();
        account.setBalance(before - amount);
        
        TransactionRecord record = new TransactionRecord(
            generateTransactionId(),
            playerId,
            TransactionRecord.TransactionType.ADMIN_TAKE,
            amount,
            before,
            account.getBalance(),
            "Admin: " + (admin != null ? admin.toString() : "Console") + " - " + reason,
            admin
        );
        account.addTransaction(record);
        
        return true;
    }
    
    @Override
    public void resetAccount(UUID playerId, String reason) {
        BankAccount account = plugin.getAccount(playerId);
        if (account == null) return;
        
        account.setBalance(plugin.getDefaultBalance());
        account.setCreditScore(100);
        account.getActiveLoans().clear();
        account.getTransactionHistory().clear();
    }
    
    @Override
    public CompletableFuture<Long> getBalanceAsync(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> getBalance(playerId));
    }
    
    @Override
    public CompletableFuture<Boolean> depositAsync(UUID playerId, long amount, String reason) {
        return CompletableFuture.supplyAsync(() -> deposit(playerId, amount, reason));
    }
    
    @Override
    public CompletableFuture<Boolean> withdrawAsync(UUID playerId, long amount, String reason) {
        return CompletableFuture.supplyAsync(() -> withdraw(playerId, amount, reason));
    }
    
    @Override
    public CompletableFuture<Boolean> transferAsync(UUID from, UUID to, long amount, String reason) {
        return CompletableFuture.supplyAsync(() -> transfer(from, to, amount, reason));
    }
    
    @Override
    public long getTotalDeposits() {
        return plugin.getAccounts().values().stream()
            .mapToLong(BankAccount::getBalance)
            .sum();
    }
    
    @Override
    public long getTotalLoans() {
        return plugin.getAccounts().values().stream()
            .mapToLong(BankAccount::getTotalLoanAmount)
            .sum();
    }
    
    @Override
    public int getAccountCount() {
        return plugin.getAccounts().size();
    }
    
    @Override
    public long getDefaultBalance() {
        return plugin.getDefaultBalance();
    }
    
    public void unregister() {
        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.unregisterService(BankService.class);
                plugin.getLogger().info("已从 RPGCore 服务注册表注销: BankService");
            } catch (Exception e) {
                plugin.getLogger().warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }
    
    public boolean isUsingRPGCore() {
        return useRPGCore;
    }
    
    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);
    }
}
