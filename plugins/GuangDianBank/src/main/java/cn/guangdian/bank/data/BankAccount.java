package cn.guangdian.bank.data;

import cn.guangdian.rpgcore.service.data.Loan;
import cn.guangdian.rpgcore.service.data.TransactionRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BankAccount {
    
    private final UUID playerId;
    private long balance;
    private long totalDeposited;
    private long totalWithdrawn;
    private int creditScore;
    private long lastInterestTime;
    private final List<Loan> activeLoans;
    private final List<TransactionRecord> transactionHistory;
    
    public BankAccount(UUID playerId) {
        this.playerId = playerId;
        this.balance = 0;
        this.totalDeposited = 0;
        this.totalWithdrawn = 0;
        this.creditScore = 100;
        this.lastInterestTime = System.currentTimeMillis();
        this.activeLoans = new ArrayList<>();
        this.transactionHistory = new ArrayList<>();
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public long getBalance() {
        return balance;
    }
    
    public void setBalance(long balance) {
        this.balance = Math.max(0, balance);
    }
    
    public long getTotalDeposited() {
        return totalDeposited;
    }
    
    public void addDeposit(long amount) {
        this.totalDeposited += amount;
    }
    
    public long getTotalWithdrawn() {
        return totalWithdrawn;
    }
    
    public void addWithdrawal(long amount) {
        this.totalWithdrawn += amount;
    }
    
    public int getCreditScore() {
        return creditScore;
    }
    
    public void setCreditScore(int creditScore) {
        this.creditScore = Math.max(0, Math.min(100, creditScore));
    }
    
    public void addCreditScore(int amount) {
        setCreditScore(this.creditScore + amount);
    }
    
    public long getLastInterestTime() {
        return lastInterestTime;
    }
    
    public void setLastInterestTime(long lastInterestTime) {
        this.lastInterestTime = lastInterestTime;
    }
    
    public List<Loan> getActiveLoans() {
        return activeLoans;
    }
    
    public void addLoan(Loan loan) {
        activeLoans.add(loan);
    }
    
    public void removeLoan(Loan loan) {
        activeLoans.remove(loan);
    }
    
    public long getTotalLoanAmount() {
        return activeLoans.stream()
            .mapToLong(Loan::getCurrentAmount)
            .sum();
    }
    
    public List<TransactionRecord> getTransactionHistory() {
        return transactionHistory;
    }
    
    public void addTransaction(TransactionRecord record) {
        transactionHistory.add(record);
        if (transactionHistory.size() > 100) {
            transactionHistory.remove(0);
        }
    }
}
