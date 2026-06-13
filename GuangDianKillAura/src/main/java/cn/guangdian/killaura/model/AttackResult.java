package cn.guangdian.killaura.model;

import org.bukkit.entity.LivingEntity;

public record AttackResult(
    boolean success,
    LivingEntity target,
    double damage,
    boolean killed,
    AttackFailReason failReason
) {

    public static AttackResult success(LivingEntity target, double damage, boolean killed) {
        return new AttackResult(true, target, damage, killed, null);
    }

    public static AttackResult fail(AttackFailReason reason) {
        return new AttackResult(false, null, 0, false, reason);
    }

    public enum AttackFailReason {
        NO_TARGET,
        OUT_OF_RANGE,
        TARGET_DEAD,
        TARGET_INVULNERABLE,
        COOLDOWN_ACTIVE,
        PLAYER_DEAD,
        KILL_AURA_DISABLED
    }
}
