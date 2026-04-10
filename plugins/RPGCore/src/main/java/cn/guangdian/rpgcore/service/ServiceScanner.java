package cn.guangdian.rpgcore.service;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.annotation.RPGService;
import cn.guangdian.rpgcore.api.ServicePriority;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 服务自动扫描器
 *
 * <p>扫描所有已加载插件的类，发现标注了 @RPGService 注解的服务类并自动注册。</p>
 *
 * <h3>扫描机制：</h3>
 * <ul>
 *   <li>在 RPGCore 启动后扫描所有已加载插件</li>
 *   <li>解析插件 Jar 文件查找标注 @RPGService 的类</li>
 *   <li>按优先级排序后自动注册到 ServiceRegistry</li>
 *   <li>支持延迟加载和单例控制</li>
 * </ul>
 *
 * @author GuangDian
 * @since 2.0.0
 */
public class ServiceScanner {

    private final RPGCore plugin;
    private final Logger logger;
    private final ServiceRegistry serviceRegistry;

    /**
     * 已发现的注解服务信息
     */
    private final Map<Class<?>, ServiceInfo> discoveredServices = new ConcurrentHashMap<>();

    /**
     * 延迟加载的服务缓存
     */
    private final Map<Class<?>, Object> lazyServiceCache = new ConcurrentHashMap<>();

    /**
     * 是否已完成扫描
     */
    private volatile boolean scanned = false;

    /**
     * 创建服务扫描器
     */
    public ServiceScanner(RPGCore plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.serviceRegistry = plugin.getServiceRegistry();
    }

    /**
     * 执行全量扫描
     *
     * <p>扫描所有已加载的插件，发现并注册服务。</p>
     */
    public void scanAllPlugins() {
        if (scanned) {
            logger.warning("ServiceScanner already scanned, skipping duplicate scan");
            return;
        }

        logger.info("Starting service annotation scan...");
        int totalFound = 0;
        int totalRegistered = 0;

        // 扫描所有已加载插件
        for (Plugin loadedPlugin : Bukkit.getPluginManager().getPlugins()) {
            if (loadedPlugin.equals(plugin)) {
                continue; // 跳过 RPGCore 自身
            }

            try {
                List<Class<?>> annotatedClasses = scanPlugin(loadedPlugin);
                if (!annotatedClasses.isEmpty()) {
                    logger.info("Found " + annotatedClasses.size() + " @RPGService classes in " + loadedPlugin.getName());
                    totalFound += annotatedClasses.size();

                    // 注册发现的服务
                    for (Class<?> clazz : annotatedClasses) {
                        if (registerAnnotatedService(clazz)) {
                            totalRegistered++;
                        }
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to scan plugin: " + loadedPlugin.getName(), e);
            }
        }

        scanned = true;
        logger.info("Service scan completed. Found: " + totalFound + ", Registered: " + totalRegistered);
    }

    /**
     * 扫描单个插件
     *
     * @param targetPlugin 目标插件
     * @return 发现的标注 @RPGService 的类列表
     */
    public List<Class<?>> scanPlugin(Plugin targetPlugin) {
        List<Class<?>> result = new ArrayList<>();

        if (!(targetPlugin instanceof JavaPlugin javaPlugin)) {
            return result;
        }

        File pluginFile = getPluginFile(javaPlugin);
        if (pluginFile == null || !pluginFile.exists()) {
            return result;
        }

        // 扫描 Jar 文件
        try (JarFile jarFile = new JarFile(pluginFile)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                // 只处理 .class 文件
                if (!name.endsWith(".class")) {
                    continue;
                }

                // 转换为类名
                String className = name.replace('/', '.').substring(0, name.length() - 6);

                try {
                    Class<?> clazz = Class.forName(className, false, targetPlugin.getClass().getClassLoader());

                    // 检查是否有 @RPGService 注解
                    if (clazz.isAnnotationPresent(RPGService.class)) {
                        result.add(clazz);
                        logger.fine("Found @RPGService: " + className);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // 忽略无法加载的类（可能是依赖问题）
                } catch (Exception e) {
                    logger.log(Level.FINE, "Error checking class: " + className, e);
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to read plugin jar: " + pluginFile.getPath(), e);
        }

        return result;
    }

    /**
     * 注册标注了 @RPGService 的服务
     *
     * @param clazz 服务实现类
     * @return 是否成功注册
     */
    public boolean registerAnnotatedService(Class<?> clazz) {
        RPGService annotation = clazz.getAnnotation(RPGService.class);
        if (annotation == null) {
            return false;
        }

        Class<?> serviceInterface = annotation.serviceInterface();
        ServicePriority priority = annotation.priority();
        boolean lazy = annotation.lazy();
        boolean singleton = annotation.singleton();

        // 保存服务信息
        ServiceInfo info = new ServiceInfo(clazz, serviceInterface, priority, lazy, singleton);
        discoveredServices.put(clazz, info);

        // 延迟加载的服务不立即实例化
        if (lazy) {
            logger.info("Discovered lazy service: " + clazz.getSimpleName() + 
                " (interface: " + serviceInterface.getSimpleName() + ")");
            return true;
        }

        // 立即实例化并注册
        try {
            Object instance = createInstance(clazz, singleton);
            if (instance != null) {
                // 使用反射调用泛型方法
                registerServiceUnsafe(serviceInterface, instance, priority);
                logger.info("Auto-registered service: " + clazz.getSimpleName() + 
                    " -> " + serviceInterface.getSimpleName() + 
                    " (priority: " + priority + ")");
                return true;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to instantiate service: " + clazz.getSimpleName(), e);
        }

        return false;
    }

    /**
     * 获取延迟加载的服务实例
     *
     * @param serviceInterface 服务接口
     * @return 服务实例，如果不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T getLazyService(Class<T> serviceInterface) {
        // 查找对应的延迟服务信息
        for (ServiceInfo info : discoveredServices.values()) {
            if (info.lazy && info.serviceInterface.equals(serviceInterface)) {
                // 检查缓存
                if (info.singleton) {
                    Object cached = lazyServiceCache.get(info.implementationClass);
                    if (cached != null) {
                        return (T) cached;
                    }
                }

                // 创建实例
                try {
                    Object instance = createInstance(info.implementationClass, info.singleton);
                    if (info.singleton && instance != null) {
                        lazyServiceCache.put(info.implementationClass, instance);
                    }
                    
                    // 注册到 ServiceRegistry
                    if (instance != null) {
                        registerServiceUnsafe(serviceInterface, instance, info.priority);
                    }
                    
                    return (T) instance;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to create lazy service: " + info.implementationClass.getSimpleName(), e);
                }
            }
        }

        return null;
    }

    /**
     * 使用反射注册服务（处理泛型类型擦除问题）
     */
    @SuppressWarnings("unchecked")
    private <T> void registerServiceUnsafe(Class<T> serviceInterface, Object instance, cn.guangdian.rpgcore.api.ServicePriority priority) {
        serviceRegistry.registerService(serviceInterface, (T) instance, priority);
    }

    /**
     * 创建服务实例
     */
    private Object createInstance(Class<?> clazz, boolean singleton) throws Exception {
        if (singleton) {
            // 检查缓存
            Object cached = lazyServiceCache.get(clazz);
            if (cached != null) {
                return cached;
            }
        }

        // 尝试无参构造器
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            if (singleton) {
                lazyServiceCache.put(clazz, instance);
            }
            return instance;
        } catch (NoSuchMethodException e) {
            logger.warning("Service class " + clazz.getSimpleName() + " has no default constructor");
            return null;
        }
    }

    /**
     * 获取插件文件
     */
    private File getPluginFile(JavaPlugin javaPlugin) {
        try {
            // 通过 JavaPlugin.getFile() 方法获取
            java.lang.reflect.Field fileField = JavaPlugin.class.getDeclaredField("file");
            fileField.setAccessible(true);
            return (File) fileField.get(javaPlugin);
        } catch (Exception e) {
            // 回退方案：通过 URL 推断
            URL url = javaPlugin.getClass().getProtectionDomain().getCodeSource().getLocation();
            String path = java.net.URLDecoder.decode(url.getFile(), java.nio.charset.StandardCharsets.UTF_8);
            return new File(path);
        }
    }

    /**
     * 获取已发现的服务数量
     */
    public int getDiscoveredServiceCount() {
        return discoveredServices.size();
    }

    /**
     * 获取已发现的服务信息
     */
    public Map<Class<?>, ServiceInfo> getDiscoveredServices() {
        return new HashMap<>(discoveredServices);
    }

    /**
     * 检查是否已扫描
     */
    public boolean isScanned() {
        return scanned;
    }

    /**
     * 重置扫描状态（用于重新加载）
     */
    public void reset() {
        scanned = false;
        discoveredServices.clear();
        lazyServiceCache.clear();
    }

    /**
     * 服务信息结构
     */
    public static class ServiceInfo {
        public final Class<?> implementationClass;
        public final Class<?> serviceInterface;
        public final ServicePriority priority;
        public final boolean lazy;
        public final boolean singleton;
        public final String name;

        ServiceInfo(Class<?> impl, Class<?> interfaceClass, ServicePriority priority, boolean lazy, boolean singleton) {
            this.implementationClass = impl;
            this.serviceInterface = interfaceClass;
            this.priority = priority;
            this.lazy = lazy;
            this.singleton = singleton;
            this.name = impl.getSimpleName();
        }

        @Override
        public String toString() {
            return "ServiceInfo{" +
                "impl=" + implementationClass.getSimpleName() +
                ", interface=" + serviceInterface.getSimpleName() +
                ", priority=" + priority +
                ", lazy=" + lazy +
                ", singleton=" + singleton +
                '}';
        }
    }
}