package cn.guangdian.rpgskill.skill;

/**
 * 技能执行结果
 */
public class SkillResult {

    private final boolean success;
    private final String message;
    private final double damageDealt;
    private final int targetsHit;

    private SkillResult(boolean success, String message, double damageDealt, int targetsHit) {
        this.success = success;
        this.message = message;
        this.damageDealt = damageDealt;
        this.targetsHit = targetsHit;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public double getDamageDealt() {
        return damageDealt;
    }

    public int getTargetsHit() {
        return targetsHit;
    }

    public static SkillResult success() {
        return new SkillResult(true, null, 0, 0);
    }

    public static SkillResult success(String message) {
        return new SkillResult(true, message, 0, 0);
    }

    public static SkillResult success(double damageDealt, int targetsHit) {
        return new SkillResult(true, null, damageDealt, targetsHit);
    }

    public static SkillResult failure(String message) {
        return new SkillResult(false, message, 0, 0);
    }

    public static SkillResult cooldown(long remainingSeconds) {
        return new SkillResult(false, "技能冷却中，剩余 " + remainingSeconds + " 秒", 0, 0);
    }

    public static SkillResult noMana(double required, double current) {
        return new SkillResult(false, "法力不足，需要 " + required + "，当前 " + current, 0, 0);
    }
}
