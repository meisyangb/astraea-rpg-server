package cn.guangdian.rpgcore.module.points;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.concurrency.LockTimeoutException;
import cn.guangdian.rpgcore.concurrency.PlayerLockManager;
import cn.guangdian.rpgcore.event.EventPublisher;
import cn.guangdian.rpgcore.event.events.PlayerDataLoadEvent;
import cn.guangdian.rpgcore.event.events.PlayerDataSaveEvent;
import cn.guangdian.rpgcore.module.RPGModule;
import cn.guangdian.rpgcore.service.api.PointsService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 点券模块
 * 
 * <p>提供玩家点券管理功能，继承 RPGModule 基类。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class PointsModule extends RPGModule implements PointsService {

    private PointsRepository repository;
    private long defaultBalance;

    /**
     * 创建点券模块
     * 
     * @param plugin 插件实例
     */
    public PointsModule(JavaPlugin plugin) {
        super(plugin, "Points");
    }

    @Override
    protected void onLoad() {
        // 加载配置
        defaultBalance = plugin.getConfig().getLong("settings.default-balance", 0);
    }

    @Override
    protected void loadConfig() {
        plugin.saveDefaultConfig();
    }

    @Override
    protected void registerServices() {
        // 初始化仓库
        repository = new PointsRepository(plugin, defaultBalance);
        
        // 注册服务
        getServices().registerService(PointsService.class, this);
        log("PointsService registered");
    }

    @Override
    protected void registerCommands() {
        // 命令注册将在后续迁移
    }

    @Override
    protected void registerListeners() {
        registerListener(this);
    }

    @Override
    protected void saveAllData() {
        // 保存所有缓存数据
        java.util.Map<UUID, PlayerPointsData> allData = repository.getCacheSize() > 0 ? 
                repository.loadAll().join() : java.util.Collections.emptyMap();
        for (java.util.Map.Entry<UUID, PlayerPointsData> entry : allData.entrySet()) {
            UUID playerId = entry.getKey();
            PlayerPointsData data = repository.getFromCache(playerId);
            if (data != null) {
                repository.save(playerId, data).join();
            }
        }
    }

    @Override
    protected void stopTasks() {
        // 停止定时任务
    }

    @Override
    protected void cleanupResources() {
        if (repository != null) {
            repository.close();
        }
    }

    // ==================== 事件监听 ====================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 异步加载玩家数据
        loadPlayerData(player.getUniqueId()).thenAccept(data -> {
            // 发布数据加载事件
            EventPublisher.publish(new PlayerDataLoadEvent(player.getUniqueId(), "Points", data));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 保存数据
        PlayerPointsData data = repository.getFromCache(playerId);
        if (data != null) {
            getAsyncExecutor().submitPlayerSave(playerId, () -> {
                repository.save(playerId, data).join();
                EventPublisher.publish(new PlayerDataSaveEvent(playerId, "Points", data));
            });
        }

        // 清理缓存
        repository.invalidate(playerId);
    }

    // ==================== PointsService 实现 ====================

    @Override
    public long getBalance(UUID playerId) {
        PlayerPointsData data = getOrCreateData(playerId);
        return data != null ? data.getBalance() : 0;
    }

    @Override
    public void setBalance(UUID playerId, long amount, String reason) {
        PlayerPointsData data = getOrCreateData(playerId);
        if (data == null) {
            logWarning("Failed to set balance: no data for " + playerId);
            return;
        }
        
        PlayerLockManager lockManager = getLockManager();
        try {
            lockManager.executeWithLock(playerId, () -> {
                data.setBalance(amount);
            });
        } catch (LockTimeoutException e) {
            logWarning("Failed to set balance due to lock timeout: " + playerId);
        }
    }

    @Override
    public void addBalance(UUID playerId, long amount, String reason) {
        PlayerPointsData data = getOrCreateData(playerId);
        if (data == null) {
            logWarning("Failed to add balance: no data for " + playerId);
            return;
        }
        
        PlayerLockManager lockManager = getLockManager();
        try {
            lockManager.executeWithLock(playerId, () -> {
                data.addBalance(amount);
            });
        } catch (LockTimeoutException e) {
            logWarning("Failed to add balance due to lock timeout: " + playerId);
        }
    }

    @Override
    public boolean removeBalance(UUID playerId, long amount, String reason) {
        PlayerPointsData data = getOrCreateData(playerId);
        if (data == null) {
            logWarning("Failed to remove balance: no data for " + playerId);
            return false;
        }
        
        PlayerLockManager lockManager = getLockManager();
        try {
            return lockManager.executeWithLock(playerId, () -> {
                return data.removeBalance(amount);
            });
        } catch (LockTimeoutException e) {
            logWarning("Failed to remove balance due to lock timeout: " + playerId);
            return false;
        }
    }

    @Override
    public boolean transfer(UUID from, UUID to, long amount, String reason) {
        if (amount <= 0) return false;
        
        PlayerPointsData fromData = getOrCreateData(from);
        PlayerPointsData toData = getOrCreateData(to);
        if (fromData == null || toData == null) {
            logWarning("Failed to transfer: no data for " + from + " or " + to);
            return false;
        }
        
        PlayerLockManager lockManager = getLockManager();
        try {
            return lockManager.executeWithDualLock(from, to, () -> {
                if (!fromData.hasBalance(amount)) {
                    return false;
                }
                
                fromData.removeBalance(amount);
                toData.addBalance(amount);
                return true;
            });
        } catch (LockTimeoutException e) {
            logWarning("Failed to transfer due to lock timeout: " + from + " -> " + to);
            return false;
        }
    }

    @Override
    public boolean hasBalance(UUID playerId, long amount) {
        PlayerPointsData data = getOrCreateData(playerId);
        return data != null && data.hasBalance(amount);
    }

    // ==================== 异步操作 ====================

    @Override
    public CompletableFuture<Long> getBalanceAsync(UUID playerId) {
        AsyncExecutor executor = getAsyncExecutor();
        return executor.execute(() -> getBalance(playerId));
    }

    @Override
    public CompletableFuture<Void> setBalanceAsync(UUID playerId, long amount, String reason) {
        return getAsyncExecutor().execute(() -> {
            setBalance(playerId, amount, reason);
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> addBalanceAsync(UUID playerId, long amount, String reason) {
        return getAsyncExecutor().execute(() -> {
            addBalance(playerId, amount, reason);
            return null;
        });
    }

    @Override
    public CompletableFuture<Boolean> removeBalanceAsync(UUID playerId, long amount, String reason) {
        return getAsyncExecutor().execute(() -> removeBalance(playerId, amount, reason));
    }

    @Override
    public CompletableFuture<Boolean> transferAsync(UUID from, UUID to, long amount, String reason) {
        return getAsyncExecutor().execute(() -> transfer(from, to, amount, reason));
    }

    // ==================== 管理操作 ====================

    @Override
    public void adminGive(UUID playerId, long amount, UUID admin, String reason) {
        addBalance(playerId, amount, "Admin:" + (admin != null ? admin.toString() : "Console") + ":" + reason);
    }

    @Override
    public boolean adminTake(UUID playerId, long amount, UUID admin, String reason) {
        return removeBalance(playerId, amount, "Admin:" + (admin != null ? admin.toString() : "Console") + ":" + reason);
    }

    @Override
    public void resetBalance(UUID playerId, String reason) {
        setBalance(playerId, defaultBalance, "Reset:" + reason);
    }

    @Override
    public long getDefaultBalance() {
        return defaultBalance;
    }

    @Override
    public int getOnlinePlayerCount() {
        return plugin.getServer().getOnlinePlayers().size();
    }

    @Override
    public long getTotalCirculation() {
        long total = 0;
        for (PlayerPointsData data : repository.loadAll().join().values()) {
            total += data.getBalance();
        }
        return total;
    }

    // ==================== 辅助方法 ====================

    private CompletableFuture<PlayerPointsData> loadPlayerData(UUID playerId) {
        return repository.load(playerId);
    }

    private PlayerPointsData getOrCreateData(UUID playerId) {
        PlayerPointsData data = repository.getFromCache(playerId);
        if (data == null) {
            try {
                data = repository.load(playerId).join();
            } catch (Exception e) {
                logWarning("Failed to load data for " + playerId + ": " + e.getMessage());
                return null;
            }
        }
        return data;
    }
}