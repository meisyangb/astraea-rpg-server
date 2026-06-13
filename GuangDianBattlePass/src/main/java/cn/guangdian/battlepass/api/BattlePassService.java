package cn.guangdian.battlepass.api;

import java.util.UUID;

public interface BattlePassService {
    
    int getPlayerLevel(UUID playerId);
    
    int getPlayerExp(UUID playerId);
    
    boolean isPremium(UUID playerId);
    
    void addExp(UUID playerId, int exp);
    
    boolean purchasePremium(UUID playerId);
    
    int getProgress(UUID playerId);
}
