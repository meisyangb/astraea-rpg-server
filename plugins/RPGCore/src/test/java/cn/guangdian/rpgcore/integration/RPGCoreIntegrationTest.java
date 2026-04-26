package cn.guangdian.rpgcore.integration;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.CacheProvider;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.concurrency.LockTimeoutException;
import cn.guangdian.rpgcore.concurrency.PlayerLockManager;
import cn.guangdian.rpgcore.database.CoreDatabase;
import cn.guangdian.rpgcore.event.EventPublisher;
import cn.guangdian.rpgcore.monitoring.MetricsExporter;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RPGCore 集成测试
 * 
 * <p>测试核心组件的集成和协作</p>
 * 
 * @author GuangDian
 * @since 2.0.0
 */
@DisplayName("RPGCore 集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RPGCoreIntegrationTest {

    @Mock
    private Server mockServer;

    @Mock
    private PluginManager mockPluginManager;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(mockServer.getPluginManager()).thenReturn(mockPluginManager);
        Bukkit.setServer(mockServer);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("测试 ServiceRegistry 服务注册与获取")
    void testServiceRegistryIntegration() {
        ServiceRegistry registry = new cn.guangdian.rpgcore.service.SimpleServiceRegistry(null);
        
        TestService service = new TestServiceImpl();
        registry.registerService(TestService.class, service);
        
        TestService retrieved = registry.getService(TestService.class);
        assertNotNull(retrieved);
        assertEquals("Hello", retrieved.getMessage());
    }

    @Test
    @Order(2)
    @DisplayName("测试 PlayerLockManager 并发锁")
    void testPlayerLockManagerIntegration() {
        Logger mockLogger = mock(Logger.class);
        PlayerLockManager lockManager = new PlayerLockManager(mockLogger, 3000);
        UUID playerId = UUID.randomUUID();
        AtomicInteger counter = new AtomicInteger(0);
        
        try {
            for (int i = 0; i < 100; i++) {
                lockManager.executeWithLock(playerId, () -> {
                    counter.incrementAndGet();
                });
            }
            
            assertEquals(100, counter.get());
        } catch (LockTimeoutException e) {
            fail("锁超时: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("测试 PlayerLockManager 双锁转账场景")
    void testPlayerLockManagerDualLockIntegration() {
        Logger mockLogger = mock(Logger.class);
        PlayerLockManager lockManager = new PlayerLockManager(mockLogger, 3000);
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        
        AtomicInteger balance1 = new AtomicInteger(100);
        AtomicInteger balance2 = new AtomicInteger(50);
        
        try {
            boolean success = lockManager.executeWithDualLock(player1, player2, () -> {
                int amount = 30;
                if (balance1.get() >= amount) {
                    balance1.addAndGet(-amount);
                    balance2.addAndGet(amount);
                    return true;
                }
                return false;
            });
            
            assertTrue(success);
            assertEquals(70, balance1.get());
            assertEquals(80, balance2.get());
        } catch (LockTimeoutException e) {
            fail("锁超时: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("测试 EventPublisher 事件发布")
    void testEventPublisherIntegration() {
        AtomicInteger callCount = new AtomicInteger(0);
        
        Listener listener = new Listener() {};
        
        TestIntegrationEvent event = new TestIntegrationEvent();
        EventPublisher.publish(event);
        
        assertTrue(EventPublisher.getTotalPublished() > 0);
    }

    @Test
    @Order(5)
    @DisplayName("测试 MetricsExporter 指标导出")
    void testMetricsExporterIntegration() {
        RPGCore mockRPGCore = mock(RPGCore.class);
        when(mockRPGCore.getDescription()).thenReturn(new org.bukkit.plugin.PluginDescriptionFile(
            "RPGCore", "2.0.0", "cn.guangdian.rpgcore.RPGCore"
        ));
        
        MetricsExporter exporter = new MetricsExporter(mockRPGCore);
        String prometheusMetrics = exporter.exportPrometheusFormat();
        
        assertNotNull(prometheusMetrics);
        assertTrue(prometheusMetrics.contains("rpgcore_info"));
        assertTrue(prometheusMetrics.contains("version=\"2.0.0\""));
    }

    @Test
    @Order(6)
    @DisplayName("测试并发场景下的服务注册")
    void testConcurrentServiceRegistration() throws Exception {
        ServiceRegistry registry = new cn.guangdian.rpgcore.service.SimpleServiceRegistry(null);
        AtomicInteger successCount = new AtomicInteger(0);
        
        CompletableFuture<Void>[] futures = new CompletableFuture[10];
        
        for (int i = 0; i < 10; i++) {
            final int index = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    TestService service = new TestServiceImpl();
                    registry.registerService(TestService.class, service);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    fail("并发注册失败: " + e.getMessage());
                }
            });
        }
        
        CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);
        
        assertTrue(successCount.get() > 0);
        assertNotNull(registry.getService(TestService.class));
    }

    @Test
    @Order(7)
    @DisplayName("测试缓存提供者集成")
    void testCacheProviderIntegration() {
        RPGCore mockRPGCore = mock(RPGCore.class);
        CacheProvider cacheProvider = new cn.guangdian.rpgcore.cache.CaffeineCacheProvider(
            1000, java.time.Duration.ofMinutes(30), true
        );
        
        cacheProvider.put("test_key", "test_value");
        Object value = cacheProvider.get("test_key", String.class);
        
        assertEquals("test_value", value);
        
        cacheProvider.invalidate("test_key");
        assertNull(cacheProvider.get("test_key", String.class));
    }

    @Test
    @Order(8)
    @DisplayName("测试插件生命周期模拟")
    void testPluginLifecycleSimulation() {
        AtomicInteger initCount = new AtomicInteger(0);
        AtomicInteger enableCount = new AtomicInteger(0);
        AtomicInteger disableCount = new AtomicInteger(0);
        
        Runnable initTask = initCount::incrementAndGet;
        Runnable enableTask = enableCount::incrementAndGet;
        Runnable disableTask = disableCount::incrementAndGet;
        
        initTask.run();
        enableTask.run();
        disableTask.run();
        
        assertEquals(1, initCount.get());
        assertEquals(1, enableCount.get());
        assertEquals(1, disableCount.get());
    }

    interface TestService {
        String getMessage();
    }

    static class TestServiceImpl implements TestService {
        @Override
        public String getMessage() {
            return "Hello";
        }
    }

    static class TestIntegrationEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();
        
        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }
        
        public static HandlerList getHandlerList() {
            return HANDLERS;
        }
    }
}
