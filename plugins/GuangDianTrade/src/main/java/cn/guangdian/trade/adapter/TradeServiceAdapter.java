package cn.guangdian.trade.adapter;

import cn.guangdian.trade.GuangDianTrade;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.event.events.PointsTransactionEvent;
import cn.guangdian.rpgcore.service.api.TradeService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * 交易服务适配器
 *
 * <p>连接 GuangDianTrade 实现与 TradeService 接口。</p>
 *
 * <p>集成 RPGCore EventBus，订阅点券交易事件进行交易统计。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class TradeServiceAdapter implements TradeService {

    private final GuangDianTrade plugin;
    private final boolean useRPGCore;
    private EventBus eventBus;
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
                this.eventBus = rpgCore.getEventBus();

                // 注册服务
                registry.registerService(TradeService.class, this);
                logger.info("已注册到 RPGCore: TradeService");

                // 订阅事件
                subscribeToEvents();

            } catch (Exception e) {
                logger.warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    /**
     * 订阅 RPGCore 事件
     *
     * <p>订阅点券交易事件，用于统计玩家间交易数据。</p>
     */
    private void subscribeToEvents() {
        if (eventBus == null) {
            return;
        }

        // 订阅点券交易事件
        eventBus.subscribe(PointsTransactionEvent.class, event -> {
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
        });

        logger.info("已订阅 PointsTransactionEvent");
    }

    @Override
    public boolean isInTrade(UUID playerId) {
        // 使用公开API方法，不再使用反射
        return plugin.isInTradeAPI(playerId);
    }

    @Override
    public UUID getTradePartner(UUID playerId) {
        // 使用公开API方法，不再使用反射
        return plugin.getTradePartnerAPI(playerId);
    }

    @Override
    public boolean sendTradeRequest(Player requester, Player target) {
        // 通过命令发送交易请求
        if (requester == null || target == null) return false;
        requester.performCommand("trade " + target.getName());
        return true;
    }

    @Override
    public boolean acceptTradeRequest(Player player) {
        // 通过命令接受交易请求
        if (player == null) return false;
        player.performCommand("trade accept");
        return true;
    }

    @Override
    public void denyTradeRequest(Player player) {
        // 通过命令拒绝交易请求
        if (player != null) {
            player.performCommand("trade deny");
        }
    }

    @Override
    public boolean cancelTrade(UUID playerId) {
        // 使用公开API方法，不再使用反射
        return plugin.cancelTradeAPI(playerId);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * 获取总交易次数
     *
     * @return 总交易次数
     */
    public long getTotalTradeCount() {
        return totalTradeCount;
    }

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

    public boolean isUsingRPGCore() {
        return useRPGCore;
    }
}