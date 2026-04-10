package cn.guangdian.battlepass.manager;

import cn.guangdian.battlepass.GuangDianBattlePass;
import cn.guangdian.battlepass.model.BattlePassLevel;
import cn.guangdian.battlepass.model.PlayerBattlePass;
import cn.guangdian.battlepass.model.Season;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BattlePassManager {
    
    private final GuangDianBattlePass plugin;
    private final SeasonManager seasonManager;
    private final RewardManager rewardManager;
    private final Map<UUID, PlayerBattlePass> playerData;
    
    public BattlePassManager(GuangDianBattlePass plugin, SeasonManager seasonManager, RewardManager rewardManager) {
        this.plugin = plugin;
        this.seasonManager = seasonManager;
        this.rewardManager = rewardManager;
        this.playerData = new ConcurrentHashMap<>();
    }
    
    public PlayerBattlePass getPlayerBattlePass(UUID playerId) {
        Season currentSeason = seasonManager.getCurrentSeason();
        if (currentSeason == null) return null;
        
        PlayerBattlePass bp = playerData.get(playerId);
        if (bp == null) {
            bp = new PlayerBattlePass(playerId, currentSeason.getSeasonId());
            playerData.put(playerId, bp);
        } else if (bp.getSeasonId() != currentSeason.getSeasonId()) {
            bp = new PlayerBattlePass(playerId, currentSeason.getSeasonId());
            playerData.put(playerId, bp);
        }
        
        return bp;
    }
    
    public void addExp(UUID playerId, int exp) {
        PlayerBattlePass bp = getPlayerBattlePass(playerId);
        if (bp == null) return;
        
        Season season = seasonManager.getCurrentSeason();
        if (season == null) return;
        
        bp.addExp(exp);
        checkLevelUp(bp, season);
    }
    
    private void checkLevelUp(PlayerBattlePass bp, Season season) {
        while (bp.getLevel() < season.getMaxLevel()) {
            BattlePassLevel level = season.getLevel(bp.getLevel());
            if (level == null) break;
            
            if (bp.getCurrentExp() >= level.getRequiredExp()) {
                bp.levelUp();
                plugin.getLogger().info("玩家 " + bp.getPlayerId() + " 升级到 " + bp.getLevel());
            } else {
                break;
            }
        }
    }
    
    public boolean claimFreeReward(Player player, int level) {
        PlayerBattlePass bp = getPlayerBattlePass(player.getUniqueId());
        if (bp == null || !bp.canClaimFreeReward(level)) return false;
        
        Season season = seasonManager.getCurrentSeason();
        if (season == null) return false;
        
        BattlePassLevel bpLevel = season.getLevel(level);
        if (bpLevel == null || bpLevel.getFreeReward() == null) return false;
        
        if (rewardManager.giveReward(player, bpLevel.getFreeReward())) {
            bp.claimFreeReward(level);
            return true;
        }
        
        return false;
    }
    
    public boolean claimPremiumReward(Player player, int level) {
        PlayerBattlePass bp = getPlayerBattlePass(player.getUniqueId());
        if (bp == null || !bp.canClaimPremiumReward(level)) return false;
        
        Season season = seasonManager.getCurrentSeason();
        if (season == null) return false;
        
        BattlePassLevel bpLevel = season.getLevel(level);
        if (bpLevel == null || bpLevel.getPremiumReward() == null) return false;
        
        if (rewardManager.giveReward(player, bpLevel.getPremiumReward())) {
            bp.claimPremiumReward(level);
            return true;
        }
        
        return false;
    }
    
    public boolean purchasePremium(UUID playerId) {
        PlayerBattlePass bp = getPlayerBattlePass(playerId);
        if (bp == null || bp.isPremium()) return false;
        
        bp.setPremium(true);
        return true;
    }
    
    public void loadPlayerData(UUID playerId, PlayerBattlePass data) {
        playerData.put(playerId, data);
    }
    
    public void unloadPlayerData(UUID playerId) {
        playerData.remove(playerId);
    }
    
    public Map<UUID, PlayerBattlePass> getAllPlayerData() {
        return playerData;
    }
    
    public int getProgress(UUID playerId) {
        PlayerBattlePass bp = getPlayerBattlePass(playerId);
        if (bp == null) return 0;
        
        Season season = seasonManager.getCurrentSeason();
        if (season == null) return 0;
        
        int totalExp = bp.getTotalExp();
        int totalRequired = 0;
        for (int i = 1; i <= season.getMaxLevel(); i++) {
            BattlePassLevel level = season.getLevel(i);
            if (level != null) {
                totalRequired += level.getRequiredExp();
            }
        }
        
        if (totalRequired == 0) return 0;
        return (int) ((totalExp * 100.0) / totalRequired);
    }
}
