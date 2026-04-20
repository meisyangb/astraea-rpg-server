package cn.guangdian.bank;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.GameLogger;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GuangDianBank 单元测试
 *
 * <p>测试 RPGCore 集成、日志系统和消息发送</p>
 *
 * @author GuangDian
 * @since 1.1.0
 */
public class GuangDianBankTest {

    @Mock
    private RPGCore mockRPGCore;

    @Mock
    private GameLogger mockGameLogger;

    @Mock
    private MiniMessageService mockMiniMessage;

    @Mock
    private Player mockPlayer;

    private GuangDianBank plugin;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        plugin = new GuangDianBank();
    }

    @Test
    @DisplayName("测试 RPGCore 可用时的日志调用")
    void testLogInfoWithRPGCore() {
        // 设置 mock
        try {
            java.lang.reflect.Field field = GuangDianBank.class.getDeclaredField("gameLogger");
            field.setAccessible(true);
            field.set(plugin, mockGameLogger);
        } catch (Exception e) {
            fail("设置 gameLogger 失败: " + e.getMessage());
        }

        // 调用日志方法
        plugin.logInfo("银行系统启动");

        // 验证
        verify(mockGameLogger).info("银行系统启动");
    }

    @Test
    @DisplayName("测试 RPGCore 不可用时正常降级")
    void testFallbackWhenRPGCoreUnavailable() {
        // 模拟 RPGCore 不可用
        try {
            java.lang.reflect.Field field = GuangDianBank.class.getDeclaredField("gameLogger");
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
        });
    }

    @Test
    @DisplayName("测试 MiniMessage 消息发送")
    void testSendMessageWithMiniMessage() {
        // 设置 mock
        try {
            java.lang.reflect.Field field = GuangDianBank.class.getDeclaredField("miniMessage");
            field.setAccessible(true);
            field.set(plugin, mockMiniMessage);
        } catch (Exception e) {
            fail("设置 miniMessage 失败: " + e.getMessage());
        }

        Component greenMessage = Component.text("✓ 成功消息");
        when(mockMiniMessage.green("✓ 成功消息")).thenReturn(greenMessage);

        // 调用消息发送
        plugin.sendSuccess(mockPlayer, "✓ 成功消息");

        // 验证
        verify(mockMiniMessage).green("✓ 成功消息");
        verify(mockPlayer).sendMessage(greenMessage);
    }

    @Test
    @DisplayName("测试消息发送降级")
    void testMessageFallback() {
        // 模拟 MiniMessage 不可用
        try {
            java.lang.reflect.Field field = GuangDianBank.class.getDeclaredField("miniMessage");
            field.setAccessible(true);
            field.set(plugin, null);
        } catch (Exception e) {
            fail("设置 miniMessage 失败: " + e.getMessage());
        }

        // 验证降级不会抛出异常
        assertDoesNotThrow(() -> {
            plugin.sendMessage(mockPlayer, "普通消息");
            plugin.sendSuccess(mockPlayer, "成功消息");
            plugin.sendError(mockPlayer, "错误消息");
        });
    }

    @Test
    @DisplayName("测试 isUsingRPGCore 方法")
    void testIsUsingRPGCore() {
        // RPGCore 可用时
        try {
            java.lang.reflect.Field field = GuangDianBank.class.getDeclaredField("gameLogger");
            field.setAccessible(true);
            field.set(plugin, mockGameLogger);
        } catch (Exception e) {
            fail("设置 gameLogger 失败: " + e.getMessage());
        }

        assertTrue(plugin.isUsingRPGCore());

        // RPGCore 不可用时
        try {
            java.lang.reflect.Field field = GuangDianBank.class.getDeclaredField("gameLogger");
            field.setAccessible(true);
            field.set(plugin, null);
        } catch (Exception e) {
            fail("设置 gameLogger 失败: " + e.getMessage());
        }

        assertFalse(plugin.isUsingRPGCore());
    }

    @Test
    @DisplayName("测试带异常的日志调用")
    void testLogWithException() {
        // 设置 mock
        try {
            java.lang.reflect.Field field = GuangDianBank.class.getDeclaredField("gameLogger");
            field.setAccessible(true);
            field.set(plugin, mockGameLogger);
        } catch (Exception e) {
            fail("设置 gameLogger 失败: " + e.getMessage());
        }

        Exception testException = new RuntimeException("数据库错误");

        // 调用带异常的日志方法
        plugin.logSevere("保存数据失败", testException);

        // 验证调用
        verify(mockGameLogger).severe("保存数据失败", testException);
    }
}
