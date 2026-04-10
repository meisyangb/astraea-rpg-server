package cn.guangdian.armorstats.combat.interceptor.impl;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.combat.DamageContext;
import cn.guangdian.armorstats.combat.interceptor.DamageInterceptor;
import cn.guangdian.armorstats.data.PlayerStats;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 防御拦截器
 * 处理多层减伤机制：
 * 1. 闪避/招架判定
 * 2. 护甲减伤（受护甲穿透影响）
 * 3. 防御力减伤（受防御穿透影响）
 * 4. 额外减伤（技能/buff等）
 * 5. 最终减伤上限
 */
public class DefenseInterceptor implements DamageInterceptor {

    private double maxDodge = 0.80;
    private double maxParry = 0.50;
    private double maxArmorReduction = 0.85;
    private double maxDefenseReduction = 0.90;
    private double maxTotalReduction = 0.95;
    private double defenseDivisor = 15000.0;
    // 新增: 穿透上限
    private double maxArmorPenetration = 0.70;
    private double maxDefensePenetration = 0.70;

    public DefenseInterceptor() {
        loadConfig();
    }

    private void loadConfig() {
        var config = GuangDianArmorStats.getInstance().getConfig();
        var damageSection = config.getConfigurationSection("damage");
        if (damageSection != null) {
            maxDodge = damageSection.getDouble("max_dodge", 0.80);
            maxParry = damageSection.getDouble("max_parry", 0.50);
            maxArmorReduction = damageSection.getDouble("max_armor_reduction", 0.85);
            maxDefenseReduction = damageSection.getDouble("max_defense_reduction", 0.90);
            maxTotalReduction = damageSection.getDouble("max_total_reduction", 0.95);
            defenseDivisor = damageSection.getDouble("defense_divisor", 15000.0);
            // 加载穿透上限
            maxArmorPenetration = damageSection.getDouble("max_armor_penetration", 0.70);
            maxDefensePenetration = damageSection.getDouble("max_defense_penetration", 0.70);
        }
    }

    public void reloadConfig() {
        loadConfig();
    }

    @Override
    public String getName() {
        return "DefenseInterceptor";
    }

    @Override
    public int getPriority() {
        return 20;
    }

    @Override
    public Type getType() {
        return Type.DEFENSE;
    }

    @Override
    public boolean process(DamageContext context) {
        if (!context.isTargetPlayer()) {
            return true;
        }

        PlayerStats targetStats = context.getTargetStats();
        if (targetStats == null) {
            return true;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();

        // ========== 1. 闪避判定 ==========
        double dodgeChance = Math.min(maxDodge, targetStats.getDodgePercent() / 100.0);
        if (random.nextDouble() < dodgeChance) {
            context.setDodged(true);
            return true;
        }

        // ========== 2. 招架判定 ==========
        double parryChance = Math.min(maxParry, targetStats.getParryPercent() / 100.0);
        if (random.nextDouble() < parryChance) {
            context.setParried(true);
            return true;
        }

        // ========== 3. 多层减伤计算 ==========
        double currentDamage = context.getFinalDamage();

        // 获取攻击者的穿透属性，并应用上限
        PlayerStats attackerStats = context.getAttackerStats();
        double armorPenetration = 0.0;
        double defensePenetration = 0.0;
        if (attackerStats != null) {
            // 应用穿透上限，防止100%穿透导致防御完全失效
            armorPenetration = Math.min(maxArmorPenetration, attackerStats.getArmorPenetration() / 100.0);
            defensePenetration = Math.min(maxDefensePenetration, attackerStats.getDefensePenetration() / 100.0);
        }

        // --- 3.1 护甲减伤 ---
        double armorPercent = targetStats.getArmorPercent() / 100.0;
        // 护甲强度抵消护甲穿透效果
        double armorStrength = targetStats.getArmorStrength() / 100.0;
        double effectiveArmorPen = Math.max(0.0, armorPenetration - armorStrength);
        // 穿透减少护甲效果
        double effectiveArmor = armorPercent * (1.0 - effectiveArmorPen);
        double armorReduction = Math.min(maxArmorReduction, effectiveArmor);
        currentDamage *= (1.0 - armorReduction);

        // --- 3.2 防御力减伤 ---
        boolean isPVP = context.isPVP();
        
        // PVP时使用PVP防御力，否则使用普通防御力
        double defenseMin;
        double defenseMax;
        if (isPVP) {
            defenseMin = targetStats.getPvpDefenseMin();
            defenseMax = targetStats.getPvpDefenseMax();
        } else {
            defenseMin = targetStats.getDefenseMin();
            defenseMax = targetStats.getDefenseMax();
        }
        
        double defense = defenseMin + random.nextDouble() * (defenseMax - defenseMin);
        if (defense <= 0 && (defenseMin > 0 || defenseMax > 0)) {
            defense = (defenseMin + defenseMax) / 2.0;
        }

        // 穿透减少防御力效果
        double effectiveDefense = defense * (1.0 - defensePenetration);

        double defenseReduction;
        if (effectiveDefense <= 0) {
            defenseReduction = 0;
        } else {
            defenseReduction = effectiveDefense / (effectiveDefense + defenseDivisor);
        }
        defenseReduction = Math.min(maxDefenseReduction, defenseReduction);
        currentDamage *= (1.0 - defenseReduction);

        // --- 3.3 额外减伤（技能/buff等）---
        double bonusReduction = targetStats.getDamageReductionBonus() / 100.0;
        currentDamage *= (1.0 - bonusReduction);

        // --- 3.4 暴击抵抗（如果是暴击）---
        if (context.isCritical()) {
            double critResist = targetStats.getCritResistPercent() / 100.0;
            currentDamage *= (1.0 - critResist);

            double critDmgResist = targetStats.getCritDamageResistPercent() / 100.0;
            currentDamage *= (1.0 - critDmgResist);
        }

        // ========== 4. 最终减伤上限 ==========
        double originalDamage = context.getBaseDamage();
        double totalReduction = 1.0 - (currentDamage / originalDamage);
        if (totalReduction > maxTotalReduction) {
            currentDamage = originalDamage * (1.0 - maxTotalReduction);
        }

        // ========== 5. 最小伤害 ==========
        double minDamage = 1.0;
        var config = GuangDianArmorStats.getInstance().getConfig();
        var damageSection = config.getConfigurationSection("damage");
        if (damageSection != null) {
            minDamage = damageSection.getDouble("min_damage", 1.0);
        }
        currentDamage = Math.max(minDamage, currentDamage);

        context.setFinalDamage(currentDamage);

        return true;
    }
}