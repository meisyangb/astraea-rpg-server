package cn.guangdian.armorstats.combat;

import cn.guangdian.armorstats.data.PlayerStats;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatContext {

    private final EntityDamageByEntityEvent event;
    private final LivingEntity attacker;
    private final LivingEntity target;
    private final Player attackerPlayer;
    private final Player defenderPlayer;
    private final PlayerStats attackerStats;
    private final PlayerStats defenderStats;
    private final boolean pvp;
    private double damage;
    private boolean critical;
    private boolean dodged;
    private boolean parried;
    private double reflectedDamage;

    public CombatContext(
            EntityDamageByEntityEvent event,
            LivingEntity attacker,
            LivingEntity target,
            Player attackerPlayer,
            Player defenderPlayer,
            PlayerStats attackerStats,
            PlayerStats defenderStats,
            boolean pvp,
            double damage
    ) {
        this.event = event;
        this.attacker = attacker;
        this.target = target;
        this.attackerPlayer = attackerPlayer;
        this.defenderPlayer = defenderPlayer;
        this.attackerStats = attackerStats;
        this.defenderStats = defenderStats;
        this.pvp = pvp;
        this.damage = damage;
    }

    public EntityDamageByEntityEvent getEvent() {
        return event;
    }

    public LivingEntity getAttacker() {
        return attacker;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public Player getAttackerPlayer() {
        return attackerPlayer;
    }

    public Player getDefenderPlayer() {
        return defenderPlayer;
    }

    public PlayerStats getAttackerStats() {
        return attackerStats;
    }

    public PlayerStats getDefenderStats() {
        return defenderStats;
    }

    public boolean isPvp() {
        return pvp;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public boolean isCritical() {
        return critical;
    }

    public void setCritical(boolean critical) {
        this.critical = critical;
    }

    public boolean isDodged() {
        return dodged;
    }

    public void setDodged(boolean dodged) {
        this.dodged = dodged;
    }

    public boolean isParried() {
        return parried;
    }

    public void setParried(boolean parried) {
        this.parried = parried;
    }

    public double getReflectedDamage() {
        return reflectedDamage;
    }

    public void setReflectedDamage(double reflectedDamage) {
        this.reflectedDamage = reflectedDamage;
    }

    public boolean isBlocked() {
        return dodged || parried;
    }
}
