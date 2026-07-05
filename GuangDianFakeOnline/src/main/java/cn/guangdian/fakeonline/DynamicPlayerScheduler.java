package cn.guangdian.fakeonline;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 动态玩家调度器
 * 负责动态添加/移除虚拟玩家，模拟真实的玩家流动
 */
public class DynamicPlayerScheduler {

    private final GuangDianFakeOnline plugin;
    private final VirtualPlayerManager virtualPlayerManager;
    private final FakeOnlineConfig config;
    private final Random random;
    
    private BukkitRunnable dynamicTask;
    private BukkitRunnable pingUpdateTask;
    
    public DynamicPlayerScheduler(GuangDianFakeOnline plugin, VirtualPlayerManager virtualPlayerManager) {
        this.plugin = plugin;
        this.virtualPlayerManager = virtualPlayerManager;
        this.config = plugin.getFakeOnlineConfig();
        this.random = new Random();
    }
    
    /**
     * 启动动态调度器
     */
    public void start() {
        if (!config.isDynamicEnabled()) {
            plugin.getLogger().info("动态玩家功能已禁用");
            return;
        }
        
        // 启动动态加入/退出任务
        startDynamicTask();
        
        // 启动Ping更新任务
        startPingUpdateTask();
        
        plugin.getLogger().info("动态玩家调度器已启动");
    }
    
    /**
     * 停止动态调度器
     */
    public void stop() {
        if (dynamicTask != null) {
            dynamicTask.cancel();
            dynamicTask = null;
        }
        
        if (pingUpdateTask != null) {
            pingUpdateTask.cancel();
            pingUpdateTask = null;
        }
        
        plugin.getLogger().info("动态玩家调度器已停止");
    }
    
    /**
     * 启动动态加入/退出任务
     */
    private void startDynamicTask() {
        int interval = config.getDynamicInterval() * 20; // 转换为ticks
        
        dynamicTask = new BukkitRunnable() {
            @Override
            public void run() {
                performDynamicUpdate();
            }
        };
        
        dynamicTask.runTaskTimer(plugin, interval, interval);
    }
    
    /**
     * 执行动态更新
     */
    private void performDynamicUpdate() {
        int currentCount = virtualPlayerManager.getCurrentVirtualCount();
        int minPlayers = config.getMinVirtualPlayers();
        int maxPlayers = config.getMaxVirtualPlayers();
        int[] changeRange = config.getChangeRange();
        
        // 随机决定是加入还是退出
        boolean shouldJoin = random.nextBoolean();
        
        // 随机变化数量
        int changeAmount = changeRange[0] + random.nextInt(changeRange[1] - changeRange[0] + 1);
        
        if (shouldJoin) {
            // 加入玩家
            int newCount = Math.min(currentCount + changeAmount, maxPlayers);
            int actualChange = newCount - currentCount;
            
            if (actualChange > 0) {
                for (int i = 0; i < actualChange; i++) {
                    String name = getRandomPlayerName();
                    virtualPlayerManager.addVirtualPlayer(name);
                }
                
                plugin.getLogger().info("动态加入 " + actualChange + " 个虚拟玩家，当前总数: " + newCount);
            }
        } else {
            // 退出玩家
            int newCount = Math.max(currentCount - changeAmount, minPlayers);
            int actualChange = currentCount - newCount;
            
            if (actualChange > 0) {
                List<UUID> virtualUUIDs = new ArrayList<>(virtualPlayerManager.getVirtualPlayers().keySet());
                
                for (int i = 0; i < actualChange && !virtualUUIDs.isEmpty(); i++) {
                    int index = random.nextInt(virtualUUIDs.size());
                    UUID uuid = virtualUUIDs.get(index);
                    
                    // 从UUID获取虚拟玩家
                    VirtualPlayer virtualPlayer = virtualPlayerManager.getVirtualPlayers().get(uuid);
                    if (virtualPlayer != null) {
                        String name = virtualPlayer.getName();
                        virtualPlayerManager.removeVirtualPlayer(name);
                    }
                    
                    virtualUUIDs.remove(index);
                }
                
                plugin.getLogger().info("动态退出 " + actualChange + " 个虚拟玩家，当前总数: " + newCount);
            }
        }
    }
    
    /**
     * 启动Ping更新任务
     */
    private void startPingUpdateTask() {
        // 每30秒更新一次Ping值
        pingUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updatePingValues();
            }
        };
        
        pingUpdateTask.runTaskTimer(plugin, 600L, 600L); // 30秒 = 600 ticks
    }
    
    /**
     * 更新所有虚拟玩家的Ping值
     */
    private void updatePingValues() {
        for (VirtualPlayer virtualPlayer : virtualPlayerManager.getVirtualPlayers().values()) {
            // 随机更新Ping值（模拟网络波动）
            int newPing = generateRandomPing();
            virtualPlayer.setPing(newPing);
        }
        
        // 重新发送PlayerInfo数据包更新Ping值
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            virtualPlayerManager.sendVirtualPlayersToPlayer(player);
        }
    }
    
    /**
     * 获取随机玩家名
     */
    private String getRandomPlayerName() {
        String[] names = {
            "新玩家", "路人甲", "路人乙", "冒险者", "旅行者",
            "勇者", "战士", "法师", "弓手", "刺客",
            "新手", "老手", "高手", "大神", "萌新"
        };
        return names[random.nextInt(names.length)] + "_" + random.nextInt(1000);
    }
    
    /**
     * 从UUID获取玩家名（简化版本）
     */
    private String getPlayerNameFromUUID(String uuidString) {
        // 这里简化处理，实际应该从VirtualPlayerManager获取
        return "玩家_" + uuidString.substring(0, 4);
    }
    
    /**
     * 生成随机Ping值
     */
    private int generateRandomPing() {
        // 20-100ms的随机值
        return 20 + random.nextInt(80);
    }
}