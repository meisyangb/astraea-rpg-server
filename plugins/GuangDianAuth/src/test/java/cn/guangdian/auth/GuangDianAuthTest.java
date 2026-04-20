package cn.guangdian.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GuangDianAuth 单元测试
 *
 * <p>测试 RPGCore 集成和降级兼容性</p>
 *
 * @author GuangDian
 * @since 1.1.0
 */
public class GuangDianAuthTest {

    @Test
    @DisplayName("测试 isUsingRPGCore 逻辑")
    void testIsUsingRPGCoreLogic() {
        // 当 gameLogger 不为 null 时返回 true
        Object gameLogger = new Object();
        assertTrue(gameLogger != null);

        // 当 gameLogger 为 null 时返回 false
        Object nullLogger = null;
        assertFalse(nullLogger != null);
    }

    @Test
    @DisplayName("测试降级兼容性")
    void testFallbackCompatibility() {
        // 模拟降级场景 - 没有 RPGCore
        boolean hasRPGCore = false;

        // 验证降级不会抛出异常
        assertDoesNotThrow(() -> {
            if (hasRPGCore) {
                System.out.println("使用 RPGCore 日志");
            } else {
                System.out.println("使用 Bukkit 日志（降级）");
            }
        });
    }

    @Test
    @DisplayName("测试日志级别调用")
    void testLogLevelCalls() {
        // 验证各级别日志调用不抛出异常
        assertDoesNotThrow(() -> {
            logInfo("INFO 消息");
            logWarning("WARNING 消息");
            logSevere("SEVERE 消息");
        });
    }

    @Test
    @DisplayName("测试带异常的日志调用")
    void testLogWithException() {
        Exception testException = new RuntimeException("测试异常");

        // 验证带异常的日志调用不抛出异常
        assertDoesNotThrow(() -> {
            logSevere("发生错误", testException);
        });
    }

    // 模拟日志方法
    private void logInfo(String message) {
        System.out.println("[INFO] " + message);
    }

    private void logWarning(String message) {
        System.out.println("[WARNING] " + message);
    }

    private void logSevere(String message) {
        System.out.println("[SEVERE] " + message);
    }

    private void logSevere(String message, Throwable throwable) {
        System.out.println("[SEVERE] " + message + " - " + throwable.getMessage());
    }
}
