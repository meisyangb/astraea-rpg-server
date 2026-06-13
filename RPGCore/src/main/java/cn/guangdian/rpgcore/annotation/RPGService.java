package cn.guangdian.rpgcore.annotation;

import cn.guangdian.rpgcore.api.ServicePriority;

import java.lang.annotation.*;

/**
 * RPG服务注解 - 自动服务发现与注册
 *
 * <p>标注在服务实现类上，RPGCore 启动时会自动扫描并注册到 ServiceRegistry。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @RPGService(serviceInterface = PointsService.class, priority = ServicePriority.NORMAL)
 * public class PointsServiceImpl implements PointsService {
 *     // 服务实现...
 * }
 * }</pre>
 *
 * <h3>扫描机制：</h3>
 * <ul>
 *   <li>RPGCore 启动时扫描所有已加载插件的类</li>
 *   <li>发现带有此注解的类后自动创建实例并注册</li>
 *   <li>支持优先级排序，高优先级服务优先返回</li>
 * </ul>
 *
 * @author GuangDian
 * @since 2.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface RPGService {

    /**
     * 服务接口类型
     *
     * <p>指定此实现类对应的服务接口。</p>
     *
     * @return 服务接口的 Class 对象
     */
    Class<?> serviceInterface();

    /**
     * 服务优先级
     *
     * <p>当存在多个相同接口的实现时，优先级高的会被优先返回。</p>
     *
     * @return 服务优先级，默认为 NORMAL
     */
    ServicePriority priority() default ServicePriority.NORMAL;

    /**
     * 服务名称
     *
     * <p>可选的服务名称，用于日志和调试。</p>
     *
     * @return 服务名称，默认为空（使用类名）
     */
    String name() default "";

    /**
     * 是否延迟加载
     *
     * <p>延迟加载的服务不会在启动时立即创建，而是在首次使用时创建。</p>
     *
     * @return 是否延迟加载，默认为 false
     */
    boolean lazy() default false;

    /**
     * 是否单例
     *
     * <p>单例服务只会创建一个实例，非单例每次获取都会创建新实例。</p>
     *
     * @return 是否单例，默认为 true
     */
    boolean singleton() default true;
}