package cn.guangdian.fakeplayer;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

/**
 * 动态玩家调度器
 * 定时随机添加/移除虚拟玩家，模拟真实玩家流动
 */
public class DynamicPlayerScheduler {

    private final GuangDianFakePlayer plugin;
    private final VirtualPlayerDataManager dataManager;
    private final Random random = new Random();
    private BukkitRunnable task;
    private boolean running = false;

    public DynamicPlayerScheduler(GuangDianFakePlayer plugin, VirtualPlayerDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    /**
     * 启动动态调度器
     */
    public void start() {
        if (running) {
            return; // 已经在运行
        }

        FakePlayerConfig config = plugin.getFakePlayerConfig();
        int interval = config.getDynamicInterval();

        task = new BukkitRunnable() {
            @Override
            public void run() {
                updateVirtualPlayers();
            }
        };

        task.runTaskTimer(plugin, interval * 20L, interval * 20L); // 转换为ticks
        running = true;

        plugin.getLogger().info("动态玩家调度器已启动，间隔: " + interval + "秒");
    }

    /**
     * 停止动态调度器
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        running = false;

        plugin.getLogger().info("动态玩家调度器已停止");
    }

    /**
     * 更新虚拟玩家数量
     */
    private void updateVirtualPlayers() {
        FakePlayerConfig config = plugin.getFakePlayerConfig();
        int currentCount = dataManager.getVirtualPlayerCount();
        int minCount = config.getMinCount();
        int maxCount = config.getMaxCount();
        int changeMin = config.getChangeMin();
        int changeMax = config.getChangeMax();

        // 随机决定变化数量
        int change = changeMin + random.nextInt(changeMax - changeMin + 1);

        // 随机决定是添加还是移除
        boolean shouldAdd = random.nextBoolean();

        if (shouldAdd) {
            // 添加虚拟玩家
            if (currentCount + change <= maxCount) {
                dataManager.createVirtualPlayers(change);
                if (config.isDebug()) {
                    plugin.getLogger().info("动态添加 " + change + " 个虚拟玩家，当前总数: " + dataManager.getVirtualPlayerCount());
                }
            } else {
                // 超过最大数量，改为移除
                int removeCount = Math.min(change, currentCount - minCount);
                if (removeCount > 0) {
                    removeRandomPlayers(removeCount);
                    if (config.isDebug()) {
                        plugin.getLogger().info("超过最大数量，移除 " + removeCount + " 个虚拟玩家，当前总数: " + dataManager.getVirtualPlayerCount());
                    }
                }
            }
        } else {
            // 移除虚拟玩家
            if (currentCount - change >= minCount) {
                removeRandomPlayers(change);
                if (config.isDebug()) {
                    plugin.getLogger().info("动态移除 " + change + " 个虚拟玩家，当前总数: " + dataManager.getVirtualPlayerCount());
                }
            } else {
                // 低于最小数量，改为添加
                int addCount = Math.min(change, maxCount - currentCount);
                if (addCount > 0) {
                    dataManager.createVirtualPlayers(addCount);
                    if (config.isDebug()) {
                        plugin.getLogger().info("低于最小数量，添加 " + addCount + " 个虚拟玩家，当前总数: " + dataManager.getVirtualPlayerCount());
                    }
                }
            }
        }
    }

    /**
     * 随机移除指定数量的虚拟玩家
     */
    private void removeRandomPlayers(int count) {
        List<String> names = dataManager.getVirtualPlayerNames();
        if (names.isEmpty()) {
            return;
        }

        for (int i = 0; i < count && !names.isEmpty(); i++) {
            int index = random.nextInt(names.size());
            String name = names.get(index);
            // 这里不需要手动移除，因为数据管理器会自动处理
            names.remove(index);
        }
    }

    /**
     * 是否正在运行
     */
    public boolean isRunning() {
        return running;
    }
}