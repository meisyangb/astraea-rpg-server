package cn.guangdian.armorstats.combat.interceptor;

import cn.guangdian.armorstats.combat.DamageContext;

/**
 * 伤害拦截器接口
 * 用于处理伤害流程中的各个阶段
 */
public interface DamageInterceptor {

    /**
     * 获取拦截器名称
     */
    String getName();

    /**
     * 获取拦截器优先级（数值越小越先执行）
     */
    default int getPriority() {
        return 100;
    }

    /**
     * 处理伤害上下文
     * @param context 伤害上下文
     * @return true继续执行下一个拦截器，false中断流程
     */
    boolean process(DamageContext context);

    /**
     * 拦截器类型
     */
    enum Type {
        PRE_DAMAGE,      // 伤害前处理（验证、初始化）
        ATTACK,          // 攻击处理
        DEFENSE,         // 防御处理
        MODIFIER,        // 伤害修正（暴击、增伤）
        POST_DAMAGE,     // 伤害后处理（吸血、反伤、日志）
        BOTH             // 攻击和防御都处理（用于BOSS属性）
    }

    /**
     * 获取拦截器类型
     */
    Type getType();
}