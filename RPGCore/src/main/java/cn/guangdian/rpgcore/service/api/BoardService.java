package cn.guangdian.rpgcore.service.api;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * 记分板服务接口
 * 
 * <p>提供玩家侧边栏记分板显示功能。</p>
 * <p>支持订阅属性变化事件自动更新。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface BoardService {

    /**
     * 获取玩家的记分板内容
     * 
     * @param player 玩家
     * @return 记分板行列表
     */
    List<String> getBoardLines(Player player);

    /**
     * 刷新玩家的记分板
     * 
     * @param player 玩家
     */
    void refreshBoard(Player player);

    /**
     * 刷新所有在线玩家的记分板
     */
    void refreshAllBoards();

    /**
     * 为玩家启用记分板
     * 
     * @param player 玩家
     */
    void enableBoard(Player player);

    /**
     * 为玩家禁用记分板
     * 
     * @param player 玩家
     */
    void disableBoard(Player player);

    /**
     * 检查玩家是否启用了记分板
     * 
     * @param playerId 玩家UUID
     * @return 是否启用
     */
    boolean isBoardEnabled(UUID playerId);

    /**
     * 订阅属性变化自动更新
     * 
     * <p>当玩家属性变化时自动刷新记分板。</p>
     * 
     * @param enabled 是否启用
     */
    void setAutoUpdateOnStatsChange(boolean enabled);

    /**
     * 检查是否订阅了属性变化自动更新
     * 
     * @return 是否启用
     */
    boolean isAutoUpdateOnStatsChangeEnabled();

    /**
     * 清理玩家的记分板缓存
     * 
     * @param playerId 玩家UUID
     */
    void clearCache(UUID playerId);

    /**
     * 检查服务是否可用
     * 
     * @return 如果服务可用返回 true
     */
    boolean isAvailable();
}