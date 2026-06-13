package cn.guangdian.rpgcore.event.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 玩家击杀怪物事件
 * 
 * <p>当玩家击杀怪物时触发，支持 MythicMobs 怪物。</p>
 * 
 * <p>使用示例：</p>
 * <pre>
 * &#64;EventHandler
 * public void onMobKill(RpgMobKillEvent event) {
 *     Player killer = event.getKiller();
 *     Entity mob = event.getMob();
 *     if (event.isMythicMob()) {
 *         String mythicType = event.getMythicMobType();
 *         // 处理 MythicMobs 击杀
 *     }
 * }
 * </pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class RpgMobKillEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player killer;
    private final Entity mob;
    private final boolean isMythicMob;
    private final String mythicMobType;
    private double expReward;
    private double moneyReward;

    public RpgMobKillEvent(Player killer, Entity mob, boolean isMythicMob, String mythicMobType) {
        super(!Bukkit.isPrimaryThread());
        this.killer = killer;
        this.mob = mob;
        this.isMythicMob = isMythicMob;
        this.mythicMobType = mythicMobType;
        this.expReward = 0;
        this.moneyReward = 0;
    }

    public RpgMobKillEvent(Player killer, Entity mob) {
        this(killer, mob, false, null);
    }

    public Player getKiller() {
        return killer;
    }

    public Entity getMob() {
        return mob;
    }

    public boolean isMythicMob() {
        return isMythicMob;
    }

    public String getMythicMobType() {
        return mythicMobType;
    }

    public double getExpReward() {
        return expReward;
    }

    public void setExpReward(double expReward) {
        this.expReward = expReward;
    }

    public double getMoneyReward() {
        return moneyReward;
    }

    public void setMoneyReward(double moneyReward) {
        this.moneyReward = moneyReward;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
