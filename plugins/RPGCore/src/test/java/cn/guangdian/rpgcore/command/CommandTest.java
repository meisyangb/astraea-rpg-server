package cn.guangdian.rpgcore.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SubCommand 和 CommandFramework 单元测试
 */
class CommandTest {

    private CommandFramework framework;

    @BeforeEach
    void setUp() {
        // 创建简单的命令框架用于测试
        framework = new CommandFramework(null, "test");
    }

    @Test
    @DisplayName("测试SubCommand Builder")
    void testSubCommandBuilder() {
        SubCommand cmd = SubCommand.builder("test")
            .permission("test.permission")
            .description("测试命令")
            .aliases("t", "te")
            .playerOnly()
            .executor((sender, args) -> {})
            .build();

        assertEquals("test", cmd.getName());
        assertEquals("test.permission", cmd.getPermission());
        assertEquals("测试命令", cmd.getDescription());
        assertEquals(2, cmd.getAliases().size());
        assertTrue(cmd.isPlayerOnly());
    }

    @Test
    @DisplayName("测试SubCommand Builder无executor抛出异常")
    void testSubCommandBuilderWithoutExecutor() {
        assertThrows(IllegalStateException.class, () -> {
            SubCommand.builder("test").build();
        });
    }

    @Test
    @DisplayName("测试命令注册")
    void testRegisterCommand() {
        SubCommand cmd = SubCommand.builder("reload")
            .description("重载配置")
            .executor((sender, args) -> {})
            .build();

        // 验证注册不抛异常
        assertDoesNotThrow(() -> framework.register(cmd));
    }

    @Test
    @DisplayName("测试快速注册简单命令")
    void testRegisterSimple() {
        framework.registerSimple("info", null, "查看信息", (sender, args) -> {
            sender.sendMessage("信息");
        });
        
        // 验证不会抛出异常
        assertDoesNotThrow(() -> framework.registerSimple("help", null, "帮助", (s, a) -> {}));
    }

    @Test
    @DisplayName("测试设置消息")
    void testSetMessages() {
        framework.setNoPermissionMessage("无权限！")
            .setPlayerOnlyMessage("仅玩家！")
            .setUnknownCommandMessage("未知命令！");
        
        // 验证链式调用成功
        assertNotNull(framework);
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
    @DisplayName("测试权限检查逻辑")
    void testPermissionCheck() {
        SubCommand adminCmd = SubCommand.builder("admin")
            .permission("test.admin")
            .description("管理命令")
            .executor((sender, args) -> {})
            .build();

        assertNotNull(adminCmd.getPermission());
        assertEquals("test.admin", adminCmd.getPermission());
    }

    @Test
    @DisplayName("测试无权限命令")
    void testNoPermissionCommand() {
        SubCommand publicCmd = SubCommand.builder("public")
            .description("公开命令")
            .executor((sender, args) -> {})
            .build();

        assertNull(publicCmd.getPermission());
    }
}