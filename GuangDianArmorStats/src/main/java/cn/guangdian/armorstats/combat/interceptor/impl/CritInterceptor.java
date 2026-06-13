package cn.guangdian.armorstats.combat.interceptor.impl;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.combat.DamageContext;
import cn.guangdian.armorstats.combat.interceptor.DamageInterceptor;
import cn.guangdian.armorstats.config.DamageDebugConfig;
import cn.guangdian.armorstats.data.PlayerStats;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 暴击拦截器
 * 处理暴击判定和暴击伤害计算
 *
 * 重要：暴击应该在攻击计算后、防御计算前处理
 * 使用 baseDamage 计算，而不是 finalDamage
 */
public class CritInterceptor implements DamageInterceptor {

    private double minCritDamage = 1.5;
    private double maxCritDamage = 3.0;  // 新增: 暴击伤害上限
    private DamageDebugConfig debugConfig;

    public CritInterceptor() {
        loadConfig();
        this.debugConfig = DamageDebugConfig.getInstance();
    }

    private void loadConfig() {
        var config = GuangDianArmorStats.getInstance().getConfig();
        var damageSection = config.getConfigurationSection("damage");
        if (damageSection != null) {
            minCritDamage = damageSection.getDouble("min_crit_damage", 1.5);
            maxCritDamage = damageSection.getDouble("max_crit_damage", 3.0);
        }
    }

    public void reloadConfig() {
        loadConfig();
    }

    public void setDebugConfig(DamageDebugConfig debugConfig) {
        this.debugConfig = debugConfig;
    }

    @Override
    public String getName() {
        return "CritInterceptor";
    }

    @Override
    public int getPriority() {
        return 12;  // 在 AttackInterceptor(10) 之后，DefenseInterceptor(20) 之前
    }

    @Override
    public Type getType() {
        return Type.ATTACK;  // ATTACK 类型，在 DEFENSE 阶段之前执行
    }

    @Override
    public boolean process(DamageContext context) {
        if (!context.isAttackerPlayer()) {
            return true;
        }

        PlayerStats stats = context.getAttackerStats();
        if (stats == null) {
            return true;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double critChance = stats.getCritChancePercent() / 100.0;

        debugConfig.logCrit("暴击几率: " + (critChance * 100) + "%");

        if (random.nextDouble() < critChance) {
            context.setCritical(true);

            // 基础暴击伤害 150%，装备提供额外暴击伤害
            final double BASE_CRIT_DAMAGE = 150.0;
            double totalCritDamage = BASE_CRIT_DAMAGE + stats.getCritDamagePercent();
            double critMultiplier = totalCritDamage / 100.0;

            // 应用暴击伤害下限
            if (critMultiplier < minCritDamage) {
                critMultiplier = minCritDamage;
            }

            // 应用暴击伤害上限，防止伤害爆炸
            if (critMultiplier > maxCritDamage) {
                debugConfig.logCrit("暴击伤害超过上限: " + critMultiplier + " -> " + maxCritDamage);
                critMultiplier = maxCritDamage;
            }

            double newDamage = context.getBaseDamage() * critMultiplier;
            context.setModifiedDamage(newDamage);
            context.setFinalDamage(newDamage);

            debugConfig.logCrit("★ 暴击! 倍率: " + critMultiplier + "x 伤害: " + context.getBaseDamage() + " -> " + newDamage);
        } else {
            debugConfig.logCrit("未暴击");
        }

        return true;
    }
}