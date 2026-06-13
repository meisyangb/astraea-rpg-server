package cn.guangdian.rpgcore.lifecycle;

import cn.guangdian.rpgcore.api.PluginLifecycleManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class PluginLifecycleManagerImpl implements PluginLifecycleManager {
    
    private final JavaPlugin hostPlugin;
    private final Logger logger;
    private final Map<String, PluginLifecycle> plugins = new ConcurrentHashMap<>();
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    
    public PluginLifecycleManagerImpl(JavaPlugin hostPlugin) {
        this.hostPlugin = hostPlugin;
        this.logger = hostPlugin.getLogger();
    }
    
    @Override
    public void registerPlugin(JavaPlugin plugin) {
        if (shuttingDown.get()) {
            logger.warning("[Lifecycle] Cannot register plugin during shutdown: " + plugin.getName());
            return;
        }
        
        String name = plugin.getName();
        if (plugins.containsKey(name)) {
            logger.warning("[Lifecycle] Plugin already registered: " + name);
            return;
        }
        
        plugins.put(name, new PluginLifecycle(plugin));
        logger.info("[Lifecycle] Registered: " + name);
    }
    
    @Override
    public void unregisterPlugin(String pluginName) {
        PluginLifecycle lifecycle = plugins.remove(pluginName);
        if (lifecycle != null) {
            lifecycle.disable();
            logger.info("[Lifecycle] Unregistered: " + pluginName);
        }
    }
    
    @Override
    public void enablePlugin(String pluginName) {
        PluginLifecycle lifecycle = plugins.get(pluginName);
        if (lifecycle != null && !lifecycle.isEnabled()) {
            lifecycle.enable();
            logger.info("[Lifecycle] Enabled: " + pluginName);
        }
    }
    
    @Override
    public void disablePlugin(String pluginName) {
        PluginLifecycle lifecycle = plugins.get(pluginName);
        if (lifecycle != null && lifecycle.isEnabled()) {
            lifecycle.disable();
            logger.info("[Lifecycle] Disabled: " + pluginName);
        }
    }
    
    @Override
    public void disableAll() {
        shuttingDown.set(true);
        plugins.values().forEach(PluginLifecycle::disable);
        plugins.clear();
        logger.info("[Lifecycle] All plugins disabled");
    }
    
    @Override
    public boolean isEnabled(String pluginName) {
        PluginLifecycle lifecycle = plugins.get(pluginName);
        return lifecycle != null && lifecycle.isEnabled();
    }
    
    @Override
    public int getActivePluginCount() {
        return (int) plugins.values().stream().filter(PluginLifecycle::isEnabled).count();
    }
    
    @Override
    public void runStartupHooks() {
        plugins.values().forEach(PluginLifecycle::runStartupHook);
    }
    
    @Override
    public void runShutdownHooks() {
        plugins.values().forEach(PluginLifecycle::runShutdownHook);
    }
    
    private static class PluginLifecycle {
        private final JavaPlugin plugin;
        private final AtomicBoolean enabled = new AtomicBoolean(false);
        private final AtomicBoolean startupHookRun = new AtomicBoolean(false);
        private final AtomicBoolean shutdownHookRun = new AtomicBoolean(false);
        
        PluginLifecycle(JavaPlugin plugin) {
            this.plugin = plugin;
            this.enabled.set(plugin.isEnabled());
        }
        
        void enable() {
            enabled.set(true);
        }
        
        void disable() {
            enabled.set(false);
        }
        
        boolean isEnabled() {
            return enabled.get();
        }
        
        void runStartupHook() {
            if (startupHookRun.compareAndSet(false, true)) {
            }
        }
        
        void runShutdownHook() {
            if (shutdownHookRun.compareAndSet(false, true)) {
            }
        }
    }
}
