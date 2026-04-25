package cn.guangdian.rpgcore.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * YAML 数据存储工具 - 已废弃
 *
 * <p><strong>已废弃</strong>：请使用 {@link cn.guangdian.rpgcore.config.ConfigurateSupport} 或
 * {@link cn.guangdian.rpgcore.config.ConfigurateManager} 替代。</p>
 *
 * <p>Configurate 提供类型安全的配置管理，支持自动序列化/反序列化，
 * 替代手动 Map 操作和 Bukkit YamlConfiguration。</p>
 *
 * <h3>迁移示例：</h3>
 * <pre>{@code
 * // 旧方式（已废弃）
 * YamlDataStore store = YamlDataStore.getInstance();
 * Map<String, Object> data = store.load(file);
 * String name = (String) data.get("name");
 *
 * // 新方式（推荐）
 * @ConfigSerializable
 * public class PlayerData {
 *     private String name;
 *     private int level;
 *     // getters...
 * }
 *
 * ConfigurateSupport<PlayerData> config = ConfigurateSupport.builder(PlayerData.class)
 *     .file("player.yml")
 *     .build();
 * PlayerData data = config.get();
 * String name = data.getName();
 * }</pre>
 *
 * @author Astraea RPG Team
 * @since 1.1.0
 * @deprecated 使用 {@link cn.guangdian.rpgcore.config.ConfigurateSupport} 替代
 * @see cn.guangdian.rpgcore.config.ConfigurateManager
 * @see org.spongepowered.configurate.objectmapping.ConfigSerializable
 */
@Deprecated(since = "2.0.0", forRemoval = true)
public final class YamlDataStore {

    private static YamlDataStore instance;
    private static final Logger logger = Logger.getLogger("YamlDataStore");

    private YamlDataStore() {}

    public static synchronized YamlDataStore getInstance() {
        if (instance == null) {
            instance = new YamlDataStore();
        }
        return instance;
    }

    /**
     * 保存数据到 YAML 文件
     *
     * @param file 文件路径
     * @param data 数据 Map
     * @throws IOException 保存失败时抛出
     */
    public void save(@NotNull File file, @NotNull Map<String, Object> data) throws IOException {
        if (data.isEmpty()) {
            return;
        }

        // 确保父目录存在
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            yaml.set(entry.getKey(), entry.getValue());
        }

        yaml.save(file);
    }

    /**
     * 从 YAML 文件加载数据
     *
     * @param file 文件路径
     * @return 数据 Map，如果文件不存在或加载失败返回空 Map
     */
    public @NotNull Map<String, Object> load(@NotNull File file) {
        if (!file.exists()) {
            return new HashMap<>();
        }

        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            Map<String, Object> data = new HashMap<>();

            for (String key : yaml.getKeys(false)) {
                data.put(key, yaml.get(key));
            }

            return data;
        } catch (Exception e) {
            logger.log(Level.WARNING, "加载 YAML 文件失败: " + file.getPath(), e);
            return new HashMap<>();
        }
    }

    /**
     * 保存 ConfigurationSection 到文件
     *
     * @param file 文件路径
     * @param section ConfigurationSection
     * @throws IOException 保存失败时抛出
     */
    public void saveSection(@NotNull File file, @NotNull org.bukkit.configuration.ConfigurationSection section) throws IOException {
        if (section.getKeys(false).isEmpty()) {
            return;
        }

        // 确保父目录存在
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        YamlConfiguration yaml = new YamlConfiguration();
        for (String key : section.getKeys(true)) {
            yaml.set(key, section.get(key));
        }

        yaml.save(file);
    }

    /**
     * 从文件加载为 ConfigurationSection
     *
     * @param file 文件路径
     * @return ConfigurationSection，如果文件不存在返回空的 YamlConfiguration
     */
    public @NotNull org.bukkit.configuration.ConfigurationSection loadAsSection(@NotNull File file) {
        if (!file.exists()) {
            return new YamlConfiguration();
        }

        try {
            return YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            logger.log(Level.WARNING, "加载 YAML 文件失败: " + file.getPath(), e);
            return new YamlConfiguration();
        }
    }

    /**
     * 删除 YAML 文件
     *
     * @param file 文件路径
     * @return 是否删除成功
     */
    public boolean delete(@NotNull File file) {
        if (file.exists()) {
            return file.delete();
        }
        return true;
    }

    /**
     * 检查文件是否存在
     */
    public boolean exists(@NotNull File file) {
        return file.exists();
    }

    /**
     * 创建备份文件
     *
     * @param file 原文件
     * @return 备份文件路径
     */
    public @Nullable File createBackup(@NotNull File file) {
        if (!file.exists()) {
            return null;
        }

        File backupFile = new File(file.getParentFile(), file.getName() + ".bak");
        try {
            java.nio.file.Files.copy(file.toPath(), backupFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return backupFile;
        } catch (IOException e) {
            logger.log(Level.WARNING, "创建备份失败: " + file.getPath(), e);
            return null;
        }
    }
}
