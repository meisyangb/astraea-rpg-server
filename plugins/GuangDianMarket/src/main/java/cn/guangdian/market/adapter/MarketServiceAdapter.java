package cn.guangdian.market.adapter;

import cn.guangdian.market.GuangDianMarket;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.points.event.PointsTransactionEvent;
import cn.guangdian.rpgcore.service.api.MarketService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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
 * <p>集成 Bukkit 事件系统，订阅点券交易事件进行统计。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class MarketServiceAdapter implements MarketService, Listener {

    private final GuangDianMarket plugin;
    private final boolean useRPGCore;
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

                // 注册服务
                registry.registerService(MarketService.class, this);
                logger.info("已注册到 RPGCore: MarketService");

                // 注册事件监听器
                Bukkit.getPluginManager().registerEvents(this, plugin);
                logger.info("已订阅 Bukkit Event: PointsTransactionEvent");

            } catch (Exception e) {
                logger.warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    /**
     * 订阅点券交易事件，用于统计市场交易量
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPointsTransaction(PointsTransactionEvent event) {
        // 记录与市场相关的交易
        String reason = event.getReason();
        if (reason != null && reason.contains("市场")) {
            // 市场交易统计
            if (event.isWithdraw()) {
                totalSalesVolume += event.getAmount();
                // 更新配置中的统计
                plugin.getConfig().set("stats.total-sales-volume", totalSalesVolume);
            }
        }
    }

    // ==================== MarketService 实现 ====================

    @Override
    public boolean listItem(UUID sellerId, ItemStack item, long price) {
        return false;
    }

    @Override
    public boolean purchaseItem(UUID buyerId, String listingId) {
        return false;
    }

    @Override
    public boolean cancelListing(UUID sellerId, String listingId) {
        return false;
    }

    @Override
    public List<Object> getPlayerListings(UUID sellerId) {
        return new ArrayList<>();
    }

    @Override
    public int getMarketSize() {
        return 0;
    }

    @Override
    public int getPlayerListingCount(UUID sellerId) {
        return 0;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * 注销服务
     */
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
}
