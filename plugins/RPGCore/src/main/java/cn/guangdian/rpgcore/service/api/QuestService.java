package cn.guangdian.rpgcore.service.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 任务服务接口
 * 
 * <p>提供任务系统的统一服务接口，支持：</p>
 * <ul>
 *   <li>任务接取、完成、放弃</li>
 *   <li>任务进度查询</li>
 *   <li>每日任务管理</li>
 *   <li>任务线/剧情线管理</li>
 * </ul>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface QuestService {

    // ==================== 任务查询 ====================

    /**
     * 获取所有可接取的任务ID列表
     * 
     * @param playerId 玩家UUID
     * @return 可接取的任务ID列表
     */
    List<String> getAvailableQuests(UUID playerId);

    /**
     * 获取玩家当前进行中的任务ID列表
     * 
     * @param playerId 玩家UUID
     * @return 进行中的任务ID列表
     */
    List<String> getActiveQuests(UUID playerId);

    /**
     * 获取玩家已完成的任务ID列表
     * 
     * @param playerId 玩家UUID
     * @return 已完成的任务ID列表
     */
    List<String> getCompletedQuests(UUID playerId);

    /**
     * 获取任务详情
     * 
     * @param questId 任务ID
     * @return 任务详情Map，包含name、description、type、objectives、rewards等
     */
    Optional<Map<String, Object>> getQuestInfo(String questId);

    /**
     * 检查任务是否存在
     * 
     * @param questId 任务ID
     * @return 是否存在
     */
    boolean questExists(String questId);

    /**
     * 获取任务名称
     * 
     * @param questId 任务ID
     * @return 任务名称
     */
    String getQuestName(String questId);

    /**
     * 获取任务类型
     * 
     * @param questId 任务ID
     * @return 任务类型 (MAIN/DAILY/SIDE/ACHIEVEMENT)
     */
    String getQuestType(String questId);

    // ==================== 任务操作 ====================

    /**
     * 接取任务
     * 
     * @param playerId 玩家UUID
     * @param questId 任务ID
     * @return 是否成功接取
     */
    boolean acceptQuest(UUID playerId, String questId);

    /**
     * 完成任务（领取奖励）
     * 
     * @param playerId 玩家UUID
     * @param questId 任务ID
     * @return 是否成功完成
     */
    boolean completeQuest(UUID playerId, String questId);

    /**
     * 放弃任务
     * 
     * @param playerId 玩家UUID
     * @param questId 任务ID
     * @return 是否成功放弃
     */
    boolean abandonQuest(UUID playerId, String questId);

    /**
     * 检查任务是否可以完成
     * 
     * @param playerId 玩家UUID
     * @param questId 任务ID
     * @return 是否可以完成
     */
    boolean canComplete(UUID playerId, String questId);

    /**
     * 检查任务是否可以接取
     * 
     * @param playerId 玩家UUID
     * @param questId 任务ID
     * @return 是否可以接取
     */
    boolean canAccept(UUID playerId, String questId);

    // ==================== 进度查询 ====================

    /**
     * 获取任务进度
     * 
     * @param playerId 玩家UUID
     * @param questId 任务ID
     * @return 进度Map，key为目标索引，value为当前进度
     */
    Map<Integer, Integer> getQuestProgress(UUID playerId, String questId);

    /**
     * 获取任务完成百分比
     * 
     * @param playerId 玩家UUID
     * @param questId 任务ID
     * @return 完成百分比 (0-100)
     */
    int getQuestProgressPercent(UUID playerId, String questId);

    /**
     * 获取目标任务进度
     * 
     * @param playerId 玩家UUID
     * @param questId 任务ID
     * @param objectiveIndex 目标索引
     * @return 当前进度
     */
    int getObjectiveProgress(UUID playerId, String questId, int objectiveIndex);

    /**
     * 获取目标任务所需数量
     * 
     * @param questId 任务ID
     * @param objectiveIndex 目标索引
     * @return 所需数量
     */
    int getObjectiveRequired(String questId, int objectiveIndex);

    // ==================== 每日任务 ====================

    /**
     * 获取今日可接取的每日任务
     * 
     * @param playerId 玩家UUID
     * @return 每日任务ID列表
     */
    List<String> getDailyQuests(UUID playerId);

    /**
     * 获取今日已完成的每日任务数量
     * 
     * @param playerId 玩家UUID
     * @return 已完成数量
     */
    int getDailyCompletedCount(UUID playerId);

    /**
     * 获取每日任务上限
     * 
     * @return 每日任务上限
     */
    int getDailyQuestLimit();

    /**
     * 检查每日任务是否已重置
     * 
     * @param playerId 玩家UUID
     * @return 是否已重置
     */
    boolean isDailyReset(UUID playerId);

    /**
     * 重置玩家每日任务
     * 
     * @param playerId 玩家UUID
     */
    void resetDailyQuests(UUID playerId);

    // ==================== 任务线 ====================

    /**
     * 获取任务线列表
     * 
     * @return 任务线ID列表
     */
    List<String> getQuestLines();

    /**
     * 获取任务线中的任务列表
     * 
     * @param questLineId 任务线ID
     * @return 任务ID列表（按顺序）
     */
    List<String> getQuestLineQuests(String questLineId);

    /**
     * 获取玩家在任务线中的进度
     * 
     * @param playerId 玩家UUID
     * @param questLineId 任务线ID
     * @return 当前任务索引（从0开始），-1表示未开始
     */
    int getQuestLineProgress(UUID playerId, String questLineId);

    /**
     * 获取任务线的下一个任务
     * 
     * @param playerId 玩家UUID
     * @param questLineId 任务线ID
     * @return 下一个任务ID，Optional.empty()表示已完成
     */
    Optional<String> getNextQuestInLine(UUID playerId, String questLineId);

    // ==================== 统计 ====================

    /**
     * 获取玩家完成的任务总数
     * 
     * @param playerId 玩家UUID
     * @return 完成总数
     */
    int getTotalCompletedCount(UUID playerId);

    /**
     * 获取玩家的成就点数
     * 
     * @param playerId 玩家UUID
     * @return 成就点数
     */
    int getAchievementPoints(UUID playerId);

    // ==================== 异步API ====================

    /**
     * 异步获取任务进度
     */
    CompletableFuture<Map<Integer, Integer>> getQuestProgressAsync(UUID playerId, String questId);

    /**
     * 异步接取任务
     */
    CompletableFuture<Boolean> acceptQuestAsync(UUID playerId, String questId);

    /**
     * 异步完成任务
     */
    CompletableFuture<Boolean> completeQuestAsync(UUID playerId, String questId);

    /**
     * 异步保存玩家数据
     */
    CompletableFuture<Void> savePlayerDataAsync(UUID playerId);

    // ==================== 服务状态 ====================

    /**
     * 检查服务是否可用
     * 
     * @return 服务是否可用
     */
    boolean isAvailable();
}