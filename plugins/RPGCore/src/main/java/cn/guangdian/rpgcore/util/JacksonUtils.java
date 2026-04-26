package cn.guangdian.rpgcore.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Jackson 工具类 - JSON/YAML 序列化与反序列化
 *
 * <p>提供统一的 JSON/YAML 处理能力，替换原有的 Gson 实现。</p>
 *
 * <h3>特性：</h3>
 * <ul>
 *   <li>支持 JSON 和 YAML 格式</li>
 *   <li>自动处理 Java 8 日期时间类型</li>
 *   <li>忽略未知字段，提高兼容性</li>
 *   <li>格式化输出，便于阅读</li>
 * </ul>
 *
 * @author GuangDian
 * @since 1.1.0
 */
public class JacksonUtils {

    private static final ObjectMapper JSON_MAPPER;
    private static final ObjectMapper YAML_MAPPER;

    static {
        // JSON Mapper 配置
        JSON_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .deactivateDefaultTyping();

        // YAML Mapper 配置 - 禁用多态类型防止反序列化攻击
        YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .deactivateDefaultTyping();
    }

    private JacksonUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 获取 JSON ObjectMapper 实例
     *
     * @return JSON ObjectMapper
     */
    public static ObjectMapper getJsonMapper() {
        return JSON_MAPPER;
    }

    /**
     * 获取 YAML ObjectMapper 实例
     *
     * @return YAML ObjectMapper
     */
    public static ObjectMapper getYamlMapper() {
        return YAML_MAPPER;
    }

    // ==================== JSON 方法 ====================

    /**
     * 将对象序列化为 JSON 字符串
     *
     * @param obj 要序列化的对象
     * @return JSON 字符串
     * @throws JsonProcessingException 序列化失败时抛出
     */
    public static String toJson(Object obj) throws JsonProcessingException {
        return JSON_MAPPER.writeValueAsString(obj);
    }

    /**
     * 将对象序列化为 JSON 字符串（安全模式，失败返回 null）
     *
     * @param obj 要序列化的对象
     * @return JSON 字符串，失败返回 null
     */
    public static String toJsonSafe(Object obj) {
        try {
            return JSON_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 将 JSON 字符串反序列化为对象
     *
     * @param json JSON 字符串
     * @param clazz 目标类型
     * @param <T> 类型参数
     * @return 反序列化后的对象
     * @throws JsonProcessingException 反序列化失败时抛出
     */
    public static <T> T fromJson(String json, Class<T> clazz) throws JsonProcessingException {
        return JSON_MAPPER.readValue(json, clazz);
    }

    /**
     * 将 JSON 字符串反序列化为对象（安全模式，失败返回 null）
     *
     * @param json JSON 字符串
     * @param clazz 目标类型
     * @param <T> 类型参数
     * @return 反序列化后的对象，失败返回 null
     */
    public static <T> T fromJsonSafe(String json, Class<T> clazz) {
        try {
            return JSON_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 从 JSON 文件读取对象
     *
     * @param file JSON 文件
     * @param clazz 目标类型
     * @param <T> 类型参数
     * @return 反序列化后的对象
     * @throws IOException 读取失败时抛出
     */
    public static <T> T fromJsonFile(File file, Class<T> clazz) throws IOException {
        return JSON_MAPPER.readValue(file, clazz);
    }

    /**
     * 将对象写入 JSON 文件
     *
     * @param file 目标文件
     * @param obj 要写入的对象
     * @throws IOException 写入失败时抛出
     */
    public static void toJsonFile(File file, Object obj) throws IOException {
        JSON_MAPPER.writeValue(file, obj);
    }

    // ==================== YAML 方法 ====================

    /**
     * 将对象序列化为 YAML 字符串
     *
     * @param obj 要序列化的对象
     * @return YAML 字符串
     * @throws JsonProcessingException 序列化失败时抛出
     */
    public static String toYaml(Object obj) throws JsonProcessingException {
        return YAML_MAPPER.writeValueAsString(obj);
    }

    /**
     * 将对象序列化为 YAML 字符串（安全模式，失败返回 null）
     *
     * @param obj 要序列化的对象
     * @return YAML 字符串，失败返回 null
     */
    public static String toYamlSafe(Object obj) {
        try {
            return YAML_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 将 YAML 字符串反序列化为对象
     *
     * @param yaml YAML 字符串
     * @param clazz 目标类型
     * @param <T> 类型参数
     * @return 反序列化后的对象
     * @throws JsonProcessingException 反序列化失败时抛出
     */
    public static <T> T fromYaml(String yaml, Class<T> clazz) throws JsonProcessingException {
        return YAML_MAPPER.readValue(yaml, clazz);
    }

    /**
     * 将 YAML 字符串反序列化为对象（安全模式，失败返回 null）
     *
     * @param yaml YAML 字符串
     * @param clazz 目标类型
     * @param <T> 类型参数
     * @return 反序列化后的对象，失败返回 null
     */
    public static <T> T fromYamlSafe(String yaml, Class<T> clazz) {
        try {
            return YAML_MAPPER.readValue(yaml, clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 从 YAML 文件读取对象
     *
     * @param file YAML 文件
     * @param clazz 目标类型
     * @param <T> 类型参数
     * @return 反序列化后的对象
     * @throws IOException 读取失败时抛出
     */
    public static <T> T fromYamlFile(File file, Class<T> clazz) throws IOException {
        return YAML_MAPPER.readValue(file, clazz);
    }

    /**
     * 从 YAML 输入流读取对象
     *
     * @param inputStream YAML 输入流
     * @param clazz 目标类型
     * @param <T> 类型参数
     * @return 反序列化后的对象
     * @throws IOException 读取失败时抛出
     */
    public static <T> T fromYamlStream(InputStream inputStream, Class<T> clazz) throws IOException {
        return YAML_MAPPER.readValue(inputStream, clazz);
    }

    /**
     * 将对象写入 YAML 文件
     *
     * @param file 目标文件
     * @param obj 要写入的对象
     * @throws IOException 写入失败时抛出
     */
    public static void toYamlFile(File file, Object obj) throws IOException {
        YAML_MAPPER.writeValue(file, obj);
    }

    // ==================== 通用方法 ====================

    /**
     * 深度克隆对象（通过序列化/反序列化）
     *
     * @param obj 要克隆的对象
     * @param clazz 目标类型
     * @param <T> 类型参数
     * @return 克隆后的对象
     */
    public static <T> T deepClone(T obj, Class<T> clazz) {
        try {
            String json = JSON_MAPPER.writeValueAsString(obj);
            return JSON_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to clone object", e);
        }
    }

    /**
     * 验证字符串是否为有效的 JSON
     *
     * @param json 要验证的字符串
     * @return 是否为有效 JSON
     */
    public static boolean isValidJson(String json) {
        try {
            JSON_MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * 验证字符串是否为有效的 YAML
     *
     * @param yaml 要验证的字符串
     * @return 是否为有效 YAML
     */
    public static boolean isValidYaml(String yaml) {
        try {
            YAML_MAPPER.readTree(yaml);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}
