package cn.guangdian.trade.placeholder;

import cn.guangdian.rpgcore.integration.PlaceholderService;
import cn.guangdian.trade.GuangDianTrade;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class TradePlaceholder {

    private final GuangDianTrade plugin;

    public TradePlaceholder(GuangDianTrade plugin) {
        this.plugin = plugin;
    }

    public void register() {
        PlaceholderService service = PlaceholderService.getInstance();
        if (service == null) return;
        
        service.register("gdtrade", (player, params) -> {
            String param = params.toLowerCase();

            switch (param) {
                case "active":
                case "活跃交易":
                case "当前交易数":
                    return String.valueOf(plugin.getActiveTradeCountAPI());

                case "total_trades":
                case "总交易":
                    return String.valueOf(plugin.getConfig().getLong("stats.total-trades", 0));
            }

            if (player == null) return "";

            UUID playerId = player.getUniqueId();

            switch (param) {
                case "in_trade":
                case "是否交易中":
                    return plugin.isInTradeAPI(playerId) ? "true" : "false";

                case "partner":
                case "交易伙伴":
                    UUID partnerId = plugin.getTradePartnerAPI(playerId);
                    if (partnerId == null) return "";
                    OfflinePlayer partner = Bukkit.getOfflinePlayer(partnerId);
                    return partner.getName() != null ? partner.getName() : "<red>未知";

                case "partner_status":
                case "伙伴状态":
                    UUID partnerId2 = plugin.getTradePartnerAPI(playerId);
                    if (partnerId2 == null) return "<gray>无";
                    OfflinePlayer partner2 = Bukkit.getOfflinePlayer(partnerId2);
                    return partner2.isOnline() ? "<green>在线" : "<red>离线";

                case "pending":
                case "待处理":
                    return String.valueOf(plugin.getPendingRequestCountAPI(playerId));

                case "history":
                case "历史交易":
                    return String.valueOf(plugin.getConfig().getLong("stats.player." + playerId + ".trades", 0));

                default:
                    return null;
            }
        });
    }

    public void unregister() {
    }
}
