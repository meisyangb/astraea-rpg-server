package cn.guangdian.rpgcore.server;

import cn.guangdian.rpgcore.RPGCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.TimeUnit;

/**
 * RPGCore 服务器服务
 * 
 * 封装 Paper 1.21.6 服务器相关 API，提供统一的服务器管理接口
 * 解决 Bukkit.spigot().restart() 等弃用问题
 */
public final class ServerService {

    private static ServerService instance;
    private final RPGCore plugin;

    private ServerService(RPGCore plugin) {
        this.plugin = plugin;
    }

    public static synchronized ServerService getInstance(RPGCore plugin) {
        if (instance == null) {
            instance = new ServerService(plugin);
        }
        return instance;
    }

    /**
     * 安全重启服务器（解决 Bukkit.spigot().restart() 弃用问题）
     * 
     * 使用 Runtime.halt() 配合启动脚本实现重启
     * 需要服务器启动脚本支持 restart 参数
     */
    public void restart() {
        plugin.getLogger().info("服务器将在 5 秒后重启...");
        
        // 通知所有玩家
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Component.text("§c服务器将在 5 秒后重启，请做好准备！"));
        }
        
        // 延迟重启
        Bukkit.getAsyncScheduler().runDelayed(plugin, (task) -> {
            // 保存所有数据
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.savePlayers();
                for (org.bukkit.World world : Bukkit.getWorlds()) {
                    world.save();
                }
                
                // 执行重启
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        // 获取启动命令
                        String javaHome = System.getProperty("java.home");
                        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
                        
                        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
                        StringBuilder cmd = new StringBuilder(javaBin);
                        
                        // 添加 JVM 参数
                        for (String jvmArg : runtimeMXBean.getInputArguments()) {
                            cmd.append(" ").append(jvmArg);
                        }
                        
                        // 添加类路径和主类
                        cmd.append(" -cp ").append(System.getProperty("java.class.path"));
                        cmd.append(" ").append(runtimeMXBean.getClassPath());
                        
                        plugin.getLogger().info("执行重启命令: " + cmd);
                        
                        // 启动新进程
                        Runtime.getRuntime().exec(cmd.toString());
                        
                        // 关闭当前服务器
                        Bukkit.shutdown();
                    } catch (Exception e) {
                        plugin.getLogger().severe("重启失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            });
        }, 5, TimeUnit.SECONDS);
    }

    /**
     * 安全关闭服务器
     */
    public void shutdown() {
        plugin.getLogger().info("服务器正在关闭...");
        
        // 通知所有玩家
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kick(Component.text("§c服务器正在关闭，请稍后再试！"));
        }
        
        // 保存数据
        Bukkit.savePlayers();
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            world.save();
        }
        
        // 关闭服务器
        Bukkit.shutdown();
    }

    /**
     * 获取服务器 TPS
     */
    public double[] getTPS() {
        return Bukkit.getTPS();
    }

    /**
     * 获取格式化后的 TPS
     */
    public String getFormattedTPS(int index) {
        double[] tps = getTPS();
        if (index < 0 || index >= tps.length) {
            return "20.00";
        }
        double value = tps[index];
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "20.00";
        }
        return String.format("%.2f", Math.min(20.0, value));
    }

    /**
     * 获取服务器运行时间（毫秒）
     */
    public long getUptime() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        return runtimeMXBean.getUptime();
    }

    /**
     * 获取服务器启动时间
     */
    public long getStartTime() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        return runtimeMXBean.getStartTime();
    }

    /**
     * 获取服务器内存使用情况
     */
    public MemoryInfo getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return new MemoryInfo(maxMemory, totalMemory, freeMemory, usedMemory);
    }

    /**
     * 广播消息给所有玩家
     */
    public void broadcast(Component message) {
        Bukkit.broadcast(message);
    }

    /**
     * 广播消息给有指定权限的玩家
     */
    public void broadcast(Component message, String permission) {
        Bukkit.broadcast(message, permission);
    }

    /**
     * 执行控制台命令
     */
    public boolean dispatchCommand(String command) {
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    /**
     * 内存信息类
     */
    public static class MemoryInfo {
        private final long maxMemory;
        private final long totalMemory;
        private final long freeMemory;
        private final long usedMemory;

        public MemoryInfo(long maxMemory, long totalMemory, long freeMemory, long usedMemory) {
            this.maxMemory = maxMemory;
            this.totalMemory = totalMemory;
            this.freeMemory = freeMemory;
            this.usedMemory = usedMemory;
        }

        public long getMaxMemory() { return maxMemory; }
        public long getTotalMemory() { return totalMemory; }
        public long getFreeMemory() { return freeMemory; }
        public long getUsedMemory() { return usedMemory; }

        public long getMaxMemoryMB() { return maxMemory / 1024 / 1024; }
        public long getTotalMemoryMB() { return totalMemory / 1024 / 1024; }
        public long getFreeMemoryMB() { return freeMemory / 1024 / 1024; }
        public long getUsedMemoryMB() { return usedMemory / 1024 / 1024; }

        public double getUsedPercentage() {
            return (double) usedMemory / maxMemory * 100;
        }

        @Override
        public String toString() {
            return String.format("Memory[used=%dMB, free=%dMB, total=%dMB, max=%dMB]",
                getUsedMemoryMB(), getFreeMemoryMB(), getTotalMemoryMB(), getMaxMemoryMB());
        }
    }
}
