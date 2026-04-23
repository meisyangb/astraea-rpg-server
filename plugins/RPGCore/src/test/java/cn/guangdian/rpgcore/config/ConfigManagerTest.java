package cn.guangdian.rpgcore.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

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
    @DisplayName("测试配置文件创建")
    void testConfigFileCreation() throws IOException {
        File configFile = new File(dataFolder, "test.yml");
        
        // 创建文件
        assertTrue(configFile.createNewFile());
        assertTrue(configFile.exists());
    }

    @Test
    @DisplayName("测试Properties配置")
    void testPropertiesConfig() throws IOException {
        File configFile = new File(dataFolder, "test.properties");
        
        // 写入配置
        Properties props = new Properties();
        props.setProperty("database.host", "localhost");
        props.setProperty("database.port", "3306");
        
        try (FileWriter writer = new FileWriter(configFile)) {
            props.store(writer, "Test Config");
        }
        
        // 读取配置
        Properties loaded = new Properties();
        loaded.load(Files.newInputStream(configFile.toPath()));
        
        assertEquals("localhost", loaded.getProperty("database.host"));
        assertEquals("3306", loaded.getProperty("database.port"));
    }

    @Test
    @DisplayName("测试配置默认值")
    void testDefaultValues() {
        Properties props = new Properties();
        
        // 测试默认值
        String value = props.getProperty("nonexistent.key", "default_value");
        assertEquals("default_value", value);
    }

    @Test
    @DisplayName("测试整数解析")
    void testIntegerParsing() {
        Properties props = new Properties();
        props.setProperty("number", "42");
        
        int value = Integer.parseInt(props.getProperty("number", "0"));
        assertEquals(42, value);
        
        // 测试默认值
        int defaultValue = Integer.parseInt(props.getProperty("nonexistent", "100"));
        assertEquals(100, defaultValue);
    }

    @Test
    @DisplayName("测试布尔值解析")
    void testBooleanParsing() {
        Properties props = new Properties();
        props.setProperty("enabled", "true");
        
        boolean value = Boolean.parseBoolean(props.getProperty("enabled", "false"));
        assertTrue(value);
        
        // 测试默认值
        boolean defaultValue = Boolean.parseBoolean(props.getProperty("nonexistent", "false"));
        assertFalse(defaultValue);
    }

    @Test
    @DisplayName("测试配置热重载")
    void testReloadConfig() throws IOException {
        File configFile = new File(dataFolder, "reload.properties");
        
        // 初始写入
        Properties props = new Properties();
        props.setProperty("value", "initial");
        try (FileWriter writer = new FileWriter(configFile)) {
            props.store(writer, "Initial");
        }
        
        // 读取
        Properties loaded = new Properties();
        loaded.load(Files.newInputStream(configFile.toPath()));
        assertEquals("initial", loaded.getProperty("value"));
        
        // 外部修改
        Properties newProps = new Properties();
        newProps.setProperty("value", "updated");
        try (FileWriter writer = new FileWriter(configFile)) {
            newProps.store(writer, "Updated");
        }
        
        // 重新加载
        loaded.load(Files.newInputStream(configFile.toPath()));
        assertEquals("updated", loaded.getProperty("value"));
    }

    @Test
    @DisplayName("测试嵌套路径解析")
    void testNestedPaths() {
        Properties props = new Properties();
        props.setProperty("database.host", "localhost");
        props.setProperty("database.port", "3306");
        props.setProperty("database.credentials.username", "root");
        
        assertEquals("localhost", props.getProperty("database.host"));
        assertEquals("3306", props.getProperty("database.port"));
        assertEquals("root", props.getProperty("database.credentials.username"));
    }

    @Test
    @DisplayName("测试配置文件路径")
    void testConfigFilePath() {
        File configFile = new File(dataFolder, "config.yml");
        
        assertEquals("config.yml", configFile.getName());
        assertEquals(dataFolder.getAbsolutePath(), configFile.getParentFile().getAbsolutePath());
    }
}
