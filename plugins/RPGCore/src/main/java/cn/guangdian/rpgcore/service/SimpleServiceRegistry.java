package cn.guangdian.rpgcore.service;

import cn.guangdian.rpgcore.api.ServicePriority;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 简单服务注册表实现
 * 
 * <p>基于内存的服务注册表，同时支持 Bukkit ServicesManager 的双重注册。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class SimpleServiceRegistry implements ServiceRegistry {

    private final Logger logger;
    private final JavaPlugin plugin;
    private final Map<Class<?>, ServiceEntry<?>> services;
    private final Object lock = new Object();

    /**
     * 创建服务注册表
     * 
     * @param plugin 插件实例
     */
    public SimpleServiceRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.services = new ConcurrentHashMap<>();
    }

    @Override
    public <T> void registerService(Class<T> serviceClass, T implementation, ServicePriority priority) {
        if (serviceClass == null) {
            throw new IllegalArgumentException("Service class cannot be null");
        }
        if (implementation == null) {
            throw new IllegalArgumentException("Implementation cannot be null");
        }
        if (priority == null) {
            priority = ServicePriority.NORMAL;
        }

        synchronized (lock) {
            // 检查是否已存在
            ServiceEntry<?> existing = services.get(serviceClass);
            if (existing != null) {
                // 比较优先级
                if (existing.priority.getLevel() >= priority.getLevel()) {
                    logger.fine("Service " + serviceClass.getSimpleName() + 
                        " already registered with higher or equal priority, skipping");
                    return;
                }
            }

            // 注册到内部注册表
            services.put(serviceClass, new ServiceEntry<>(implementation, priority));

            // 同时注册到 Bukkit ServicesManager
            try {
                // 映射优先级：RPGCore的NORMAL -> Bukkit的Normal
                org.bukkit.plugin.ServicePriority bukkitPriority = mapPriority(priority);
                Bukkit.getServicesManager().register(
                    serviceClass, 
                    implementation, 
                    plugin, 
                    bukkitPriority
                );
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to register service to Bukkit ServicesManager", e);
            }
        }

        logger.info("Registered service: " + serviceClass.getSimpleName() + 
            " (priority: " + priority + ")");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) {
        if (serviceClass == null) {
            throw new IllegalArgumentException("Service class cannot be null");
        }

        ServiceEntry<?> entry = services.get(serviceClass);
        if (entry != null) {
            return (T) entry.implementation;
        }

        // 尝试从 Bukkit ServicesManager 获取
        try {
            T bukkitService = Bukkit.getServicesManager().load(serviceClass);
            if (bukkitService != null) {
                return bukkitService;
            }
        } catch (Exception ignored) {
            // Bukkit 服务不可用
        }

        throw new IllegalStateException("Service not found: " + serviceClass.getSimpleName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getOptionalService(Class<T> serviceClass) {
        if (serviceClass == null) {
            return Optional.empty();
        }

        ServiceEntry<?> entry = services.get(serviceClass);
        if (entry != null) {
            return Optional.of((T) entry.implementation);
        }

        // 尝试从 Bukkit ServicesManager 获取
        try {
            T bukkitService = Bukkit.getServicesManager().load(serviceClass);
            if (bukkitService != null) {
                return Optional.of(bukkitService);
            }
        } catch (Exception ignored) {
            // Bukkit 服务不可用
        }

        return Optional.empty();
    }

    @Override
    public <T> boolean hasService(Class<T> serviceClass) {
        if (serviceClass == null) {
            return false;
        }

        if (services.containsKey(serviceClass)) {
            return true;
        }

        // 检查 Bukkit ServicesManager
        try {
            return Bukkit.getServicesManager().isProvidedFor(serviceClass);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public <T> void unregisterService(Class<T> serviceClass) {
        if (serviceClass == null) {
            return;
        }

        synchronized (lock) {
            ServiceEntry<?> entry = services.remove(serviceClass);
            
            if (entry != null) {
                // 从 Bukkit ServicesManager 注销
                try {
                    Bukkit.getServicesManager().unregisterAll(plugin);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to unregister service from Bukkit ServicesManager", e);
                }

                logger.info("Unregistered service: " + serviceClass.getSimpleName());
            }
        }
    }

    @Override
    public int getServiceCount() {
        return services.size();
    }

    @Override
    public void clear() {
        synchronized (lock) {
            services.clear();
            
            // 从 Bukkit ServicesManager 注销所有
            try {
                Bukkit.getServicesManager().unregisterAll(plugin);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to clear services from Bukkit ServicesManager", e);
            }
        }

        logger.info("Cleared all registered services");
    }

    /**
     * 获取所有已注册的服务类型
     */
    public Set<Class<?>> getRegisteredServiceTypes() {
        return Collections.unmodifiableSet(services.keySet());
    }

    /**
     * 映射RPGCore优先级到Bukkit优先级
     */
    private org.bukkit.plugin.ServicePriority mapPriority(ServicePriority priority) {
        switch (priority) {
            case HIGHEST:
                return org.bukkit.plugin.ServicePriority.Highest;
            case HIGH:
                return org.bukkit.plugin.ServicePriority.High;
            case NORMAL:
                return org.bukkit.plugin.ServicePriority.Normal;
            case LOW:
                return org.bukkit.plugin.ServicePriority.Low;
            case LOWEST:
                return org.bukkit.plugin.ServicePriority.Lowest;
            default:
                return org.bukkit.plugin.ServicePriority.Normal;
        }
    }

    /**
     * 服务条目
     */
    private static class ServiceEntry<T> {
        final T implementation;
        final ServicePriority priority;

        ServiceEntry(T implementation, ServicePriority priority) {
            this.implementation = implementation;
            this.priority = priority;
        }
    }
}