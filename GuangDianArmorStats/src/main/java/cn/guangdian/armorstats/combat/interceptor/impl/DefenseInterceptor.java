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
 * 2. 护甲减伤（递减曲线，受护甲穿透影响）
 * 3. 防御力减伤（递减曲线，受防御穿透影响）
 * 4. 暴击抵抗（上限50%，总暴击减伤上限75%）
 * 5. 额外减伤（上限25%）
 * 6. 最终减伤上限95%
 *
 * 收益曲线设计参考暗黑3/流放之路/FF14:
 * - 闪避上限60%，招架上限30%，总回避率不会过高
 * - 护甲改用递减公式: armorReduction = armor%/(armor%+armorDivisor)×(1-effectivePen)
 * - 防御力已有递减: defense/(defense+defenseDivisor)
 * - 暴击抵抗上限50%，暴伤抵抗上限50%，总暴击减伤上限75%
 * - 额外减伤上限25%
 */
public class DefenseInterceptor implements DamageInterceptor {

    // 闪避/招架上限
    private double maxDodge = 0.60;         // 从0.80降到0.60
    private double maxParry = 0.30;         // 从0.50降到0.30

    // 护甲减伤（递减曲线）
    private double armorDivisor = 50.0;     // 护甲递减参数
    private double maxArmorReduction = 0.50; // 从0.85降到0.50

    // 防御力减伤（递减曲线）
    private double maxDefenseReduction = 0.90;
    private double defenseDivisor = 10000.0;  // 修复: 从15000改为10000，与配置一致
    private double maxTotalReduction = 0.95;

    // 穿透上限
    private double maxArmorPenetration = 0.70;
    private double maxDefensePenetration = 0.70;

    // 暴击抵抗上限（新增）
    private double maxCritResist = 0.50;     // 暴击抵抗上限50%
    private double maxCritDamageResist = 0.50; // 暴伤抵抗上限50%
    private double maxTotalCritResist = 0.75;  // 总暴击减伤上限75%

    // 额外减伤上限（新增）
    private double maxBonusReduction = 0.25;   // 额外减伤上限25%

    public DefenseInterceptor() {
        loadConfig();
    }

    private void loadConfig() {
        var config = GuangDianArmorStats.getInstance().getConfig();
        var damageSection = config.getConfigurationSection("damage");
        if (damageSection != null) {
            maxDodge = damageSection.getDouble("max_dodge", 0.60);
            maxParry = damageSection.getDouble("max_parry", 0.30);
            armorDivisor = damageSection.getDouble("armor_divisor", 50.0);
            maxArmorReduction = damageSection.getDouble("max_armor_reduction", 0.50);
            maxDefenseReduction = damageSection.getDouble("max_defense_reduction", 0.90);
            maxTotalReduction = damageSection.getDouble("max_total_reduction", 0.95);
            defenseDivisor = damageSection.getDouble("defense_divisor", 10000.0);
            // 穿透上限
            maxArmorPenetration = damageSection.getDouble("max_armor_penetration", 0.70);
            maxDefensePenetration = damageSection.getDouble("max_defense_penetration", 0.70);
            // 暴击抵抗上限
            maxCritResist = damageSection.getDouble("max_crit_resist", 0.50);
            maxCritDamageResist = damageSection.getDouble("max_crit_damage_resist", 0.50);
            maxTotalCritResist = damageSection.getDouble("max_total_crit_resist", 0.75);
            // 额外减伤上限
            maxBonusReduction = damageSection.getDouble("max_bonus_reduction", 0.25);
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

        // --- 3.1 护甲减伤（递减曲线） ---
        // 公式: armorReduction = armor%/(armor%+armorDivisor) × (1 - effectivePen)
        // 递减参数50: 25%护甲→20%减伤, 50%护甲→33%减伤, 85%护甲→41%减伤
        double armorPercent = targetStats.getArmorPercent() / 100.0;
        // 护甲强度抵消护甲穿透效果
        double armorStrength = targetStats.getArmorStrength() / 100.0;
        double effectiveArmorPen = Math.max(0.0, armorPenetration - armorStrength);
        // 递减公式: 原始护甲减伤 = armorPercent/(armorPercent+armorDivisor)
        double rawArmorReduction = armorPercent / (armorPercent + armorDivisor);
        // 穿透削弱护甲效果
        double armorReduction = rawArmorReduction * (1.0 - effectiveArmorPen);
        armorReduction = Math.min(maxArmorReduction, Math.max(0.0, armorReduction));
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

        // --- 3.3 额外减伤（技能/buff等），上限25% ---
        double bonusReduction = Math.min(maxBonusReduction, targetStats.getDamageReductionBonus() / 100.0);
        currentDamage *= (1.0 - bonusReduction);

        // --- 3.4 暴击抵抗（如果是暴击），上限50%+50%，总上限75% ---
        if (context.isCritical()) {
            double critResist = Math.min(maxCritResist, targetStats.getCritResistPercent() / 100.0);
            double critDmgResist = Math.min(maxCritDamageResist, targetStats.getCritDamageResistPercent() / 100.0);
            // 总暴击减伤不超过75%
            double totalCritResist = Math.min(maxTotalCritResist, critResist + critDmgResist);
            currentDamage *= (1.0 - totalCritResist);
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