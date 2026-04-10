package cn.guangdian.rpgcore.storage;

import cn.guangdian.rpgcore.api.DataRepository;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * YAML 数据仓库实现
 * 
 * <p>基于 YAML 文件的数据存储实现，支持按玩家分文件存储。</p>
 * 
 * @param <T> 数据类型
 * @author GuangDian
 * @since 1.0.0
 */
public abstract class YamlRepository<T> implements DataRepository<T> {

    protected final JavaPlugin plugin;
    protected final File dataFolder;
    protected final Map<UUID, T> cache;
    protected final String fileExtension;

    /**
     * 创建 YAML 数据仓库
     * 
     * @param plugin 插件实例
     * @param subFolder 子文件夹名称（可选）
     */
    public YamlRepository(JavaPlugin plugin, String subFolder) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
        this.fileExtension = ".yml";

        if (subFolder != null && !subFolder.isEmpty()) {
            this.dataFolder = new File(plugin.getDataFolder(), subFolder);
        } else {
            this.dataFolder = plugin.getDataFolder();
        }

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    /**
     * 创建 YAML 数据仓库（使用插件根目录）
     */
    public YamlRepository(JavaPlugin plugin) {
        this(plugin, "data");
    }

    @Override
    public CompletableFuture<T> load(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            // 先检查缓存
            T cached = cache.get(id);
            if (cached != null) {
                return cached;
            }

            // 从文件加载
            File file = getFile(id);
            if (!file.exists()) {
                T defaultData = createDefault(id);
                cache.put(id, defaultData);
                return defaultData;
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            T data = deserialize(config);
            
            if (data != null) {
                cache.put(id, data);
            }

            return data;
        });
    }

    @Override
    public CompletableFuture<Void> save(UUID id, T data) {
        return CompletableFuture.runAsync(() -> {
            cache.put(id, data);

            File file = getFile(id);
            FileConfiguration config = new YamlConfiguration();

            serialize(data, config);

            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save data for " + id + ": " + e.getMessage());
                throw new RuntimeException("Save failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> delete(UUID id) {
        return CompletableFuture.runAsync(() -> {
            cache.remove(id);

            File file = getFile(id);
            if (file.exists()) {
                file.delete();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            if (cache.containsKey(id)) {
                return true;
            }
            return getFile(id).exists();
        });
    }

    @Override
    public CompletableFuture<Map<UUID, T>> loadAll(Collection<UUID> ids) {
        Map<UUID, T> result = new ConcurrentHashMap<>();
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (UUID id : ids) {
            futures.add(load(id).thenAccept(data -> result.put(id, data)));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> result);
    }

    @Override
    public CompletableFuture<Map<UUID, T>> loadAll() {
        Map<UUID, T> result = new ConcurrentHashMap<>();
        
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(fileExtension));
        if (files == null || files.length == 0) {
            return CompletableFuture.completedFuture(result);
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (File file : files) {
            String fileName = file.getName();
            String uuidStr = fileName.substring(0, fileName.length() - fileExtension.length());
            
            try {
                UUID id = UUID.fromString(uuidStr);
                futures.add(load(id).thenAccept(data -> result.put(id, data)));
            } catch (IllegalArgumentException ignored) {
                // 文件名不是有效的UUID，跳过
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> result);
    }

    @Override
    public CompletableFuture<Void> saveAll(Map<UUID, T> data) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (Map.Entry<UUID, T> entry : data.entrySet()) {
            futures.add(save(entry.getKey(), entry.getValue()));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Override
    public int count() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(fileExtension));
        return files == null ? 0 : files.length;
    }

    @Override
    public T getFromCache(UUID id) {
        return cache.get(id);
    }

    @Override
    public void invalidate(UUID id) {
        cache.remove(id);
    }

    @Override
    public void invalidateAll() {
        cache.clear();
    }

    @Override
    public CompletableFuture<Void> warmup(Collection<UUID> ids) {
        return loadAll(ids).thenAccept(data -> {
            // 数据已经加载到缓存中
        });
    }

    @Override
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * 异步关闭仓库，带超时保护
     * 
     * @param timeoutSeconds 超时时间（秒）
     * @return 关闭完成的Future
     */
    public CompletableFuture<Void> closeAsync(int timeoutSeconds) {
        if (cache.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        // 复制当前缓存数据
        Map<UUID, T> dataToSave = new HashMap<>(cache);
        cache.clear();

        // 异步保存所有数据
        return saveAll(dataToSave)
            .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .exceptionally(ex -> {
                if (ex.getCause() instanceof TimeoutException) {
                    plugin.getLogger().warning(
                        "YamlRepository close timeout after " + timeoutSeconds + "s, " +
                        dataToSave.size() + " records may be lost"
                    );
                } else {
                    plugin.getLogger().log(Level.SEVERE, 
                        "YamlRepository close failed: " + ex.getMessage(), ex);
                }
                return null;
            });
    }

    @Override
    public void close() {
        // 工业级优化: 异步关闭带30秒超时保护
        // 避免阻塞主线程导致服务器关闭延迟
        try {
            closeAsync(30).get(35, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            plugin.getLogger().warning("YamlRepository close forced after timeout, some data may be lost");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "YamlRepository close error: " + e.getMessage(), e);
        }
    }

    /**
     * 非阻塞关闭（用于异步场景）
     * 调用者需自行处理返回的Future
     */
    public CompletableFuture<Void> closeNonBlocking() {
        return closeAsync(30);
    }

    /**
     * 获取数据文件
     */
    protected File getFile(UUID id) {
        return new File(dataFolder, id.toString() + fileExtension);
    }

    /**
     * 创建默认数据
     */
    protected abstract T createDefault(UUID id);

    /**
     * 从配置反序列化
     */
    protected abstract T deserialize(FileConfiguration config);

    /**
     * 序列化到配置
     */
    protected abstract void serialize(T data, FileConfiguration config);
}