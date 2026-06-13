package cn.guangdian.market.adapter;

import cn.guangdian.market.GuangDianMarket;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.event.events.PointsTransactionEvent;
import cn.guangdian.rpgcore.service.api.MarketService;
import cn.guangdian.rpgcore.util.OfflinePlayerCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 市场服务适配器
 *
 * <p>连接 GuangDianMarket 实现与 MarketService 接口。</p>
 *
 * <p>集成 RPGCore EventBus，订阅点券交易事件进行统计。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class MarketServiceAdapter implements MarketService {

    private final GuangDianMarket plugin;
    private final boolean useRPGCore;
    private EventBus eventBus;
    private Logger logger;
    private long totalSalesVolume = 0;

    public MarketServiceAdapter(GuangDianMarket plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        this.logger = plugin.getLogger();

        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                this.eventBus = rpgCore.getEventBus();

                // 注册服务
                registry.registerService(MarketService.class, this);
                logger.info("已注册到 RPGCore: MarketService");

                // 订阅点券交易事件
                subscribeToEvents();

            } catch (Exception e) {
                logger.warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    /**
     * 订阅 RPGCore 事件
     *
     * <p>订阅点券交易事件，用于统计市场交易量。</p>
     */
    private void subscribeToEvents() {
        if (eventBus == null) {
            return;
        }

        // 订阅点券交易事件
        eventBus.subscribe(PointsTransactionEvent.class, event -> {
            // 记录与市场相关的交易
            String reason = event.getReason();
            if (reason != null && reason.contains("市场")) {
                // 市场交易统计
                if (event.isWithdraw()) {
                    totalSalesVolume += event.getAmount();
                    // 更新配置中的统计
                    plugin.getConfig().set("stats.total-sales-volume", totalSalesVolume);
                    plugin.getConfig().set("stats.total-transactions",
                        plugin.getConfig().getLong("stats.total-transactions", 0) + 1);
                }
            }
        });

        logger.info("已订阅 PointsTransactionEvent");
    }

    @Override
    public boolean listItem(UUID sellerId, ItemStack item, long price) {
        // 使用公开API方法，不再使用反射
        return plugin.listItemAPI(sellerId, item, price);
    }

    @Override
    public boolean purchaseItem(UUID buyerId, String listingId) {
        // 解析上架ID
        try {
            UUID listingUUID = UUID.fromString(listingId);
            // 使用公开API方法购买物品
            return plugin.purchaseItemAPI(buyerId, listingUUID);
        } catch (IllegalArgumentException ignored) {
            // 无效的UUID格式
        }
        return false;
    }

    @Override
    public boolean cancelListing(UUID sellerId, String listingId) {
        try {
            UUID listingUUID = UUID.fromString(listingId);
            return plugin.cancelListingAPI(sellerId, listingUUID);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public List<Object> getPlayerListings(UUID sellerId) {
        // 使用公开API方法
        List<?> items = plugin.getPlayerListingsAPI(sellerId);
        List<Object> result = new ArrayList<>();
        if (items != null) result.addAll(items);
        return result;
    }

    @Override
    public int getMarketSize() {
        // 使用公开API方法
        return plugin.getMarketSizeAPI();
    }

    @Override
    public int getPlayerListingCount(UUID sellerId) {
        // 使用公开API方法
        return plugin.getPlayerListingCountAPI(sellerId);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * 获取总销售额
     *
     * @return 总销售额
     */
    public long getTotalSalesVolume() {
        return totalSalesVolume;
    }

    public void unregister() {
        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.unregisterService(MarketService.class);
                logger.info("已从 RPGCore 注销: MarketService");
            } catch (Exception e) {
                logger.warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }

    public boolean isUsingRPGCore() {
        return useRPGCore;
    }
}