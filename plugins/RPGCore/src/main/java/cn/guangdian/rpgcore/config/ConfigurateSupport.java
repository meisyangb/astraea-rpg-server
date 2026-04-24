package cn.guangdian.rpgcore.config;

import cn.guangdian.rpgcore.RPGCore;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Configurate 配置支持类 - 推荐使用
 *
 * <p>提供类型安全的配置管理，支持自动加载、保存和默认值。
 * 替代传统的 Bukkit YamlConfiguration 和自定义配置管理。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 1. 定义配置类
 * @ConfigSerializable
 * public class DatabaseConfig {
 *     private String host = "localhost";
 *     private int port = 3306;
 *     private String username = "root";
 *     private String password = "";
 *
 *     // Getters...
 *     public String getHost() { return host; }
 *     public int getPort() { return port; }
 * }
 *
 * // 2. 加载配置
 * ConfigurateSupport<DatabaseConfig> config = ConfigurateSupport.builder(DatabaseConfig.class)
 *     .file("database.yml")
 *     .defaultResource("database-default.yml")
 *     .autoSave(true)
 *     .build();
 *
 * DatabaseConfig dbConfig = config.get();
 * String host = dbConfig.getHost();
 *
 * // 3. 修改并保存
 * config.update(c -> {
 *     // 修改配置
 * });
 * config.save();
 * }</pre>
 *
 * @param <T> 配置类型
 * @author GuangDian
 * @since 2.0.0
 * @see ConfigurateManager
 * @deprecated 使用 ConfigurateManager 替代自定义配置管理
 */
@Deprecated(since = "2.0.0", forRemoval = false)
public class ConfigurateSupport<T> {

    private final Class<T> configClass;
    private final Path filePath;
    private final Path defaultResource;
    private final ClassLoader classLoader;
    private final boolean autoSave;
    private final Consumer<T> validator;
    private final Supplier<T> defaultFactory;

    private T config;
    private final Object lock = new Object();

    private ConfigurateSupport(Builder<T> builder) {
        this.configClass = builder.configClass;
        this.filePath = builder.filePath;
        this.defaultResource = builder.defaultResource;
        this.classLoader = builder.classLoader;
        this.autoSave = builder.autoSave;
        this.validator = builder.validator;
        this.defaultFactory = builder.defaultFactory;

        load();
    }

    /**
     * 获取配置实例
     *
     * @return 配置实例
     */
    public T get() {
        synchronized (lock) {
            return config;
        }
    }

    /**
     * 安全获取配置（如果加载失败返回默认值）
     *
     * @return 配置实例或默认值
     */
    public T getOrDefault() {
        synchronized (lock) {
            if (config == null && defaultFactory != null) {
                return defaultFactory.get();
            }
            return config;
        }
    }

    /**
     * 更新配置
     *
     * @param updater 更新器
     */
    public void update(Consumer<T> updater) {
        synchronized (lock) {
            updater.accept(config);
            if (validator != null) {
                validator.accept(config);
            }
            if (autoSave) {
                save();
            }
        }
    }

    /**
     * 保存配置
     *
     * @return 是否保存成功
     */
    public boolean save() {
        synchronized (lock) {
            try {
                YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                        .path(filePath)
                        .build();

                CommentedConfigurationNode node = loader.createNode();
                ObjectMapper<T> mapper = ObjectMapper.factory().get(configClass);
                mapper.save(config, node);
                loader.save(node);

                return true;
            } catch (ConfigurateException e) {
                RPGCore.getInstance().getLogger().severe("保存配置失败: " + filePath + " - " + e.getMessage());
                return false;
            }
        }
    }

    /**
     * 重新加载配置
     *
     * @return 是否加载成功
     */
    public boolean reload() {
        synchronized (lock) {
            return load();
        }
    }

    /**
     * 加载配置
     *
     * @return 是否加载成功
     */
    private boolean load() {
        try {
            // 如果文件不存在，复制默认配置
            if (!Files.exists(filePath) && defaultResource != null) {
                saveDefaultConfig();
            }

            // 如果文件仍不存在，使用默认工厂创建
            if (!Files.exists(filePath)) {
                if (defaultFactory != null) {
                    config = defaultFactory.get();
                    save();
                    return true;
                }
                return false;
            }

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(filePath)
                    .build();

            CommentedConfigurationNode node = loader.load();
            ObjectMapper<T> mapper = ObjectMapper.factory().get(configClass);
            config = mapper.load(node);

            if (validator != null) {
                validator.accept(config);
            }

            return true;
        } catch (ConfigurateException e) {
            RPGCore.getInstance().getLogger().severe("加载配置失败: " + filePath + " - " + e.getMessage());
            if (defaultFactory != null) {
                config = defaultFactory.get();
            }
            return false;
        }
    }

    /**
     * 保存默认配置
     */
    private void saveDefaultConfig() {
        if (defaultResource == null || classLoader == null) {
            return;
        }

        try {
            Files.createDirectories(filePath.getParent());

            try (InputStream in = classLoader.getResourceAsStream(defaultResource.toString())) {
                if (in != null) {
                    Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (Exception e) {
            RPGCore.getInstance().getLogger().warning("保存默认配置失败: " + defaultResource);
        }
    }

    /**
     * 获取配置文件路径
     *
     * @return 文件路径
     */
    public Path getFilePath() {
        return filePath;
    }

    /**
     * 检查配置是否已加载
     *
     * @return 如果已加载返回 true
     */
    public boolean isLoaded() {
        synchronized (lock) {
            return config != null;
        }
    }

    /**
     * 创建构建器
     *
     * @param configClass 配置类
     * @param <T>         配置类型
     * @return 构建器
     */
    public static <T> Builder<T> builder(Class<T> configClass) {
        return new Builder<>(configClass);
    }

    /**
     * 构建器
     *
     * @param <T> 配置类型
     */
    public static class Builder<T> {
        private final Class<T> configClass;
        private Path filePath;
        private Path defaultResource;
        private ClassLoader classLoader;
        private boolean autoSave = false;
        private Consumer<T> validator;
        private Supplier<T> defaultFactory;

        private Builder(Class<T> configClass) {
            this.configClass = configClass;
        }

        /**
         * 设置配置文件路径
         *
         * @param file 文件
         * @return 构建器
         */
        public Builder<T> file(File file) {
            this.filePath = file.toPath();
            return this;
        }

        /**
         * 设置配置文件路径
         *
         * @param path 路径
         * @return 构建器
         */
        public Builder<T> file(Path path) {
            this.filePath = path;
            return this;
        }

        /**
         * 设置配置文件路径
         *
         * @param fileName 文件名（相对于插件数据目录）
         * @return 构建器
         */
        public Builder<T> file(String fileName) {
            this.filePath = RPGCore.getInstance().getDataFolder().toPath().resolve(fileName);
            return this;
        }

        /**
         * 设置默认资源路径
         *
         * @param resource 资源路径
         * @return 构建器
         */
        public Builder<T> defaultResource(String resource) {
            this.defaultResource = Path.of(resource);
            return this;
        }

        /**
         * 设置类加载器
         *
         * @param classLoader 类加载器
         * @return 构建器
         */
        public Builder<T> classLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        /**
         * 启用自动保存
         *
         * @return 构建器
         */
        public Builder<T> autoSave() {
            this.autoSave = true;
            return this;
        }

        /**
         * 启用/禁用自动保存
         *
         * @param autoSave 是否自动保存
         * @return 构建器
         */
        public Builder<T> autoSave(boolean autoSave) {
            this.autoSave = autoSave;
            return this;
        }

        /**
         * 设置验证器
         *
         * @param validator 验证器
         * @return 构建器
         */
        public Builder<T> validator(Consumer<T> validator) {
            this.validator = validator;
            return this;
        }

        /**
         * 设置默认工厂
         *
         * @param factory 工厂
         * @return 构建器
         */
        public Builder<T> defaultFactory(Supplier<T> factory) {
            this.defaultFactory = factory;
            return this;
        }

        /**
         * 构建配置支持
         *
         * @return 配置支持实例
         */
        public ConfigurateSupport<T> build() {
            if (filePath == null) {
                throw new IllegalStateException("必须设置配置文件路径");
            }
            if (classLoader == null) {
                classLoader = Thread.currentThread().getContextClassLoader();
            }
            return new ConfigurateSupport<>(this);
        }
    }

    /**
     * 缓存管理
     */
    private static final ConcurrentHashMap<String, ConfigurateSupport<?>> CACHE = new ConcurrentHashMap<>();

    /**
     * 获取或创建配置支持（带缓存）
     *
     * @param fileName    文件名
     * @param configClass 配置类
     * @param <T>         配置类型
     * @return 配置支持实例
     */
    @SuppressWarnings("unchecked")
    public static <T> ConfigurateSupport<T> getOrCreate(String fileName, Class<T> configClass) {
        return (ConfigurateSupport<T>) CACHE.computeIfAbsent(fileName, k ->
                builder(configClass)
                        .file(fileName)
                        .classLoader(RPGCore.getInstance().getClass().getClassLoader())
                        .build()
        );
    }

    /**
     * 清除缓存
     */
    public static void clearCache() {
        CACHE.clear();
    }
}
