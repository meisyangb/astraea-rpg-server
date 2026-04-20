package cn.guangdian.rpgskill.executor;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.sound.SoundService;
import cn.guangdian.rpgskill.skill.SkillContext;
import cn.guangdian.rpgskill.skill.SkillDefinition;
import cn.guangdian.rpgskill.skill.SkillResult;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

/**
 * 治疗执行器
 */
public class HealExecutor implements SkillExecutor {

    @Override
    public SkillResult execute(SkillDefinition definition, SkillContext context) {
        Player caster = context.getCaster();

        // 获取参数
        double healPercent = definition.getParam("heal-percent", 0.0);
        double healAmount = definition.getParam("heal-amount", 0.0);
        String sound = definition.getParam("sound", "BLOCK_BEACON_POWER_SELECT");

        double maxHealth = caster.getAttribute(Attribute.MAX_HEALTH).getValue();
        double currentHealth = caster.getHealth();

        double heal;
        if (healPercent > 0) {
            heal = maxHealth * (healPercent / 100.0);
        } else {
            heal = healAmount;
        }

        double newHealth = Math.min(currentHealth + heal, maxHealth);
        caster.setHealth(newHealth);

        // 播放效果
        Location loc = caster.getLocation().add(0, 2, 0);
        caster.getWorld().spawnParticle(Particle.HEART, loc, 10, 0.5, 0.5, 0.5, 0.1);

        // 播放音效
        SoundService soundService = RPGCore.getInstance().getSoundService();
        soundService.playSound(loc, sound, 1.0f, 1.0f);

        return SkillResult.success("恢复 " + (int) heal + " 点生命值");
    }

    @Override
    public String getTypeId() {
        return "heal";
    }
}
