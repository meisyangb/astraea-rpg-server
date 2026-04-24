package cn.guangdian.rpgcore.inject;

import cn.guangdian.rpgcore.RPGCore;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Guice 依赖注入支持类 - 推荐使用
 *
 * <p>提供便捷的 Guice 注入器创建和管理功能。插件应该使用此类创建子注入器，
 * 而不是直接使用 Guice.createInjector()。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 在插件中创建子注入器
 * public class MyPlugin extends AbstractRPGPlugin {
 *     @Override
 *     protected void onPluginEnable() {
 *         // 方式1: 使用简单绑定
 *         GuiceSupport.createChildInjector(this, binder -> {
 *             binder.bind(MyService.class).to(MyServiceImpl.class);
 *             binder.bind(MyRepository.class).to(MyRepositoryImpl.class);
 *         }).injectMembers(this);
 *
 *         // 方式2: 使用 Module
 *         GuiceSupport.createChildInjector(this, new MyModule())
 *             .injectMembers(this);
 *
 *         // 方式3: 获取实例
 *         MyService service = GuiceSupport.getInstance(MyService.class);
 *     }
 * }
 * }</pre>
 *
 * @author GuangDian
 * @since 2.0.0
 * @see RPGCoreModule
 */
public final class GuiceSupport {

    private static final AtomicReference<Injector> ROOT_INJECTOR = new AtomicReference<>();

    private GuiceSupport() {
        // 工具类，禁止实例化
    }

    /**
     * 初始化根注入器（由 RPGCore 调用）
     *
     * @param rpgCore RPGCore 实例
     */
    public static synchronized void initialize(RPGCore rpgCore) {
        if (ROOT_INJECTOR.get() == null) {
            Injector injector = Guice.createInjector(new RPGCoreModule(rpgCore));
            ROOT_INJECTOR.set(injector);
            ServiceInjector.initialize(injector);
        }
    }

    /**
     * 获取根注入器
     *
     * @return Guice Injector
     * @throws IllegalStateException 如果尚未初始化
     */
    public static Injector getRootInjector() {
        Injector injector = ROOT_INJECTOR.get();
        if (injector == null) {
            throw new IllegalStateException("Guice 尚未初始化，请确保 RPGCore 已加载");
        }
        return injector;
    }

    /**
     * 检查是否已初始化
     *
     * @return 如果已初始化返回 true
     */
    public static boolean isInitialized() {
        return ROOT_INJECTOR.get() != null;
    }

    /**
     * 创建子注入器
     *
     * @param modules 额外的模块
     * @return 子注入器
     */
    public static Injector createChildInjector(Module... modules) {
        return getRootInjector().createChildInjector(modules);
    }

    /**
     * 创建子注入器（使用简单绑定）
     *
     * @param binder 绑定配置
     * @return 子注入器
     */
    public static Injector createChildInjector(java.util.function.Consumer<com.google.inject.Binder> binder) {
        return createChildInjector(new Module() {
            @Override
            public void configure(com.google.inject.Binder b) {
                binder.accept(b);
            }
        });
    }

    /**
     * 获取指定类型的实例
     *
     * @param type 类型
     * @param <T>  类型参数
     * @return 实例
     */
    public static <T> T getInstance(Class<T> type) {
        return getRootInjector().getInstance(type);
    }

    /**
     * 注入成员
     *
     * @param instance 实例
     */
    public static void injectMembers(Object instance) {
        getRootInjector().injectMembers(instance);
    }

    /**
     * 注入成员（使用子注入器）
     *
     * @param instance 实例
     * @param modules  额外的模块
     */
    public static void injectMembers(Object instance, Module... modules) {
        createChildInjector(modules).injectMembers(instance);
    }

    /**
     * 构建器模式创建子注入器
     *
     * @return 构建器
     */
    public static ChildInjectorBuilder childInjector() {
        return new ChildInjectorBuilder();
    }

    /**
     * 子注入器构建器
     */
    public static class ChildInjectorBuilder {
        private final List<Module> modules = new ArrayList<>();

        /**
         * 添加模块
         *
         * @param module 模块
         * @return 构建器
         */
        public ChildInjectorBuilder with(Module module) {
            modules.add(module);
            return this;
        }

        /**
         * 添加多个模块
         *
         * @param modules 模块数组
         * @return 构建器
         */
        public ChildInjectorBuilder with(Module... modules) {
            for (Module module : modules) {
                this.modules.add(module);
            }
            return this;
        }

        /**
         * 使用简单绑定
         *
         * @param binder 绑定配置
         * @return 构建器
         */
        public ChildInjectorBuilder withBinding(java.util.function.Consumer<com.google.inject.Binder> binder) {
            modules.add(new Module() {
                @Override
                public void configure(com.google.inject.Binder b) {
                    binder.accept(b);
                }
            });
            return this;
        }

        /**
         * 构建注入器
         *
         * @return 子注入器
         */
        public Injector build() {
            return createChildInjector(modules.toArray(new Module[0]));
        }

        /**
         * 构建并注入成员
         *
         * @param instance 实例
         */
        public void inject(Object instance) {
            build().injectMembers(instance);
        }
    }
}
