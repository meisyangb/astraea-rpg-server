package cn.guangdian.armorstats.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * 技能施放事件
 *
 * <p>当玩家施放技能时触发。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class SkillCastEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String skillId;
    private final String skillName;
    private final double manaCost;
    private final double cooldown;
    private boolean cancelled = false;

    public SkillCastEvent(Player player, String skillId, String skillName, double manaCost, double cooldown) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.skillId = skillId;
        this.skillName = skillName;
        this.manaCost = manaCost;
        this.cooldown = cooldown;
    }

    public Player getPlayer() {
        return player;
    }

    public String getSkillId() {
        return skillId;
    }

    public String getSkillName() {
        return skillName;
    }

    public double getManaCost() {
        return manaCost;
    }

    public double getCooldown() {
        return cooldown;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
