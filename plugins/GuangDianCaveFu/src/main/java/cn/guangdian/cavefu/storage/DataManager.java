package cn.guangdian.cavefu.storage;

import cn.guangdian.cavefu.GuangDianCaveFu;
import cn.guangdian.cavefu.cave.Cave;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.database.CoreDatabase;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 数据存储管理器
 * 
 * <p>支持双模式存储：YAML（本地文件）和 MySQL（数据库）。</p>
 * <p>自动根据 RPGCore 数据库状态选择存储方式。</p>
 * 
 * <p>优化特性：</p>
 * <ul>
 *   <li>双模式存储：自动选择 YAML 或 MySQL</li>
 *   <li>异步保存：使用 RPGCore 统一 AsyncExecutor</li>
 *   <li>防重复保存：使用脏标记避免不必要的IO</li>
 *   <li>安全关闭：服务器关闭时等待保存完成</li>
 *   <li>线程安全：使用 RPGCore PlayerLockManager</li>
 * </ul>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class DataManager {

    private final GuangDianCaveFu plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;

    private final Map<Integer, Cave> cavesById = new ConcurrentHashMap<>();
    private final Map<UUID, Cave> cavesByOwner = new ConcurrentHashMap<>();
    private final Map<UUID, Cave> cavesByMember = new ConcurrentHashMap<>();
    private int nextCaveId = 1;

    // 使用 RPGCore 统一服务
    private AsyncExecutor asyncExecutor;
    private final Object saveLock = new Object();
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private volatile boolean isShuttingDown = false;

    private DatabaseDataManager databaseDataManager;
    private boolean useDatabase = false;

    public DataManager(GuangDianCaveFu plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        
        // 优先使用 RPGCore AsyncExecutor
        if (Bukkit.getPluginManager().isPluginEnabled("RPGCore")) {
            try {
                asyncExecutor = RPGCore.getInstance().getAsyncExecutor();
                plugin.getLogger().info("使用 RPGCore 统一 AsyncExecutor");
            } catch (Exception e) {
                plugin.getLogger().warning("无法获取 RPGCore AsyncExecutor: " + e.getMessage());
            }
        }
    }

    /**
     * 加载数据（自动选择存储模式）
     */
    public void load() {
        useDatabase = CoreDatabase.isEnabled();

        if (useDatabase) {
            plugin.getLogger().info("检测到数据库已启用，使用 MySQL 存储模式");
            databaseDataManager = new DatabaseDataManager(plugin);
            databaseDataManager.load();
            
            cavesById.putAll(databaseDataManager.getCavesById());
            cavesByOwner.putAll(databaseDataManager.getCavesByOwner());
            cavesByMember.putAll(databaseDataManager.getCavesByMember());
            nextCaveId = databaseDataManager.getNextCaveId();
        } else {
            plugin.getLogger().info("使用 YAML 文件存储模式");
            loadFromYaml();
        }

        plugin.getLogger().info("已加载 " + cavesById.size() + " 个洞府");
    }

    /**
     * 从 YAML 文件加载数据
     */
    private void loadFromYaml() {
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建数据文件: " + e.getMessage());
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        cavesById.clear();
        cavesByOwner.clear();
        cavesByMember.clear();

        nextCaveId = dataConfig.getInt("next-cave-id", 1);

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

    /**
     * 异步保存数据
     */
    public void save() {
        markDirty();
        saveAsync();
    }

    /**
     * 标记数据为脏，需要保存
     */
    public void markDirty() {
        dirty.set(true);
    }

    /**
     * 异步保存数据（如果数据有变化）
     */
    public void saveAsync() {
        if (!dirty.get()) {
            return;
        }

        if (asyncExecutor != null) {
            asyncExecutor.execute(() -> saveSync());
        } else {
            // 使用统一调度器，自动降级到 Bukkit Scheduler
            cn.guangdian.rpgcore.integration.UnifiedScheduler.runAsync(plugin, () -> saveSync());
        }
    }

    /**
     * 强制异步保存（忽略脏标记）
     */
    public CompletableFuture<Void> saveAsyncForce() {
        if (asyncExecutor != null) {
            return asyncExecutor.execute(() -> saveSync());
        } else {
            return CompletableFuture.runAsync(() -> saveSync());
        }
    }

    /**
     * 同步保存数据（内部方法）
     */
    private void saveSync() {
        synchronized (saveLock) {
            try {
                if (useDatabase && databaseDataManager != null) {
                    databaseDataManager.saveSync();
                } else {
                    saveToYaml();
                }
                dirty.set(false);
            } catch (Exception e) {
                plugin.getLogger().severe("保存数据失败: " + e.getMessage());
            }
        }
    }

    /**
     * 保存到 YAML 文件
     */
    private void saveToYaml() {
        if (dataConfig == null) {
            dataConfig = new YamlConfiguration();
        }

        dataConfig.set("next-cave-id", nextCaveId);

        Map<String, Object> cavesData = new HashMap<>();
        for (Cave cave : cavesById.values()) {
            cavesData.put(String.valueOf(cave.getId()), cave.serialize());
        }
        dataConfig.set("caves", cavesData);

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存数据失败: " + e.getMessage());
        }
    }

    /**
     * 同步保存并等待完成（用于服务器关闭）
     */
    public void saveSyncAndAwait() {
        isShuttingDown = true;

        try {
            CompletableFuture<Void> saveFuture = saveAsyncForce();
            saveFuture.get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            plugin.getLogger().warning("保存数据超时，强制关闭");
        } catch (Exception e) {
            plugin.getLogger().severe("保存数据时发生错误: " + e.getMessage());
        }
    }

    /**
     * 定时保存任务（建议每5分钟调用一次）
     */
    public void scheduleAutoSave() {
        if (dirty.get()) {
            saveAsync();
        }
    }

    /**
     * 创建新洞府
     */
    public Cave createCave(UUID ownerUuid, String ownerName, int level, String worldName, int centerX, int centerZ) {
        if (useDatabase && databaseDataManager != null) {
            Cave cave = databaseDataManager.createCave(ownerUuid, ownerName, level, worldName, centerX, centerZ);
            if (cave != null) {
                cavesById.put(cave.getId(), cave);
                cavesByOwner.put(ownerUuid, cave);
                cavesByMember.put(ownerUuid, cave);
                return cave;
            }
            return null;
        }

        int id = nextCaveId++;
        Cave cave = new Cave(id, ownerUuid, ownerName, level, worldName, centerX, centerZ);
        cavesById.put(id, cave);
        cavesByOwner.put(ownerUuid, cave);
        cavesByMember.put(ownerUuid, cave);
        save();
        return cave;
    }

    /**
     * 删除洞府
     */
    public void deleteCave(int id) {
        Cave cave = cavesById.remove(id);
        if (cave != null) {
            cavesByOwner.remove(cave.getOwnerUuid());
            for (UUID memberUuid : cave.getMembers().keySet()) {
                cavesByMember.remove(memberUuid);
            }

            if (useDatabase && databaseDataManager != null) {
                // 使用新的异步删除方法，同步等待结果
                databaseDataManager.deleteCaveAsync(id).join();
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
        return nextCaveId;
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
        // 使用 RPGCore AsyncExecutor 时始终返回 true
        return asyncExecutor != null || !isShuttingDown;
    }

    public boolean hasPendingSave() {
        return dirty.get();
    }

    public boolean isUsingDatabase() {
        return useDatabase;
    }
}
