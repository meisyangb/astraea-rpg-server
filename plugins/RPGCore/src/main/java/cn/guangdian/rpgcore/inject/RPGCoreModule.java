package cn.guangdian.rpgcore.inject;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.*;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.sound.SoundService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * RPGCore Guice 模块配置 - 完整依赖注入支持
 *
 * <p>配置 RPGCore 核心服务的依赖注入绑定。推荐使用 Guice 进行依赖注入，
 * 替代手动获取服务实例的方式。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 在插件中注入服务
 * public class MyPlugin extends AbstractRPGPlugin {
 *     @Inject
 *     private MyService myService;
 *
 *     @Inject
 *     private MiniMessageService miniMessage;
 *
 *     @Override
 *     protected void onPluginEnable() {
 *         // 服务已自动注入，直接使用
 *         myService.doSomething();
 *     }
 * }
 *
 * // 创建自定义 Module
 * public class MyModule extends AbstractModule {
 *     @Override
 *     protected void configure() {
 *         bind(MyService.class).to(MyServiceImpl.class).in(Singleton.class);
 *     }
 * }
 * }</pre>
 *
 * @author GuangDian
 * @since 2.0.0
 * @deprecated 使用 {@link GuiceSupport} 获取 Injector 并创建子注入器
 */
@Deprecated(since = "2.0.0", forRemoval = false)
public class RPGCoreModule extends AbstractModule {

    private final RPGCore rpgCore;

    public RPGCoreModule(RPGCore rpgCore) {
        this.rpgCore = rpgCore;
    }

    @Override
    protected void configure() {
        // 绑定 RPGCore 实例
        bind(RPGCore.class).toInstance(rpgCore);

        // 绑定 JavaPlugin（RPGCore 本身）
        bind(JavaPlugin.class).toInstance(rpgCore);

        // ServiceRegistry 由 Provider 提供（支持动态获取）
        bind(ServiceRegistry.class).toProvider(() -> rpgCore.getServiceRegistry());

        // SyncScheduler 由 Provider 提供
        bind(SyncScheduler.class).toProvider(() -> rpgCore.getScheduler());

        // ExternalServiceIntegration 由 Provider 提供
        bind(ExternalServiceIntegration.class).toProvider(() -> rpgCore.getExternalServices());

        // AsyncExecutor 由 Provider 提供
        bind(AsyncExecutor.class).toProvider(() -> rpgCore.getAsyncExecutor());

        // CacheProvider 由 Provider 提供
        bind(CacheProvider.class).toProvider(() -> rpgCore.getCacheProvider());

        // EventBus 已废弃，返回 null
        // bind(EventBus.class).toProvider(() -> rpgCore.getEventBus());

        // ConfigManager 由 Provider 提供
        bind(ConfigManager.class).toProvider(() -> rpgCore.getConfigManager());

        // CronScheduler 由 Provider 提供
        bind(CronScheduler.class).toProvider(() -> rpgCore.getCronScheduler());

        // HttpClient 由 Provider 提供
        bind(HttpClient.class).toProvider(() -> rpgCore.getHttpClient());

        // DataExporter 由 Provider 提供
        bind(DataExporter.class).toProvider(() -> rpgCore.getDataExporter());

        // AuditLog 由 Provider 提供
        bind(AuditLog.class).toProvider(() -> rpgCore.getAuditLog());

        // ExceptionHandler 由 Provider 提供
        bind(ExceptionHandler.class).toProvider(() -> rpgCore.getExceptionHandler());
    }

    /**
     * 提供 MiniMessageService
     */
    @Provides
    @Singleton
    public MiniMessageService provideMiniMessageService() {
        return rpgCore.getMiniMessageService();
    }

    /**
     * 提供 SoundService
     */
    @Provides
    @Singleton
    public SoundService provideSoundService() {
        return rpgCore.getSoundService();
    }

    /**
     * 提供 MessageService
     */
    @Provides
    @Singleton
    public cn.guangdian.rpgcore.service.api.MessageService provideMessageService() {
        return rpgCore.getMessageService();
    }

    /**
     * 提供 TextDisplayService
     */
    @Provides
    @Singleton
    public cn.guangdian.rpgcore.service.api.TextDisplayService provideTextDisplayService() {
        return rpgCore.getTextDisplayService();
    }

    /**
     * 提供 GameLogger
     */
    @Provides
    @Singleton
    public GameLogger provideGameLogger() {
        return rpgCore.getGameLogger();
    }

    /**
     * 提供 ConfigMigrator
     */
    @Provides
    @Singleton
    public ConfigMigrator provideConfigMigrator() {
        return rpgCore.getConfigMigrator();
    }
}
