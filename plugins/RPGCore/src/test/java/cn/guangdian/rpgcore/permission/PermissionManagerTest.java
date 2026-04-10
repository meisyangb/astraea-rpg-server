package cn.guangdian.rpgcore.permission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PermissionManager 单元测试
 */
class PermissionManagerTest {

    private PermissionManager manager;

    @BeforeEach
    void setUp() {
        manager = new PermissionManager(null, "guangdian.test");
    }

    @Test
    @DisplayName("测试权限注册")
    void testRegisterPermission() {
        String perm = manager.register("feature.fly", "飞行权限", PermissionManager.DefaultPermission.OP);
        
        assertEquals("guangdian.test.feature.fly", perm);
        assertTrue(manager.getRegisteredPermissions().contains(perm));
    }

    @Test
    @DisplayName("测试命令权限注册")
    void testRegisterCommandPermission() {
        String perm = manager.registerCommand("reload", "重载配置");
        
        assertEquals("guangdian.test.command.reload", perm);
    }

    @Test
    @DisplayName("测试管理权限注册")
    void testRegisterAdminPermission() {
        String perm = manager.registerAdmin("bypass", "绕过限制");
        
        assertEquals("guangdian.test.admin.bypass", perm);
    }

    @Test
    @DisplayName("测试标准权限集")
    void testRegisterStandardPermissions() {
        manager.registerStandardPermissions();
        
        assertTrue(manager.getRegisteredPermissions().contains("guangdian.test.admin.*"));
        assertTrue(manager.getRegisteredPermissions().contains("guangdian.test.command.reload"));
        assertTrue(manager.getRegisteredPermissions().contains("guangdian.test.command.help"));
    }

    @Test
    @DisplayName("测试权限信息获取")
    void testGetPermissionInfo() {
        manager.register("feature.test", "测试功能", PermissionManager.DefaultPermission.TRUE);
        
        PermissionManager.PermissionNode info = manager.getPermissionInfo("guangdian.test.feature.test");
        
        assertNotNull(info);
        assertEquals("guangdian.test.feature.test", info.getFullPath());
        assertEquals("测试功能", info.getDescription());
        assertEquals(PermissionManager.DefaultPermission.TRUE, info.getDefaultValue());
    }

    @Test
    @DisplayName("测试默认权限枚举值")
    void testDefaultPermissionYmlValues() {
        assertEquals("true", PermissionManager.DefaultPermission.TRUE.getYmlValue());
        assertEquals("op", PermissionManager.DefaultPermission.OP.getYmlValue());
        assertEquals("false", PermissionManager.DefaultPermission.FALSE.getYmlValue());
    }

    @Test
    @DisplayName("测试生成plugin.yml权限配置")
    void testGeneratePluginYmlPermissions() {
        manager.register("test", "测试权限", PermissionManager.DefaultPermission.OP);
        
        String yml = manager.generatePluginYmlPermissions();
        
        assertTrue(yml.contains("permissions:"));
        assertTrue(yml.contains("description: 测试权限"));
        assertTrue(yml.contains("default: op"));
    }
}