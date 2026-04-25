package cn.guangdian.market.placeholder;

import cn.guangdian.market.GuangDianMarket;
import cn.guangdian.rpgcore.integration.PlaceholderService;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class MarketPlaceholder {

    private final GuangDianMarket plugin;

    public MarketPlaceholder(GuangDianMarket plugin) {
        this.plugin = plugin;
    }

    public void register() {
        PlaceholderService service = PlaceholderService.getInstance();
        if (service == null) return;
        
        service.register("gdmarket", (player, params) -> {
            String param = params.toLowerCase();

            switch (param) {
                case "listings":
                case "市场物品数":
                case "物品总数":
                    return String.valueOf(plugin.getMarketSizeAPI());

                case "my_listings":
                case "我的上架":
                case "上架数量":
                    if (player == null) return "0";
                    return String.valueOf(plugin.getPlayerListingCountAPI(player.getUniqueId()));

                case "pending":
                case "待领取":
                    if (player == null) return "0";
                    return String.valueOf(plugin.getPendingReturnsCount(player.getUniqueId()));

                case "max_listings":
                case "最大上架":
                    return String.valueOf(plugin.getMaxListingsPerPlayer());

                case "fee":
                case "手续费":
                    return String.valueOf((long) plugin.getFeePercent()) + "%";

                case "total_sales":
                case "总交易":
                    return String.valueOf(plugin.getConfig().getLong("stats.total-sales", 0));

                default:
                    if (param.startsWith("top_price_")) {
                        try {
                            int index = Integer.parseInt(param.substring(10)) - 1;
                            if (index >= 0) {
                                return formatPrice(plugin.getTopPrice(index));
                            }
                        } catch (NumberFormatException ignored) {}
                        return "0";
                    }

                    if (param.startsWith("top_item_")) {
                        try {
                            int index = Integer.parseInt(param.substring(9)) - 1;
                            if (index >= 0) {
                                return plugin.getTopItemName(index);
                            }
                        } catch (NumberFormatException ignored) {}
                        return "-";
                    }

                    return null;
            }
        });
    }

    private String formatPrice(long price) {
        if (price >= 100000000) {
            return String.format("%.2f亿", price / 100000000.0);
        } else if (price >= 10000) {
            return String.format("%.1f万", price / 10000.0);
        }
        return String.format("%,d", price);
    }

    public void unregister() {
    }
}
