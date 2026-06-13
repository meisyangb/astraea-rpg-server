package cn.guangdian.armorstats.combat.interceptor.impl;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.boss.BossStats;
import cn.guangdian.armorstats.boss.BossStatsManager;
import cn.guangdian.armorstats.combat.DamageContext;
import cn.guangdian.armorstats.combat.DamageSource;
import cn.guangdian.armorstats.combat.interceptor.DamageInterceptor;
import cn.guangdian.armorstats.config.DamageDebugConfig;
import cn.guangdian.armorstats.data.PlayerStats;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class BossStatsInterceptor implements DamageInterceptor {

    private volatile BossStatsManager bossStatsManager;
    private boolean overrideMythicDamage = true;
    private DamageDebugConfig debugConfig;
    // 新增: 穿透上限配置
    private double maxArmorPenetration = 0.70;
    private double maxDefensePenetration = 0.70;

    public BossStatsInterceptor() {
        this.debugConfig = DamageDebugConfig.getInstance();
        loadConfig();
    }

    private void loadConfig() {
        var config = GuangDianArmorStats.getInstance().getConfig();
        var damageSection = config.getConfigurationSection("damage");
        if (damageSection != null) {
            maxArmorPenetration = damageSection.getDouble("max_armor_penetration", 0.70);
            maxDefensePenetration = damageSection.getDouble("max_defense_penetration", 0.70);
        }
    }

    public void setBossStatsManager(BossStatsManager bossStatsManager) {
        this.bossStatsManager = bossStatsManager;
    }

    public void setOverrideMythicDamage(boolean override) {
        this.overrideMythicDamage = override;
    }

    public void setDebugConfig(DamageDebugConfig debugConfig) {
        this.debugConfig = debugConfig;
    }

    public void reloadConfig() {
        loadConfig();
    }

    @Override
    public String getName() {
        return "BossStatsInterceptor";
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public Type getType() {
        return Type.BOTH;
    }

    @Override
    public boolean process(DamageContext context) {
        BossStatsManager manager = this.bossStatsManager;
        if (manager == null) {
            debugConfig.logBossStats("BossStatsManager 未初始化");
            return true;
        }

        LivingEntity attacker = context.getAttacker();
        LivingEntity target = context.getTarget();

        debugConfig.logBossStats("处理伤害 - 攻击者: " +
            (attacker != null ? attacker.getType().name() : "null") +
            " 目标: " + (target != null ? target.getType().name() : "null"));

        BossStats attackerStats = null;
        BossStats targetStats = null;

        if (attacker != null) {
            attackerStats = manager.getBossStats(attacker);
            debugConfig.logBossStats("攻击者BOSS属性: " +
                (attackerStats != null ? attackerStats.getDisplayName() : "null"));

            if (attackerStats != null) {
                context.setAttackerBossStats(attackerStats);

                boolean shouldOverrideDamage = overrideMythicDamage && !isSkillDamage(context);
                debugConfig.logBossStats("覆盖伤害: " + shouldOverrideDamage +
                    " 技能伤害: " + isSkillDamage(context));

                if (shouldOverrideDamage) {
                    processBossAttack(context, attackerStats);
                    debugConfig.logBossStats("BOSS攻击处理完成, 最终伤害: " + context.getFinalDamage());
                }

                if (context.isDodged() || context.isParried()) {
                    return true;
                }
            }
        }

        if (target != null && attackerStats == null) {
            targetStats = manager.getBossStats(target);
            debugConfig.logBossStats("目标BOSS属性: " +
                (targetStats != null ? targetStats.getDisplayName() : "null"));

            if (targetStats != null) {
                context.setTargetBossStats(targetStats);
                processBossDefense(context, targetStats);
                debugConfig.logBossStats("BOSS防御处理完成, 最终伤害: " + context.getFinalDamage());
            }
        }

        return true;
    }

    private boolean isSkillDamage(DamageContext context) {
        if (context.isSkillDamage()) {
            return true;
        }

        DamageSource source = context.getDamageSource();
        if (source == DamageSource.SKILL) {
            return true;
        }

        if (source == DamageSource.MYTHICMOB_SKILL) {
            return true;
        }

        return false;
    }

    private void processBossAttack(DamageContext context, BossStats stats) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        double baseDamage = stats.getMinAttack() +
            random.nextDouble() * (stats.getMaxAttack() - stats.getMinAttack());

        baseDamage *= stats.getDamageMultiplier();

        if (random.nextDouble() * 100 < stats.getCritChance()) {
            baseDamage *= (1.0 + stats.getCritDamage() / 100.0);
            context.setCritical(true);
        }

        Player targetPlayer = context.getTargetPlayer();
        if (targetPlayer != null) {
            PlayerStats targetStats = context.getTargetStats();
            if (targetStats != null) {
                // 应用穿透上限，防止100%穿透
                double armorPen = Math.min(maxArmorPenetration, stats.getArmorPenetration() / 100.0);
                double defPen = Math.min(maxDefensePenetration, stats.getDefensePenetration() / 100.0);

                double effectiveArmor = (targetStats.getArmorPercent() / 100.0) * (1.0 - armorPen);
                baseDamage *= (1.0 - Math.min(0.85, effectiveArmor));

                double effectiveDefense = targetStats.getDefenseAverage() * (1.0 - defPen);
                baseDamage *= (1.0 - Math.min(0.90, effectiveDefense / (effectiveDefense + 15000.0)));
            }
        }

        Map<String, Double> elementalDamage = stats.getElementalDamage();
        if (!elementalDamage.isEmpty()) {
            double totalElemental = 0;
            for (double dmg : elementalDamage.values()) {
                totalElemental += dmg;
            }
            baseDamage += totalElemental;
        }

        context.setBaseDamage(baseDamage);
        context.setFinalDamage(baseDamage);
        context.setDamageSource(DamageSource.MYTHICMOB);
        context.setBossAttack(true);
    }

    private void processBossDefense(DamageContext context, BossStats stats) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        double dodgeChance = stats.getDodgeChance();
        if (dodgeChance > 0 && random.nextDouble() * 100 < dodgeChance) {
            context.setDodged(true);
            context.setFinalDamage(0);
            debugConfig.logBossStats("BOSS闪避!");
            return;
        }

        double parryChance = stats.getParryChance();
        if (parryChance > 0 && random.nextDouble() * 100 < parryChance) {
            context.setParried(true);
            context.setFinalDamage(0);
            debugConfig.logBossStats("BOSS招架!");
            return;
        }

        double currentDamage = context.getFinalDamage();
        double originalDamage = context.getBaseDamage();

        debugConfig.logBossStats("防御计算 - 原始伤害: " + originalDamage + " 当前伤害: " + currentDamage);
        debugConfig.logBossStats("BOSS属性 - 护甲%: " + stats.getArmorPercent() + " 防御力: " + stats.getDefense() + " 伤害减免%: " + stats.getDamageReduction());

        // 应用玩家穿透上限
        PlayerStats attackerStats = context.getAttackerStats();
        double armorPen = 0.0;
        double defPen = 0.0;
        if (attackerStats != null) {
            armorPen = Math.min(maxArmorPenetration, attackerStats.getArmorPenetration() / 100.0);
            defPen = Math.min(maxDefensePenetration, attackerStats.getDefensePenetration() / 100.0);
        }

        double armorReduction = stats.getArmorPercent() / 100.0;
        if (armorReduction > 0) {
            // 玩家穿透减少BOSS护甲效果
            double effectiveArmorReduction = armorReduction * (1.0 - armorPen);
            double before = currentDamage;
            currentDamage *= (1.0 - Math.min(0.85, effectiveArmorReduction));
            debugConfig.logBossStats("护甲减伤(穿透" + (armorPen * 100) + "%): " + before + " -> " + currentDamage);
        }

        double defense = stats.getDefense();
        if (defense > 0) {
            // 玩家穿透减少BOSS防御效果
            double effectiveDefense = defense * (1.0 - defPen);
            double defenseReduction = Math.min(0.90, effectiveDefense / (effectiveDefense + 15000.0));
            double before = currentDamage;
            currentDamage *= (1.0 - defenseReduction);
            debugConfig.logBossStats("防御减伤(" + (defenseReduction * 100) + "%, 穿透" + (defPen * 100) + "%): " + before + " -> " + currentDamage);
        }

        double damageReduction = stats.getDamageReduction() / 100.0;
        if (damageReduction > 0) {
            double before = currentDamage;
            currentDamage *= (1.0 - damageReduction);
            debugConfig.logBossStats("伤害减免(" + (damageReduction * 100) + "%): " + before + " -> " + currentDamage);
        }

        double totalReduction = 1.0 - (currentDamage / originalDamage);
        debugConfig.logBossStats("总减伤率: " + (totalReduction * 100) + "%");

        if (totalReduction > 0.95) {
            currentDamage = originalDamage * 0.05;
            debugConfig.logBossStats("减伤超过95%，限制为5%: " + currentDamage);
        }

        context.setFinalDamage(Math.max(1.0, currentDamage));
        debugConfig.logBossStats("最终伤害: " + context.getFinalDamage());
    }
}