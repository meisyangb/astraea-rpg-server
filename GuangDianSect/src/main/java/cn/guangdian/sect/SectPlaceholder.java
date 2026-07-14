package cn.guangdian.sect;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI扩展
 * 
 * 支持的占位符:
 * - %gdsect_name% 或 %gdsect_宗门% - 玩家门派名称
 * - %gdsect_rank% 或 %gdsect_职位% - 玩家门派职位
 * - %gdsect_type% - 门派类型 (正道/魔道等)
 * - %gdsect_element% - 门派修炼属性
 * - %gdsect_contribution% 或 %gdsect_贡献% - 门派贡献值
 * - %gdsect_in_sect% - 是否已加入门派
 */
public class SectPlaceholder extends PlaceholderExpansion {
    private final GuangDianSect plugin;
    
    public SectPlaceholder(GuangDianSect plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "gdsect";
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
        
        SectPlayer data = plugin.getPlayerData(player);
        Sect sect = plugin.getPlayerSect(player);
        SectRank rank = plugin.getPlayerRank(player);
        
        String param = params.toLowerCase();
        
        // 门派名称
        if (param.equals("name") || param.equals("宗门") || param.equals("门派") || param.equals("sect")) {
            return sect != null ? sect.getName() : "无门派";
        }
        
        // 门派职位
        if (param.equals("rank") || param.equals("职位") || param.equals("等级")) {
            return rank != null ? rank.getName() : "无";
        }
        
        // 门派类型
        if (param.equals("type") || param.equals("类型")) {
            return sect != null ? sect.getType() : "无";
        }
        
        // 门派修炼属性
        if (param.equals("element") || param.equals("属性") || param.equals("修炼属性")) {
            return sect != null ? sect.getElement() : "无";
        }
        
        // 门派贡献值
        if (param.equals("contribution") || param.equals("贡献") || param.equals("贡献值")) {
            return data != null ? String.valueOf(data.getContribution()) : "0";
        }
        
        // 是否已加入门派
        if (param.equals("in_sect") || param.equals("是否有门派") || param.equals("已加入")) {
            return plugin.isInSect(player) ? "true" : "false";
        }
        
        // 门派颜色
        if (param.equals("color") || param.equals("颜色")) {
            return sect != null ? sect.getColor() : "<white>";
        }
        
        // 门派ID
        if (param.equals("id") || param.equals("sectid")) {
            return sect != null ? sect.getId() : "";
        }
        
        return null;
    }
}