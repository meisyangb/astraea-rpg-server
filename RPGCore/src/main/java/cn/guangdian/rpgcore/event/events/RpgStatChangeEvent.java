package cn.guangdian.rpgcore.event.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 属性变更事件
 * 
 * <p>当玩家属性发生变化时触发。</p>
 * 
 * <p>使用示例：</p>
 * <pre>
 * &#64;EventHandler
 * public void onStatChange(RpgStatChangeEvent event) {
 *     Player player = event.getPlayer();
 *     String statType = event.getStatType();
 *     double oldValue = event.getOldValue();
 *     double newValue = event.getNewValue();
 *     // 处理属性变更
 * }
 * </pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class RpgStatChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final String statType;
    private final double oldValue;
    private final double newValue;
    private final String source;

    public RpgStatChangeEvent(Player player, String statType, double oldValue, 
                               double newValue, String source) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.statType = statType;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.source = source;
    }

    public RpgStatChangeEvent(Player player, String statType, double oldValue, double newValue) {
        this(player, statType, oldValue, newValue, "UNKNOWN");
    }

    public Player getPlayer() {
        return player;
    }

    public String getStatType() {
        return statType;
    }

    public double getOldValue() {
        return oldValue;
    }

    public double getNewValue() {
        return newValue;
    }

    public String getSource() {
        return source;
    }

    public double getDifference() {
        return newValue - oldValue;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
