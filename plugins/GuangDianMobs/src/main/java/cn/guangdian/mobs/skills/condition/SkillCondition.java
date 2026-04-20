package cn.guangdian.mobs.skills.condition;

import cn.guangdian.mobs.model.MobSkill;
import org.bukkit.entity.LivingEntity;

/**
 * 技能条件接口
 */
public interface SkillCondition {

    /**
     * 检查条件是否满足
     *
     * @param caster 技能释放者
     * @param target 技能目标
     * @param skill 技能
     * @return 是否满足条件
     */
    boolean check(LivingEntity caster, LivingEntity target, MobSkill skill);

    /**
     * 获取条件类型
     */
    String getType();
}
