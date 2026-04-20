package cn.guangdian.rpgskill.skill;

/**
 * 技能类型
 */
public enum SkillType {
    /**
     * 主动技能 - 需要玩家主动触发
     */
    ACTIVE,

    /**
     * 被动技能 - 自动触发或持续生效
     */
    PASSIVE,

    /**
     * 切换技能 - 开启/关闭状态
     */
    TOGGLE
}
