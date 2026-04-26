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
    private final ConcurrentHashMap<Class<?>, ServiceEntry<?>> services;

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

        final ServicePriority finalPriority = priority;
        final boolean[] registered = {false};
        
        services.compute(serviceClass, (key, existing) -> {
            if (existing != null && existing.priority.getLevel() >= finalPriority.getLevel()) {
                logger.fine("Service " + serviceClass.getSimpleName() + 
                    " already registered with higher or equal priority, skipping");
                return existing;
            }
            
            registered[0] = true;
            return new ServiceEntry<>(implementation, finalPriority);
        });

        // 只有在成功注册时才注册到 Bukkit
        if (registered[0]) {
            try {
                org.bukkit.plugin.ServicePriority bukkitPriority = mapPriority(finalPriority);
                Bukkit.getServicesManager().register(
                    serviceClass, 
                    implementation, 
                    plugin, 
                    bukkitPriority
                );
                
                logger.info("Registered service: " + serviceClass.getSimpleName() + 
                    " (priority: " + finalPriority + ")");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to register service to Bukkit ServicesManager", e);
            }
        }
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
        } catch (IllegalStateException e) {
            // Bukkit 服务管理器未就绪
            logger.log(Level.FINE, "Bukkit ServicesManager not ready", e);
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
        } catch (IllegalStateException e) {
            // Bukkit 服务管理器未就绪
            logger.log(Level.FINE, "Bukkit ServicesManager not ready", e);
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
        } catch (IllegalStateException e) {
            // Bukkit 服务管理器未就绪
            logger.log(Level.FINE, "Bukkit ServicesManager not ready", e);
            return false;
        }
    }

    @Override
    public <T> void unregisterService(Class<T> serviceClass) {
        if (serviceClass == null) {
            return;
        }

        ServiceEntry<?> entry = services.remove(serviceClass);

        if (entry != null) {
            try {
                Bukkit.getServicesManager().unregister(entry.implementation);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to unregister service from Bukkit ServicesManager", e);
            }

            logger.info("Unregistered service: " + serviceClass.getSimpleName());
        }
    }

    @Override
    public int getServiceCount() {
        return services.size();
    }

    @Override
    public void clear() {
        services.clear();
        
        try {
            Bukkit.getServicesManager().unregisterAll(plugin);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to clear services from Bukkit ServicesManager", e);
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