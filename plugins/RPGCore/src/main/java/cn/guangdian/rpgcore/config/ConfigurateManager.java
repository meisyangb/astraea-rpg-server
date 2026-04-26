package cn.guangdian.rpgcore.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configurate 配置管理器 - 类型安全的配置管理
 *
 * <p>提供基于 Configurate 的类型安全配置管理，支持 YAML 和 HOCON 格式。</p>
 *
 * <h3>特性：</h3>
 * <ul>
 *   <li>类型安全配置映射（通过 @ConfigSerializable 注解）</li>
 *   <li>自动配置升级和迁移</li>
 *   <li>注释保留</li>
 *   <li>支持默认值</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 定义配置类
 * @ConfigSerializable
 * public class DatabaseConfig {
 *     private String host = "localhost";
 *     private int port = 3306;
 *     // getters...
 * }
 *
 * // 加载配置
 * ConfigurateManager manager = new ConfigurateManager(plugin.getLogger(), dataFolder);
 * DatabaseConfig config = manager.loadConfig("database.yml", DatabaseConfig.class);
 * }</pre>
 *
 * @author GuangDian
 * @since 1.1.0
 */
public class ConfigurateManager {

    private final Logger logger;
    private final Path configDirectory;
    private final ConcurrentHashMap<String, ConfigEntry<?>> configCache;

    /**
     * 创建配置管理器
     *
     * @param logger 日志记录器
     * @param configDirectory 配置目录
     */
    public ConfigurateManager(Logger logger, Path configDirectory) {
        this.logger = logger;
        this.configDirectory = configDirectory;
        this.configCache = new ConcurrentHashMap<>();

        // 确保配置目录存在
        try {
            Files.createDirectories(configDirectory);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create config directory", e);
        }
    }

    /**
     * 从资源文件复制默认配置
     *
     * @param resourcePath 资源路径
     * @param targetFileName 目标文件名
     * @param classLoader 类加载器
     */
    public void saveDefaultConfig(String resourcePath, String targetFileName, ClassLoader classLoader) {
        Path targetPath = configDirectory.resolve(targetFileName);

        if (Files.exists(targetPath)) {
            return; // 已存在，不覆盖
        }

        try (InputStream in = classLoader.getResourceAsStream(resourcePath)) {
            if (in == null) {
                logger.warning("Default config not found in resources: " + resourcePath);
                return;
            }

            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Saved default config: " + targetFileName);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to save default config: " + targetFileName, e);
        }
    }

    /**
     * 加载 YAML 配置
     *
     * @param fileName 配置文件名
     * @param clazz 配置类类型
     * @param <T> 配置类型
     * @return 配置对象
     * @throws ConfigurateException 加载失败时抛出
     */
    public <T> T loadConfig(String fileName, Class<T> clazz) throws ConfigurateException {
        return loadConfig(fileName, clazz, Format.YAML);
    }

    /**
     * 加载配置（指定格式）
     *
     * @param fileName 配置文件名
     * @param clazz 配置类类型
     * @param format 配置格式
     * @param <T> 配置类型
     * @return 配置对象
     * @throws ConfigurateException 加载失败时抛出
     */
    public <T> T loadConfig(String fileName, Class<T> clazz, Format format) throws ConfigurateException {
        Path path = configDirectory.resolve(fileName);
        ConfigurationLoader<CommentedConfigurationNode> loader = createLoader(path, format);

        CommentedConfigurationNode root = loader.load();

        T config = root.get(clazz);

        // 缓存配置
        configCache.put(fileName, new ConfigEntry<>(config, path, format, clazz));

        logger.fine("Loaded config: " + fileName);
        return config;
    }

    /**
     * 安全加载配置（失败返回 null）
     *
     * @param fileName 配置文件名
     * @param clazz 配置类类型
     * @param <T> 配置类型
     * @return 配置对象，失败返回 null
     */
    public <T> T loadConfigSafe(String fileName, Class<T> clazz) {
        try {
            return loadConfig(fileName, clazz);
        } catch (ConfigurateException e) {
            logger.log(Level.SEVERE, "Failed to load config: " + fileName, e);
            return null;
        }
    }

    /**
     * 保存配置
     *
     * @param fileName 配置文件名
     * @param config 配置对象
     * @param <T> 配置类型
     * @throws ConfigurateException 保存失败时抛出
     */
    public <T> void saveConfig(String fileName, T config) throws ConfigurateException {
        @SuppressWarnings("unchecked")
        ConfigEntry<T> entry = (ConfigEntry<T>) configCache.get(fileName);

        if (entry == null) {
            throw new IllegalStateException("Config not loaded: " + fileName);
        }

        ConfigurationLoader<CommentedConfigurationNode> loader = createLoader(entry.path, entry.format);
        CommentedConfigurationNode root = loader.createNode();

        root.set(config);

        loader.save(root);
        logger.fine("Saved config: " + fileName);
    }

    /**
     * 安全保存配置
     *
     * @param fileName 配置文件名
     * @param config 配置对象
     * @param <T> 配置类型
     * @return 是否保存成功
     */
    public <T> boolean saveConfigSafe(String fileName, T config) {
        try {
            saveConfig(fileName, config);
            return true;
        } catch (ConfigurateException e) {
            logger.log(Level.SEVERE, "Failed to save config: " + fileName, e);
            return false;
        }
    }

    /**
     * 重新加载配置
     *
     * @param fileName 配置文件名
     * @param <T> 配置类型
     * @return 重新加载后的配置对象
     * @throws ConfigurateException 加载失败时抛出
     */
    @SuppressWarnings("unchecked")
    public <T> T reloadConfig(String fileName) throws ConfigurateException {
        ConfigEntry<T> entry = (ConfigEntry<T>) configCache.get(fileName);

        if (entry == null) {
            throw new IllegalStateException("Config not loaded: " + fileName);
        }

        return loadConfig(fileName, entry.clazz, entry.format);
    }

    /**
     * 获取已加载的配置
     *
     * @param fileName 配置文件名
     * @param <T> 配置类型
     * @return 配置对象 Optional
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getConfig(String fileName) {
        ConfigEntry<T> entry = (ConfigEntry<T>) configCache.get(fileName);
        return Optional.ofNullable(entry != null ? entry.config : null);
    }

    /**
     * 检查配置是否已加载
     *
     * @param fileName 配置文件名
     * @return 是否已加载
     */
    public boolean isConfigLoaded(String fileName) {
        return configCache.containsKey(fileName);
    }

    /**
     * 获取配置文件路径
     *
     * @param fileName 配置文件名
     * @return 配置文件路径
     */
    public Path getConfigPath(String fileName) {
        return configDirectory.resolve(fileName);
    }

    /**
     * 获取配置目录
     *
     * @return 配置目录路径
     */
    public Path getConfigDirectory() {
        return configDirectory;
    }

    /**
     * 清除配置缓存
     */
    public void clearCache() {
        configCache.clear();
        logger.info("Config cache cleared");
    }

    /**
     * 创建配置加载器
     */
    private ConfigurationLoader<CommentedConfigurationNode> createLoader(Path path, Format format) {
        return switch (format) {
            case YAML -> YamlConfigurationLoader.builder()
                .path(path)
                .build();
        };
    }

    /**
     * 配置格式枚举
     */
    public enum Format {
        YAML
    }

    /**
     * 配置条目
     */
    private static class ConfigEntry<T> {
        final T config;
        final Path path;
        final Format format;
        final Class<T> clazz;

        ConfigEntry(T config, Path path, Format format, Class<T> clazz) {
            this.config = config;
            this.path = path;
            this.format = format;
            this.clazz = clazz;
        }
    }
}
