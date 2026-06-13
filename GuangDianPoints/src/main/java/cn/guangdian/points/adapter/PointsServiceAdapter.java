package cn.guangdian.points.adapter;

import cn.guangdian.points.GuangDianPoints;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.event.events.PointsTransactionEvent;
import cn.guangdian.rpgcore.service.api.PointsService;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * PointsService 适配器
 * 
 * <p>连接旧的 GuangDianPoints 实现与新的 PointsService 接口，
 * 支持两种运行模式：</p>
 * 
 * <ul>
 *   <li>RPGCore 模式：当 RPGCore 可用时，通过 ServiceRegistry 注册服务，并通过 EventBus 发布事件</li>
 *   <li>独立模式：当 RPGCore 不可用时，使用旧的实现</li>
 * </ul>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class PointsServiceAdapter implements PointsService {

    private final GuangDianPoints plugin;
    private final boolean useRPGCore;
    private EventBus eventBus;

    public PointsServiceAdapter(GuangDianPoints plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        
        // 如果 RPGCore 可用，注册服务
        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                this.eventBus = rpgCore.getEventBus();
                
                registry.registerService(PointsService.class, this);
                plugin.getLogger().info("已注册到 RPGCore 服务注册表: PointsService");
                
                // 订阅其他插件的事件（示例）
                subscribeToEvents();
            } catch (Exception e) {
                plugin.getLogger().warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    /**
     * 订阅其他插件发布的事件
     */
    private void subscribeToEvents() {
        if (eventBus == null) return;
        
        // 订阅公会事件，用于公会点券奖励等
        // eventBus.subscribe(RpgGuildEvent.class, event -> {
        //     // 处理公会事件
        // });
        
        plugin.getLogger().info("已订阅 RPGCore 事件系统");
    }

    @Override
    public long getBalance(UUID playerId) {
        return plugin.getBalance(playerId);
    }

    @Override
    public void setBalance(UUID playerId, long amount, String reason) {
        long before = plugin.getBalance(playerId);
        plugin.setBalance(playerId, amount, reason);
        publishEvent(playerId, PointsTransactionEvent.TransactionType.SET, 
            amount, before, amount, reason, null);
    }

    @Override
    public void addBalance(UUID playerId, long amount, String reason) {
        long before = plugin.getBalance(playerId);
        plugin.addBalance(playerId, amount, reason);
        publishEvent(playerId, PointsTransactionEvent.TransactionType.DEPOSIT, 
            amount, before, before + amount, reason, null);
    }

    @Override
    public boolean removeBalance(UUID playerId, long amount, String reason) {
        long before = plugin.getBalance(playerId);
        boolean success = plugin.removeBalance(playerId, amount, reason);
        if (success) {
            publishEvent(playerId, PointsTransactionEvent.TransactionType.WITHDRAW, 
                amount, before, before - amount, reason, null);
        }
        return success;
    }

    @Override
    public boolean transfer(UUID from, UUID to, long amount, String reason) {
        long fromBefore = plugin.getBalance(from);
        long toBefore = plugin.getBalance(to);
        boolean success = plugin.transferBalance(from, to, amount);
        if (success) {
            // 发布转出事件
            publishEvent(from, PointsTransactionEvent.TransactionType.TRANSFER_OUT, 
                amount, fromBefore, fromBefore - amount, reason, to);
            // 发布转入事件
            publishEvent(to, PointsTransactionEvent.TransactionType.TRANSFER_IN, 
                amount, toBefore, toBefore + amount, reason, from);
        }
        return success;
    }

    @Override
    public boolean hasBalance(UUID playerId, long amount) {
        return plugin.getBalance(playerId) >= amount;
    }

    @Override
    public CompletableFuture<Long> getBalanceAsync(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> getBalance(playerId));
    }

    @Override
    public CompletableFuture<Void> setBalanceAsync(UUID playerId, long amount, String reason) {
        return CompletableFuture.runAsync(() -> setBalance(playerId, amount, reason));
    }

    @Override
    public CompletableFuture<Void> addBalanceAsync(UUID playerId, long amount, String reason) {
        return CompletableFuture.runAsync(() -> addBalance(playerId, amount, reason));
    }

    @Override
    public CompletableFuture<Boolean> removeBalanceAsync(UUID playerId, long amount, String reason) {
        return CompletableFuture.supplyAsync(() -> removeBalance(playerId, amount, reason));
    }

    @Override
    public CompletableFuture<Boolean> transferAsync(UUID from, UUID to, long amount, String reason) {
        return CompletableFuture.supplyAsync(() -> transfer(from, to, amount, reason));
    }

    @Override
    public void adminGive(UUID playerId, long amount, UUID admin, String reason) {
        plugin.addBalance(playerId, amount, "Admin:" + (admin != null ? admin.toString() : "Console") + ":" + reason);
    }

    @Override
    public boolean adminTake(UUID playerId, long amount, UUID admin, String reason) {
        return plugin.removeBalance(playerId, amount, "Admin:" + (admin != null ? admin.toString() : "Console") + ":" + reason);
    }

    @Override
    public void resetBalance(UUID playerId, String reason) {
        plugin.setBalance(playerId, plugin.getDefaultBalance(), "Reset:" + reason);
    }

    @Override
    public long getDefaultBalance() {
        return plugin.getDefaultBalance();
    }

    @Override
    public int getOnlinePlayerCount() {
        return Bukkit.getOnlinePlayers().size();
    }

    @Override
    public long getTotalCirculation() {
        long total = 0;
        for (long balance : plugin.getAllBalances().values()) {
            total += balance;
        }
        return total;
    }

    /**
     * 注销服务
     */
    public void unregister() {
        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.unregisterService(PointsService.class);
                plugin.getLogger().info("已从 RPGCore 服务注册表注销: PointsService");
            } catch (Exception e) {
                plugin.getLogger().warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }

    /**
     * 发布点券交易事件
     */
    private void publishEvent(UUID playerId, PointsTransactionEvent.TransactionType type,
                               long amount, long before, long after, String reason, UUID relatedPlayer) {
        if (eventBus != null) {
            try {
                PointsTransactionEvent event = new PointsTransactionEvent(
                    playerId, type, amount, before, after, reason, relatedPlayer);
                eventBus.publish(event);
            } catch (Exception e) {
                plugin.getLogger().warning("发布事件失败: " + e.getMessage());
            }
        }
    }

    /**
     * 检查是否使用 RPGCore
     */
    public boolean isUsingRPGCore() {
        return useRPGCore;
    }
}