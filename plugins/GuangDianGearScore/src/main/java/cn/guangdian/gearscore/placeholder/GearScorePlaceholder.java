package cn.guangdian.gearscore.placeholder;

import cn.guangdian.gearscore.GuangDianGearScore;
import cn.guangdian.rpgcore.integration.PlaceholderService;
import org.bukkit.OfflinePlayer;

public class GearScorePlaceholder {

    private final GuangDianGearScore plugin;

    public GearScorePlaceholder(GuangDianGearScore plugin) {
        this.plugin = plugin;
    }

    public void register() {
        PlaceholderService service = PlaceholderService.getInstance();
        if (service == null) return;
        
        service.register("gdgs", (player, params) -> {
            if (player == null) return "";
            
            String lowerParams = params.toLowerCase();
            
            if (lowerParams.equals("score")) {
                return String.valueOf(plugin.getPlayerScore(player.getUniqueId()));
            }
            
            if (lowerParams.equals("score_formatted")) {
                return formatNumber(plugin.getPlayerScore(player.getUniqueId()));
            }
            
            if (lowerParams.equals("rank")) {
                int rank = plugin.getPlayerRank(player.getUniqueId());
                return rank > 0 ? String.valueOf(rank) : "-";
            }
            
            if (lowerParams.equals("rank_formatted")) {
                int rank = plugin.getPlayerRank(player.getUniqueId());
                return formatRank(rank);
            }
            
            if (lowerParams.startsWith("top_")) {
                try {
                    String remainder = lowerParams.substring(4);
                    
                    if (remainder.endsWith("_name")) {
                        int index = Integer.parseInt(remainder.substring(0, remainder.length() - 5)) - 1;
                        if (index >= 0 && index < 10) {
                            return plugin.getTopPlayerName(index);
                        }
                    } else if (remainder.endsWith("_score")) {
                        int index = Integer.parseInt(remainder.substring(0, remainder.length() - 6)) - 1;
                        if (index >= 0 && index < 10) {
                            return formatNumber(plugin.getTopPlayerScore(index));
                        }
                    } else {
                        int index = Integer.parseInt(remainder) - 1;
                        if (index >= 0 && index < 10) {
                            return plugin.getTopPlayerName(index);
                        }
                    }
                } catch (NumberFormatException e) {
                    return "";
                }
            }
            
            return "";
        });
    }

    public void unregister() {
    }

    private String formatNumber(long num) {
        if (num >= 100000000) {
            return String.format("%.2f亿", num / 100000000.0);
        } else if (num >= 10000) {
            return String.format("%.2f万", num / 10000.0);
        }
        return String.format("%,d", num);
    }

    private String formatRank(int rank) {
        if (rank == 1) return "<gold>🥇第1名";
        if (rank == 2) return "<gray>🥈第2名";
        if (rank == 3) return "<red>🥉第3名";
        if (rank > 0) return "<yellow>第" + rank + "名";
        return "<gray>-";
    }
}
