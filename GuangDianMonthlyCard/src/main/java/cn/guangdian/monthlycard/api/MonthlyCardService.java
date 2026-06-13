package cn.guangdian.monthlycard.api;

import cn.guangdian.monthlycard.data.MonthlyCardData;
import cn.guangdian.monthlycard.data.MonthlyCardType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonthlyCardService {
    
    Optional<MonthlyCardType> getCardType(String typeId);
    
    List<MonthlyCardType> getAllCardTypes();
    
    MonthlyCardData getPlayerData(UUID playerId);
    
    boolean hasActiveCard(UUID playerId);
    
    boolean activateCard(UUID playerId, String cardTypeId);
    
    boolean activateCard(UUID playerId, String cardTypeId, boolean charge);
    
    boolean canClaimToday(UUID playerId);
    
    boolean claimDailyReward(UUID playerId);
    
    long getRemainingDays(UUID playerId);
    
    int getTotalClaimedDays(UUID playerId);
    
    void extendCard(UUID playerId, int additionalDays);
    
    void setCard(UUID playerId, String cardTypeId, int durationDays);
    
    void removeCard(UUID playerId);
    
    void reloadConfig();
}
