package cn.guangdian.rpgcore.service.api;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * RPG属性服务接口
 * 
 * <p>提供玩家RPG属性的管理功能。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface StatsService {

    /**
     * 获取玩家属性数据
     * 
     * @param playerId 玩家UUID
     * @return 玩家属性数据（具体类型由实现定义）
     */
    Object getPlayerStats(UUID playerId);

    /**
     * 刷新玩家属性
     * 
     * @param player 玩家
     */
    void refreshPlayerStats(Player player);

    /**
     * 计算伤害
     * 
     * @param attacker 攻击者
     * @param target 目标
     * @param baseDamage 基础伤害
     * @return 最终伤害
     */
    double calculateDamage(Player attacker, Player target, double baseDamage);

    /**
     * 获取玩家总攻击力
     * 
     * @param player 玩家
     * @return 总攻击力
     */
    double getTotalAttack(Player player);

    /**
     * 获取玩家总防御力
     * 
     * @param player 玩家
     * @return 总防御力
     */
    double getTotalDefense(Player player);

    /**
     * 获取玩家总生命值
     * 
     * @param player 玩家
     * @return 总生命值
     */
    double getTotalHealth(Player player);

    /**
     * 获取玩家暴击率
     * 
     * @param player 玩家
     * @return 暴击率（0.0 - 1.0）
     */
    double getCritRate(Player player);

    /**
     * 获取玩家暴击伤害
     * 
     * @param player 玩家
     * @return 暴击伤害倍率
     */
    double getCritDamage(Player player);

    /**
     * 清理玩家属性缓存
     * 
     * @param playerId 玩家UUID
     */
    void clearPlayerCache(UUID playerId);

    /**
     * 保存玩家数据
     * 
     * @param playerId 玩家UUID
     * @return 操作完成的 CompletableFuture
     */
    CompletableFuture<Void> savePlayerData(UUID playerId);
}