package cn.guangdian.market.placeholder;

import cn.guangdian.market.GuangDianMarket;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 市场占位符扩展
 *
 * <p>提供市场相关的 PlaceholderAPI 占位符。</p>
 *
 * <h3>可用占位符：</h3>
 * <ul>
 *   <li>%gdmarket_listings% - 市场物品总数</li>
 *   <li>%gdmarket_my_listings% - 玩家上架物品数量</li>
 *   <li>%gdmarket_pending% - 玩家待领取的过期物品数量</li>
 *   <li>%gdmarket_total_sales% - 市场总交易次数 (配置项)</li>
 * </ul>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class MarketPlaceholder extends PlaceholderExpansion {

    private final GuangDianMarket plugin;

    public MarketPlaceholder(GuangDianMarket plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "gdmarket";
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

        switch (param) {
            case "listings":
            case "市场物品数":
            case "物品总数":
                // 市场物品总数
                return String.valueOf(plugin.getMarketSizeAPI());

            case "my_listings":
            case "我的上架":
            case "上架数量":
                // 玩家上架物品数量
                if (player == null) return "0";
                return String.valueOf(plugin.getPlayerListingCountAPI(player.getUniqueId()));

            case "pending":
            case "待领取":
                // 玩家待领取的过期物品数量 - 通过内部Map获取
                if (player == null) return "0";
                // 使用公开方法获取
                return String.valueOf(plugin.getPendingReturnsCount(player.getUniqueId()));

            case "max_listings":
            case "最大上架":
                // 玩家最大上架数量 (配置)
                return String.valueOf(plugin.getMaxListingsPerPlayer());

            case "fee":
            case "手续费":
                // 手续费百分比
                return String.valueOf((long) plugin.getFeePercent()) + "%";

            case "total_sales":
            case "总交易":
                // 历史总交易次数 (从配置读取)
                return String.valueOf(plugin.getConfig().getLong("stats.total-sales", 0));

            default:
                // 尝试解析带参数的占位符
                if (param.startsWith("top_price_")) {
                    // %gdmarket_top_price_1% - 市场最贵物品价格
                    try {
                        int index = Integer.parseInt(param.substring(10)) - 1;
                        if (index >= 0) {
                            return formatPrice(plugin.getTopPrice(index));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                    return "0";
                }

                if (param.startsWith("top_item_")) {
                    // %gdmarket_top_item_1% - 市场最贵物品名称
                    try {
                        int index = Integer.parseInt(param.substring(9)) - 1;
                        if (index >= 0) {
                            return plugin.getTopItemName(index);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                    return "-";
                }

                return null;
        }
    }

    /**
     * 格式化价格显示
     */
    private String formatPrice(long price) {
        if (price >= 100000000) {
            return String.format("%.2f亿", price / 100000000.0);
        } else if (price >= 10000) {
            return String.format("%.1f万", price / 10000.0);
        }
        return String.format("%,d", price);
    }
}