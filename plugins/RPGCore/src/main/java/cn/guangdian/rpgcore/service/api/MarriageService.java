package cn.guangdian.rpgcore.service.api;

import java.util.UUID;

/**
 * 结婚服务接口
 * 
 * <p>提供结婚系统相关功能。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface MarriageService {

    /**
     * 检查玩家是否已婚
     * 
     * @param playerId 玩家UUID
     * @return 如果已婚返回 true
     */
    boolean isMarried(UUID playerId);

    /**
     * 获取玩家的伴侣
     * 
     * @param playerId 玩家UUID
     * @return 伴侣名称，如果未婚返回 null
     */
    String getPartner(UUID playerId);

    /**
     * 获取结婚对象
     * 
     * @param playerId 玩家UUID
     * @return 结婚对象（插件特定类型）
     */
    Object getMarriage(UUID playerId);

    /**
     * 让两个玩家结婚
     * 
     * @param player1 玩家1 UUID
     * @param player2 玩家2 UUID
     * @return 如果成功返回 true
     */
    boolean marry(UUID player1, UUID player2);

    /**
     * 离婚
     * 
     * @param playerId 玩家UUID
     * @return 如果成功返回 true
     */
    boolean divorce(UUID playerId);

    /**
     * 获取结婚天数
     * 
     * @param playerId 玩家UUID
     * @return 结婚天数，如果未婚返回 0
     */
    long getMarriageDays(UUID playerId);

    /**
     * 检查服务是否可用
     * 
     * @return 如果服务可用返回 true
     */
    boolean isAvailable();
}