package cn.guangdian.points.adapter;

import cn.guangdian.points.GuangDianPoints;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.points.event.PointsTransactionEvent;
import cn.guangdian.rpgcore.service.api.PointsService;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * PointsService 适配器
 *
 * <p>连接 GuangDianPoints 实现与 PointsService 接口</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class PointsServiceAdapter implements PointsService {

    private final GuangDianPoints plugin;

    public PointsServiceAdapter(GuangDianPoints plugin) {
        this.plugin = plugin;

        // 注册到 RPGCore 服务注册表
        if (Bukkit.getPluginManager().isPluginEnabled("RPGCore")) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                registry.registerService(PointsService.class, this);
                plugin.getLogger().info("已注册到 RPGCore 服务注册表: PointsService");
            } catch (Exception e) {
                plugin.getLogger().warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    @Override
    public long getBalance(UUID playerId) {
        return plugin.getBalance(playerId);
    }

    @Override
    public void setBalance(UUID playerId, long amount, String reason) {
        plugin.setBalance(playerId, amount);
        publishEvent(playerId, PointsTransactionEvent.TransactionType.SET, amount, reason);
    }

    @Override
    public void addBalance(UUID playerId, long amount, String reason) {
        long before = plugin.getBalance(playerId);
        plugin.addBalance(playerId, amount);
        publishEvent(playerId, PointsTransactionEvent.TransactionType.DEPOSIT, amount, reason);
    }

    @Override
    public boolean removeBalance(UUID playerId, long amount, String reason) {
        boolean success = plugin.removeBalance(playerId, amount, reason);
        if (success) {
            publishEvent(playerId, PointsTransactionEvent.TransactionType.WITHDRAW, amount, reason);
        }
        return success;
    }

    @Override
    public boolean transfer(UUID from, UUID to, long amount, String reason) {
        boolean success = plugin.transferBalance(from, to, amount);
        if (success) {
            publishEvent(from, PointsTransactionEvent.TransactionType.TRANSFER_OUT, amount, reason);
            publishEvent(to, PointsTransactionEvent.TransactionType.TRANSFER_IN, amount, reason);
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
        addBalance(playerId, amount, "管理员给予: " + admin + " - " + reason);
    }

    @Override
    public boolean adminTake(UUID playerId, long amount, UUID admin, String reason) {
        return removeBalance(playerId, amount, "管理员扣除: " + admin + " - " + reason);
    }

    @Override
    public void resetBalance(UUID playerId, String reason) {
        plugin.setBalance(playerId, 0);
        publishEvent(playerId, PointsTransactionEvent.TransactionType.RESET, 0, reason);
    }

    @Override
    public long getDefaultBalance() {
        return plugin.getConfig().getLong("default-balance", 0);
    }

    @Override
    public int getOnlinePlayerCount() {
        return Bukkit.getOnlinePlayers().size();
    }

    @Override
    public long getTotalCirculation() {
        return Bukkit.getOnlinePlayers().stream()
            .mapToLong(p -> plugin.getBalance(p.getUniqueId()))
            .sum();
    }

    /**
     * 发布点券交易事件（使用 Bukkit 事件系统）
     */
    private void publishEvent(UUID playerId, PointsTransactionEvent.TransactionType type,
                               long amount, String reason) {
        try {
            long balance = plugin.getBalance(playerId);
            PointsTransactionEvent event = new PointsTransactionEvent(
                playerId, type, amount, balance - amount, balance, reason, null);
            Bukkit.getPluginManager().callEvent(event);
        } catch (Exception e) {
            plugin.getLogger().warning("发布事件失败: " + e.getMessage());
        }
    }

    /**
     * 检查是否使用 RPGCore
     */
    public boolean isUsingRPGCore() {
        return Bukkit.getPluginManager().isPluginEnabled("RPGCore");
    }

    /**
     * 注销服务
     */
    public void unregister() {
        if (Bukkit.getPluginManager().isPluginEnabled("RPGCore")) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                registry.unregisterService(PointsService.class);
                plugin.getLogger().info("已从 RPGCore 服务注册表注销: PointsService");
            } catch (Exception e) {
                plugin.getLogger().warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }
}
