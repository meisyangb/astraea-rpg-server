package cn.guangdian.signin.api;

import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SignInService {
    
    boolean canSignIn(UUID playerId);
    
    boolean signIn(UUID playerId);
    
    int getConsecutiveDays(UUID playerId);
    
    int getTotalDays(UUID playerId);
    
    LocalDate getLastSignInDate(UUID playerId);
    
    List<SignInRecord> getSignInHistory(UUID playerId, int limit);
    
    void resetConsecutiveDays(UUID playerId);
    
    void giveReward(Player player, int consecutiveDays);
    
    class SignInRecord {
        private final LocalDate date;
        private final int consecutiveDays;
        
        public SignInRecord(LocalDate date, int consecutiveDays) {
            this.date = date;
            this.consecutiveDays = consecutiveDays;
        }
        
        public LocalDate getDate() {
            return date;
        }
        
        public int getConsecutiveDays() {
            return consecutiveDays;
        }
    }
}
