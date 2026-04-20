package cn.guangdian.mobs.skills.condition;

import cn.guangdian.mobs.model.MobSkill;
import org.bukkit.entity.LivingEntity;

/**
 * 血量条件
 * 格式: health <percentage>
 */
public class HealthCondition implements SkillCondition {

    private final double minHealthPercent;
    private final double maxHealthPercent;

    public HealthCondition(double minPercent, double maxPercent) {
        this.minHealthPercent = minPercent;
        this.maxHealthPercent = maxPercent;
    }

    @Override
    public boolean check(LivingEntity caster, LivingEntity target, MobSkill skill) {
        double healthPercent = (caster.getHealth() / caster.getMaxHealth()) * 100;
        return healthPercent >= minHealthPercent && healthPercent <= maxHealthPercent;
    }

    @Override
    public String getType() {
        return "health";
    }
}
