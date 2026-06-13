package cn.guangdian.rpgcore.service.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 掉落控制服务接口
 * 
 * <p>提供玩家掉落物品控制功能，支持全局和玩家级别的开关。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface DropControlService {

    /**
     * 检查全局掉落是否启用
     * 
     * @return 如果启用返回 true
     */
    boolean isGlobalDropEnabled();

    /**
     * 设置全局掉落开关
     * 
     * @param enabled 是否启用
     */
    void setGlobalDropEnabled(boolean enabled);

    /**
     * 检查玩家是否可以丢弃物品
     * 
     * @param playerId 玩家UUID
     * @return 如果可以丢弃返回 true
     */
    boolean canPlayerDrop(UUID playerId);

    /**
     * 设置玩家掉落开关
     * 
     * @param playerId 玩家UUID
     * @param enabled 是否启用
     */
    void setPlayerDropEnabled(UUID playerId, boolean enabled);

    /**
     * 切换玩家掉落开关
     * 
     * @param playerId 玩家UUID
     * @return 切换后的状态
     */
    boolean togglePlayerDrop(UUID playerId);

    /**
     * 获取玩家掉落状态
     * 
     * @param playerId 玩家UUID
     * @return 状态，null 表示使用默认值
     */
    Boolean getPlayerDropStatus(UUID playerId);

    /**
     * 清除玩家状态（玩家退出时调用）
     * 
     * @param playerId 玩家UUID
     */
    void clearPlayerStatus(UUID playerId);

    /**
     * 检查玩家是否有绕过权限
     * 
     * @param playerId 玩家UUID
     * @return 如果有绕过权限返回 true
     */
    boolean hasBypassPermission(UUID playerId);

    /**
     * 检查玩家是否有使用权限
     * 
     * @param playerId 玩家UUID
     * @return 如果有使用权限返回 true
     */
    boolean hasUsePermission(UUID playerId);

    /**
     * 获取启用掉落的玩家数量
     * 
     * @return 玩家数量
     */
    int getEnabledPlayerCount();

    /**
     * 检查服务是否可用
     * 
     * @return 如果服务可用返回 true
     */
    boolean isAvailable();
}