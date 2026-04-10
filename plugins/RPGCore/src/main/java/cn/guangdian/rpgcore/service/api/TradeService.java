package cn.guangdian.rpgcore.service.api;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 交易服务接口
 * 
 * <p>提供玩家间交易功能。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface TradeService {

    /**
     * 检查玩家是否在交易中
     * 
     * @param playerId 玩家UUID
     * @return 如果在交易中返回 true
     */
    boolean isInTrade(UUID playerId);

    /**
     * 获取交易伙伴
     * 
     * @param playerId 玩家UUID
     * @return 交易伙伴UUID，如果没有返回 null
     */
    UUID getTradePartner(UUID playerId);

    /**
     * 发送交易请求
     * 
     * @param requester 请求者
     * @param target 目标玩家
     * @return 如果成功发送返回 true
     */
    boolean sendTradeRequest(Player requester, Player target);

    /**
     * 接受交易请求
     * 
     * @param player 接受者
     * @return 如果成功返回 true
     */
    boolean acceptTradeRequest(Player player);

    /**
     * 拒绝交易请求
     * 
     * @param player 拒绝者
     */
    void denyTradeRequest(Player player);

    /**
     * 取消当前交易
     * 
     * @param playerId 玩家UUID
     * @return 如果成功取消返回 true
     */
    boolean cancelTrade(UUID playerId);

    /**
     * 检查服务是否可用
     * 
     * @return 如果服务可用返回 true
     */
    boolean isAvailable();
}