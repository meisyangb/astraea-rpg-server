package cn.guangdian.armorstats.combat.interceptor.impl;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.combat.DamageContext;
import cn.guangdian.armorstats.combat.DamageSource;
import cn.guangdian.armorstats.combat.interceptor.DamageInterceptor;
import cn.guangdian.armorstats.config.DamageDebugConfig;
import cn.guangdian.armorstats.data.PlayerStats;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 攻击拦截器
 * 只负责计算攻击伤害，防御减伤由 DefenseInterceptor 处理
 */
public class AttackInterceptor implements DamageInterceptor {

    private DamageDebugConfig debugConfig;

    public AttackInterceptor() {
        this.debugConfig = DamageDebugConfig.getInstance();
    }

    @Override
    public String getName() {
        return "AttackInterceptor";
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public Type getType() {
        return Type.ATTACK;
    }

    public void setDebugConfig(DamageDebugConfig debugConfig) {
        this.debugConfig = debugConfig;
    }

    @Override
    public boolean process(DamageContext context) {
        if (!context.isAttackerPlayer()) {
            return true;
        }

        Player attacker = context.getAttackerPlayer();
        PlayerStats stats = context.getAttackerStats();
        if (stats == null) {
            debugConfig.logAttack("玩家属性为空: " + attacker.getName());
            return true;
        }

        // 技能伤害：只设置基础伤害，不减伤
        if (context.isSkillDamage() || context.getDamageSource() == DamageSource.SKILL) {
            double baseDamage = context.getBaseDamage();
            context.setBaseDamage(baseDamage);
            context.setFinalDamage(baseDamage);
            debugConfig.logAttack("技能伤害: " + baseDamage);
            return true;
        }

        // 普通攻击：计算攻击力
        ThreadLocalRandom random = ThreadLocalRandom.current();
        boolean isPVP = context.isPVP();
        
        // 基础攻击力为 1，装备提供额外攻击力
        final double BASE_ATTACK = 1.0;
        double minAttack = BASE_ATTACK + (isPVP ? 
            stats.getMinAttack() + stats.getPvpMinAttack() : stats.getMinAttack());
        double maxAttack = BASE_ATTACK + (isPVP ? 
            stats.getMaxAttack() + stats.getPvpMaxAttack() : stats.getMaxAttack());

        double baseDamage = minAttack + random.nextDouble() * (maxAttack - minAttack);
        
        debugConfig.logAttack("玩家: " + attacker.getName());
        debugConfig.logAttack("攻击力范围: " + minAttack + " - " + maxAttack);
        debugConfig.logAttack("计算伤害: " + baseDamage);
        debugConfig.logAttack("是否PVP: " + isPVP);
        
        context.setBaseDamage(baseDamage);
        context.setFinalDamage(baseDamage);
        context.setDamageSource(DamageSource.ATTACK);

        // 尝试触发被动技能
        tryTriggerPassiveSkills(attacker, baseDamage);

        return true;
    }

    /**
     * 尝试触发被动技能
     * <p>技能由 RPGSkill 统一管理，ArmorStats 不再直接触发技能</p>
     */
    private void tryTriggerPassiveSkills(Player attacker, double damage) {
        var plugin = GuangDianArmorStats.getInstance();
        if (plugin == null) return;

        var skillIntegration = plugin.getSkillIntegration();
        if (skillIntegration == null || !skillIntegration.isEnabled()) return;

        // 技能触发由 RPGSkill 处理，ArmorStats 只负责属性计算
        // 如果需要触发装备相关的被动技能，可以通过 RPGSkillAPI 触发
    }
}