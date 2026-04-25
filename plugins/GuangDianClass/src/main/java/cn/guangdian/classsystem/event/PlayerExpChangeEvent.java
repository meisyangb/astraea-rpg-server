package cn.guangdian.classsystem.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家经验变更事件
 *
 * <p>当玩家经验值发生变化时触发。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class PlayerExpChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final double oldExp;
    private final double newExp;
    private final double delta;
    private final ChangeReason reason;

    public enum ChangeReason {
        KILL_MOB,       // 击杀怪物
        QUEST,          // 任务奖励
        ITEM,           // 使用物品
        COMMAND,        // 命令给予
        ADMIN,          // 管理员操作
        OTHER           // 其他
    }

    public PlayerExpChangeEvent(Player player, double oldExp, double newExp, ChangeReason reason) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.oldExp = oldExp;
        this.newExp = newExp;
        this.delta = newExp - oldExp;
        this.reason = reason;
    }

    public Player getPlayer() {
        return player;
    }

    public double getOldExp() {
        return oldExp;
    }

    public double getNewExp() {
        return newExp;
    }

    public double getDelta() {
        return delta;
    }

    public ChangeReason getReason() {
        return reason;
    }

    public boolean isGain() {
        return delta > 0;
    }

    public boolean isLoss() {
        return delta < 0;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
