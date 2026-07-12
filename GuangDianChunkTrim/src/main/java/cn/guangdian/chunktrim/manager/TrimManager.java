package cn.guangdian.chunktrim.manager;

import cn.guangdian.chunktrim.GuangDianChunkTrim;
import cn.guangdian.chunktrim.task.TrimTask;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

/**
 * 裁剪管理器
 */
public class TrimManager {

    private final GuangDianChunkTrim plugin;
    private final Map<String, TrimTask> activeTasks;
    private final Map<String, PendingTrim> pendingTrims;

    public TrimManager(GuangDianChunkTrim plugin) {
        this.plugin = plugin;
        this.activeTasks = new HashMap<>();
        this.pendingTrims = new HashMap<>();
    }

    /**
     * 保存待执行的裁剪参数
     */
    public void savePendingTrim(World world, int centerX, int centerZ, int radius) {
        pendingTrims.put(world.getName(), new PendingTrim(world, centerX, centerZ, radius));
    }
    
    /**
     * 获取待执行的裁剪参数
     */
    public PendingTrim getPendingTrim(String worldName) {
        return pendingTrims.get(worldName);
    }
    
    /**
     * 移除待执行的裁剪参数
     */
    public void removePendingTrim(String worldName) {
        pendingTrims.remove(worldName);
    }

    /**
     * 启动裁剪任务
     */
    public boolean startTrim(World world, int centerX, int centerZ, int radius) {
        String worldName = world.getName();
        
        if (activeTasks.containsKey(worldName)) {
            return false;
        }
        
        TrimTask task = new TrimTask(plugin, world, centerX, centerZ, radius);
        activeTasks.put(worldName, task);
        task.start();
        
        return true;
    }

    /**
     * 暂停裁剪
     */
    public boolean pauseTrim(String worldName) {
        TrimTask task = activeTasks.get(worldName);
        if (task != null) {
            task.pause();
            return true;
        }
        return false;
    }

    /**
     * 继续裁剪
     */
    public boolean continueTrim(String worldName) {
        TrimTask task = activeTasks.get(worldName);
        if (task != null) {
            task.continueTask();
            return true;
        }
        return false;
    }

    /**
     * 取消裁剪
     */
    public boolean cancelTrim(String worldName) {
        TrimTask task = activeTasks.remove(worldName);
        if (task != null) {
            task.cancel();
            return true;
        }
        return false;
    }

    /**
     * 获取任务进度
     */
    public TrimTask getTask(String worldName) {
        return activeTasks.get(worldName);
    }

    /**
     * 是否有任务运行
     */
    public boolean hasActiveTask(String worldName) {
        return activeTasks.containsKey(worldName);
    }

    /**
     * 取消所有任务
     */
    public void cancelAll() {
        for (TrimTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
    }

    /**
     * 获取所有活动任务
     */
    public Map<String, TrimTask> getActiveTasks() {
        return activeTasks;
    }
    
    /**
     * 待执行的裁剪参数
     */
    public static class PendingTrim {
        public final World world;
        public final int centerX;
        public final int centerZ;
        public final int radius;
        
        public PendingTrim(World world, int centerX, int centerZ, int radius) {
            this.world = world;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.radius = radius;
        }
    }
}