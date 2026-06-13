package cn.guangdian.mobs.skills.condition;

import cn.guangdian.mobs.model.MobSkill;
import org.bukkit.entity.LivingEntity;

/**
 * 目标在范围内条件
 * 格式: targetwithin <distance>
 */
public class TargetWithinCondition implements SkillCondition {

    private final double distance;

    public TargetWithinCondition(double distance) {
        this.distance = distance;
    }

    @Override
    public boolean check(LivingEntity caster, LivingEntity target, MobSkill skill) {
        if (target == null) return false;

        // 检查世界一致性，防止跨世界距离计算异常
        if (caster.getWorld() == null || target.getWorld() == null) return false;
        if (!caster.getWorld().equals(target.getWorld())) return false;

        return caster.getLocation().distance(target.getLocation()) <= distance;
    }

    @Override
    public String getType() {
        return "targetwithin";
    }
}
