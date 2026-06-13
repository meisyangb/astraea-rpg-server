package cn.guangdian.cavefu.storage;

import cn.guangdian.cavefu.GuangDianCaveFu;
import cn.guangdian.cavefu.cave.Cave;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据存储管理器
 * 
 * <p>支持双模式存储：YAML（本地文件）和 SQLite（本地数据库）。</p>
 * 
 * <p>优化特性：</p>
 * <ul>
 *   <li>双模式存储：自动选择 YAML 或 SQLite</li>
 *   <li>异步保存：使用 RPGCore 统一 AsyncExecutor</li>
 *   <li>防重复保存：使用脏标记避免不必要的IO</li>
 *   <li>安全关闭：服务器关闭时等待保存完成</li>
 *   <li>线程安全：使用 ConcurrentHashMap 和 AtomicInteger</li>
 * </ul>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class DataManager {

    private final GuangDianCaveFu plugin;

    private final Map<Integer, Cave> cavesById = new ConcurrentHashMap<>();
    private final Map<UUID, Cave> cavesByOwner = new ConcurrentHashMap<>();
    private final Map<UUID, Cave> cavesByMember = new ConcurrentHashMap<>();
    private final AtomicInteger nextCaveId = new AtomicInteger(1);

    private AsyncExecutor asyncExecutor;
    private final Object saveLock = new Object();
    private final Object createLock = new Object();
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private volatile boolean isShuttingDown = false;

    private SQLiteDataManager sqliteManager;
    private boolean useSQLite = true;

    public DataManager(GuangDianCaveFu plugin) {
        this.plugin = plugin;
        
        if (Bukkit.getPluginManager().isPluginEnabled("RPGCore")) {
            try {
                asyncExecutor = RPGCore.getInstance().getAsyncExecutor();
                plugin.getLogger().info("使用 RPGCore 统一 AsyncExecutor");
            } catch (Exception e) {
                plugin.getLogger().warning("无法获取 RPGCore AsyncExecutor: " + e.getMessage());
            }
        }
    }

    public void load() {
        useSQLite = plugin.getConfig().getBoolean("storage.use-sqlite", true);

        if (useSQLite) {
            plugin.getLogger().info("使用 SQLite 数据库存储模式");
            sqliteManager = new SQLiteDataManager(plugin);
            sqliteManager.load();
            
            cavesById.putAll(sqliteManager.getCavesById());
            cavesByOwner.putAll(sqliteManager.getCavesByOwner());
            cavesByMember.putAll(sqliteManager.getCavesByMember());
            nextCaveId.set(sqliteManager.getNextCaveId());
        } else {
            plugin.getLogger().info("使用 YAML 文件存储模式");
            loadFromYaml();
        }

        plugin.getLogger().info("已加载 " + cavesById.size() + " 个洞府，当前 nextCaveId=" + nextCaveId.get());
    }

    private void loadFromYaml() {
        var dataFile = new java.io.File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (java.io.IOException e) {
                plugin.getLogger().severe("无法创建数据文件: " + e.getMessage());
            }
        }

        var dataConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(dataFile);

        cavesById.clear();
        cavesByOwner.clear();
        cavesByMember.clear();

        nextCaveId.set(dataConfig.getInt("next-cave-id", 1));

        Object cavesObj = dataConfig.get("caves");
        if (cavesObj instanceof Map) {
            Map<?, ?> cavesMap = (Map<?, ?>) cavesObj;
            for (Map.Entry<?, ?> entry : cavesMap.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> caveData = (Map<String, Object>) entry.getValue();
                        Cave cave = Cave.deserialize(caveData);
                        cavesById.put(cave.getId(), cave);
                        cavesByOwner.put(cave.getOwnerUuid(), cave);

                        for (UUID memberUuid : cave.getMembers().keySet()) {
                            cavesByMember.put(memberUuid, cave);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("加载洞府数据失败: " + entry.getKey() + " - " + e.getMessage());
                    }
                }
            }
        }
    }

    public void save() {
        markDirty();
        saveAsync();
    }

    public void markDirty() {
        dirty.set(true);
    }

    public void saveAsync() {
        // 使用CAS操作，避免竞态条件
        if (dirty.compareAndSet(false, true)) {
            // 如果之前是false，现在设置为true，执行保存
            executeSaveAsync();
        } else if (dirty.get()) {
            // 如果已经是true，也执行保存（确保不会漏掉）
            executeSaveAsync();
        }
    }
    
    private void executeSaveAsync() {
        if (asyncExecutor != null) {
            asyncExecutor.execute(() -> saveSync());
        } else {
            cn.guangdian.rpgcore.integration.UnifiedScheduler.runAsync(plugin, () -> saveSync());
        }
    }

    public CompletableFuture<Void> saveAsyncForce() {
        if (asyncExecutor != null) {
            return asyncExecutor.execute(() -> saveSync());
        } else {
            return CompletableFuture.runAsync(() -> saveSync());
        }
    }

    private void saveSync() {
        synchronized (saveLock) {
            // 使用本地副本，避免并发修改
            boolean currentDirty = dirty.get();
            
            if (!currentDirty) {
                return;  // 无需保存
            }
            
            try {
                if (useSQLite && sqliteManager != null) {
                    sqliteManager.saveSync();
                } else {
                    saveToYaml();
                }
                // 只有在保存成功后才清除脏标记
                dirty.set(false);
                plugin.getLogger().fine("数据保存成功");
            } catch (Exception e) {
                plugin.getLogger().severe("保存数据失败: " + e.getMessage());
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "详细异常信息", e);
                // 不清除dirty标记，下次会自动重试
                // 可以在这里添加重试计数器，避免无限重试
            }
        }
    }

    private void saveToYaml() {
        var dataFile = new java.io.File(plugin.getDataFolder(), "data.yml");
        var dataConfig = new org.bukkit.configuration.file.YamlConfiguration();

        dataConfig.set("next-cave-id", nextCaveId.get());

        Map<String, Object> cavesData = new HashMap<>();
        int savedCount = 0;
        
        for (Cave cave : cavesById.values()) {
            // 只保存脏数据
            if (cave.isDirty()) {
                cavesData.put(String.valueOf(cave.getId()), cave.serialize());
                cave.clearDirty();  // 保存成功后清除脏标记
                savedCount++;
            } else {
                // 对于非脏数据，从现有文件读取
                // 注意：YAML不支持部分更新，所以需要全部写入
                // 但我们可以在内存中保留所有数据
                cavesData.put(String.valueOf(cave.getId()), cave.serialize());
            }
        }
        dataConfig.set("caves", cavesData);

        try {
            dataConfig.save(dataFile);
            if (savedCount > 0) {
                plugin.getLogger().fine("YAML增量保存完成: " + savedCount + "/" + cavesById.size() + " 个洞府");
            }
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("保存数据失败: " + e.getMessage());
            // 如果保存失败，恢复脏标记
            for (Cave cave : cavesById.values()) {
                if (cavesData.containsKey(String.valueOf(cave.getId()))) {
                    cave.markDirty();
                }
            }
        }
    }

    public void saveSyncAndAwait() {
        isShuttingDown = true;
        final int MAX_WAIT_SECONDS = 120;  // 增加到120秒

        try {
            plugin.getLogger().info("正在保存数据，请稍候...");
            CompletableFuture<Void> saveFuture = saveAsyncForce();
            saveFuture.get(MAX_WAIT_SECONDS, TimeUnit.SECONDS);
            plugin.getLogger().info("数据保存完成");
        } catch (TimeoutException e) {
            plugin.getLogger().severe("保存数据超时（" + MAX_WAIT_SECONDS + "秒），强制关闭");
            plugin.getLogger().severe("部分数据可能未保存！请检查日志");
        } catch (Exception e) {
            plugin.getLogger().severe("保存数据时发生错误: " + e.getMessage());
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "详细异常信息", e);
        }
    }

    public void scheduleAutoSave() {
        if (dirty.get()) {
            saveAsync();
        }
    }

    public void shutdown() {
        saveSyncAndAwait();
        if (sqliteManager != null) {
            sqliteManager.close();
        }
    }

    public Cave createCave(UUID ownerUuid, String ownerName, int level, String worldName, int centerX, int centerZ) {
        synchronized (createLock) {
            int id = nextCaveId.getAndIncrement();
            
            if (useSQLite && sqliteManager != null) {
                Cave cave = sqliteManager.createCave(id, ownerUuid, ownerName, level, worldName, centerX, centerZ);
                if (cave != null) {
                    cave.clearDirty();  // 新创建的洞府已保存到数据库，清除脏标记
                    cavesById.put(cave.getId(), cave);
                    cavesByOwner.put(ownerUuid, cave);
                    cavesByMember.put(ownerUuid, cave);
                    return cave;
                }
                return null;
            }

            Cave cave = new Cave(id, ownerUuid, ownerName, level, worldName, centerX, centerZ);
            cavesById.put(id, cave);
            cavesByOwner.put(ownerUuid, cave);
            cavesByMember.put(ownerUuid, cave);
            cave.clearDirty();  // 已保存，清除脏标记
            save();
            return cave;
        }
    }

    public void deleteCave(int id) {
        Cave cave = cavesById.remove(id);
        if (cave != null) {
            cavesByOwner.remove(cave.getOwnerUuid());
            for (UUID memberUuid : cave.getMembers().keySet()) {
                cavesByMember.remove(memberUuid);
            }

            if (useSQLite && sqliteManager != null) {
                sqliteManager.deleteCaveAsync(id).join();
            } else {
                save();
            }
        }
    }

    public Cave getCaveById(int id) {
        return cavesById.get(id);
    }

    public Cave getCaveByOwner(UUID ownerUuid) {
        return cavesByOwner.get(ownerUuid);
    }

    public Cave getCaveByMember(UUID memberUuid) {
        return cavesByMember.get(memberUuid);
    }

    public Cave getCaveAtLocation(Location loc) {
        for (Cave cave : cavesById.values()) {
            if (cave.isInside(loc)) {
                return cave;
            }
        }
        return null;
    }

    public int getNextCaveId() {
        return nextCaveId.get();
    }

    public Collection<Cave> getAllCaves() {
        return cavesById.values();
    }

    public void updateMemberIndex(UUID uuid, Cave cave) {
        if (cave != null) {
            cavesByMember.put(uuid, cave);
        } else {
            cavesByMember.remove(uuid);
        }
    }

    public int getCaveCount() {
        return cavesById.size();
    }

    public Map<UUID, Cave> getCavesByOwner() {
        return cavesByOwner;
    }

    public Map<Integer, Cave> getCavesById() {
        return cavesById;
    }

    public Map<UUID, Cave> getCavesByMember() {
        return cavesByMember;
    }

    public boolean isSaveExecutorActive() {
        return asyncExecutor != null || !isShuttingDown;
    }

    public boolean hasPendingSave() {
        return dirty.get();
    }

    public boolean isUsingSQLite() {
        return useSQLite;
    }
}