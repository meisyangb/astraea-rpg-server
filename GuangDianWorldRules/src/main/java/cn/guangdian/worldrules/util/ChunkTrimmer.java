package cn.guangdian.worldrules.util;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.worldrules.GuangDianWorldRules;
import cn.guangdian.worldrules.model.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.WorldBorder;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 区块裁剪工具
 * 删除指定区域外的所有区块，并设置世界边界防止生成新区块
 */
public class ChunkTrimmer {

    private final GuangDianWorldRules plugin;
    private final MiniMessageService miniMessage;

    public ChunkTrimmer(GuangDianWorldRules plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessageService.getInstance();
    }

    /**
     * 执行区块裁剪
     * @param world 世界
     * @param region 保留区域
     * @param deleteFiles 是否删除区块文件
     * @return 裁剪结果
     */
    public TrimResult trimWorld(World world, ProtectedRegion region, boolean deleteFiles) {
        TrimResult result = new TrimResult();
        result.worldName = world.getName();
        result.regionName = region.getName();

        // 1. 设置世界边界
        setWorldBorder(world, region);
        result.borderSet = true;

        // 2. 计算需要保留的区块范围
        int minChunkX = region.getMinX() >> 4;
        int maxChunkX = region.getMaxX() >> 4;
        int minChunkZ = region.getMinZ() >> 4;
        int maxChunkZ = region.getMaxZ() >> 4;

        result.totalChunksInRange = (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);

        // 3. 卸载并删除区域外的区块
        Set<Long> keepChunks = new HashSet<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                keepChunks.add(chunkKey(cx, cz));
            }
        }

        // 遍历所有已加载的区块
        for (Chunk chunk : world.getLoadedChunks()) {
            long key = chunkKey(chunk.getX(), chunk.getZ());
            if (!keepChunks.contains(key)) {
                // 卸载区块
                if (world.unloadChunk(chunk.getX(), chunk.getZ(), false)) {
                    result.unloadedChunks++;
                }
            }
        }

        // 4. 删除区块文件（如果需要）
        if (deleteFiles) {
            result.deletedFiles = deleteChunkFiles(world, keepChunks);
        }

        return result;
    }

    /**
     * 设置世界边界
     */
    private void setWorldBorder(World world, ProtectedRegion region) {
        WorldBorder border = world.getWorldBorder();

        // 计算中心点
        double centerX = (region.getMinX() + region.getMaxX()) / 2.0;
        double centerZ = (region.getMinZ() + region.getMaxZ()) / 2.0;

        // 计算需要的直径（加上一些缓冲）
        int width = region.getMaxX() - region.getMinX() + 1;
        int depth = region.getMaxZ() - region.getMinZ() + 1;
        double diameter = Math.max(width, depth) + 32; // 额外16格缓冲

        border.setCenter(centerX, centerZ);
        border.setSize(diameter);
        border.setDamageAmount(0); // 不造成伤害
        border.setDamageBuffer(0);
        border.setWarningDistance(0);
        border.setWarningTime(0);
    }

    /**
     * 删除区块文件
     */
    private int deleteChunkFiles(World world, Set<Long> keepChunks) {
        int deleted = 0;

        // 获取世界文件夹
        File worldFolder = world.getWorldFolder();
        File regionFolder = new File(worldFolder, "region");

        if (!regionFolder.exists()) {
            return 0;
        }

        // 遍历所有 .mca 文件
        File[] mcaFiles = regionFolder.listFiles((dir, name) -> name.endsWith(".mca"));
        if (mcaFiles == null) {
            return 0;
        }

        for (File mcaFile : mcaFiles) {
            // 解析文件名: r.X.Z.mca
            String name = mcaFile.getName();
            if (!name.startsWith("r.") || !name.endsWith(".mca")) {
                continue;
            }

            try {
                String[] parts = name.substring(2, name.length() - 4).split("\\.");
                if (parts.length != 2) {
                    continue;
                }

                int regionX = Integer.parseInt(parts[0]);
                int regionZ = Integer.parseInt(parts[1]);

                // 检查这个区域文件是否包含需要保留的区块
                // 每个 region 文件包含 32x32 个区块
                boolean shouldKeep = false;
                for (int cx = regionX * 32; cx < (regionX + 1) * 32; cx++) {
                    for (int cz = regionZ * 32; cz < (regionZ + 1) * 32; cz++) {
                        if (keepChunks.contains(chunkKey(cx, cz))) {
                            shouldKeep = true;
                            break;
                        }
                    }
                    if (shouldKeep) break;
                }

                if (!shouldKeep) {
                    // 删除文件
                    if (mcaFile.delete()) {
                        deleted++;
                        plugin.getLogger().info("删除区块文件: " + name);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("处理区块文件失败: " + name + " - " + e.getMessage());
            }
        }

        return deleted;
    }

    /**
     * 生成区块键
     */
    private long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /**
     * 裁剪结果
     */
    public static class TrimResult {
        public String worldName;
        public String regionName;
        public boolean borderSet;
        public int totalChunksInRange;
        public int unloadedChunks;
        public int deletedFiles;

        @Override
        public String toString() {
            return String.format(
                    "TrimResult{world=%s, region=%s, border=%s, chunksInRange=%d, unloaded=%d, deleted=%d}",
                    worldName, regionName, borderSet, totalChunksInRange, unloadedChunks, deletedFiles
            );
        }
    }
}
