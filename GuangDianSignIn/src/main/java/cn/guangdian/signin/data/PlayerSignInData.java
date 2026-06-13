package cn.guangdian.signin.data;

import java.time.LocalDate;
import java.util.UUID;

public class PlayerSignInData {
    
    private final UUID playerId;
    private LocalDate lastSignInDate;
    private int consecutiveDays;
    private int totalDays;
    
    public PlayerSignInData(UUID playerId) {
        this.playerId = playerId;
        this.lastSignInDate = null;
        this.consecutiveDays = 0;
        this.totalDays = 0;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public LocalDate getLastSignInDate() {
        return lastSignInDate;
    }
    
    public void setLastSignInDate(LocalDate lastSignInDate) {
        this.lastSignInDate = lastSignInDate;
    }
    
    public int getConsecutiveDays() {
        return consecutiveDays;
    }
    
    public void setConsecutiveDays(int consecutiveDays) {
        this.consecutiveDays = consecutiveDays;
    }
    
    public int getTotalDays() {
        return totalDays;
    }
    
    public void setTotalDays(int totalDays) {
        this.totalDays = totalDays;
    }
}
