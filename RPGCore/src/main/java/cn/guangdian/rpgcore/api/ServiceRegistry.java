package cn.guangdian.rpgcore.api;

import java.util.Optional;

/**
 * 服务注册接口 - 统一服务管理
 * 
 * <p>ServiceRegistry 提供了一个服务定位器模式，用于模块间的服务发现和依赖注入。
 * 替代现有的 getInstance() 静态方法，提供更灵活的服务管理。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 注册服务
 * serviceRegistry.registerService(PointsService.class, pointsServiceImpl);
 * 
 * // 获取服务（必须存在）
 * PointsService pointsService = serviceRegistry.getService(PointsService.class);
 * 
 * // 获取服务（可选）
 * Optional<SkillService> skillService = serviceRegistry.getOptionalService(SkillService.class);
 * if (skillService.isPresent()) {
 *     skillService.get().triggerSkill(player, "fireball");
 * }
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface ServiceRegistry {

    /**
     * 注册服务实现
     * 
     * <p>将服务实现注册到服务注册表中，其他模块可以通过服务接口获取实现。</p>
     * 
     * @param serviceClass 服务接口的 Class 对象
     * @param implementation 服务实现实例
     * @param priority 服务优先级
     * @param <T> 服务类型
     * @throws IllegalArgumentException 如果 serviceClass 或 implementation 为 null
     * @throws IllegalStateException 如果服务已存在且不允许覆盖
     */
    <T> void registerService(Class<T> serviceClass, T implementation, ServicePriority priority);

    /**
     * 注册服务实现（默认优先级）
     * 
     * @param serviceClass 服务接口的 Class 对象
     * @param implementation 服务实现实例
     * @param <T> 服务类型
     */
    default <T> void registerService(Class<T> serviceClass, T implementation) {
        registerService(serviceClass, implementation, ServicePriority.NORMAL);
    }

    /**
     * 获取服务（必须存在）
     * 
     * <p>获取指定类型的服务实现。如果服务不存在，将抛出异常。</p>
     * 
     * @param serviceClass 服务接口的 Class 对象
     * @return 服务实现实例
     * @param <T> 服务类型
     * @throws IllegalArgumentException 如果 serviceClass 为 null
     * @throws IllegalStateException 如果服务不存在
     */
    <T> T getService(Class<T> serviceClass);

    /**
     * 获取服务（可选）
     * 
     * <p>获取指定类型的服务实现。如果服务不存在，返回空的 Optional。</p>
     * 
     * @param serviceClass 服务接口的 Class 对象
     * @return 包含服务实现的 Optional，如果不存在则返回空 Optional
     * @param <T> 服务类型
     */
    <T> Optional<T> getOptionalService(Class<T> serviceClass);

    /**
     * 检查服务是否存在
     * 
     * @param serviceClass 服务接口的 Class 对象
     * @return 如果服务存在返回 true
     */
    <T> boolean hasService(Class<T> serviceClass);

    /**
     * 注销服务
     * 
     * @param serviceClass 服务接口的 Class 对象
     * @param <T> 服务类型
     */
    <T> void unregisterService(Class<T> serviceClass);

    /**
     * 获取已注册的服务数量
     * 
     * @return 服务数量
     */
    int getServiceCount();

    /**
     * 清空所有服务
     * 
     * <p>通常在插件禁用时调用。</p>
     */
    void clear();
}