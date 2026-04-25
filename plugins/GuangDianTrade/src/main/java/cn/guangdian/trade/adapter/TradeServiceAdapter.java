package cn.guangdian.trade.adapter;

import cn.guangdian.trade.GuangDianTrade;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.points.event.PointsTransactionEvent;
import cn.guangdian.rpgcore.service.api.TradeService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * 交易服务适配器
 *
 * <p>连接 GuangDianTrade 实现与 TradeService 接口。</p>
 *
 * <p>集成 Bukkit 事件系统，订阅点券交易事件进行交易统计。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class TradeServiceAdapter implements TradeService, Listener {

    private final GuangDianTrade plugin;
    private final boolean useRPGCore;
    private Logger logger;
    private long totalTradeCount = 0;

    public TradeServiceAdapter(GuangDianTrade plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        this.logger = plugin.getLogger();

        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();

                // 注册服务
                registry.registerService(TradeService.class, this);
                logger.info("已注册到 RPGCore: TradeService");

                // 注册事件监听器
                Bukkit.getPluginManager().registerEvents(this, plugin);
                logger.info("已订阅 Bukkit Event: PointsTransactionEvent");

            } catch (Exception e) {
                logger.warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    /**
     * 订阅点券交易事件 - 用于统计玩家间交易数据
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPointsTransaction(PointsTransactionEvent event) {
        // 记录玩家间转账（可能是交易相关）
        if (event.isTransfer()) {
            totalTradeCount++;
            // 更新统计
            plugin.getConfig().set("stats.total-trades", totalTradeCount);

            // 记录玩家个人交易次数
            UUID playerId = event.getPlayerId();
            String path = "stats.player." + playerId + ".trades";
            int playerTrades = plugin.getConfig().getInt(path, 0);
            plugin.getConfig().set(path, playerTrades + 1);
        }
    }

    @Override
    public boolean isInTrade(UUID playerId) {
        return plugin.isInTrade(playerId);
    }

    @Override
    public UUID getTradePartner(UUID playerId) {
        return plugin.getTradePartner(playerId);
    }

    @Override
    public boolean sendTradeRequest(Player requester, Player target) {
        return plugin.sendTradeRequest(requester, target);
    }

    @Override
    public boolean acceptTradeRequest(Player player) {
        return plugin.acceptTradeRequest(player);
    }

    @Override
    public void denyTradeRequest(Player player) {
        plugin.denyTradeRequest(player);
    }

    @Override
    public boolean cancelTrade(UUID playerId) {
        return plugin.cancelTradeAPI(playerId);
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
                registry.unregisterService(TradeService.class);
                logger.info("已从 RPGCore 注销: TradeService");
            } catch (Exception e) {
                logger.warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }
}
