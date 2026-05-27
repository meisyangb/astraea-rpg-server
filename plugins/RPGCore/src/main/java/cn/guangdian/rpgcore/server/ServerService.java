package cn.guangdian.rpgcore.server;

import cn.guangdian.rpgcore.RPGCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
     * 使用 ProcessBuilder 启动新进程实现重启
     * 支持直接运行 jar 或使用启动脚本
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
                Bukkit.getScheduler().runTask(plugin, this::executeRestart);
            });
        }, 5, TimeUnit.SECONDS);
    }
    
    /**
     * 执行重启进程
     */
    private void executeRestart() {
        try {
            ProcessBuilder pb = createRestartProcessBuilder();
            if (pb == null) {
                plugin.getLogger().severe("无法创建重启进程：无法确定服务器启动方式");
                return;
            }
            
            pb.directory(new File("."));
            pb.inheritIO();
            
            plugin.getLogger().info("执行重启命令: " + String.join(" ", pb.command()));
            
            Process process = pb.start();
            
            // 等待短暂时间确认进程启动
            boolean started = process.waitFor(2, TimeUnit.SECONDS);
            if (!started || !process.isAlive()) {
                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    plugin.getLogger().severe("重启进程启动失败，退出码: " + exitCode);
                    return;
                }
            }
            
            plugin.getLogger().info("新服务器进程已启动，关闭当前服务器...");
            Bukkit.shutdown();
            
        } catch (Exception e) {
            plugin.getLogger().severe("重启失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建重启进程的 ProcessBuilder
     * 优先使用启动脚本，其次使用 jar 文件
     */
    private ProcessBuilder createRestartProcessBuilder() {
        File serverDir = new File(".");
        
        // 1. 优先查找启动脚本
        String[] scriptNames = {"start.bat", "start.cmd", "start.sh", "run.bat", "run.sh"};
        for (String scriptName : scriptNames) {
            File script = new File(serverDir, scriptName);
            if (script.exists() && script.isFile()) {
                List<String> command = new ArrayList<>();
                if (scriptName.endsWith(".sh")) {
                    command.add("bash");
                }
                command.add(script.getAbsolutePath());
                return new ProcessBuilder(command);
            }
        }
        
        // 2. 查找服务器 jar 文件
        File[] jarFiles = serverDir.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".jar") && 
            (name.toLowerCase().contains("paper") || 
             name.toLowerCase().contains("spigot") || 
             name.toLowerCase().contains("bukkit") ||
             name.toLowerCase().contains("server"))
        );
        
        if (jarFiles != null && jarFiles.length > 0) {
            File serverJar = jarFiles[0];
            String javaHome = System.getProperty("java.home");
            String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
            
            List<String> command = new ArrayList<>();
            command.add(javaBin);
            
            // 添加当前 JVM 参数
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            for (String jvmArg : runtimeMXBean.getInputArguments()) {
                if (!jvmArg.contains("-agentlib:jdwp") && !jvmArg.contains("-Xrunjdwp")) {
                    command.add(jvmArg);
                }
            }
            
            command.add("-jar");
            command.add(serverJar.getName());
            command.add("nogui");
            
            return new ProcessBuilder(command);
        }
        
        return null;
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
