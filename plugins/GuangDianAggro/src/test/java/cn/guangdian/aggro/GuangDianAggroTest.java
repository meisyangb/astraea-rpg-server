package cn.guangdian.aggro;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.GameLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GuangDianAggro 单元测试
 *
 * <p>测试 RPGCore 集成、日志系统和调试日志</p>
 *
 * @author GuangDian
 * @since 1.1.0
 */
public class GuangDianAggroTest {

    @Mock
    private RPGCore mockRPGCore;

    @Mock
    private GameLogger mockGameLogger;

    private GuangDianAggro plugin;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        plugin = new GuangDianAggro();
    }

    @Test
    @DisplayName("测试 RPGCore 可用时的日志调用")
    void testLogInfoWithRPGCore() {
        // 设置 mock
        try {
            java.lang.reflect.Field field = GuangDianAggro.class.getDeclaredField("gameLogger");
            field.setAccessible(true);
            field.set(plugin, mockGameLogger);
        } catch (Exception e) {
            fail("设置 gameLogger 失败: " + e.getMessage());
        }

        // 调用日志方法
        plugin.logInfo("仇恨系统启动");

        // 验证
        verify(mockGameLogger).info("仇恨系统启动");
    }

    @Test
    @DisplayName("测试 RPGCore 不可用时正常降级")
    void testFallbackWhenRPGCoreUnavailable() {
        // 模拟 RPGCore 不可用
        try {
            java.lang.reflect.Field field = GuangDianAggro.class.getDeclaredField("gameLogger");
            field.setAccessible(true);
            field.set(plugin, null);
        } catch (Exception e) {
            fail("设置 gameLogger 失败: " + e.getMessage());
        }

        // 验证降级不会抛出异常
        assertDoesNotThrow(() -> {
            plugin.logInfo("测试消息");
            plugin.logWarning("警告消息");
            plugin.logSevere("错误消息");
            plugin.logDebug("调试消息");
        });
    }

    @Test
    @DisplayName("测试 DEBUG 日志级别")
    void testDebugLogLevel() {
        // 设置 mock
        try {
            java.lang.reflect.Field field = GuangDianAggro.class.getDeclaredField("gameLogger");
            field.setAccessible(true);
            field.set(plugin, mockGameLogger);
        } catch (Exception e) {
            fail("设置 gameLogger 失败: " + e.getMessage());
        }

        // 调用 DEBUG 日志
        plugin.logDebug("仇恨衰减统计");

        // 验证
        verify(mockGameLogger).debug("仇恨衰减统计");
    }

    @Test
    @DisplayName("测试 DEBUG 日志在降级时不输出")
    void testDebugNotOutputWhenFallback() {
        // 模拟 RPGCore 不可用
        try {
            java.lang.reflect.Field field = GuangDianAggro.class.getDeclaredField("gameLogger");
            field.setAccessible(true);
            field.set(plugin, null);
        } catch (Exception e) {
            fail("设置 gameLogger 失败: " + e.getMessage());
        }

        // 调用 DEBUG 日志 - 应该静默处理
        assertDoesNotThrow(() -> plugin.logDebug("调试消息"));

        // 由于没有 mock Bukkit Logger，无法验证，但应该不抛出异常
    }

    @Test
    @DisplayName("测试 isUsingRPGCore 方法")
    void testIsUsingRPGCore() {
        // RPGCore 可用时
        try {
            java.lang.reflect.Field field = GuangDianAggro.class.getDeclaredField("gameLogger");
            field.setAccessible(true);
            field.set(plugin, mockGameLogger);
        } catch (Exception e) {
            fail("设置 gameLogger 失败: " + e.getMessage());
        }

        assertTrue(plugin.isUsingRPGCore());

        // RPGCore 不可用时
        try {
            java.lang.reflect.Field field = GuangDianAggro.class.getDeclaredField("gameLogger");
            field.setAccessible(true);
            field.set(plugin, null);
        } catch (Exception e) {
            fail("设置 gameLogger 失败: " + e.getMessage());
        }

        assertFalse(plugin.isUsingRPGCore());
    }

    @Test
    @DisplayName("测试所有日志级别")
    void testAllLogLevels() {
        // 设置 mock
        try {
            java.lang.reflect.Field field = GuangDianAggro.class.getDeclaredField("gameLogger");
            field.setAccessible(true);
            field.set(plugin, mockGameLogger);
        } catch (Exception e) {
            fail("设置 gameLogger 失败: " + e.getMessage());
        }

        // 测试各级别日志
        plugin.logInfo("INFO 消息");
        plugin.logWarning("WARNING 消息");
        plugin.logSevere("SEVERE 消息");
        plugin.logDebug("DEBUG 消息");

        // 验证调用
        verify(mockGameLogger).info("INFO 消息");
        verify(mockGameLogger).warning("WARNING 消息");
        verify(mockGameLogger).severe("SEVERE 消息");
        verify(mockGameLogger).debug("DEBUG 消息");
    }

    @Test
    @DisplayName("测试带异常的日志调用")
    void testLogWithException() {
        // 设置 mock
        try {
            java.lang.reflect.Field field = GuangDianAggro.class.getDeclaredField("gameLogger");
            field.setAccessible(true);
            field.set(plugin, mockGameLogger);
        } catch (Exception e) {
            fail("设置 gameLogger 失败: " + e.getMessage());
        }

        Exception testException = new RuntimeException("仇恨计算错误");

        // 调用带异常的日志方法
        plugin.logSevere("处理仇恨失败", testException);

        // 验证调用
        verify(mockGameLogger).severe("处理仇恨失败", testException);
    }
}
