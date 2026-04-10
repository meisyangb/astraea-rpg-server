package cn.guangdian.rpgcore.service.api;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Tab列表服务接口
 * 
 * <p>提供玩家Tab列表显示功能。</p>
 * <p>支持称号、公会标签、自定义前缀后缀。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface TabService {

    /**
     * 获取玩家在Tab中显示的名称
     * 
     * @param player 玩家
     * @return 显示名称（含前缀后缀）
     */
    String getTabName(Player player);

    /**
     * 获取玩家Tab前缀
     * 
     * @param player 玩家
     * @return 前缀字符串
     */
    String getPrefix(Player player);

    /**
     * 获取玩家Tab后缀
     * 
     * @param player 玩家
     * @return 后缀字符串
     */
    String getSuffix(Player player);

    /**
     * 刷新玩家的Tab显示
     * 
     * @param player 玩家
     */
    void refreshTabName(Player player);

    /**
     * 刷新所有在线玩家的Tab显示
     */
    void refreshAllTabNames();

    /**
     * 设置玩家的自定义Tab名称
     * 
     * @param playerId 玩家UUID
     * @param customName 自定义名称（null表示清除）
     */
    void setCustomName(UUID playerId, String customName);

    /**
     * 获取玩家的自定义Tab名称
     * 
     * @param playerId 玩家UUID
     * @return 自定义名称（未设置返回null）
     */
    String getCustomName(UUID playerId);

    /**
     * 订阅称号变化自动更新
     * 
     * @param enabled 是否启用
     */
    void setAutoUpdateOnTitleChange(boolean enabled);

    /**
     * 订阅公会变化自动更新
     * 
     * @param enabled 是否启用
     */
    void setAutoUpdateOnGuildChange(boolean enabled);

    /**
     * 清理玩家的Tab缓存
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