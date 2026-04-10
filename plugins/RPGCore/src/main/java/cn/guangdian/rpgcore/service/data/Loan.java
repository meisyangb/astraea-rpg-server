package cn.guangdian.rpgcore.service.data;

import java.util.UUID;

public class Loan {
    
    private final String loanId;
    private final UUID playerId;
    private final long principalAmount;
    private long currentAmount;
    private final double interestRate;
    private final long startTime;
    private final long dueTime;
    private LoanStatus status;
    private int overdueDays;
    
    public Loan(String loanId, UUID playerId, long principalAmount, 
                double interestRate, long durationDays) {
        this.loanId = loanId;
        this.playerId = playerId;
        this.principalAmount = principalAmount;
        this.currentAmount = principalAmount;
        this.interestRate = interestRate;
        this.startTime = System.currentTimeMillis();
        this.dueTime = startTime + (durationDays * 24 * 60 * 60 * 1000L);
        this.status = LoanStatus.ACTIVE;
        this.overdueDays = 0;
    }
    
    public String getLoanId() {
        return loanId;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public long getPrincipalAmount() {
        return principalAmount;
    }
    
    public long getCurrentAmount() {
        return currentAmount;
    }
    
    public void setCurrentAmount(long currentAmount) {
        this.currentAmount = Math.max(0, currentAmount);
    }
    
    public double getInterestRate() {
        return interestRate;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getDueTime() {
        return dueTime;
    }
    
    public LoanStatus getStatus() {
        return status;
    }
    
    public void setStatus(LoanStatus status) {
        this.status = status;
    }
    
    public int getOverdueDays() {
        return overdueDays;
    }
    
    public void setOverdueDays(int overdueDays) {
        this.overdueDays = overdueDays;
    }
    
    public boolean isOverdue() {
        return System.currentTimeMillis() > dueTime;
    }
    
    public long getRemainingTime() {
        return Math.max(0, dueTime - System.currentTimeMillis());
    }
    
    public double calculateInterest() {
        long days = (System.currentTimeMillis() - startTime) / (24 * 60 * 60 * 1000L);
        return currentAmount * interestRate * days / 100.0;
    }
    
    public long getTotalRepayment() {
        return currentAmount + (long) calculateInterest();
    }
    
    public enum LoanStatus {
        ACTIVE,
        OVERDUE,
        PAID,
        DEFAULTED
    }
}
