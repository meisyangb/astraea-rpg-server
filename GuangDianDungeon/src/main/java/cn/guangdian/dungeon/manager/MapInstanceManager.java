package cn.guangdian.dungeon.manager;

import cn.guangdian.dungeon.GuangDianDungeon;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 地图实例管理器 — 枚举法，每个地图最多 3 个并发实例
 *
 * 设计原则：
 * 1. 地图存储在 plugins/GuangDianDungeon/map/ 下，每个子文件夹 = 一张地图
 * 2. 每个地图最多 3 个并发实例（CAS 原子计数器保证，无锁竞争）
 * 3. 实例生命周期：复制 → 使用 → 卸载删除
 * 4. 事件驱动：副本完成/失败时调用 destroyInstance 销毁实例
 * 5. 定时保底：周期性清理孤儿世界 + 残留文件夹
 */
public class MapInstanceManager {

    private final GuangDianDungeon plugin;
    private static final int MAX_INSTANCES_PER_MAP = 3;
    private static final String INSTANCE_PREFIX = "dungeon_";
    private static final long CLEANUP_INTERVAL_TICKS = 1200L; // 60秒

    // mapName -> 活跃实例集合（ConcurrentHashMap.newKeySet 保证 O(1) 查找/删除 + 线程安全）
    private final Map<String, Set<String>> activeInstances;
    // mapName -> 原子计数器（CAS 替代 synchronized，无锁竞争）
    private final Map<String, AtomicInteger> instanceCounters;
    // 定时保底任务
    private BukkitTask cleanupTask;

    public MapInstanceManager(GuangDianDungeon plugin) {
        this.plugin = plugin;
        this.activeInstances = new ConcurrentHashMap<>();
        this.instanceCounters = new ConcurrentHashMap<>();
    }

    /**
     * 启动定时保底清理任务
     * 每隔固定时间检查并清理孤儿世界和残留文件夹
     */
    public void startPeriodicCleanup() {
        if (cleanupTask != null) return;
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            cleanupOrphanInstances();
        }, CLEANUP_INTERVAL_TICKS, CLEANUP_INTERVAL_TICKS);
        plugin.getLogger().info("已启动定时保底清理任务，间隔: " + (CLEANUP_INTERVAL_TICKS / 20) + "秒");
    }

    /**
     * 停止定时保底清理任务
     */
    public void stopPeriodicCleanup() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    /**
     * 获取所有可用地图名称
     */
    public List<String> getAvailableMaps() {
        File mapDir = new File(plugin.getDataFolder(), "map");
        if (!mapDir.exists() || !mapDir.isDirectory()) return Collections.emptyList();

        File[] dirs = mapDir.listFiles(File::isDirectory);
        if (dirs == null) return Collections.emptyList();

        List<String> names = new ArrayList<>();
        for (File dir : dirs) {
            if (new File(dir, "level.dat").exists()) {
                names.add(dir.getName());
            }
        }
        return names;
    }

    /**
     * 检查地图文件夹是否存在
     */
    public boolean mapExists(String mapName) {
        File mapDir = new File(plugin.getDataFolder(), "map/" + mapName);
        return mapDir.exists() && mapDir.isDirectory() && new File(mapDir, "level.dat").exists();
    }

    /**
     * 获取地图当前活跃实例数
     */
    public int getActiveInstanceCount(String mapName) {
        AtomicInteger count = instanceCounters.get(mapName);
        return count != null ? count.get() : 0;
    }

    /**
     * 创建副本实例（CAS 原子计数器保证并发安全，无锁竞争）
     *
     * 流程：
     * 1. CAS 原子预占名额（如果已满立即返回 null）
     * 2. 复制地图文件夹
     * 3. 删除 uid.dat 避免世界冲突
     * 4. 加载世界
     * 5. 失败时自动回滚计数器
     *
     * @param mapName 地图名称（对应 map/ 下的文件夹名）
     * @return 实例世界名，如果地图已满返回 null
     */
    public String createInstance(String mapName) {
        AtomicInteger counter = instanceCounters.computeIfAbsent(mapName, k -> new AtomicInteger(0));

        // CAS 自旋：尝试将计数 +1，如果已达上限则失败
        // 比 synchronized 更轻量，不会阻塞其他线程
        int current;
        do {
            current = counter.get();
            if (current >= MAX_INSTANCES_PER_MAP) {
                plugin.getLogger().warning("地图 " + mapName + " 已达到最大实例数 (" + MAX_INSTANCES_PER_MAP + ")");
                return null;
            }
        } while (!counter.compareAndSet(current, current + 1));

        // 生成实例名
        String instanceName = INSTANCE_PREFIX + mapName + "_" + UUID.randomUUID().toString().substring(0, 8);

        // 复制地图文件夹
        File mapDir = new File(plugin.getDataFolder(), "map/" + mapName);
        File instanceDir = new File(Bukkit.getWorldContainer(), instanceName);

        if (!mapDir.exists()) {
            plugin.getLogger().severe("地图文件夹不存在: " + mapDir.getAbsolutePath());
            counter.decrementAndGet();
            return null;
        }

        try {
            copyWorldFolder(mapDir.toPath(), instanceDir.toPath());
        } catch (IOException e) {
            plugin.getLogger().severe("复制地图失败: " + mapName + " - " + e.getMessage());
            counter.decrementAndGet();
            return null;
        }

        // 删除 uid.dat（避免世界冲突）
        deleteUidFile(instanceDir);

        // 加载世界（必须在主线程）
        World world = new WorldCreator(instanceName).createWorld();
        if (world == null) {
            plugin.getLogger().severe("加载实例世界失败: " + instanceName);
            deleteWorldFolder(instanceDir);
            counter.decrementAndGet();
            return null;
        }

        // 记录活跃实例
        activeInstances.computeIfAbsent(mapName, k -> ConcurrentHashMap.newKeySet()).add(instanceName);
        plugin.getLogger().info("创建副本实例: " + instanceName + " (地图: " + mapName + ", 当前实例数: " + counter.get() + "/" + MAX_INSTANCES_PER_MAP + ")");
        return instanceName;
    }

    /**
     * 销毁副本实例（卸载世界 → 异步删除文件夹）
     *
     * 事件驱动：副本完成/失败/超时时调用
     */
    public void destroyInstance(String instanceName) {
        if (instanceName == null) return;

        // 卸载世界（必须在主线程）
        World world = Bukkit.getWorld(instanceName);
        if (world != null) {
            for (Player player : world.getPlayers()) {
                teleportToExitWorld(player);
            }
            Bukkit.unloadWorld(world, false);
        }

        // 异步删除世界文件夹（不阻塞主线程）
        File worldDir = new File(Bukkit.getWorldContainer(), instanceName);
        if (worldDir.exists()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                deleteWorldFolder(worldDir);
            });
        }

        // 从追踪中移除（O(1) 操作）
        String mapName = getMapNameFromInstance(instanceName);
        if (mapName != null) {
            Set<String> instances = activeInstances.get(mapName);
            if (instances != null) {
                instances.remove(instanceName);
            }
            AtomicInteger counter = instanceCounters.get(mapName);
            if (counter != null) {
                counter.decrementAndGet();
            }
        }

        plugin.getLogger().info("销毁副本实例: " + instanceName);
    }

    /**
     * 获取实例所属的地图名称
     */
    public String getMapNameFromInstance(String instanceName) {
        if (instanceName == null || !instanceName.startsWith(INSTANCE_PREFIX)) return null;
        String withoutPrefix = instanceName.substring(INSTANCE_PREFIX.length());
        int lastUnderscore = withoutPrefix.lastIndexOf('_');
        if (lastUnderscore <= 0) return null;
        return withoutPrefix.substring(0, lastUnderscore);
    }

    /**
     * 清理所有活跃实例（插件关闭时调用）
     */
    public void cleanupAll() {
        for (String mapName : new ArrayList<>(activeInstances.keySet())) {
            Set<String> instances = activeInstances.get(mapName);
            if (instances != null) {
                for (String instanceName : new ArrayList<>(instances)) {
                    destroyInstance(instanceName);
                }
            }
        }
        activeInstances.clear();
        instanceCounters.clear();
    }

    /**
     * 清理孤儿实例（服务端崩溃残留 / 定时保底）
     *
     * 检测条件：
     * 1. 文件夹名以 "dungeon_" 开头
     * 2. 该世界未被 Bukkit 加载
     * 3. 存在超过 30 秒（避免误删正在创建中的实例）
     */
    public void cleanupOrphanInstances() {
        File worldContainer = Bukkit.getWorldContainer();
        File[] worldDirs = worldContainer.listFiles(File::isDirectory);
        if (worldDirs == null) return;

        long now = System.currentTimeMillis();
        int cleaned = 0;

        for (File dir : worldDirs) {
            String name = dir.getName();
            if (!name.startsWith(INSTANCE_PREFIX)) continue;
            if (Bukkit.getWorld(name) != null) continue; // 已加载的世界跳过

            // 检查文件夹最后修改时间，避免误删正在创建的实例
            long lastModified = dir.lastModified();
            if (now - lastModified < 30000) continue; // 30秒内的跳过

            try {
                deleteWorldFolder(dir);
                cleaned++;
                plugin.getLogger().info("清理残留世界: " + name);
            } catch (Exception e) {
                plugin.getLogger().warning("清理残留世界失败: " + name + " - " + e.getMessage());
            }
        }

        // 同步计数器与实际状态（修复崩溃导致的计数偏移）
        syncCounters();

        if (cleaned > 0) {
            plugin.getLogger().info("共清理 " + cleaned + " 个残留副本世界");
        }
    }

    /**
     * 将玩家传送到出口世界
     */
    public void teleportToExitWorld(Player player) {
        String exitWorldName = plugin.getConfig().getString("exit-world.world", "world");
        double x = plugin.getConfig().getDouble("exit-world.x", 0);
        double y = plugin.getConfig().getDouble("exit-world.y", 64);
        double z = plugin.getConfig().getDouble("exit-world.z", 0);
        float yaw = (float) plugin.getConfig().getDouble("exit-world.yaw", 0);
        float pitch = (float) plugin.getConfig().getDouble("exit-world.pitch", 0);

        World exitWorld = Bukkit.getWorld(exitWorldName);
        if (exitWorld == null) {
            exitWorld = Bukkit.getWorlds().get(0);
        }

        player.teleport(new org.bukkit.Location(exitWorld, x, y, z, yaw, pitch));
    }

    // ========== 内部方法 ==========

    /**
     * 同步计数器与实际加载的世界状态
     * 修复服务端崩溃重启后计数器与实际世界不匹配的问题
     */
    private void syncCounters() {
        for (Map.Entry<String, AtomicInteger> entry : instanceCounters.entrySet()) {
            String mapName = entry.getKey();
            AtomicInteger counter = entry.getValue();

            // 统计实际加载的实例世界数
            int actualCount = 0;
            Set<String> instances = activeInstances.get(mapName);
            if (instances != null) {
                for (String instanceName : instances) {
                    if (Bukkit.getWorld(instanceName) != null) {
                        actualCount++;
                    } else {
                        // 世界未加载，从追踪中移除
                        instances.remove(instanceName);
                    }
                }
            }

            // 如果计数器与实际不符，修正
            if (counter.get() != actualCount) {
                plugin.getLogger().warning("修正地图 " + mapName + " 的实例计数: " + counter.get() + " -> " + actualCount);
                counter.set(actualCount);
            }
        }
    }

    /**
     * 复制世界文件夹（NIO walkFileTree，跳过 uid.dat 和 session.lock）
     */
    private void copyWorldFolder(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                if (fileName.equals("uid.dat") || fileName.equals("session.lock")) {
                    return FileVisitResult.CONTINUE;
                }
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteUidFile(File worldDir) {
        new File(worldDir, "uid.dat").delete();
        new File(worldDir, "session.lock").delete();
    }

    /**
     * 递归删除世界文件夹
     */
    private void deleteWorldFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteWorldFolder(file);
                }
            }
        }
        folder.delete();
    }
}
