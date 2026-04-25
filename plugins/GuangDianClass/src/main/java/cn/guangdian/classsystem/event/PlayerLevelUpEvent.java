package cn.guangdian.classsystem.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家升级事件
 * 
 * <p>当玩家等级提升时触发，所有插件都可以监听此事件。</p>
 * 
 * <p>使用示例：</p>
 * <pre>
 * // Bukkit 事件监听
 * &#64;EventHandler
 * public void onLevelUp(PlayerLevelUpEvent event) {
 *     Player player = event.getPlayer();
 *     int newLevel = event.getNewLevel();
 *     // 处理升级逻辑
 * }
 * </pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class PlayerLevelUpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final int oldLevel;
    private final int newLevel;
    private final String source;

    /**
     * 创建升级事件
     * 
     * @param player 玩家
     * @param oldLevel 旧等级
     * @param newLevel 新等级
     * @param source 来源
     */
    public PlayerLevelUpEvent(Player player, int oldLevel, int newLevel, String source) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.source = source;
    }

    /**
     * 创建升级事件（默认来源）
     * 
     * @param player 玩家
     * @param oldLevel 旧等级
     * @param newLevel 新等级
     */
    public PlayerLevelUpEvent(Player player, int oldLevel, int newLevel) {
        this(player, oldLevel, newLevel, "GuangDianClass");
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
    
    /**
     * 获取升级级数
     * 
     * @return 升级了多少级
     */
    public int getLevelsGained() {
        return newLevel - oldLevel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
