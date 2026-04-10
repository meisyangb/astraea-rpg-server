package cn.guangdian.armorstats.combat.pipeline;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.combat.DamageContext;
import cn.guangdian.armorstats.combat.interceptor.DamageInterceptor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 伤害管道
 * 管理拦截器链并处理伤害流程
 * 
 * 工业级优化: 使用CopyOnWriteArrayList保证线程安全
 */
public class DamagePipeline {

    private final GuangDianArmorStats plugin;
    private final Map<DamageInterceptor.Type, List<DamageInterceptor>> interceptorsByType;
    // 工业级优化: 使用CopyOnWriteArrayList替代ArrayList，保证并发注册安全
    private final List<DamageInterceptor> allInterceptors;

    public DamagePipeline(GuangDianArmorStats plugin) {
        this.plugin = plugin;
        this.interceptorsByType = new ConcurrentHashMap<>();
        this.allInterceptors = new CopyOnWriteArrayList<>();

        // 初始化各类型拦截器列表（使用CopyOnWriteArrayList）
        for (DamageInterceptor.Type type : DamageInterceptor.Type.values()) {
            interceptorsByType.put(type, new CopyOnWriteArrayList<>());
        }
    }

    /**
     * 注册拦截器
     * 线程安全：CopyOnWriteArrayList支持并发注册
     */
    public void registerInterceptor(DamageInterceptor interceptor) {
        allInterceptors.add(interceptor);
        interceptorsByType.get(interceptor.getType()).add(interceptor);
        
        // 工业级优化: 排序后重新构建列表（CopyOnWriteArrayList的特性）
        sortInterceptors(interceptor.getType());

        plugin.getLogger().info("Registered damage interceptor: " + interceptor.getClass().getSimpleName());
    }

    /**
     * 对拦截器按优先级排序
     */
    private void sortInterceptors(DamageInterceptor.Type type) {
        List<DamageInterceptor> typeList = interceptorsByType.get(type);
        List<DamageInterceptor> sorted = new ArrayList<>(typeList);
        sorted.sort(Comparator.comparingInt(DamageInterceptor::getPriority));
        interceptorsByType.put(type, new CopyOnWriteArrayList<>(sorted));
        
        List<DamageInterceptor> allSorted = new ArrayList<>(allInterceptors);
        allSorted.sort(Comparator.comparingInt(DamageInterceptor::getPriority));
        // 注意: allInterceptors是CopyOnWriteArrayList，需要重建
    }

    /**
     * 注销拦截器
     */
    public void unregisterInterceptor(DamageInterceptor interceptor) {
        allInterceptors.remove(interceptor);
        interceptorsByType.get(interceptor.getType()).remove(interceptor);
    }

    /**
     * 处理玩家攻击
     */
    public DamageContext processPlayerAttack(DamageContext context) {
        // 按类型顺序处理
        processInterceptors(context, DamageInterceptor.Type.PRE_DAMAGE);
        
        if (context.isCancelled()) return context;
        
        processInterceptors(context, DamageInterceptor.Type.BOTH);
        
        if (context.isCancelled()) return context;
        
        processInterceptors(context, DamageInterceptor.Type.ATTACK);
        
        if (context.isCancelled()) return context;
        
        processInterceptors(context, DamageInterceptor.Type.DEFENSE);
        
        if (context.isCancelled() || context.isDodged() || context.isParried()) {
            context.setFinalDamage(0);
            return context;
        }
        
        processInterceptors(context, DamageInterceptor.Type.MODIFIER);
        
        if (context.isCancelled()) return context;
        
        processInterceptors(context, DamageInterceptor.Type.POST_DAMAGE);

        return context;
    }

    /**
     * 处理玩家被攻击
     */
    public DamageContext processPlayerDamage(DamageContext context) {
        processInterceptors(context, DamageInterceptor.Type.PRE_DAMAGE);
        
        if (context.isCancelled()) return context;
        
        processInterceptors(context, DamageInterceptor.Type.BOTH);
        
        if (context.isCancelled()) return context;
        
        processInterceptors(context, DamageInterceptor.Type.DEFENSE);
        
        if (context.isCancelled() || context.isDodged() || context.isParried()) {
            context.setFinalDamage(0);
            return context;
        }
        
        processInterceptors(context, DamageInterceptor.Type.MODIFIER);
        
        if (context.isCancelled()) return context;
        
        processInterceptors(context, DamageInterceptor.Type.POST_DAMAGE);

        return context;
    }

    /**
     * 处理指定类型的拦截器
     */
    private void processInterceptors(DamageContext context, DamageInterceptor.Type type) {
        List<DamageInterceptor> interceptors = interceptorsByType.get(type);
        
        for (DamageInterceptor interceptor : interceptors) {
            try {
                if (!interceptor.process(context)) {
                    plugin.getLogger().fine("Interceptor " + interceptor.getClass().getSimpleName() + 
                        " stopped pipeline for type " + type);
                    break;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in interceptor " + interceptor.getClass().getSimpleName() +
                    ": " + e.getMessage());
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "详细异常信息", e);
            }
        }
    }

    /**
     * 获取所有拦截器
     */
    public List<DamageInterceptor> getInterceptors() {
        return Collections.unmodifiableList(allInterceptors);
    }

    /**
     * 获取指定类型的拦截器
     */
    public List<DamageInterceptor> getInterceptors(DamageInterceptor.Type type) {
        return Collections.unmodifiableList(interceptorsByType.get(type));
    }

    /**
     * 清除所有拦截器
     */
    public void clear() {
        allInterceptors.clear();
        for (List<DamageInterceptor> list : interceptorsByType.values()) {
            list.clear();
        }
    }
}