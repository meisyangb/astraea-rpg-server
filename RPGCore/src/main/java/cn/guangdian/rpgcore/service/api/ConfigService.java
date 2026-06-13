package cn.guangdian.rpgcore.service.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 统一配置服务接口
 *
 * <p>提供标准化的配置管理功能，支持多命名空间、类型安全的配置访问。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 获取配置
 * ConfigService config = rpgCore.getService(ConfigService.class);
 *
 * // 获取字符串
 * String value = config.getString("points", "currency.name", "点券");
 *
 * // 获取整数列表
 * List<Integer> levels = config.getIntList("forge", "levels");
 *
 * // 设置值
 * config.set("points", "currency.symbol", "💎");
 *
 * // 保存配置
 * config.save("points");
 * }</pre>
 *
 * @author GuangDian
 * @since 2.0.0
 */
public interface ConfigService {

    // ==================== 基础操作 ====================

    /**
     * 获取字符串配置
     *
     * @param namespace 命名空间（插件名）
     * @param path      配置路径
     * @param def       默认值
     * @return 配置值或默认值
     */
    String getString(String namespace, String path, String def);

    /**
     * 获取整数配置
     *
     * @param namespace 命名空间
     * @param path      配置路径
     * @param def       默认值
     * @return 配置值或默认值
     */
    int getInt(String namespace, String path, int def);

    /**
     * 获取长整数配置
     *
     * @param namespace 命名空间
     * @param path      配置路径
     * @param def       默认值
     * @return 配置值或默认值
     */
    long getLong(String namespace, String path, long def);

    /**
     * 获取双精度浮点数配置
     *
     * @param namespace 命名空间
     * @param path      配置路径
     * @param def       默认值
     * @return 配置值或默认值
     */
    double getDouble(String namespace, String path, double def);

    /**
     * 获取布尔值配置
     *
     * @param namespace 命名空间
     * @param path      配置路径
     * @param def       默认值
     * @return 配置值或默认值
     */
    boolean getBoolean(String namespace, String path, boolean def);

    /**
     * 获取可选字符串
     *
     * @param namespace 命名空间
     * @param path      配置路径
     * @return Optional 包装的配置值
     */
    Optional<String> getStringOptional(String namespace, String path);

    /**
     * 获取字符串列表
     *
     * @param namespace 命名空间
     * @param path      配置路径
     * @return 字符串列表
     */
    List<String> getStringList(String namespace, String path);

    /**
     * 获取整数列表
     *
     * @param namespace 命名空间
     * @param path      配置路径
     * @return 整数列表
     */
    List<Integer> getIntList(String namespace, String path);

    /**
     * 获取配置节点的所有键
     *
     * @param namespace 命名空间
     * @param path      配置路径
     * @return 键集合
     */
    Set<String> getKeys(String namespace, String path);

    /**
     * 检查配置路径是否存在
     *
     * @param namespace 命名空间
     * @param path      配置路径
     * @return 是否存在
     */
    boolean contains(String namespace, String path);

    // ==================== 写入操作 ====================

    /**
     * 设置配置值
     *
     * @param namespace 命名空间
     * @param path      配置路径
     * @param value     值
     */
    void set(String namespace, String path, Object value);

    /**
     * 删除配置路径
     *
     * @param namespace 命名空间
     * @param path      配置路径
     */
    void remove(String namespace, String path);

    // ==================== 文件操作 ====================

    /**
     * 保存配置到文件
     *
     * @param namespace 命名空间
     */
    void save(String namespace);

    /**
     * 保存所有配置
     */
    void saveAll();

    /**
     * 重新加载配置
     *
     * @param namespace 命名空间
     */
    void reload(String namespace);

    /**
     * 重新加载所有配置
     */
    void reloadAll();

    // ==================== 命名空间管理 ====================

    /**
     * 注册命名空间
     *
     * @param namespace     命名空间
     * @param configHolder 配置持有者
     */
    void registerNamespace(String namespace, ConfigHolder configHolder);

    /**
     * 注销命名空间
     *
     * @param namespace 命名空间
     */
    void unregisterNamespace(String namespace);

    /**
     * 检查命名空间是否存在
     *
     * @param namespace 命名空间
     * @return 是否存在
     */
    boolean hasNamespace(String namespace);

    /**
     * 获取所有命名空间
     *
     * @return 命名空间集合
     */
    Set<String> getNamespaces();

    // ==================== 扩展功能 ====================

    /**
     * 获取嵌套配置节
     *
     * @param namespace 命名空间
     * @param path      配置路径
     * @return 配置节 Map
     */
    Map<String, Object> getSection(String namespace, String path);

    /**
     * 获取配置版本
     *
     * @param namespace 命名空间
     * @return 配置版本
     */
    int getConfigVersion(String namespace);

    /**
     * 检查配置是否需要迁移
     *
     * @param namespace     命名空间
     * @param targetVersion 目标版本
     * @return 是否需要迁移
     */
    boolean needsMigration(String namespace, int targetVersion);

    /**
     * 配置持有者接口
     */
    interface ConfigHolder {
        /**
         * 获取配置值
         */
        Object get(String path);

        /**
         * 设置配置值
         */
        void set(String path, Object value);

        /**
         * 检查路径是否存在
         */
        boolean contains(String path);

        /**
         * 保存配置
         */
        void save();

        /**
         * 重新加载配置
         */
        void reload();

        /**
         * 获取所有键
         */
        Set<String> getKeys(String path);
    }
}