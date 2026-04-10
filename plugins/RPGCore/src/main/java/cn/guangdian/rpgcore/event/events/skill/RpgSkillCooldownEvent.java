package cn.guangdian.rpgcore.event.events.skill;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * 技能冷却事件
 * 
 * <p>当技能进入冷却或冷却结束时触发。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @EventHandler
 * public void onSkillCooldown(RpgSkillCooldownEvent event) {
 *     Player player = event.getPlayer();
 *     String skillId = event.getSkillId();
 *     
 *     if (event.isCooldownStart()) {
 *         // 技能进入冷却
 *         long cooldownMs = event.getCooldownMs();
 *         sendActionBar(player, "技能 " + event.getSkillName() + " 冷却 " + (cooldownMs / 1000) + "秒");
 *     } else {
 *         // 冷却结束
 *         sendActionBar(player, "技能 " + event.getSkillName() + " 已就绪!");
 *     }
 * }
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class RpgSkillCooldownEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final UUID playerId;
    private final Player player;
    private final String skillId;
    private final String skillName;
    private final long cooldownMs;
    private final boolean cooldownStart;
    private final long remainingMs;

    /**
     * 创建技能冷却事件
     * 
     * @param player 玩家
     * @param skillId 技能ID
     * @param skillName 技能名称
     * @param cooldownMs 冷却时间（毫秒）
     * @param cooldownStart 是否为冷却开始（false表示冷却结束）
     */
    public RpgSkillCooldownEvent(Player player, String skillId, String skillName, 
                                  long cooldownMs, boolean cooldownStart) {
        this(player, skillId, skillName, cooldownMs, cooldownStart, cooldownStart ? cooldownMs : 0);
    }

    /**
     * 创建技能冷却事件（带剩余时间）
     * 
     * @param player 玩家
     * @param skillId 技能ID
     * @param skillName 技能名称
     * @param cooldownMs 总冷却时间（毫秒）
     * @param cooldownStart 是否为冷却开始
     * @param remainingMs 剩余冷却时间（毫秒）
     */
    public RpgSkillCooldownEvent(Player player, String skillId, String skillName, 
                                  long cooldownMs, boolean cooldownStart, long remainingMs) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.playerId = player.getUniqueId();
        this.skillId = skillId;
        this.skillName = skillName;
        this.cooldownMs = cooldownMs;
        this.cooldownStart = cooldownStart;
        this.remainingMs = remainingMs;
    }

    /**
     * 获取玩家
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * 获取玩家UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * 获取技能ID
     */
    public String getSkillId() {
        return skillId;
    }

    /**
     * 获取技能名称
     */
    public String getSkillName() {
        return skillName;
    }

    /**
     * 获取冷却时间（毫秒）
     */
    public long getCooldownMs() {
        return cooldownMs;
    }

    /**
     * 获取冷却时间（秒）
     */
    public double getCooldownSeconds() {
        return cooldownMs / 1000.0;
    }

    /**
     * 获取冷却时间（ticks）
     */
    public long getCooldownTicks() {
        return cooldownMs / 50; // 1秒 = 20 ticks = 1000ms
    }

    /**
     * 是否为冷却开始
     */
    public boolean isCooldownStart() {
        return cooldownStart;
    }

    /**
     * 是否为冷却结束
     */
    public boolean isCooldownEnd() {
        return !cooldownStart;
    }

    /**
     * 获取剩余冷却时间（毫秒）
     */
    public long getRemainingMs() {
        return remainingMs;
    }

    /**
     * 获取剩余冷却时间（秒）
     */
    public double getRemainingSeconds() {
        return remainingMs / 1000.0;
    }

    /**
     * 检查技能是否可用（冷却结束）
     */
    public boolean isReady() {
        return remainingMs <= 0;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}