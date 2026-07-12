package cn.guangdian.chunktrim.task;

import cn.guangdian.chunktrim.GuangDianChunkTrim;
import cn.guangdian.chunktrim.config.TrimConfig;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 区块裁剪任务
 */
public class TrimTask implements Runnable {

    private final GuangDianChunkTrim plugin;
    private final World world;
    private final int centerX;
    private final int centerZ;
    private final int radius;

    private BukkitTask task;
    private final AtomicBoolean running;
    private final AtomicBoolean paused;
    private final AtomicInteger processedChunks;
    private final AtomicInteger deletedChunks;
    private int totalChunks;
    private long startTime;

    public TrimTask(GuangDianChunkTrim plugin, World world, int centerX, int centerZ, int radius) {
        this.plugin = plugin;
        this.world = world;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        
        this.running = new AtomicBoolean(false);
        this.paused = new AtomicBoolean(false);
        this.processedChunks = new AtomicInteger(0);
        this.deletedChunks = new AtomicInteger(0);
    }

    /**
     * 启动裁剪
     */
    public void start() {
        if (running.get()) return;
        
        running.set(true);
        paused.set(false);
        startTime = System.currentTimeMillis();
        
        // 计算需要检查的区块数量
        totalChunks = calculateTotalChunks();
        processedChunks.set(0);
        deletedChunks.set(0);
        
        plugin.getLogger().info("=== 开始裁剪世界: " + world.getName() + " ===");
        plugin.getLogger().info("中心坐标: (" + centerX + ", " + centerZ + ")");
        plugin.getLogger().info("保留半径: " + radius + " 格");
        plugin.getLogger().info("预计需要检查: " + totalChunks + " 个区块文件");
        
        // 启动异步任务
        task = Bukkit.getScheduler().runTaskAsynchronously(plugin, this);
    }

    /**
     * 暂停裁剪
     */
    public void pause() {
        paused.set(true);
        plugin.getLogger().info("裁剪任务已暂停: " + world.getName());
    }

    /**
     * 继续裁剪
     */
    public void continueTask() {
        paused.set(false);
        plugin.getLogger().info("裁剪任务继续: " + world.getName());
    }

    /**
     * 取消裁剪
     */
    public void cancel() {
        running.set(false);
        if (task != null) {
            task.cancel();
        }
        plugin.getLogger().info("裁剪任务已取消: " + world.getName());
    }

    @Override
    public void run() {
        try {
            TrimConfig config = plugin.getTrimConfig();
            int chunksPerSecond = config.getChunksPerSecond();
            int bufferSize = config.getBufferSize();
            
            // 扩展的保留范围（区块坐标）
            int minChunkX = (centerX - radius) >> 4;
            int maxChunkX = (centerX + radius) >> 4;
            int minChunkZ = (centerZ - radius) >> 4;
            int maxChunkZ = (centerZ + radius) >> 4;
            
            // 获取region文件夹
            File regionFolder = new File(world.getWorldFolder(), "region");
            if (!regionFolder.exists()) {
                plugin.getLogger().warning("世界没有region文件夹: " + world.getName());
                finish();
                return;
            }
            
            // 遍历所有.mca文件
            File[] regionFiles = regionFolder.listFiles((dir, name) -> name.endsWith(".mca"));
            if (regionFiles == null || regionFiles.length == 0) {
                plugin.getLogger().warning("没有找到区块文件: " + world.getName());
                finish();
                return;
            }
            
            int delay = 1000 / chunksPerSecond; // 每个区块的延迟
            
            for (File regionFile : regionFiles) {
                if (!running.get()) break;
                while (paused.get()) {
                    Thread.sleep(100);
                    if (!running.get()) break;
                }
                
                // 解析region文件名: r.x.z.mca
                String fileName = regionFile.getName();
                String[] parts = fileName.replace("r.", "").replace(".mca", "").split("\\.");
                
                if (parts.length == 2) {
                    int regionX = Integer.parseInt(parts[0]);
                    int regionZ = Integer.parseInt(parts[1]);
                    
                    // 计算这个region文件的区块范围
                    int regionMinX = regionX * 32;
                    int regionMaxX = regionX * 32 + 31;
                    int regionMinZ = regionZ * 32;
                    int regionMaxZ = regionZ * 32 + 31;
                    
                    // 判断这个region是否在保留范围内
                    boolean shouldKeep = intersects(
                        regionMinX, regionMaxX, regionMinZ, regionMaxZ,
                        minChunkX, maxChunkX, minChunkZ, maxChunkZ,
                        bufferSize
                    );
                    
                    if (!shouldKeep) {
                        // 删除region文件
                        if (regionFile.delete()) {
                            deletedChunks.incrementAndGet();
                            plugin.getLogger().info("删除: " + fileName + " (区域外)");
                        }
                    }
                }
                
                processedChunks.incrementAndGet();
                
                // 定期报告进度
                int processed = processedChunks.get();
                if (processed % 10 == 0) {
                    reportProgress(processed, regionFiles.length);
                }
                
                Thread.sleep(delay);
            }
            
            finish();
            
        } catch (Exception e) {
            plugin.getLogger().severe("裁剪任务出错: " + e.getMessage());
            e.printStackTrace();
            running.set(false);
        }
    }

    /**
     * 判断两个区域是否相交
     */
    private boolean intersects(int rMinX, int rMaxX, int rMinZ, int rMaxZ,
                               int kMinX, int kMaxX, int kMinZ, int kMaxZ,
                               int buffer) {
        // 扩展保留范围
        kMinX -= buffer;
        kMaxX += buffer;
        kMinZ -= buffer;
        kMaxZ += buffer;
        
        // 检查是否相交
        return rMinX <= kMaxX && rMaxX >= kMinX && 
               rMinZ <= kMaxZ && rMaxZ >= kMinZ;
    }

    /**
     * 计算总区块数
     */
    private int calculateTotalChunks() {
        File regionFolder = new File(world.getWorldFolder(), "region");
        File[] regionFiles = regionFolder.listFiles((dir, name) -> name.endsWith(".mca"));
        return regionFiles != null ? regionFiles.length : 0;
    }

    /**
     * 报告进度
     */
    private void reportProgress(int processed, int total) {
        double percent = (double) processed / total * 100;
        int deleted = deletedChunks.get();
        
        String msg = String.format("[%s] 进度: %.1f%% (%d/%d) | 已删除: %d 个区域文件",
            world.getName(), percent, processed, total, deleted);
        
        plugin.getLogger().info(msg);
    }

    /**
     * 完成裁剪
     */
    private void finish() {
        running.set(false);
        
        long duration = System.currentTimeMillis() - startTime;
        int deleted = deletedChunks.get();
        int processed = processedChunks.get();
        
        plugin.getLogger().info("=== 裁剪完成 ===");
        plugin.getLogger().info("世界: " + world.getName());
        plugin.getLogger().info("检查文件数: " + processed);
        plugin.getLogger().info("删除区域文件: " + deleted);
        plugin.getLogger().info("耗时: " + (duration / 1000) + " 秒");
    }

    // Getters
    public boolean isRunning() { return running.get(); }
    public boolean isPaused() { return paused.get(); }
    public int getProgress() { 
        if (totalChunks == 0) return 0;
        return (int) ((double) processedChunks.get() / totalChunks * 100); 
    }
    public int getDeletedChunks() { return deletedChunks.get(); }
    public World getWorld() { return world; }
}