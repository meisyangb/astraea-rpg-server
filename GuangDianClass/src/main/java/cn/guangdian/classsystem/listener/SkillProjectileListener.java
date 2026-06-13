package cn.guangdian.classsystem.listener;

import cn.guangdian.classsystem.GuangDianClass;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * 技能投射物监听器
 * 
 * 处理技能投射物（火球、冰球、魔法弹等）击中目标的事件
 */
public class SkillProjectileListener implements Listener {

    private final GuangDianClass plugin;

    public SkillProjectileListener(GuangDianClass plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        
        // 检查是否为技能投射物
        if (!projectile.hasMetadata("skill_damage")) {
            return;
        }
        
        // 获取技能伤害
        List<MetadataValue> damageValues = projectile.getMetadata("skill_damage");
        if (damageValues.isEmpty()) return;
        
        double damage = damageValues.get(0).asDouble();
        
        // 获取技能效果
        String effect = null;
        if (projectile.hasMetadata("skill_effect")) {
            List<MetadataValue> effectValues = projectile.getMetadata("skill_effect");
            if (!effectValues.isEmpty()) {
                effect = effectValues.get(0).asString();
            }
        }
        
        // 获取射击者
        Player shooter = null;
        if (projectile.getShooter() instanceof Player) {
            shooter = (Player) projectile.getShooter();
        }
        
        // 处理击中的实体
        Entity hitEntity = event.getHitEntity();
        if (hitEntity != null && hitEntity instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) hitEntity;
            
            // 造成伤害
            if (shooter != null) {
                target.damage(damage, shooter);
            } else {
                target.damage(damage);
            }
            
            // 应用特殊效果
            applySpecialEffect(target, effect, damage);
            
            // 播放击中效果
            playHitEffect(target, effect);
        }
        
        // 处理击中的方块
        if (event.getHitBlock() != null) {
            // 在击中位置播放效果
            playHitEffect(event.getHitBlock().getLocation(), effect, damage);
        }
        
        // 移除投射物
        projectile.remove();
    }

    /**
     * 应用特殊效果
     */
    private void applySpecialEffect(LivingEntity target, String effect, double damage) {
        if (effect == null) return;
        
        switch (effect.toLowerCase()) {
            case "ice":
            case "freeze":
                // 冰冻效果：减速并造成额外伤害
                target.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, 100, 2, false, true
                ));
                target.addPotionEffect(new PotionEffect(
                    PotionEffectType.WEAKNESS, 100, 1, false, true
                ));
                break;
                
            case "fire":
            case "fireball":
                // 燃烧效果
                target.setFireTicks(100); // 5秒燃烧
                break;
                
            case "magic":
                // 魔法效果：虚弱
                target.addPotionEffect(new PotionEffect(
                    PotionEffectType.WEAKNESS, 60, 1, false, true
                ));
                break;
        }
    }

    /**
     * 播放击中效果（实体）
     */
    private void playHitEffect(LivingEntity target, String effect) {
        if (effect == null) {
            // 默认效果
            target.getWorld().spawnParticle(Particle.FLAME, 
                target.getLocation(), 20, 0.5, 1, 0.5, 0.1);
            return;
        }
        
        switch (effect.toLowerCase()) {
            case "ice":
            case "freeze":
                target.getWorld().spawnParticle(Particle.SNOWFLAKE, 
                    target.getLocation(), 50, 1, 1, 1, 0.1);
                target.getWorld().playSound(target.getLocation(), 
                    Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
                break;
                
            case "fire":
            case "fireball":
                target.getWorld().spawnParticle(Particle.FLAME, 
                    target.getLocation(), 50, 1, 1, 1, 0.1);
                target.getWorld().playSound(target.getLocation(), 
                    Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
                break;
                
            case "magic":
                target.getWorld().spawnParticle(Particle.ENCHANT, 
                    target.getLocation(), 50, 1, 1, 1, 0.1);
                target.getWorld().playSound(target.getLocation(), 
                    Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
                break;
        }
    }

    /**
     * 播放击中效果（方块）
     */
    private void playHitEffect(org.bukkit.Location location, String effect, double damage) {
        if (effect == null) {
            location.getWorld().spawnParticle(Particle.FLAME, location, 20, 0.5, 0.5, 0.5, 0.1);
            return;
        }
        
        switch (effect.toLowerCase()) {
            case "ice":
            case "freeze":
                location.getWorld().spawnParticle(Particle.SNOWFLAKE, location, 50, 1, 1, 1, 0.1);
                location.getWorld().playSound(location, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
                break;
                
            case "fire":
            case "fireball":
                location.getWorld().spawnParticle(Particle.FLAME, location, 50, 1, 1, 1, 0.1);
                location.getWorld().playSound(location, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
                break;
                
            case "magic":
                location.getWorld().spawnParticle(Particle.ENCHANT, location, 50, 1, 1, 1, 0.1);
                location.getWorld().playSound(location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
                break;
        }
    }
}
