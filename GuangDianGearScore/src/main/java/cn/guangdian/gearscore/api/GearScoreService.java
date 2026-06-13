package cn.guangdian.gearscore.api;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface GearScoreService {
    
    long getPlayerScore(UUID uuid);
    
    long getPlayerScore(Player player);
    
    int getPlayerRank(UUID uuid);
    
    List<Map.Entry<UUID, Long>> getTopPlayers(int count);
    
    String getTopPlayerName(int index);
    
    long getTopPlayerScore(int index);
    
    void updatePlayerScore(Player player);
}
