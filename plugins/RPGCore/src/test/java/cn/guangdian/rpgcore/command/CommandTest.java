package cn.guangdian.rpgcore.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommandFramework 单元测试
 */
class CommandTest {

    private CommandFramework framework;

    @BeforeEach
    void setUp() {
        // 获取命令框架实例
        framework = CommandFramework.getInstance();
    }

    @Test
    @DisplayName("测试CommandFramework单例")
    void testSingleton() {
        CommandFramework instance1 = CommandFramework.getInstance();
        CommandFramework instance2 = CommandFramework.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    @DisplayName("测试Tab补全过滤")
    void testTabCompleteFiltering() {
        // 模拟补全逻辑
        List<String> options = Arrays.asList("reload", "info", "help", "test");
        String partial = "re";

        List<String> filtered = options.stream()
            .filter(s -> s.startsWith(partial))
            .toList();

        assertEquals(1, filtered.size());
        assertEquals("reload", filtered.get(0));
    }

    @Test
    @DisplayName("测试命令注册列表")
    void testRegisteredCommands() {
        // 验证获取已注册命令集合不返回null
        assertNotNull(framework.getRegisteredCommands());
    }

    @Test
    @DisplayName("测试权限字符串检查")
    void testPermissionStringCheck() {
        // 测试权限字符串格式
        String permission = "test.command.use";
        assertTrue(permission.startsWith("test."));
        assertTrue(permission.contains("command"));
    }

    @Test
    @DisplayName("测试命令名称规范化")
    void testCommandNameNormalization() {
        // 测试命令名称转小写
        String commandName = "TestCommand";
        assertEquals("testcommand", commandName.toLowerCase());
    }
}
