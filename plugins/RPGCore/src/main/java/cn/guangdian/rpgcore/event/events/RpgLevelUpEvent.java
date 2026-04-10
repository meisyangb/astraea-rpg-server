package cn.guangdian.rpgcore.event.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import cn.guangdian.rpgcore.event.CoreEvent;

/**
 * 玩家升级事件
 * 
 * <p>当玩家等级提升时触发，所有插件都可以监听此事件。</p>
 * 
 * <p>同时支持 Bukkit 事件系统和 RPGCore EventBus。</p>
 * 
 * <p>使用示例：</p>
 * <pre>
 * // Bukkit 事件监听
 * &#64;EventHandler
 * public void onLevelUp(RpgLevelUpEvent event) {
 *     Player player = event.getPlayer();
 *     int newLevel = event.getNewLevel();
 *     // 处理升级逻辑
 * }
 * 
 * // RPGCore EventBus 订阅
 * eventBus.subscribe(RpgLevelUpEvent.class, event -> {
 *     // 处理升级逻辑
 * });
 * </pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class RpgLevelUpEvent extends CoreEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final int oldLevel;
    private final int newLevel;
    private final String source;

    public RpgLevelUpEvent(Player player, int oldLevel, int newLevel, String source) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.source = source;
    }

    public RpgLevelUpEvent(Player player, int oldLevel, int newLevel) {
        this(player, oldLevel, newLevel, "UNKNOWN");
    }

    public Player getPlayer() {
        return player;
    }

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }

    public String getSource() {
        return source;
    }

    @Override
    public String getEventName() {
        return "RpgLevelUpEvent";
    }

    // ==================== Bukkit 事件支持 ====================
    
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
