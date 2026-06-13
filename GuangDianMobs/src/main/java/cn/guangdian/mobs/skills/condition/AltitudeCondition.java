package cn.guangdian.mobs.skills.condition;

import cn.guangdian.mobs.model.MobSkill;
import org.bukkit.entity.LivingEntity;

/**
 * 高度条件 - 检查施法者是否在指定高度范围内
 * 格式: altitude <min>-<max>  例: altitude 0-64 或 altitude <64
 */
public class AltitudeCondition implements SkillCondition {

    private final double minY;
    private final double maxY;

    public AltitudeCondition(double minY, double maxY) {
        this.minY = minY;
        this.maxY = maxY;
    }

    @Override
    public boolean check(LivingEntity caster, LivingEntity target, MobSkill skill) {
        double y = caster.getLocation().getY();
        return y >= minY && y <= maxY;
    }

    @Override
    public String getType() {
        return "ALTITUDE";
    }
}
