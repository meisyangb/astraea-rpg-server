package cn.guangdian.armorstats.combat.interceptor.impl;

import cn.guangdian.armorstats.GuangDianArmorStats;
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
 */
public class PostDamageInterceptor implements DamageInterceptor {

    private final GuangDianArmorStats plugin;
    private CombatLogManager combatLogManager;

    public PostDamageInterceptor(GuangDianArmorStats plugin) {
        this.plugin = plugin;
    }

    public void setCombatLogManager(CombatLogManager combatLogManager) {
        this.combatLogManager = combatLogManager;
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

        double lifestealChance = stats.getLifestealPercent();
        if (lifestealChance <= 0) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() * 100 > lifestealChance) {
            return;  // 未触发
        }

        // 基础吸血倍率 5%，装备提供额外倍率
        final double BASE_LIFESTEAL_MULTIPLIER = 5.0;
        double lifestealMultiplier = (BASE_LIFESTEAL_MULTIPLIER + stats.getLifestealMultiplier()) / 100.0;
        double resistMultiplier = 1.0;
        if (context.hasTargetStats()) {
            resistMultiplier -= Math.max(0.0, context.getTargetStats().getLifestealResistPercent() / 100.0);
        }

        double healAmount = context.getFinalDamage() * lifestealMultiplier * Math.max(0.0, resistMultiplier);
        if (healAmount <= 0) return;

        double maxHealth = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
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

        double reflectChance = stats.getDamageReflectPercent();
        if (reflectChance <= 0) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() * 100 >= reflectChance) {
            return;
        }

        double reflectPercent = stats.getReflectPercent() / 100.0;
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
     * 躲避反伤 - 闪避成功后有概率反弹伤害
     */
    private void applyDodgeReflect(DamageContext context) {
        Player defender = context.getTargetPlayer();
        PlayerStats stats = context.getTargetStats();
        LivingEntity attacker = context.getAttacker();
        if (defender == null || stats == null || attacker == null) return;

        double reflectChance = stats.getDodgeReflectPercent();
        if (reflectChance <= 0) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() * 100 >= reflectChance) {
            return; // 未触发
        }

        double reflectRatio = stats.getDodgeReflectRatio() / 100.0;
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
        Player target = context.getTargetPlayer();
        LivingEntity targetEntity = context.getTarget();

        if (attacker != null && targetEntity != null) {
            combatLogManager.logDamage(attacker, targetEntity, 
                context.getFinalDamage(), context.isCritical(), context.isPVP());
        }

        if (context.isDodged() && target != null) {
            combatLogManager.logDodge(target);
        }

        if (context.isParried() && target != null) {
            combatLogManager.logParry(target);
        }
    }
}