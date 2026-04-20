package cn.guangdian.rpgskill.skill;

/**
 * 技能触发类型
 */
public enum TriggerType {
    /**
     * 右键点击触发
     */
    RIGHT_CLICK,

    /**
     * 左键点击触发
     */
    LEFT_CLICK,

    /**
     * 攻击时触发
     */
    ON_HIT,

    /**
     * 被攻击时触发
     */
    ON_DAMAGE_TAKEN,

    /**
     * 击杀目标时触发
     */
    ON_KILL,

    /**
     * 持续生效（被动）
     */
    PASSIVE,

    /**
     * 定时触发
     */
    TIMER
}
