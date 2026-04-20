package cn.guangdian.rpgskill.executor;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.sound.SoundService;
import cn.guangdian.rpgskill.skill.SkillContext;
import cn.guangdian.rpgskill.skill.SkillDefinition;
import cn.guangdian.rpgskill.skill.SkillResult;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * 单体目标执行器
 * 对单个目标造成伤害
 */
public class SingleTargetExecutor implements SkillExecutor {

    @Override
    public SkillResult execute(SkillDefinition definition, SkillContext context) {
        Player caster = context.getCaster();
        LivingEntity target = context.getTarget();

        if (target == null) {
            return SkillResult.failure("没有目标");
        }

        // 获取参数
        double damageMultiplier = definition.getParam("damage-multiplier", 1.0);
        String effect = definition.getParam("effect", "none");
        String sound = definition.getParam("sound", "ENTITY_PLAYER_ATTACK_CRIT");
        boolean trueDamage = definition.getParam("true-damage", false);
        double baseDamage = context.getBaseDamage();

        double damage = baseDamage * damageMultiplier;

        // 播放效果
        Location targetLoc = target.getLocation();
        playEffect(caster, targetLoc, effect);

        // 应用伤害
        if (trueDamage) {
            double newHealth = Math.max(0, target.getHealth() - damage);
            target.setHealth(newHealth);
        } else {
            target.damage(damage, caster);
        }

        // 播放音效
        SoundService soundService = RPGCore.getInstance().getSoundService();
        soundService.playSound(targetLoc, sound, 1.0f, 1.0f);

        return SkillResult.success(damage, 1);
    }

    private void playEffect(Player caster, Location location, String effect) {
        switch (effect.toLowerCase()) {
            case "lightning" -> {
                location.getWorld().strikeLightningEffect(location);
            }
            case "fire" -> {
                location.getWorld().spawnParticle(Particle.FLAME, location.add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
            }
            case "magic" -> {
                location.getWorld().spawnParticle(Particle.WITCH, location.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
            }
            default -> {
                location.getWorld().spawnParticle(Particle.CRIT, location.add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
            }
        }
    }

    @Override
    public String getTypeId() {
        return "single_target";
    }
}
