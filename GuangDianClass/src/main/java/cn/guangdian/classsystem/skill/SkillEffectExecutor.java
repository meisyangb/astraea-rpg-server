package cn.guangdian.classsystem.skill;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.enums.Skill;
import cn.guangdian.classsystem.model.SkillOrb;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;

/**
 * 技能效果执行器 - 简化版
 * 
 * 只包含简单技能效果：
 * - 火球术：发射火球
 * - 闪电术：召唤闪电
 * - 冰冻术：冰冻敌人
 * - 护盾：减伤效果
 * - 狂暴：攻击力翻倍
 * - 被动效果：永久属性加成
 */
public class SkillEffectExecutor {

    private final GuangDianClass plugin;

    public SkillEffectExecutor(GuangDianClass plugin) {
        this.plugin = plugin;
    }

    /**
     * 执行主动技能效果
     */
    public void executeSkill(Player player, Skill skill) {
        String effect = skill.getEffect();
        double damage = skill.getDamageMult() * 10.0; // 基础伤害10
        double range = skill.getRange();

        switch (effect) {
            // 战士技能
            case "slash" -> executeSlash(player, damage, range);
            case "shield" -> executeShield(player);
            case "rage" -> executeRage(player);
            
            // 法师技能
            case "fireball" -> executeFireball(player, damage);
            case "lightning" -> executeLightning(player, damage);
            case "ice" -> executeIce(player, damage, range);
            
            default -> executeDefault(player, damage, range);
        }
    }

    /**
     * 执行被动技能效果
     */
    public void executePassiveSkill(Player player, Skill skill) {
        String effect = skill.getEffect();
        
        switch (effect) {
            case "attack_boost" -> player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0));
            case "mana_boost" -> player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, Integer.MAX_VALUE, 0));
            case "health_defense_boost" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, Integer.MAX_VALUE, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0));
            }
            case "mana_regen" -> player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 0));
        }
    }

    // ========================================
    // 战士技能
    // ========================================

    /**
     * 斩击 - 对周围敌人造成伤害
     */
    private void executeSlash(Player player, double damage, double range) {
        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.SQUID_INK, loc, 30, range, 1, range, 0.1);
        player.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
        
        damageNearby(player, loc, range, damage);
        player.sendMessage(Component.text("斩击！").color(NamedTextColor.DARK_PURPLE));
    }

    /**
     * 护盾 - 减伤效果
     */
    private void executeShield(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 1)); // 5秒，减伤50%
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation(), 20, 1, 1, 1, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.0f);
        player.sendMessage(Component.text("护盾激活！").color(NamedTextColor.AQUA));
    }

    /**
     * 狂暴 - 攻击力翻倍
     */
    private void executeRage(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1)); // 10秒，攻击力翻倍
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 0)); // 10秒，加速
        player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, player.getLocation(), 20, 1, 1, 1, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.0f);
        player.sendMessage(Component.text("狂暴！攻击力翻倍！").color(NamedTextColor.RED));
    }

    // ========================================
    // 法师技能
    // ========================================

    /**
     * 火球术 - 发射火球
     */
    private void executeFireball(Player player, double damage) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        
        Fireball fireball = player.getWorld().spawn(eyeLoc, Fireball.class);
        fireball.setShooter(player);
        fireball.setVelocity(direction.multiply(2.0));
        fireball.setYield(0);
        fireball.setMetadata("skill_damage", new FixedMetadataValue(plugin, damage));
        
        player.getWorld().playSound(eyeLoc, Sound.ENTITY_GHAST_SHOOT, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.FLAME, eyeLoc, 20, 0.5, 0.5, 0.5, 0.1);
        player.sendMessage(Component.text("火球术！").color(NamedTextColor.RED));
    }

    /**
     * 闪电术 - 召唤闪电
     */
    private void executeLightning(Player player, double damage) {
        Location targetLoc = player.getLocation().add(player.getLocation().getDirection().multiply(8));
        
        player.getWorld().strikeLightning(targetLoc);
        player.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, targetLoc, 50, 2, 2, 2, 0.2);
        
        damageNearby(player, targetLoc, 3, damage);
        player.sendMessage(Component.text("闪电术！").color(NamedTextColor.YELLOW));
    }

    /**
     * 冰冻术 - 冰冻周围敌人
     */
    private void executeIce(Player player, double damage, double range) {
        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 50, range, 1, range, 0.1);
        player.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
        
        Collection<Entity> nearby = player.getWorld().getNearbyEntities(loc, range, 2, range,
            e -> e instanceof LivingEntity && !e.equals(player));
        for (Entity entity : nearby) {
            LivingEntity living = (LivingEntity) entity;
            living.damage(damage, player);
            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2)); // 5秒减速
            living.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1)); // 5秒虚弱
        }
        player.sendMessage(Component.text("冰冻术！").color(NamedTextColor.AQUA));
    }

    // ========================================
    // 辅助方法
    // ========================================

    private void executeDefault(Player player, double damage, double range) {
        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.FLAME, loc, 20, range, 1, range, 0.1);
        damageNearby(player, loc, range, damage);
    }

    private void damageNearby(Player player, Location loc, double range, double damage) {
        Collection<Entity> nearby = player.getWorld().getNearbyEntities(loc, range, 2, range,
            e -> e instanceof LivingEntity && !e.equals(player));
        for (Entity entity : nearby) {
            ((LivingEntity) entity).damage(damage, player);
        }
    }

    /**
     * 执行技能效果（使用 SkillOrb - 兼容旧版本）
     */
    public void executeSkillEffect(Player player, SkillOrb skill, Entity target, double damage) {
        String effect = skill.getEffect();
        
        switch (effect) {
            case "fireball" -> executeFireball(player, damage);
            case "lightning" -> executeLightning(player, damage);
            case "ice" -> executeIce(player, damage, skill.getRange());
            case "slash" -> executeSlash(player, damage, skill.getRange());
            case "shield" -> executeShield(player);
            case "rage" -> executeRage(player);
            default -> executeDefault(player, damage, skill.getRange());
        }
    }
}