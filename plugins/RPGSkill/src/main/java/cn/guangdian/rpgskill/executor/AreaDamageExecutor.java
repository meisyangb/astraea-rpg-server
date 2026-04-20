package cn.guangdian.rpgskill.executor;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.sound.SoundService;
import cn.guangdian.rpgskill.skill.SkillContext;
import cn.guangdian.rpgskill.skill.SkillDefinition;
import cn.guangdian.rpgskill.skill.SkillResult;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * 范围伤害执行器
 * 对范围内所有敌人造成伤害
 */
public class AreaDamageExecutor implements SkillExecutor {

    @Override
    public SkillResult execute(SkillDefinition definition, SkillContext context) {
        Player caster = context.getCaster();
        if (caster == null || !caster.isOnline()) {
            return SkillResult.failure("施法者无效");
        }

        Location location = context.getLocation();
        if (location == null || location.getWorld() == null) {
            return SkillResult.failure("目标位置无效");
        }

        World world = location.getWorld();

        // 获取参数
        double range = definition.getParam("range", 5.0);
        double damageMultiplier = definition.getParam("damage-multiplier", 1.0);
        String effect = definition.getParam("effect", "none");
        String particle = definition.getParam("particle", "FLAME");
        String sound = definition.getParam("sound", "ENTITY_GENERIC_EXPLODE");
        double baseDamage = context.getBaseDamage();

        // 播放效果
        playEffect(world, location, effect, particle, caster);

        // 获取范围内目标
        List<Entity> nearby = world.getNearbyEntities(location, range, range, range)
                .stream()
                .filter(e -> e instanceof LivingEntity && !e.equals(caster))
                .toList();

        double totalDamage = 0;
        int hitCount = 0;

        for (Entity entity : nearby) {
            LivingEntity target = (LivingEntity) entity;
            Location targetLoc = target.getLocation();
            
            // 安全检查：目标位置和世界有效性
            if (targetLoc.getWorld() == null || !targetLoc.getWorld().equals(world)) {
                continue;
            }

            double distance = location.distance(targetLoc);

            if (distance <= range) {
                double damage = baseDamage * damageMultiplier;

                // 应用伤害
                target.damage(damage, caster);
                totalDamage += damage;
                hitCount++;

                // 应用状态效果
                applyStatusEffects(target, definition);
            }
        }

        // 播放音效
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            SoundService soundService = rpgCore.getSoundService();
            if (soundService != null) {
                soundService.playSound(location, sound, 1.0f, 1.0f);
            }
        }

        return SkillResult.success(totalDamage, hitCount);
    }

    private void playEffect(World world, Location location, String effect, String particle, Player caster) {
        Particle p = getParticle(particle);

        switch (effect.toLowerCase()) {
            case "fire" -> {
                world.spawnParticle(Particle.FLAME, location, 50, 2, 1, 2, 0.1);
            }
            case "lightning" -> {
                world.spawnParticle(Particle.ELECTRIC_SPARK, location, 100, 3, 2, 3, 0.2);
                // 召唤视觉闪电
                for (Entity entity : world.getNearbyEntities(location, 5, 5, 5)) {
                    if (entity instanceof LivingEntity && !entity.equals(caster)) {
                        Location entityLoc = entity.getLocation();
                        if (entityLoc.getWorld() != null) {
                            entityLoc.getWorld().strikeLightningEffect(entityLoc);
                        }
                    }
                }
            }
            case "ice" -> {
                world.spawnParticle(Particle.SNOWFLAKE, location, 50, 2, 1, 2, 0.1);
            }
            case "magic" -> {
                world.spawnParticle(Particle.ENCHANT, location, 50, 2, 1, 2, 0.1);
            }
            default -> {
                if (p != null) {
                    world.spawnParticle(p, location, 30, 2, 1, 2, 0.1);
                }
            }
        }
    }

    private void applyStatusEffects(LivingEntity target, SkillDefinition definition) {
        List<String> statusEffects = definition.getParam("status-effects", List.of());
        int duration = definition.getParam("duration", 3) * 20; // 转换为tick

        for (String effectStr : statusEffects) {
            String[] parts = effectStr.split(":");
            if (parts.length >= 2) {
                try {
                    PotionEffectType type = getPotionEffectType(parts[0]);
                    int level = Integer.parseInt(parts[1]) - 1;

                    if (type != null && level >= 0) {
                        target.addPotionEffect(new PotionEffect(type, duration, level));
                    }
                } catch (NumberFormatException ignored) {
                    // 忽略无效的数字格式
                }
            }
        }
    }

    private Particle getParticle(String name) {
        if (name == null || name.isEmpty()) {
            return Particle.FLAME;
        }
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Particle.FLAME;
        }
    }

    private PotionEffectType getPotionEffectType(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return switch (name.toLowerCase()) {
            case "slowness", "缓慢" -> PotionEffectType.SLOWNESS;
            case "weakness", "虚弱" -> PotionEffectType.WEAKNESS;
            case "poison", "中毒" -> PotionEffectType.POISON;
            case "blindness", "致盲" -> PotionEffectType.BLINDNESS;
            case "wither", "凋零" -> PotionEffectType.WITHER;
            default -> null;
        };
    }

    @Override
    public String getTypeId() {
        return "area_damage";
    }
}
