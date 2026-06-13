package cn.guangdian.rpgcore.service.api;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 显示服务接口
 * 
 * <p>提供玩家名称显示、前缀后缀、血条显示等功能。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface DisplayService {

    /**
     * 获取玩家显示前缀
     * 
     * @param player 玩家
     * @return 前缀字符串
     */
    String getPrefix(Player player);

    /**
     * 获取玩家显示后缀
     * 
     * @param player 玩家
     * @return 后缀字符串
     */
    String getSuffix(Player player);

    /**
     * 获取玩家显示名称数据
     * 
     * @param playerId 玩家UUID
     * @return 显示名称数据（具体类型由实现定义）
     */
    Object getDisplayData(UUID playerId);

    /**
     * 更新玩家显示
     * 
     * <p>刷新玩家的名称、前缀后缀和血条显示。</p>
     * 
     * @param player 玩家
     */
    void updatePlayerDisplay(Player player);

    /**
     * 批量更新所有玩家显示
     */
    void refreshAllDisplays();

    /**
     * 清理玩家显示缓存
     * 
     * @param playerId 玩家UUID
     */
    void clearDisplayCache(UUID playerId);

    /**
     * 切换玩家是否显示头顶标签
     * 
     * @param player 玩家
     * @param enabled 是否启用
     */
    void setDisplayEnabled(Player player, boolean enabled);

    /**
     * 检查玩家是否启用了头顶标签显示
     * 
     * @param playerId 玩家UUID
     * @return 是否启用
     */
    boolean isDisplayEnabled(UUID playerId);

    /**
     * 获取RPG血量值
     * 
     * @param player 玩家
     * @return RPG血量值
     */
    int getRPGHealth(Player player);

    /**
     * 检查服务是否可用
     * 
     * @return 如果服务可用返回 true
     */
    boolean isAvailable();
}