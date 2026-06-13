package cn.guangdian.classsystem.skill;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.model.SkillOrb;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Random;

/**
 * 技能效果执行器
 * 
 * 实现各种技能的真实效果：
 * - 火球术：发射真实的火球
 * - 闪电术：召唤闪电
 * - 冰冻术：发射冰球并冰冻目标
 * - 治疗术：恢复生命值
 */
public class SkillEffectExecutor {

    private final GuangDianClass plugin;
    private final Random random = new Random();

    public SkillEffectExecutor(GuangDianClass plugin) {
        this.plugin = plugin;
    }

    /**
     * 执行技能效果
     */
    public void executeSkillEffect(Player player, SkillOrb skill, Entity target, double damage) {
        String effect = skill.getEffect();
        
        if (effect == null || effect.isEmpty() || "none".equals(effect)) {
            // 默认效果：直接造成伤害
            executeDefaultEffect(player, target, damage);
            return;
        }
        
        switch (effect.toLowerCase()) {
            case "fire":
            case "fireball":
                executeFireball(player, skill, target, damage);
                break;
                
            case "lightning":
            case "thunder":
                executeLightning(player, skill, target, damage);
                break;
                
            case "ice":
            case "freeze":
                executeIceEffect(player, skill, target, damage);
                break;
                
            case "heal":
                executeHealEffect(player, skill, damage);
                break;
                
            case "explosion":
                executeExplosionEffect(player, skill, target, damage);
                break;
                
            case "magic":
                executeMagicEffect(player, skill, target, damage);
                break;
                
            default:
                executeDefaultEffect(player, target, damage);
                break;
        }
    }

    /**
     * 火球术效果
     */
    private void executeFireball(Player player, SkillOrb skill, Entity target, double damage) {
        // 发射真实的火球
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        
        // 创建火球
        Fireball fireball = player.getWorld().spawn(eyeLoc, Fireball.class);
        fireball.setShooter(player);
        fireball.setVelocity(direction.multiply(2.0));
        fireball.setYield(0); // 不破坏方块
        fireball.setMetadata("skill_damage", new FixedMetadataValue(plugin, damage));
        fireball.setMetadata("skill_id", new FixedMetadataValue(plugin, skill.getSkillId()));
        
        // 播放音效和粒子
        player.getWorld().playSound(eyeLoc, Sound.ENTITY_GHAST_SHOOT, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.FLAME, eyeLoc, 30, 0.5, 0.5, 0.5, 0.1);
        
        // 5秒后移除火球（如果没有击中目标）
        new BukkitRunnable() {
            @Override
            public void run() {
                if (fireball.isValid()) {
                    fireball.remove();
                }
            }
        }.runTaskLater(plugin, 100L);
    }

    /**
     * 闪电术效果
     */
    private void executeLightning(Player player, SkillOrb skill, Entity target, double damage) {
        Location targetLoc;
        
        if (target != null && target instanceof LivingEntity) {
            targetLoc = target.getLocation();
        } else {
            // 没有目标，在玩家前方召唤闪电
            targetLoc = player.getLocation().add(player.getLocation().getDirection().multiply(5));
        }
        
        // 召唤闪电
        LightningStrike lightning = player.getWorld().strikeLightning(targetLoc);
        lightning.setMetadata("skill_damage", new FixedMetadataValue(plugin, damage));
        lightning.setMetadata("skill_id", new FixedMetadataValue(plugin, skill.getSkillId()));
        
        // 播放音效和粒子
        player.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, targetLoc, 100, 2, 2, 2, 0.2);
        
        // 对范围内的实体造成伤害
        Collection<Entity> nearbyEntities = player.getWorld().getNearbyEntities(
            targetLoc, 3, 3, 3,
            entity -> entity instanceof LivingEntity && !entity.equals(player)
        );
        
        for (Entity entity : nearbyEntities) {
            LivingEntity living = (LivingEntity) entity;
            living.damage(damage, player);
        }
    }

    /**
     * 冰冻术效果
     */
    private void executeIceEffect(Player player, SkillOrb skill, Entity target, double damage) {
        // 发射冰球（使用Snowball）
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        
        Snowball snowball = player.launchProjectile(Snowball.class, direction.multiply(2.0));
        snowball.setMetadata("skill_damage", new FixedMetadataValue(plugin, damage));
        snowball.setMetadata("skill_id", new FixedMetadataValue(plugin, skill.getSkillId()));
        snowball.setMetadata("skill_effect", new FixedMetadataValue(plugin, "ice"));
        
        // 播放音效和粒子
        player.getWorld().playSound(eyeLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.SNOWFLAKE, eyeLoc, 30, 0.5, 0.5, 0.5, 0.1);
        
        // 冰球飞行轨迹粒子
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (!snowball.isValid() || ticks >= 100) {
                    cancel();
                    return;
                }
                
                player.getWorld().spawnParticle(Particle.SNOWFLAKE, 
                    snowball.getLocation(), 5, 0.2, 0.2, 0.2, 0.05);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * 治疗术效果
     */
    private void executeHealEffect(Player player, SkillOrb skill, double damage) {
        // 恢复生命值（damage在这里作为治疗量）
        double healAmount = damage;
        double maxHealth = player.getMaxHealth();
        double currentHealth = player.getHealth();
        double newHealth = Math.min(currentHealth + healAmount, maxHealth);
        
        player.setHealth(newHealth);
        
        // 播放音效和粒子
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.HEART, 
            player.getLocation().add(0, 2, 0), 20, 1, 1, 1, 0.1);
        
        player.sendMessage(net.kyori.adventure.text.Component.text(
            "§a恢复 §e" + String.format("%.1f", healAmount) + " §a点生命值!"));
    }

    /**
     * 爆炸效果
     */
    private void executeExplosionEffect(Player player, SkillOrb skill, Entity target, double damage) {
        Location targetLoc;
        
        if (target != null) {
            targetLoc = target.getLocation();
        } else {
            targetLoc = player.getLocation().add(player.getLocation().getDirection().multiply(5));
        }
        
        // 创建爆炸（不破坏方块）
        player.getWorld().createExplosion(targetLoc, 3.0f, false, false);
        
        // 播放音效和粒子
        player.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 10, 2, 1, 2, 0.1);
        
        // 对范围内的实体造成伤害
        Collection<Entity> nearbyEntities = player.getWorld().getNearbyEntities(
            targetLoc, 5, 5, 5,
            entity -> entity instanceof LivingEntity && !entity.equals(player)
        );
        
        for (Entity entity : nearbyEntities) {
            LivingEntity living = (LivingEntity) entity;
            living.damage(damage, player);
        }
    }

    /**
     * 魔法效果
     */
    private void executeMagicEffect(Player player, SkillOrb skill, Entity target, double damage) {
        // 发射魔法弹（使用EnderPearl）
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        
        EnderPearl pearl = player.launchProjectile(EnderPearl.class, direction.multiply(2.0));
        pearl.setMetadata("skill_damage", new FixedMetadataValue(plugin, damage));
        pearl.setMetadata("skill_id", new FixedMetadataValue(plugin, skill.getSkillId()));
        pearl.setMetadata("skill_effect", new FixedMetadataValue(plugin, "magic"));
        
        // 播放音效和粒子
        player.getWorld().playSound(eyeLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.ENCHANT, eyeLoc, 30, 0.5, 0.5, 0.5, 0.1);
        
        // 魔法弹飞行轨迹粒子
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (!pearl.isValid() || ticks >= 100) {
                    cancel();
                    return;
                }
                
                player.getWorld().spawnParticle(Particle.ENCHANT, 
                    pearl.getLocation(), 10, 0.2, 0.2, 0.2, 0.05);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * 默认效果（直接造成伤害）
     */
    private void executeDefaultEffect(Player player, Entity target, double damage) {
        if (target != null && target instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) target;
            living.damage(damage, player);
            
            // 播放基础粒子效果
            player.getWorld().spawnParticle(Particle.FLAME, 
                target.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
        }
    }
}
