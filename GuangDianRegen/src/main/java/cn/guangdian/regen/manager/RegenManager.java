package cn.guangdian.regen.manager;

import cn.guangdian.regen.GuangDianRegen;
import cn.guangdian.regen.model.RegenBlock;
import cn.guangdian.regen.model.RegenRegion;
import cn.guangdian.regen.model.RegenType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * 刷新管理器
 */
public class RegenManager {

    private final GuangDianRegen plugin;
    private final RegionManager regionManager;

    // 待刷新方块队列: Location -> RegenTask
    private final Map<Location, RegenTask> pendingTasks = new ConcurrentHashMap<>();

    public RegenManager(GuangDianRegen plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        startRegenTask();
    }

    /**
     * 处理方块破坏
     */
    public void handleBlockBreak(Player player, Block block) {
        Location location = block.getLocation();
        Material material = block.getType();

        // 查找区域
        RegenRegion region = regionManager.getRegionAt(location);
        if (region == null) {
            plugin.getLogger().info("玩家 " + player.getName() + " 破坏方块 " + material.name() +
                " 在 " + location.getWorld().getName() + " " + location.getBlockX() + "," +
                location.getBlockY() + "," + location.getBlockZ() + " - 未找到区域");
            return;
        }

        plugin.getLogger().info("玩家 " + player.getName() + " 在区域 " + region.getName() + " 破坏方块 " + material.name());

        // 处理矿场/农场类型的方块
        if (region.getType() == RegenType.MINE || region.getType() == RegenType.FARM) {
            if (!region.hasBlockConfig(material)) {
                plugin.getLogger().info("区域 " + region.getName() + " 没有配置方块 " + material.name());
                return;
            }

            RegenBlock regenBlock = region.getBlockConfig(material);

            // 处理掉落
            handleDrops(player, location, regenBlock);

            // 替换为临时方块
            block.setType(regenBlock.getReplace());

            // 添加到刷新队列
            RegenTask task = new RegenTask(location, material, regenBlock.getDelay());
            pendingTasks.put(location, task);

            plugin.getLogger().info("方块 " + material.name() + " 已加入刷新队列，延迟 " + regenBlock.getDelay() + " 秒");
        }
        // 处理林场类型的树木
        else if (region.getType() == RegenType.FOREST) {
            RegenRegion.TreeConfig treeConfig = region.getTreeConfig(material);
            if (treeConfig == null) {
                return;
            }

            // 记录树木底部位置
            Location treeBase = findTreeBase(block);

            // 替换为空气（树木被砍伐）
            block.setType(Material.AIR);

            // 添加到刷新队列
            RegenTask task = new RegenTask(treeBase, material, treeConfig.getDelay());
            pendingTasks.put(treeBase, task);
        }
    }

    /**
     * 查找树木底部位置
     */
    private Location findTreeBase(Block block) {
        Location loc = block.getLocation().clone();
        while (loc.getBlockY() > 0) {
            Block below = loc.clone().subtract(0, 1, 0).getBlock();
            if (below.getType() == Material.AIR || below.getType().name().contains("GRASS") ||
                below.getType().name().contains("DIRT") || below.getType().name().contains("STONE")) {
                return loc;
            }
            loc.subtract(0, 1, 0);
        }
        return block.getLocation();
    }

    /**
     * 处理掉落
     */
    private void handleDrops(Player player, Location location, RegenBlock regenBlock) {
        for (RegenBlock.DropConfig drop : regenBlock.getDrops()) {
            // 概率判定
            if (Math.random() >= drop.getChance()) {
                continue;
            }

            int amount = drop.getRandomAmount();

            switch (drop.getType()) {
                case ITEM:
                    try {
                        Material material = Material.valueOf(drop.getValue().toUpperCase());
                        ItemStack item = new ItemStack(material, amount);
                        player.getInventory().addItem(item);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("无效的物品类型: " + drop.getValue());
                    }
                    break;

                case EXPERIENCE:
                    player.giveExp(amount);
                    break;

                case COMMAND:
                    String command = drop.getValue()
                            .replace("{player}", player.getName())
                            .replace("{x}", String.valueOf(location.getBlockX()))
                            .replace("{y}", String.valueOf(location.getBlockY()))
                            .replace("{z}", String.valueOf(location.getBlockZ()));
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    break;
            }
        }
    }

    /**
     * 启动刷新任务
     */
    private void startRegenTask() {
        plugin.getLogger().info("启动方块刷新任务...");

        // 使用 AsyncScheduler 定时检查刷新队列
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, scheduledTask -> {
            long currentTime = System.currentTimeMillis();
            int blocksPerTick = plugin.getConfig().getInt("settings.blocks_per_tick", 10);
            int processed = 0;

            // 检查待刷新方块
            java.util.List<Location> toRemove = new java.util.ArrayList<>();

            for (Map.Entry<Location, RegenTask> entry : pendingTasks.entrySet()) {
                if (processed >= blocksPerTick) {
                    break;
                }

                Location location = entry.getKey();
                RegenTask task = entry.getValue();

                // 检查是否到达刷新时间
                if (currentTime >= task.getRegenTime()) {
                    // 在主线程执行方块刷新
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        regenerateBlock(location, task.getOriginalMaterial());
                    });

                    toRemove.add(location);
                    processed++;

                    plugin.getLogger().info("刷新方块 " + task.getOriginalMaterial().name() +
                        " 在 " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
                }
            }

            // 移除已处理的方块
            for (Location loc : toRemove) {
                pendingTasks.remove(loc);
            }
        }, 0, 1, TimeUnit.SECONDS);

        plugin.getLogger().info("方块刷新任务已启动");
    }

    /**
     * 刷新方块
     */
    private void regenerateBlock(Location location, Material material) {
        World world = location.getWorld();
        if (world == null) {
            plugin.getLogger().warning("刷新失败: 世界为空");
            return;
        }

        // 检查区块是否加载
        if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            plugin.getLogger().warning("刷新失败: 区块未加载 " + location.getBlockX() + "," + location.getBlockZ());
            return;
        }

        // 查找区域配置
        RegenRegion region = regionManager.getRegionAt(location);

        // 根据区域类型决定刷新方式
        if (region != null && region.getType() == RegenType.FOREST && material.name().contains("LOG")) {
            // 只有林场类型才生成树木
            generateTree(location, material);
        } else {
            // 其他类型直接恢复方块
            Block block = world.getBlockAt(location);
            Material beforeType = block.getType();
            block.setType(material);
            plugin.getLogger().info("方块刷新完成: " + beforeType.name() + " -> " + material.name() +
                " 在 " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
        }

        // 播放粒子效果
        if (plugin.getConfig().getBoolean("settings.particle_preview", true)) {
            playRegenEffect(location);
        }
    }

    /**
     * 生成树木
     */
    private void generateTree(Location base, Material logMaterial) {
        World world = base.getWorld();
        if (world == null) return;

        // 查找区域配置
        RegenRegion region = regionManager.getRegionAt(base);
        if (region == null) return;

        RegenRegion.TreeConfig treeConfig = region.getTreeConfig(logMaterial);
        if (treeConfig == null) return;

        int height = treeConfig.getMinHeight() +
                     (int) (Math.random() * (treeConfig.getMaxHeight() - treeConfig.getMinHeight() + 1));

        // 获取树叶材质
        Material leavesMaterial = getLeavesMaterial(logMaterial);

        // 生成树干
        for (int y = 0; y < height; y++) {
            Block logBlock = world.getBlockAt(base.getBlockX(), base.getBlockY() + y, base.getBlockZ());
            logBlock.setType(logMaterial);
        }

        // 生成树叶
        int leavesStart = height - 2;
        for (int y = leavesStart; y <= height + 1; y++) {
            int radius = (y == height + 1) ? 1 : 2;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && z == 0 && y < height) continue; // 树干位置
                    if (Math.abs(x) == radius && Math.abs(z) == radius && Math.random() < 0.5) continue; // 随机减少角落树叶

                    Block leavesBlock = world.getBlockAt(
                            base.getBlockX() + x,
                            base.getBlockY() + y,
                            base.getBlockZ() + z
                    );
                    if (leavesBlock.getType() == Material.AIR) {
                        leavesBlock.setType(leavesMaterial);
                    }
                }
            }
        }
    }

    /**
     * 获取树叶材质
     */
    private Material getLeavesMaterial(Material logMaterial) {
        String logName = logMaterial.name();
        if (logName.contains("OAK")) return Material.OAK_LEAVES;
        if (logName.contains("SPRUCE")) return Material.SPRUCE_LEAVES;
        if (logName.contains("BIRCH")) return Material.BIRCH_LEAVES;
        if (logName.contains("JUNGLE")) return Material.JUNGLE_LEAVES;
        if (logName.contains("ACACIA")) return Material.ACACIA_LEAVES;
        if (logName.contains("DARK_OAK")) return Material.DARK_OAK_LEAVES;
        if (logName.contains("MANGROVE")) return Material.MANGROVE_LEAVES;
        if (logName.contains("CHERRY")) return Material.CHERRY_LEAVES;
        return Material.OAK_LEAVES; // 默认
    }

    /**
     * 播放刷新特效
     */
    private void playRegenEffect(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        // 使用粒子效果
        world.spawnParticle(
                org.bukkit.Particle.HAPPY_VILLAGER,
                location.clone().add(0.5, 0.5, 0.5),
                10,
                0.5, 0.5, 0.5, 0
        );
    }

    /**
     * 清理所有待刷新方块
     */
    public void clearAllTasks() {
        pendingTasks.clear();
    }

    /**
     * 获取待刷新方块数量
     */
    public int getPendingCount() {
        return pendingTasks.size();
    }

    /**
     * 刷新任务
     */
    private static class RegenTask {
        private final Location location;
        private final Material originalMaterial;
        private final long regenTime;

        public RegenTask(Location location, Material originalMaterial, int delaySeconds) {
            this.location = location;
            this.originalMaterial = originalMaterial;
            this.regenTime = System.currentTimeMillis() + (delaySeconds * 1000L);
        }

        public Location getLocation() {
            return location;
        }

        public Material getOriginalMaterial() {
            return originalMaterial;
        }

        public long getRegenTime() {
            return regenTime;
        }
    }
}
