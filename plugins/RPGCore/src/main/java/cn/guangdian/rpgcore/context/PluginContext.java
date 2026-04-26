package cn.guangdian.rpgcore.context;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 插件上下文管理器
 * 
 * <p>使用 ThreadLocal 存储当前线程的插件上下文，替代堆栈跟踪方式，
 * 提供更可靠和高效的插件识别机制。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 在插件入口设置上下文
 * public class MyPlugin extends JavaPlugin {
 *     @Override
 *     public void onEnable() {
 *         PluginContext.setPlugin(this);
 *         // 插件逻辑
 *     }
 * }
 * 
 * // 获取当前插件
 * String pluginName = PluginContext.getCurrentPluginName();
 * }</pre>
 * 
 * @author GuangDian
 * @since 2.0.0
 */
public final class PluginContext {

    private static final ThreadLocal<String> CURRENT_PLUGIN = new ThreadLocal<>();
    
    private static final ConcurrentHashMap<String, Plugin> PLUGIN_CACHE = new ConcurrentHashMap<>();
    
    private static final AtomicReference<String> LAST_PLUGIN = new AtomicReference<>("Unknown");

    private PluginContext() {
    }

    /**
     * 设置当前线程的插件上下文
     * 
     * @param pluginName 插件名称
     */
    public static void setPlugin(String pluginName) {
        if (pluginName != null && !pluginName.isEmpty()) {
            CURRENT_PLUGIN.set(pluginName);
            LAST_PLUGIN.set(pluginName);
            
            Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
            if (plugin != null) {
                PLUGIN_CACHE.put(pluginName, plugin);
            }
        }
    }

    /**
     * 设置当前线程的插件上下文
     * 
     * @param plugin 插件实例
     */
    public static void setPlugin(Plugin plugin) {
        if (plugin != null) {
            setPlugin(plugin.getName());
        }
    }

    /**
     * 获取当前线程的插件名称
     * 
     * @return 插件名称，如果未设置则返回 "Unknown"
     */
    public static String getCurrentPluginName() {
        String pluginName = CURRENT_PLUGIN.get();
        if (pluginName != null) {
            return pluginName;
        }
        
        pluginName = detectPluginFromStack();
        if (pluginName != null) {
            CURRENT_PLUGIN.set(pluginName);
            return pluginName;
        }
        
        return LAST_PLUGIN.get();
    }

    /**
     * 获取当前线程的插件实例
     * 
     * @return 插件实例，如果未找到则返回 null
     */
    public static Plugin getCurrentPlugin() {
        String pluginName = getCurrentPluginName();
        if (pluginName == null || "Unknown".equals(pluginName)) {
            return null;
        }
        
        Plugin plugin = PLUGIN_CACHE.get(pluginName);
        if (plugin == null) {
            plugin = Bukkit.getPluginManager().getPlugin(pluginName);
            if (plugin != null) {
                PLUGIN_CACHE.put(pluginName, plugin);
            }
        }
        
        return plugin;
    }

    /**
     * 清除当前线程的插件上下文
     */
    public static void clear() {
        CURRENT_PLUGIN.remove();
    }

    /**
     * 检查当前插件是否被授权
     * 
     * @param requiredPermission 需要的权限（可选）
     * @return 如果授权返回 true
     */
    public static boolean isAuthorized(String requiredPermission) {
        String pluginName = getCurrentPluginName();
        
        if ("Unknown".equals(pluginName)) {
            return false;
        }
        
        if (isSystemPlugin(pluginName)) {
            return true;
        }
        
        if (requiredPermission != null) {
            Plugin plugin = getCurrentPlugin();
            if (plugin == null) {
                return false;
            }
            
            return plugin.isEnabled();
        }
        
        return true;
    }

    /**
     * 检查是否为系统插件
     */
    private static boolean isSystemPlugin(String pluginName) {
        return "RPGCore".equals(pluginName) ||
               "Unknown".equals(pluginName) ||
               pluginName.startsWith("Bukkit") ||
               pluginName.startsWith("Minecraft");
    }

    /**
     * 从堆栈跟踪检测插件（降级方案）
     */
    private static String detectPluginFromStack() {
        try {
            StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
            
            for (StackWalker.StackFrame frame : walker.walk(s -> s.skip(2).limit(20).toList())) {
                Class<?> clazz = frame.getDeclaringClass();
                String className = clazz.getName();
                
                if (className.startsWith("cn.guangdian.rpgcore")) {
                    continue;
                }
                if (className.startsWith("org.bukkit")) {
                    continue;
                }
                if (className.startsWith("java.")) {
                    continue;
                }
                if (className.startsWith("javax.")) {
                    continue;
                }
                if (className.startsWith("com.google.")) {
                    continue;
                }
                
                String[] parts = className.split("\\.");
                if (parts.length >= 3) {
                    String potentialPluginName = parts[2];
                    Plugin plugin = Bukkit.getPluginManager().getPlugin(potentialPluginName);
                    if (plugin != null) {
                        return potentialPluginName;
                    }
                }
                
                return clazz.getSimpleName();
            }
        } catch (Exception e) {
        }
        
        return null;
    }

    /**
     * 获取统计信息
     */
    public static String getStats() {
        return String.format("PluginContext{cached=%d, last=%s}", 
            PLUGIN_CACHE.size(), LAST_PLUGIN.get());
    }
}
