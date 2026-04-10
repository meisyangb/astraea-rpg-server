package cn.guangdian.rpgcore.service.api;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 技能服务接口
 * 
 * <p>提供玩家技能的完整管理功能，包括学习、升级、释放、冷却等。</p>
 * 
 * <h3>功能模块：</h3>
 * <ul>
 *   <li>技能学习与遗忘</li>
 *   <li>技能升级与等级</li>
 *   <li>技能释放与冷却</li>
 *   <li>技能点管理</li>
 *   <li>被动技能处理</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 获取服务
 * Optional<SkillService> service = serviceRegistry.getOptionalService(SkillService.class);
 * if (service.isPresent()) {
 *     SkillService skills = service.get();
 *     
 *     // 学习技能
 *     boolean learned = skills.learnSkill(player.getUniqueId(), "fireball");
 *     
 *     // 升级技能
 *     boolean upgraded = skills.upgradeSkill(player.getUniqueId(), "fireball");
 *     
 *     // 释放技能
 *     boolean cast = skills.triggerActiveSkill(player, "fireball");
 *     
 *     // 检查冷却
 *     long remaining = skills.getCooldownRemaining(player.getUniqueId(), "fireball");
 * }
 * }</pre>
 * 
 * <h3>相关事件：</h3>
 * <ul>
 *   <li>{@link cn.guangdian.rpgcore.event.events.skill.RpgSkillLearnEvent} - 技能学习</li>
 *   <li>{@link cn.guangdian.rpgcore.event.events.skill.RpgSkillUpgradeEvent} - 技能升级</li>
 *   <li>{@link cn.guangdian.rpgcore.event.events.skill.RpgSkillCastEvent} - 技能释放</li>
 *   <li>{@link cn.guangdian.rpgcore.event.events.skill.RpgSkillCooldownEvent} - 技能冷却</li>
 *   <li>{@link cn.guangdian.rpgcore.event.events.skill.RpgSkillDamageEvent} - 技能伤害</li>
 *   <li>{@link cn.guangdian.rpgcore.event.events.skill.RpgSkillPointEvent} - 技能点变化</li>
 * </ul>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface SkillService {

    // ==================== 技能学习与遗忘 ====================

    /**
     * 学习技能
     * 
     * @param playerId 玩家UUID
     * @param skillId 技能ID
     * @return 如果学习成功返回 true
     */
    boolean learnSkill(UUID playerId, String skillId);

    /**
     * 学习技能（带消耗检查）
     * 
     * @param playerId 玩家UUID
     * @param skillId 技能ID
     * @param cost 技能点消耗
     * @return 如果学习成功返回 true
     */
    boolean learnSkill(UUID playerId, String skillId, int cost);

    /**
     * 遗忘技能
     * 
     * @param playerId 玩家UUID
     * @param skillId 技能ID
     * @return 如果遗忘成功返回 true
     */
    boolean forgetSkill(UUID playerId, String skillId);

    /**
     * 遗忘技能（返还技能点）
     * 
     * @param playerId 玩家UUID
     * @param skillId 技能ID
     * @param refund 是否返还技能点
     * @return 如果遗忘成功返回 true
     */
    boolean forgetSkill(UUID playerId, String skillId, boolean refund);

    /**
     * 检查玩家是否拥有指定技能
     * 
     * @param playerId 玩家UUID
     * @param skillId 技能ID
     * @return 如果拥有返回 true
     */
    boolean hasSkill(UUID playerId, String skillId);

    /**
     * 获取玩家已学习的技能列表
     * 
     * @param playerId 玩家UUID
     * @return 技能ID列表
     */
    List<String> getLearnedSkills(UUID playerId);

    /**
     * 获取玩家已学习的技能数量
     * 
     * @param playerId 玩家UUID
     * @return 技能数量
     */
    int getLearnedSkillCount(UUID playerId);

    // ==================== 技能等级 ====================

    /**
     * 获取技能等级
     * 
     * @param playerId 玩家UUID
     * @param skillId 技能ID
     * @return 技能等级，如果未学习返回 0
     */
    int getSkillLevel(UUID playerId, String skillId);

    /**
     * 设置技能等级
     * 
     * @param playerId 玩家UUID
     * @param skillId 技能ID
     * @param level 等级
     * @return 如果设置成功返回 true
     */
    boolean setSkillLevel(UUID playerId, String skillId, int level);

    /**
     * 升级技能
     * 
     * @param playerId 玩家UUID
     * @param skillId 技能ID
     * @return 如果升级成功返回 true
     */
    boolean upgradeSkill(UUID playerId, String skillId);

    /**
     * 获取技能最大等级
     * 
     * @param skillId 技能ID
     * @return 最大等级
     */
    int getSkillMaxLevel(String skillId);

    /**
     * 检查技能是否已满级
     * 
     * @param playerId 玩家UUID
     * @param skillId 技能ID
     * @return 如果已满级返回 true
     */
    boolean isSkillMaxLevel(UUID playerId, String skillId);

    /**
     * 获取升级所需技能点
     * 
     * @param skillId 技能ID
     * @param currentLevel 当前等级
     * @return 升级所需技能点
     */
    int getUpgradeCost(String skillId, int currentLevel);

    // ==================== 技能释放与冷却 ====================

    /**
     * 触发主动技能
     * 
     * @param player 玩家
     * @param skillId 技能ID
     * @return 如果触发成功返回 true
     */
    boolean triggerActiveSkill(Player player, String skillId);

    /**
     * 触发技能（带参数）
     * 
     * @param player 玩家
     * @param skillId 技能ID
     * @param args 额外参数
     * @return 如果触发成功返回 true
     */
    boolean triggerSkill(Player player, String skillId, Map<String, Object> args);

    /**
     * 检查技能是否可用（冷却是否结束）
     * 
     * @param playerId 玩家UUID
     * @param skillId 技能ID
     * @return 如果可用返回 true
     */
    boolean isSkillAvailable(UUID playerId, String skillId);

    /**
     * 获取技能冷却剩余时间（毫秒）
     * 
     * @param playerId 玩家UUID
     * @param skillId 技能ID
     * @return 剩余冷却时间（毫秒），如果没有冷却返回 0
     */
    long getCooldownRemaining(UUID playerId, String skillId);

    /**
     * 获取技能冷却剩余时间（秒）
     * 
     * @param playerId 玩家UUID
     * @param skillId 技能ID
     * @return 剩余冷却时间（秒）
     */
    default double getCooldownRemainingSeconds(UUID playerId, String skillId) {
        return getCooldownRemaining(playerId, skillId) / 1000.0;
    }

    /**
     * 设置技能冷却
     * 
     * @param playerId 玩家UUID
     * @param skillId 技能ID
     * @param cooldownMs 冷却时间（毫秒）
     */
    void setCooldown(UUID playerId, String skillId, long cooldownMs);

    /**
     * 重置技能冷却
     * 
     * @param playerId 玩家UUID
     * @param skillId 技能ID
     */
    void resetCooldown(UUID playerId, String skillId);

    /**
     * 重置所有技能冷却
     * 
     * @param playerId 玩家UUID
     */
    void resetAllCooldowns(UUID playerId);

    /**
     * 获取技能冷却时间配置
     * 
     * @param skillId 技能ID
     * @param level 技能等级
     * @return 冷却时间（毫秒）
     */
    long getSkillCooldown(String skillId, int level);

    // ==================== 技能点管理 ====================

    /**
     * 获取玩家技能点
     * 
     * @param playerId 玩家UUID
     * @return 技能点数量
     */
    int getSkillPoints(UUID playerId);

    /**
     * 设置玩家技能点
     * 
     * @param playerId 玩家UUID
     * @param points 技能点数量
     */
    void setSkillPoints(UUID playerId, int points);

    /**
     * 添加技能点
     * 
     * @param playerId 玩家UUID
     * @param amount 数量
     * @param reason 原因
     */
    void addSkillPoints(UUID playerId, int amount, String reason);

    /**
     * 消耗技能点
     * 
     * @param playerId 玩家UUID
     * @param amount 数量
     * @param reason 原因
     * @return 如果消耗成功返回 true
     */
    boolean consumeSkillPoints(UUID playerId, int amount, String reason);

    /**
     * 检查是否有足够技能点
     * 
     * @param playerId 玩家UUID
     * @param amount 需要的数量
     * @return 如果足够返回 true
     */
    boolean hasEnoughSkillPoints(UUID playerId, int amount);

    // ==================== 被动技能 ====================

    /**
     * 触发被动技能
     * 
     * @param player 玩家
     * @param triggerType 触发类型
     * @param context 上下文数据
     */
    void triggerPassiveSkills(Player player, String triggerType, Map<String, Object> context);

    /**
     * 获取玩家指定类型的被动技能
     * 
     * @param playerId 玩家UUID
     * @param triggerType 触发类型
     * @return 技能ID列表
     */
    List<String> getPassiveSkills(UUID playerId, String triggerType);

    // ==================== 技能信息 ====================

    /**
     * 获取技能名称
     * 
     * @param skillId 技能ID
     * @return 技能名称
     */
    String getSkillName(String skillId);

    /**
     * 获取技能描述
     * 
     * @param skillId 技能ID
     * @param level 技能等级
     * @return 技能描述
     */
    String getSkillDescription(String skillId, int level);

    /**
     * 获取技能类型
     * 
     * @param skillId 技能ID
     * @return 技能类型（active/passive/passive_auto）
     */
    String getSkillType(String skillId);

    /**
     * 检查技能是否存在
     * 
     * @param skillId 技能ID
     * @return 如果存在返回 true
     */
    boolean skillExists(String skillId);

    /**
     * 获取所有可用技能ID
     * 
     * @return 技能ID列表
     */
    List<String> getAllSkills();

    // ==================== 异步操作 ====================

    /**
     * 异步获取技能等级
     * 
     * @param playerId 玩家UUID
     * @param skillId 技能ID
     * @return CompletableFuture
     */
    CompletableFuture<Integer> getSkillLevelAsync(UUID playerId, String skillId);

    /**
     * 异步获取玩家技能点
     * 
     * @param playerId 玩家UUID
     * @return CompletableFuture
     */
    CompletableFuture<Integer> getSkillPointsAsync(UUID playerId);

    /**
     * 异步保存玩家技能数据
     * 
     * @param playerId 玩家UUID
     * @return CompletableFuture
     */
    CompletableFuture<Void> savePlayerDataAsync(UUID playerId);

    // ==================== 服务状态 ====================

    /**
     * 检查技能服务是否可用
     * 
     * @return 如果可用返回 true
     */
    boolean isAvailable();

    /**
     * 重置玩家所有技能数据
     * 
     * @param playerId 玩家UUID
     */
    void resetPlayerSkills(UUID playerId);
}