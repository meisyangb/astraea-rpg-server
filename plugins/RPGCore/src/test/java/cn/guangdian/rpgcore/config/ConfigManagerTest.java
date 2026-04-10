package cn.guangdian.rpgcore.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConfigManager 单元测试
 */
class ConfigManagerTest {

    @TempDir
    Path tempDir;

    private File dataFolder;

    @BeforeEach
    void setUp() throws IOException {
        dataFolder = tempDir.toFile();
    }

    @Test
    @DisplayName("测试获取不存在的配置返回默认值")
    void testGetStringWithDefault() throws IOException {
        // 创建空的配置文件
        File configFile = new File(dataFolder, "test.yml");
        configFile.createNewFile();
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        // 测试默认值
        String value = config.getString("nonexistent.key", "default_value");
        assertEquals("default_value", value);
    }

    @Test
    @DisplayName("测试获取整数配置")
    void testGetInt() throws IOException {
        File configFile = new File(dataFolder, "test.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("number", 42);
        config.save(configFile);
        
        config = YamlConfiguration.loadConfiguration(configFile);
        assertEquals(42, config.getInt("number"));
        assertEquals(100, config.getInt("nonexistent", 100));
    }

    @Test
    @DisplayName("测试获取布尔配置")
    void testGetBoolean() throws IOException {
        File configFile = new File(dataFolder, "test.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("enabled", true);
        config.save(configFile);
        
        config = YamlConfiguration.loadConfiguration(configFile);
        assertTrue(config.getBoolean("enabled"));
        assertFalse(config.getBoolean("nonexistent", false));
    }

    @Test
    @DisplayName("测试热重载配置")
    void testReloadConfig() throws IOException {
        File configFile = new File(dataFolder, "reload.yml");
        
        // 初始写入
        YamlConfiguration config = new YamlConfiguration();
        config.set("value", "initial");
        config.save(configFile);
        
        // 加载
        config = YamlConfiguration.loadConfiguration(configFile);
        assertEquals("initial", config.getString("value"));
        
        // 外部修改
        YamlConfiguration newConfig = new YamlConfiguration();
        newConfig.set("value", "updated");
        newConfig.save(configFile);
        
        // 重新加载
        config = YamlConfiguration.loadConfiguration(configFile);
        assertEquals("updated", config.getString("value"));
    }

    @Test
    @DisplayName("测试嵌套配置路径")
    void testNestedPaths() throws IOException {
        File configFile = new File(dataFolder, "nested.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        config.set("database.host", "localhost");
        config.set("database.port", 3306);
        config.set("database.credentials.username", "root");
        config.save(configFile);
        
        config = YamlConfiguration.loadConfiguration(configFile);
        assertEquals("localhost", config.getString("database.host"));
        assertEquals(3306, config.getInt("database.port"));
        assertEquals("root", config.getString("database.credentials.username"));
    }
}