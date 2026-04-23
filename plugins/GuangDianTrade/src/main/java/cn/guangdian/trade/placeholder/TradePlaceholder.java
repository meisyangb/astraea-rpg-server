package cn.guangdian.trade.placeholder;

import cn.guangdian.trade.GuangDianTrade;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 交易占位符扩展
 *
 * <p>提供交易相关的 PlaceholderAPI 占位符。</p>
 *
 * <h3>可用占位符：</h3>
 * <ul>
 *   <li>%gdtrade_in_trade% - 玩家是否在交易中 (true/false)</li>
 *   <li>%gdtrade_partner% - 交易伙伴名称 (无则返回空)</li>
 *   <li>%gdtrade_partner_status% - 交易伙伴状态 (在线/离线)</li>
 *   <li>%gdtrade_pending% - 玩家待处理的交易请求数</li>
 *   <li>%gdtrade_active% - 服务器当前活跃交易数</li>
 *   <li>%gdtrade_history% - 玩家历史交易次数 (配置项)</li>
 * </ul>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class TradePlaceholder extends PlaceholderExpansion {

    private final GuangDianTrade plugin;

    public TradePlaceholder(GuangDianTrade plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "gdtrade";
    }

    @Override
    public @NotNull String getAuthor() {
        return "GuangDian";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String param = params.toLowerCase();

        // 不需要玩家对象的占位符
        switch (param) {
            case "active":
            case "活跃交易":
            case "当前交易数":
                // 服务器当前活跃交易数
                return String.valueOf(plugin.getActiveTradeCountAPI());

            case "total_trades":
            case "总交易":
                // 历史总交易次数 (从配置读取)
                return String.valueOf(plugin.getConfig().getLong("stats.total-trades", 0));
        }

        // 需要玩家对象的占位符
        if (player == null) return "";

        UUID playerId = player.getUniqueId();

        switch (param) {
            case "in_trade":
            case "是否交易中":
                // 玩家是否在交易中
                return plugin.isInTradeAPI(playerId) ? "true" : "false";

            case "partner":
            case "交易伙伴":
                // 交易伙伴名称
                UUID partnerId = plugin.getTradePartnerAPI(playerId);
                if (partnerId == null) return "";
                OfflinePlayer partner = Bukkit.getOfflinePlayer(partnerId);
                return partner.getName() != null ? partner.getName() : "<red>未知";

            case "partner_status":
            case "伙伴状态":
                // 交易伙伴状态
                UUID partnerId2 = plugin.getTradePartnerAPI(playerId);
                if (partnerId2 == null) return "<gray>无";
                OfflinePlayer partner2 = Bukkit.getOfflinePlayer(partnerId2);
                return partner2.isOnline() ? "<green>在线" : "<red>离线";

            case "pending":
            case "待处理":
                // 待处理的交易请求数
                return String.valueOf(plugin.getPendingRequestCountAPI(playerId));

            case "history":
            case "历史交易":
                // 玩家历史交易次数
                return String.valueOf(plugin.getConfig().getLong("stats.player." + playerId + ".trades", 0));

            default:
                return null;
        }
    }
}