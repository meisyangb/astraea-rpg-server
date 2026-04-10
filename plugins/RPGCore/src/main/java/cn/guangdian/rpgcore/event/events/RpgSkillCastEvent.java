package cn.guangdian.rpgcore.event.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 技能释放事件
 * 
 * <p>当玩家释放技能时触发。</p>
 * 
 * <p>使用示例：</p>
 * <pre>
 * &#64;EventHandler
 * public void onSkillCast(RpgSkillCastEvent event) {
 *     Player caster = event.getCaster();
 *     String skillId = event.getSkillId();
 *     if (!event.isCancelled()) {
 *         // 处理技能逻辑
 *     }
 * }
 * </pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class RpgSkillCastEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player caster;
    private final String skillId;
    private final String skillName;
    private final int manaCost;
    private final int cooldownTicks;
    private boolean cancelled;

    public RpgSkillCastEvent(Player caster, String skillId, String skillName, 
                              int manaCost, int cooldownTicks) {
        super(!Bukkit.isPrimaryThread());
        this.caster = caster;
        this.skillId = skillId;
        this.skillName = skillName;
        this.manaCost = manaCost;
        this.cooldownTicks = cooldownTicks;
        this.cancelled = false;
    }

    public Player getCaster() {
        return caster;
    }

    public String getSkillId() {
        return skillId;
    }

    public String getSkillName() {
        return skillName;
    }

    public int getManaCost() {
        return manaCost;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
