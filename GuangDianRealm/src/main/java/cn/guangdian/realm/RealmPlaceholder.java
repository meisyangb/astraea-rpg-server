package cn.guangdian.realm;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI扩展
 * 
 * 支持的占位符:
 * - %gdrealm_realm% 或 %gdrealm_境界% - 玩家当前境界
 * - %gdrealm_cultivation% 或 %gdrealm_修为% - 玩家当前修为
 * - %gdrealm_next_realm% - 下一境界名称
 * - %gdrealm_required% - 突破所需修为
 * - %gdrealm_progress% - 突破进度百分比
 */
public class RealmPlaceholder extends PlaceholderExpansion {
    private final GuangDianRealm plugin;
    
    public RealmPlaceholder(GuangDianRealm plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "gdrealm";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "GuangDian";
    }
    
    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) {
            return "";
        }
        
        Player player = offlinePlayer.getPlayer();
        if (player == null) {
            return "";
        }
        
        CultivationPlayer data = plugin.getPlayerData(player);
        Realm currentRealm = plugin.getCurrentRealm(player);
        Realm nextRealm = plugin.getNextRealm(player);
        
        String param = params.toLowerCase();
        
        // 境界名称
        if (param.equals("realm") || param.equals("境界") || param.equals("境界名")) {
            return currentRealm != null ? currentRealm.getName() : "凡人";
        }
        
        // 当前修为
        if (param.equals("cultivation") || param.equals("修为") || param.equals("当前修为")) {
            return String.valueOf(data != null ? data.getCultivation() : 0);
        }
        
        // 累计修为
        if (param.equals("total_cultivation") || param.equals("累计修为")) {
            return String.valueOf(data != null ? data.getTotalGained() : 0);
        }
        
        // 下一境界
        if (param.equals("next_realm") || param.equals("下一境界")) {
            return nextRealm != null ? nextRealm.getName() : "已达最高";
        }
        
        // 所需修为
        if (param.equals("required") || param.equals("所需修为")) {
            return nextRealm != null ? String.valueOf(nextRealm.getRequiredCultivation()) : "0";
        }
        
        // 进度百分比
        if (param.equals("progress") || param.equals("进度") || param.equals("突破进度")) {
            if (nextRealm == null) return "100%";
            long current = data != null ? data.getCultivation() : 0;
            long required = nextRealm.getRequiredCultivation();
            int percent = (int) Math.min(100, current * 100 / required);
            return percent + "%";
        }
        
        // 境界类型
        if (param.equals("realm_type") || param.equals("境界类型")) {
            return currentRealm != null ? currentRealm.getRealmTypeName() : "凡人";
        }
        
        // 境界等级
        if (param.equals("realm_stage") || param.equals("境界等级")) {
            return currentRealm != null ? String.valueOf(currentRealm.getStage()) : "0";
        }
        
        return null;
    }
}