package cn.guangdian.rpgcore.service.api;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 清理服务接口
 * 
 * <p>提供地面掉落物清理功能，支持定时自动清理和手动触发。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface CleanerService {

    /**
     * 清理结果
     */
    class CleanResult {
        private final int itemsRemoved;
        private final int entitiesChecked;
        private final long timeTakenMs;
        private final String worldName;

        public CleanResult(int itemsRemoved, int entitiesChecked, long timeTakenMs, String worldName) {
            this.itemsRemoved = itemsRemoved;
            this.entitiesChecked = entitiesChecked;
            this.timeTakenMs = timeTakenMs;
            this.worldName = worldName;
        }

        public int getItemsRemoved() { return itemsRemoved; }
        public int getEntitiesChecked() { return entitiesChecked; }
        public long getTimeTakenMs() { return timeTakenMs; }
        public String getWorldName() { return worldName; }

        @Override
        public String toString() {
            return String.format("CleanResult{removed=%d, checked=%d, time=%dms, world=%s}",
                itemsRemoved, entitiesChecked, timeTakenMs, worldName);
        }
    }

    /**
     * 执行全局清理（所有启用的世界）
     * 
     * @return 清理结果
     */
    CompletableFuture<CleanResult> cleanAll();

    /**
     * 清理指定世界
     * 
     * @param world 世界
     * @return 清理结果
     */
    CompletableFuture<CleanResult> cleanWorld(World world);

    /**
     * 清理指定半径内的掉落物
     * 
     * @param center 中心位置
     * @param radius 半径
     * @return 清理的物品数量
     */
    CompletableFuture<Integer> cleanRadius(Location center, double radius);

    /**
     * 检查物品是否应该被清理
     * 
     * @param item 物品
     * @return 如果应该被清理返回 true
     */
    boolean shouldClean(Item item);

    /**
     * 检查世界是否启用清理
     * 
     * @param worldName 世界名称
     * @return 如果启用返回 true
     */
    boolean isWorldEnabled(String worldName);

    /**
     * 启用/禁用自动清理
     * 
     * @param enabled 是否启用
     */
    void setAutoCleanEnabled(boolean enabled);

    /**
     * 检查自动清理是否启用
     * 
     * @return 如果启用返回 true
     */
    boolean isAutoCleanEnabled();

    /**
     * 获取自动清理间隔（秒）
     * 
     * @return 间隔秒数
     */
    int getAutoCleanInterval();

    /**
     * 设置自动清理间隔
     * 
     * @param seconds 间隔秒数
     */
    void setAutoCleanInterval(int seconds);

    /**
     * 获取上次清理统计
     * 
     * @return 总清理物品数
     */
    long getTotalCleanedCount();

    /**
     * 重置统计
     */
    void resetStats();

    /**
     * 检查服务是否可用
     * 
     * @return 如果服务可用返回 true
     */
    boolean isAvailable();
}