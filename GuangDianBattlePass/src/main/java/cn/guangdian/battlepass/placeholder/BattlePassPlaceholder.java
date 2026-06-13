package cn.guangdian.battlepass.placeholder;

import cn.guangdian.battlepass.GuangDianBattlePass;
import cn.guangdian.battlepass.model.PlayerBattlePass;
import cn.guangdian.battlepass.model.Season;

import java.util.UUID;

public class BattlePassPlaceholder {
    
    private final GuangDianBattlePass plugin;
    
    public BattlePassPlaceholder(GuangDianBattlePass plugin) {
        this.plugin = plugin;
    }
    
    public String onRequest(UUID playerId, String params) {
        if (playerId == null) return "";
        
        Season season = plugin.getSeasonManager().getCurrentSeason();
        if (season == null) return "无赛季";
        
        PlayerBattlePass bp = plugin.getBattlePassManager().getPlayerBattlePass(playerId);
        if (bp == null) return "无数据";
        
        switch (params.toLowerCase()) {
            case "level":
                return String.valueOf(bp.getLevel());
            case "exp":
                return String.valueOf(bp.getCurrentExp());
            case "total_exp":
                return String.valueOf(bp.getTotalExp());
            case "max_level":
                return String.valueOf(season.getMaxLevel());
            case "is_premium":
                return bp.isPremium() ? "是" : "否";
            case "progress":
                return String.valueOf(plugin.getBattlePassManager().getProgress(playerId));
            case "season_name":
                return season.getSeasonName();
            case "season_id":
                return String.valueOf(season.getSeasonId());
            case "remaining_days":
                return String.valueOf(season.getRemainingDays());
            case "unclaimed_free":
                return String.valueOf(bp.getUnclaimedFreeRewards(season.getMaxLevel()));
            case "unclaimed_premium":
                return String.valueOf(bp.getUnclaimedPremiumRewards(season.getMaxLevel()));
            default:
                return null;
        }
    }
}
