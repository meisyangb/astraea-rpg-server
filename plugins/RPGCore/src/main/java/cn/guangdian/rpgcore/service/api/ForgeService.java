package cn.guangdian.rpgcore.service.api;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 锻造服务接口
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface ForgeService {

    /**
     * 获取玩家锻造等级
     */
    int getForgeLevel(UUID playerId);

    /**
     * 获取玩家锻造经验
     */
    long getForgeExp(UUID playerId);

    /**
     * 添加锻造经验
     */
    void addForgeExp(UUID playerId, long amount);

    /**
     * 获取玩家已解锁的配方
     */
    List<String> getUnlockedRecipes(UUID playerId);

    /**
     * 检查配方是否已解锁
     */
    boolean hasRecipeUnlocked(UUID playerId, String recipeId);

    /**
     * 解锁配方
     */
    boolean unlockRecipe(UUID playerId, String recipeId);

    /**
     * 检查玩家是否正在锻造
     */
    boolean isForging(UUID playerId);

    /**
     * 开始锻造
     */
    boolean startForge(UUID playerId, String recipeId);

    /**
     * 取消锻造
     */
    boolean cancelForge(UUID playerId);

    /**
     * 获取锻造进度 (0.0 - 1.0)
     */
    double getForgeProgress(UUID playerId);

    /**
     * 获取配方列表
     */
    List<String> getAvailableRecipes();

    /**
     * 检查服务是否可用
     */
    boolean isAvailable();
}