package cn.guangdian.rpgcore.inject;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 服务注入器 - 已废弃
 *
 * <p><strong>已废弃</strong>：请使用 {@link GuiceSupport} 替代。</p>
 *
 * <p>GuiceSupport 提供更完整的依赖注入功能，包括子注入器创建、
 * 自动成员注入等高级特性。</p>
 *
 * <h3>迁移示例：</h3>
 * <pre>{@code
 * // 旧方式（已废弃）
 * public class MyListener {
 *     @Inject private BankService bank;
 *
 *     public MyListener() {
 *         ServiceInjector.inject(this);
 *     }
 * }
 *
 * // 新方式（推荐）
 * public class MyListener {
 *     @Inject private BankService bank;
 *
 *     public MyListener() {
 *         GuiceSupport.injectMembers(this);
 *     }
 * }
 *
 * // 或者使用子注入器
 * public class MyPlugin extends AbstractRPGPlugin {
 *     @Override
 *     protected void onPluginEnable() {
 *         GuiceSupport.childInjector()
 *             .with(new MyModule())
 *             .inject(this);
 *     }
 * }
 * }</pre>
 *
 * @author GuangDian
 * @since 2.0.0
 * @deprecated 使用 {@link GuiceSupport} 替代
 * @see GuiceSupport
 */
@Deprecated(since = "2.0.0", forRemoval = true)
public class ServiceInjector {

    private static final Logger LOGGER = Logger.getLogger(ServiceInjector.class.getName());
    private static Injector injector;

    /**
     * 初始化注入器（由 RPGCore 调用）
     *
     * @param guiceInjector Guice Injector 实例
     */
    public static void initialize(Injector guiceInjector) {
        injector = guiceInjector;
        LOGGER.info("ServiceInjector initialized with Guice");
    }

    /**
     * 对目标对象进行字段注入
     *
     * <p>扫描目标对象中所有带有 @Inject 注解的字段，
     * 尝试从 Guice 或 ServiceRegistry 获取对应服务并注入。</p>
     *
     * @param target 目标对象
     */
    public static void inject(Object target) {
        if (target == null) {
            return;
        }

        Class<?> clazz = target.getClass();
        int injectedCount = 0;

        // 遍历所有字段（包括继承的）
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class)) {
                    if (injectField(target, field)) {
                        injectedCount++;
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }

        if (injectedCount > 0) {
            LOGGER.fine("Injected " + injectedCount + " fields into " + target.getClass().getSimpleName());
        }
    }

    /**
     * 注入单个字段
     *
     * @param target 目标对象
     * @param field  字段
     * @return 是否成功注入
     */
    private static boolean injectField(Object target, Field field) {
        field.setAccessible(true);

        try {
            // 如果字段已有值，跳过
            if (field.get(target) != null) {
                return false;
            }
        } catch (IllegalAccessException e) {
            LOGGER.log(Level.WARNING, "Cannot access field: " + field.getName(), e);
            return false;
        }

        Class<?> fieldType = field.getType();
        Object service = null;

        // 1. 尝试从 Guice Injector 获取
        if (injector != null) {
            try {
                service = injector.getInstance(Key.get(fieldType));
            } catch (Exception e) {
                // Guice 中未找到，继续尝试 ServiceRegistry
            }
        }

        // 2. 尝试从 RPGCore ServiceRegistry 获取
        if (service == null) {
            service = getServiceFromRegistry(fieldType);
        }

        // 3. 注入字段
        if (service != null) {
            try {
                field.set(target, service);
                return true;
            } catch (IllegalAccessException e) {
                LOGGER.log(Level.WARNING, "Failed to inject field: " + field.getName(), e);
            }
        } else {
            LOGGER.warning("No service found for field: " + field.getName() +
                " (type: " + fieldType.getSimpleName() + ") in " + target.getClass().getSimpleName());
        }

        return false;
    }

    /**
     * 从 RPGCore ServiceRegistry 获取服务
     *
     * @param serviceType 服务类型
     * @return 服务实例，如果不存在返回 null
     */
    @SuppressWarnings("unchecked")
    private static <T> T getServiceFromRegistry(Class<T> serviceType) {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) {
            return null;
        }

        ServiceRegistry registry = rpgCore.getServiceRegistry();
        if (registry == null) {
            return null;
        }

        try {
            return registry.getService(serviceType);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 使用 Guice 创建实例并自动注入
     *
     * @param clazz 类类型
     * @return 实例
     * @throws IllegalStateException 如果 Guice 未初始化
     */
    public static <T> T createInstance(Class<T> clazz) {
        if (injector == null) {
            throw new IllegalStateException("ServiceInjector not initialized. Call initialize() first.");
        }
        return injector.getInstance(clazz);
    }

    /**
     * 获取 Guice Injector
     *
     * @return Injector 实例
     * @throws IllegalStateException 如果未初始化
     */
    public static Injector getInjector() {
        if (injector == null) {
            throw new IllegalStateException("ServiceInjector not initialized. Call initialize() first.");
        }
        return injector;
    }

    /**
     * 检查是否已初始化
     *
     * @return 是否已初始化
     */
    public static boolean isInitialized() {
        return injector != null;
    }

    /**
     * 清理注入器（插件禁用时调用）
     */
    public static synchronized void cleanup() {
        injector = null;
        LOGGER.info("ServiceInjector cleaned up");
    }
}
