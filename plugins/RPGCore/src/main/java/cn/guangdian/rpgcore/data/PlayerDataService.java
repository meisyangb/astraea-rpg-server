package cn.guangdian.rpgcore.data;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * 玩家数据服务基类 - RPGCore 核心服务
 *
 * <p>提供统一的玩家数据缓存、自动保存、序列化等功能。</p>
 * <p>所有需要存储玩家数据的插件应继承此类而非自己实现缓存逻辑。</p>
 *
 * <h2>使用示例:</h2>
 * <pre>{@code
 * // 1. 定义数据类
 * public class PointData {
 *     private long balance = 0;
 *     // getters/setters...
 * }
 *
 * // 2. 继承 PlayerDataService
 * public class PointsDataService extends PlayerDataService<PointData> {
 *     public PointsDataService() {
 *         super("points", PointData.class);
 *     }
 *
 *     @Override
 *     protected PointData createDefaultData() {
 *         return new PointData();
 *     }
 *
 *     @Override
 *     protected Map<String, Object> serialize(PointData data) {
 *         Map<String, Object> map = new HashMap<>();
 *         map.put("balance", data.getBalance());
 *         return map;
 *     }
 *
 *     @Override
 *     protected PointData deserialize(Map<String, Object> data) {
 *         PointData pointData = new PointData();
 *         pointData.setBalance((Long) data.getOrDefault("balance", 0L));
 *         return pointData;
 *     }
 * }
 *
 * // 3. 使用服务
 * PointsDataService service = new PointsDataService();
 * service.startAutoSave(6000); // 5分钟自动保存
 *
 * PointData data = service.getData(playerUUID);
 * data.setBalance(data.getBalance() + 100);
 * service.setData(playerUUID, data);
 * }</pre>
 *
 * @param <T> 数据类型
 * @author Astraea RPG Team
 * @since 1.1.0
 */
public abstract class PlayerDataService<T> {

    protected final String serviceName;
    protected final Class<T> dataType;
    protected final Map<UUID, T> cache;
    protected final Set<UUID> dirtyCache; // 标记已修改的数据

    protected final Logger logger;
    protected final SyncScheduler scheduler;
    protected final File dataFolder;

    private Long autoSaveTaskId = null;
    private boolean shutdownHookRegistered = false;

    /**
     * 构造函数
     *
     * @param serviceName 服务名称 (用于日志和文件命名)
     * @param dataType 数据类型
     */
    protected PlayerDataService(@NotNull String serviceName, @NotNull Class<T> dataType) {
        this.serviceName = serviceName;
        this.dataType = dataType;
        this.cache = new ConcurrentHashMap<>();
        this.dirtyCache = ConcurrentHashMap.newKeySet();

        RPGCore rpgCore = RPGCore.getInstance();
        this.logger = rpgCore != null ? rpgCore.getLogger() : Logger.getLogger(serviceName);
        this.scheduler = rpgCore != null ? rpgCore.getScheduler() : null;
        this.dataFolder = rpgCore != null ? new File(rpgCore.getDataFolder(), "data") : new File("data");

        if (!this.dataFolder.exists()) {
            this.dataFolder.mkdirs();
        }

        logger.info("[PlayerDataService] " + serviceName + " 初始化完成");
    }

    // ==================== 数据访问 ====================

    /**
     * 获取玩家数据 (如果不存在则创建默认数据)
     */
    public @NotNull T getData(@NotNull UUID playerId) {
        return cache.computeIfAbsent(playerId, id -> {
            T data = loadDataFromFile(id);
            if (data == null) {
                data = createDefaultData();
            }
            return data;
        });
    }

    /**
     * 获取玩家数据 (可能为 null)
     */
    public @Nullable T getDataOrNull(@NotNull UUID playerId) {
        return cache.get(playerId);
    }

    /**
     * 设置玩家数据 (自动标记为 dirty)
     */
    public void setData(@NotNull UUID playerId, @NotNull T data) {
        cache.put(playerId, data);
        dirtyCache.add(playerId);
    }

    /**
     * 移除玩家数据
     */
    public @Nullable T removeData(@NotNull UUID playerId) {
        dirtyCache.remove(playerId);
        return cache.remove(playerId);
    }

    /**
     * 检查是否有玩家数据
     */
    public boolean hasData(@NotNull UUID playerId) {
        return cache.containsKey(playerId);
    }

    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * 获取脏数据数量
     */
    public int getDirtyCount() {
        return dirtyCache.size();
    }

    // ==================== 批量操作 ====================

    /**
     * 保存所有脏数据到文件
     *
     * @return 保存的数据数量
     */
    public int saveAllDirty() {
        if (dirtyCache.isEmpty()) {
            return 0;
        }

        int saved = 0;
        for (UUID playerId : dirtyCache) {
            T data = cache.get(playerId);
            if (data != null) {
                saveDataToFile(playerId, data);
                saved++;
            }
        }
        dirtyCache.clear();

        if (saved > 0) {
            logger.fine("[PlayerDataService] " + serviceName + " 保存了 " + saved + " 条数据");
        }
        return saved;
    }

    /**
     * 保存所有数据 (包括未修改的)
     */
    public void saveAll() {
        for (Map.Entry<UUID, T> entry : cache.entrySet()) {
            saveDataToFile(entry.getKey(), entry.getValue());
        }
        dirtyCache.clear();
        logger.info("[PlayerDataService] " + serviceName + " 保存了全部 " + cache.size() + " 条数据");
    }

    /**
     * 清空所有缓存 (不保存)
     */
    public void clearCache() {
        cache.clear();
        dirtyCache.clear();
    }

    // ==================== 自动保存 ====================

    /**
     * 启动自动保存任务
     *
     * @param intervalTicks 保存间隔 (ticks)
     */
    public void startAutoSave(long intervalTicks) {
        if (autoSaveTaskId != null) {
            logger.warning("[PlayerDataService] " + serviceName + " 自动保存已在运行");
            return;
        }

        if (scheduler == null) {
            logger.severe("[PlayerDataService] " + serviceName + " Scheduler 未初始化，无法启动自动保存");
            return;
        }

        autoSaveTaskId = scheduler.runSyncRepeating(() -> {
            try {
                int saved = saveAllDirty();
                if (saved > 0) {
                    logger.fine("[PlayerDataService] " + serviceName + " 自动保存: " + saved + " 条");
                }
            } catch (Exception e) {
                logger.severe("[PlayerDataService] " + serviceName + " 自动保存失败: " + e.getMessage());
                e.printStackTrace();
            }
        }, intervalTicks, intervalTicks);

        registerShutdownHook();

        logger.info("[PlayerDataService] " + serviceName + " 自动保存已启动 (间隔: " + (intervalTicks / 20) + "秒)");
    }

    /**
     * 停止自动保存
     */
    public void stopAutoSave() {
        if (autoSaveTaskId != null && scheduler != null) {
            scheduler.cancelTask(autoSaveTaskId);
            autoSaveTaskId = null;
            logger.info("[PlayerDataService] " + serviceName + " 自动保存已停止");
        }
    }

    // ==================== 抽象方法 (子类实现) ====================

    /**
     * 创建默认数据
     */
    protected abstract @NotNull T createDefaultData();

    /**
     * 序列化数据为 Map
     */
    protected abstract @NotNull Map<String, Object> serialize(@NotNull T data);

    /**
     * 从 Map 反序列化数据
     */
    protected abstract @NotNull T deserialize(@NotNull Map<String, Object> data);

    // ==================== 文件操作 (可重写) ====================

    /**
     * 从文件加载数据 (默认使用 YAML)
     */
    protected @Nullable T loadDataFromFile(@NotNull UUID playerId) {
        File file = getPlayerDataFile(playerId);
        if (!file.exists()) {
            return null;
        }

        try {
            YamlDataStore store = YamlDataStore.getInstance();
            Map<String, Object> dataMap = store.load(file);
            if (dataMap == null || dataMap.isEmpty()) {
                return null;
            }
            return deserialize(dataMap);
        } catch (Exception e) {
            logger.warning("[PlayerDataService] 加载玩家数据失败: " + playerId + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 保存数据到文件 (默认使用 YAML)
     */
    protected void saveDataToFile(@NotNull UUID playerId, @NotNull T data) {
        try {
            File file = getPlayerDataFile(playerId);
            Map<String, Object> dataMap = serialize(data);
            YamlDataStore store = YamlDataStore.getInstance();
            store.save(file, dataMap);
        } catch (Exception e) {
            logger.severe("[PlayerDataService] 保存玩家数据失败: " + playerId + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取玩家数据文件路径
     */
    protected @NotNull File getPlayerDataFile(@NotNull UUID playerId) {
        return new File(dataFolder, serviceName + "/" + playerId.toString() + ".yml");
    }

    // ==================== 内部方法 ====================

    private void registerShutdownHook() {
        if (shutdownHookRegistered) {
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("[PlayerDataService] " + serviceName + " 正在关闭...");
            saveAll();
        }, serviceName + "-shutdown-hook"));

        shutdownHookRegistered = true;
    }

    /**
     * 获取统计信息
     */
    public @NotNull String getStats() {
        return String.format("缓存: %d, 脏数据: %d, 自动保存: %s",
            cache.size(),
            dirtyCache.size(),
            autoSaveTaskId != null ? "启用" : "禁用"
        );
    }
}
