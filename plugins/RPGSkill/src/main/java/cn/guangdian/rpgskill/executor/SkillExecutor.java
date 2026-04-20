package cn.guangdian.rpgskill.executor;

import cn.guangdian.rpgskill.skill.SkillContext;
import cn.guangdian.rpgskill.skill.SkillDefinition;
import cn.guangdian.rpgskill.skill.SkillResult;

/**
 * 技能执行器接口
 * 所有技能效果都需要实现此接口
 */
public interface SkillExecutor {

    /**
     * 执行技能
     *
     * @param definition 技能定义
     * @param context    执行上下文
     * @return 执行结果
     */
    SkillResult execute(SkillDefinition definition, SkillContext context);

    /**
     * 获取执行器类型ID
     *
     * @return 类型ID
     */
    String getTypeId();
}
