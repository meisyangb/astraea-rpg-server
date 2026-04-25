package cn.guangdian.cleaner.adapter;

import cn.guangdian.cleaner.GuangDianCleaner;
import cn.guangdian.cleaner.manager.CleanManager;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.service.api.CleanerService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cleaner 服务适配器
 * 
 * <p>连接 GuangDianCleaner 与 RPGCore 服务系统，
 * 使用 RPGCore 的 AsyncExecutor 替代本地线程池。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class CleanerServiceAdapter implements CleanerService {

    private final GuangDianCleaner plugin;
    private final CleanManager cleanManager;
    private final boolean useRPGCore;
    private AsyncExecutor asyncExecutor;
    private final AtomicLong totalCleanedCount = new AtomicLong(0);

    public CleanerServiceAdapter(GuangDianCleaner plugin, CleanManager cleanManager) {
        this.plugin = plugin;
        this.cleanManager = cleanManager;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");

        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                this.asyncExecutor = rpgCore.getAsyncExecutor();

                registry.registerService(CleanerService.class, this);
                plugin.getLogger().info("已注册到 RPGCore: CleanerService");
                plugin.getLogger().info("使用 RPGCore AsyncExecutor（统一线程池）");

            } catch (Exception e) {
                plugin.getLogger().warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    @Override
    public CompletableFuture<CleanResult> cleanAll() {
        final long startTime = System.currentTimeMillis();
        
        return executeAsync(() -> {
            int totalRemoved = 0;
            int totalChecked = 0;

            for (World world : Bukkit.getWorlds()) {
                if (!isWorldEnabled(world.getName())) {
                    continue;
                }

                Collection<Item> items = world.getEntitiesByClass(Item.class);
                totalChecked += items.size();

                for (Item item : items) {
                    if (shouldClean(item)) {
                        final Item itemToRemove = item;
                        runSync(itemToRemove::remove);
                        totalRemoved++;
                    }
                }
            }

            totalCleanedCount.addAndGet(totalRemoved);
            long timeTaken = System.currentTimeMillis() - startTime;

            return new CleanResult(totalRemoved, totalChecked, timeTaken, "ALL");
        });
    }

    @Override
    public CompletableFuture<CleanResult> cleanWorld(World world) {
        final long startTime = System.currentTimeMillis();
        final String worldName = world.getName();

        return executeAsync(() -> {
            Collection<Item> items = world.getEntitiesByClass(Item.class);
            int checked = items.size();
            final int[] removed = {0};

            for (Item item : items) {
                if (shouldClean(item)) {
                    final Item itemToRemove = item;
                    runSync(() -> {
                        itemToRemove.remove();
                        removed[0]++;
                    });
                }
            }

            totalCleanedCount.addAndGet(removed[0]);
            long timeTaken = System.currentTimeMillis() - startTime;

            return new CleanResult(removed[0], checked, timeTaken, worldName);
        });
    }

    @Override
    public CompletableFuture<Integer> cleanRadius(Location center, double radius) {
        return executeAsync(() -> {
            World world = center.getWorld();
            if (world == null) return 0;

            double radiusSq = radius * radius;
            Collection<Item> items = world.getEntitiesByClass(Item.class);
            final int[] removed = {0};

            for (Item item : items) {
                if (item.getLocation().distanceSquared(center) <= radiusSq) {
                    if (shouldClean(item)) {
                        final Item itemToRemove = item;
                        runSync(() -> {
                            itemToRemove.remove();
                            removed[0]++;
                        });
                    }
                }
            }

            totalCleanedCount.addAndGet(removed[0]);
            return removed[0];
        });
    }

    @Override
    public boolean shouldClean(Item item) {
        return cleanManager.shouldCleanItem(item);
    }

    @Override
    public boolean isWorldEnabled(String worldName) {
        return cleanManager.isWorldEnabled(worldName);
    }

    @Override
    public void setAutoCleanEnabled(boolean enabled) {
        if (enabled) {
            cleanManager.startAutoCleanTask();
        } else {
            cleanManager.stopAutoCleanTask();
        }
    }

    @Override
    public boolean isAutoCleanEnabled() {
        return cleanManager.isAutoCleanEnabled();
    }

    @Override
    public int getAutoCleanInterval() {
        return plugin.getConfigManager().getAutoCleanInterval();
    }

    @Override
    public void setAutoCleanInterval(int seconds) {
        plugin.getConfigManager().setAutoCleanInterval(seconds);
    }

    @Override
    public long getTotalCleanedCount() {
        return totalCleanedCount.get();
    }

    @Override
    public void resetStats() {
        totalCleanedCount.set(0);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * 异步执行任务并返回结果（使用 RPGCore AsyncExecutor 或默认异步执行）
     */
    private <T> CompletableFuture<T> executeAsync(Callable<T> task) {
        if (asyncExecutor != null) {
            return asyncExecutor.execute(task);
        } else {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return task.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    /**
     * 在主线程执行任务
     */
    private void runSync(Runnable task) {
        plugin.runSync(task);
    }

    /**
     * 注销服务
     */
    public void unregister() {
        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.unregisterService(CleanerService.class);
                plugin.getLogger().info("已从 RPGCore 注销: CleanerService");
            } catch (Exception e) {
                plugin.getLogger().warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }

    /**
     * 检查是否使用 RPGCore
     */
    public boolean isUsingRPGCore() {
        return useRPGCore;
    }

    /**
     * 获取 AsyncExecutor（如果使用 RPGCore）
     */
    public AsyncExecutor getAsyncExecutor() {
        return asyncExecutor;
    }
}