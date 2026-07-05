package cn.guangdian.armorstats.combat.interceptor.impl;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.boss.BossStats;
import cn.guangdian.armorstats.boss.BossStatsManager;
import cn.guangdian.armorstats.combat.DamageContext;
import cn.guangdian.armorstats.combat.interceptor.DamageInterceptor;
import cn.guangdian.armorstats.data.PlayerStats;
import cn.guangdian.armorstats.manager.CombatLogManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 后处理拦截器
 * 处理吸血、反伤、状态效果
 * 
 * 平衡性修复:
 * - 吸血有几率上限（防止过高几率导致每次攻击都回血）
 * - 吸血回血量有上限（单次最多回最大生命值的15%）
 * - BOSS和怪物有吸血抵抗属性（减少被吸血量）
 * - 基础吸血倍率可配置
 */
public class PostDamageInterceptor implements DamageInterceptor {

    private final GuangDianArmorStats plugin;
    private CombatLogManager combatLogManager;
    private BossStatsManager bossStatsManager;

    // 吸血平衡配置
    private double maxLifestealChance = 0.30;      // 最大吸血几率上限(0.0-1.0)
    private double maxLifestealHealthPercent = 0.15; // 单次吸血最多回最大生命值的百分比
    private double baseLifestealMultiplier = 5.0;    // 基础吸血倍率(%)
    private double defaultMobLifestealResist = 30.0;  // 普通怪物默认吸血抵抗(%)

    // 反伤平衡配置（新增）
    private double maxReflectChance = 0.30;         // 反伤触发几率上限30%
    private double maxReflectRatio = 0.20;           // 反伤比例上限20%

    public PostDamageInterceptor(GuangDianArmorStats plugin) {
        this.plugin = plugin;
        loadLifestealConfig();
    }

    /**
     * 加载吸血和反伤平衡配置
     */
    private void loadLifestealConfig() {
        var config = plugin.getConfig();
        var lifestealSection = config.getConfigurationSection("lifesteal");
        if (lifestealSection != null) {
            maxLifestealChance = lifestealSection.getDouble("max_chance", 0.30);
            maxLifestealHealthPercent = lifestealSection.getDouble("max_health_percent", 0.15);
            baseLifestealMultiplier = lifestealSection.getDouble("base_multiplier", 5.0);
            defaultMobLifestealResist = lifestealSection.getDouble("default_mob_resist", 30.0);
        }
        // 反伤平衡配置
        var reflectSection = config.getConfigurationSection("reflect");
        if (reflectSection != null) {
            maxReflectChance = reflectSection.getDouble("max_chance", 0.30);
            maxReflectRatio = reflectSection.getDouble("max_ratio", 0.20);
        }
    }

    public void setCombatLogManager(CombatLogManager combatLogManager) {
        this.combatLogManager = combatLogManager;
    }

    public void setBossStatsManager(BossStatsManager bossStatsManager) {
        this.bossStatsManager = bossStatsManager;
    }

    public void reloadConfig() {
        loadLifestealConfig();
    }

    @Override
    public String getName() {
        return "PostDamageInterceptor";
    }

    @Override
    public int getPriority() {
        return 100;  // 最后执行
    }

    @Override
    public Type getType() {
        return Type.POST_DAMAGE;
    }

    @Override
    public boolean process(DamageContext context) {
        // 处理吸血
        if (context.isAttackerPlayer() && !context.isDodged() && !context.isParried()) {
            applyLifesteal(context);
        }

        // 处理反伤
        if (context.isTargetPlayer() && !context.isDodged() && !context.isParried()) {
            applyReflect(context);
        }

        // 处理躲避反伤（闪避成功时触发）
        if (context.isTargetPlayer() && context.isDodged()) {
            applyDodgeReflect(context);
        }

        // 处理状态效果
        if (context.isAttackerPlayer() && !context.isDodged() && !context.isParried()) {
            applyStatusEffects(context);
        }

        // 战斗日志
        if (combatLogManager != null && !context.isDodged() && !context.isParried()) {
            logDamage(context);
        }

        return true;
    }

    private void applyLifesteal(DamageContext context) {
        Player attacker = context.getAttackerPlayer();
        PlayerStats stats = context.getAttackerStats();
        if (attacker == null || stats == null) return;

        double lifestealChance = stats.getLifestealPercent() / 100.0;
        if (lifestealChance <= 0) return;

        // 1. 吸血几率上限
        lifestealChance = Math.min(lifestealChance, maxLifestealChance);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() > lifestealChance) {
            return;  // 未触发
        }

        // 2. 计算吸血倍率
        double lifestealMultiplier = (baseLifestealMultiplier + stats.getLifestealMultiplier()) / 100.0;

        // 3. 计算目标的吸血抵抗
        double resistMultiplier = 1.0;
        
        // 玩家目标的吸血抵抗
        if (context.hasTargetStats()) {
            resistMultiplier -= Math.max(0.0, context.getTargetStats().getLifestealResistPercent() / 100.0);
        }
        
        // BOSS/怪物的吸血抵抗（新增）
        LivingEntity targetEntity = context.getTarget();
        if (targetEntity != null && !context.isTargetPlayer()) {
            // 从DamageContext获取BossStats（如果BossStatsInterceptor启用了）
            if (context.hasTargetBossStats()) {
                BossStats bossStats = context.getTargetBossStats();
                resistMultiplier -= Math.max(0.0, bossStats.getLifestealResistPercent() / 100.0);
            }
            // 从BossStatsManager直接获取（兼容BossStatsInterceptor未启用的情况）
            else if (bossStatsManager != null) {
                BossStats bossStats = bossStatsManager.getBossStats(targetEntity);
                if (bossStats != null) {
                    resistMultiplier -= Math.max(0.0, bossStats.getLifestealResistPercent() / 100.0);
                } else {
                    // 没有配置的怪物使用默认吸血抵抗
                    resistMultiplier -= Math.max(0.0, defaultMobLifestealResist / 100.0);
                }
            }
        }

        resistMultiplier = Math.max(0.0, resistMultiplier);

        // 4. 计算回血量
        double healAmount = context.getFinalDamage() * lifestealMultiplier * resistMultiplier;

        // 5. 吸血回血上限（单次最多回最大生命值的maxLifestealHealthPercent）
        double maxHealth = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
        double maxHealPerHit = maxHealth * maxLifestealHealthPercent;
        healAmount = Math.min(healAmount, maxHealPerHit);

        if (healAmount <= 0) return;

        double newHealth = Math.min(attacker.getHealth() + healAmount, maxHealth);
        attacker.setHealth(newHealth);

        if (combatLogManager != null) {
            combatLogManager.logLifesteal(attacker, healAmount);
        }
    }

    private void applyReflect(DamageContext context) {
        Player defender = context.getTargetPlayer();
        PlayerStats stats = context.getTargetStats();
        LivingEntity attacker = context.getAttacker();
        if (defender == null || stats == null || attacker == null) return;

        double reflectChance = Math.min(maxReflectChance, stats.getDamageReflectPercent() / 100.0);
        if (reflectChance <= 0) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() >= reflectChance) {
            return;
        }

        double reflectPercent = Math.min(maxReflectRatio, stats.getReflectPercent() / 100.0);
        double reflectDamage = context.getFinalDamage() * reflectPercent;

        attacker.damage(reflectDamage, defender);

        if (combatLogManager != null) {
            combatLogManager.logReflect(defender, attacker, reflectDamage);
        }
    }

    private void applyStatusEffects(DamageContext context) {
        Player attacker = context.getAttackerPlayer();
        PlayerStats stats = context.getAttackerStats();
        LivingEntity target = context.getTarget();
        if (attacker == null || stats == null || target == null) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 中毒
        if (stats.getPoisonPercent() > 0 && random.nextDouble() < stats.getPoisonPercent() / 100.0) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
            if (combatLogManager != null) {
                combatLogManager.logStatusEffect(attacker, target, "poison");
            }
        }

        // 冰冻
        if (stats.getFreezePercent() > 0 && random.nextDouble() < stats.getFreezePercent() / 100.0) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2));
            target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, 128, false, false));
            if (combatLogManager != null) {
                combatLogManager.logStatusEffect(attacker, target, "freeze");
            }
        }

        // 致盲
        if (stats.getBlindPercent() > 0 && random.nextDouble() < stats.getBlindPercent() / 100.0) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0));
            if (combatLogManager != null) {
                combatLogManager.logStatusEffect(attacker, target, "blind");
            }
        }

        // 燃烧 - 持续火焰伤害（3秒）
        if (stats.getBurnPercent() > 0 && random.nextDouble() < stats.getBurnPercent() / 100.0) {
            target.setFireTicks(60); // 3秒火焰视觉效果
            if (combatLogManager != null) {
                combatLogManager.logStatusEffect(attacker, target, "burn");
            }
        }

        // 灼烧 - 可叠加伤害效果（使用火焰效果表示）
        if (stats.getScorchPercent() > 0 && random.nextDouble() < stats.getScorchPercent() / 100.0) {
            // 灼烧使用更长的火焰时间，实际伤害由灼烧层数决定
            int currentFireTicks = target.getFireTicks();
            target.setFireTicks(Math.max(currentFireTicks, 100)); // 5秒火焰效果
            if (combatLogManager != null) {
                combatLogManager.logStatusEffect(attacker, target, "scorch");
            }
        }
    }

    /**
     * 躲避反伤 - 闪避成功后有概率反弹伤害（受反伤上限约束）
     */
    private void applyDodgeReflect(DamageContext context) {
        Player defender = context.getTargetPlayer();
        PlayerStats stats = context.getTargetStats();
        LivingEntity attacker = context.getAttacker();
        if (defender == null || stats == null || attacker == null) return;

        double reflectChance = Math.min(maxReflectChance, stats.getDodgeReflectPercent() / 100.0);
        if (reflectChance <= 0) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() >= reflectChance) {
            return; // 未触发
        }

        double reflectRatio = Math.min(maxReflectRatio, stats.getDodgeReflectRatio() / 100.0);
        if (reflectRatio <= 0) return;

        // 反弹原始伤害的一定比例
        double reflectDamage = context.getBaseDamage() * reflectRatio;
        if (reflectDamage <= 0) return;

        attacker.damage(reflectDamage, defender);

        if (combatLogManager != null) {
            combatLogManager.logReflect(defender, attacker, reflectDamage);
        }
    }

    private void logDamage(DamageContext context) {
        Player attacker = context.getAttackerPlayer();
        Player targetPlayer = context.getTargetPlayer();
        LivingEntity targetEntity = context.getTarget();

        if (attacker != null && targetEntity != null) {
            combatLogManager.logDamage(attacker, targetEntity, 
                context.getFinalDamage(), context.isCritical(), context.isPVP());
        }

        if (context.isDodged() && targetPlayer != null) {
            combatLogManager.logDodge(targetPlayer);
        }

        if (context.isParried() && targetPlayer != null) {
            combatLogManager.logParry(targetPlayer);
        }
    }
}