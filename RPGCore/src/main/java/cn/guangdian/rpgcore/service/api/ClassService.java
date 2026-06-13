package cn.guangdian.rpgcore.service.api;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * 职业服务接口
 *
 * <p>提供玩家职业信息查询功能。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 获取服务
 * Optional<ClassService> service = serviceRegistry.getOptionalService(ClassService.class);
 * if (service.isPresent()) {
 *     ClassService classService = service.get();
 *
 *     // 获取职业名称
 *     String className = classService.getPlayerClassName(player.getUniqueId());
 *
 *     // 获取职业阶位
 *     int tier = classService.getPlayerTier(player.getUniqueId());
 *
 *     // 获取职业属性加成
 *     Map<String, Double> stats = classService.getPlayerClassStats(player.getUniqueId());
 * }
 * }</pre>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public interface ClassService {

    /**
     * 获取玩家职业名称
     *
     * @param playerId 玩家UUID
     * @return 职业名称，如果未选择职业返回 null
     */
    String getPlayerClassName(UUID playerId);

    /**
     * 获取玩家职业ID
     *
     * @param playerId 玩家UUID
     * @return 职业ID，如果未选择职业返回 null
     */
    String getPlayerClassId(UUID playerId);

    /**
     * 获取玩家阶位
     *
     * @param playerId 玩家UUID
     * @return 阶位（1-9），如果未选择职业返回 1
     */
    int getPlayerTier(UUID playerId);

    /**
     * 获取玩家转职阶数
     *
     * @param playerId 玩家UUID
     * @return 转职阶数（0=未转职, 1=一转, 2=二转, 3=三转, 4=神级）
     */
    int getPlayerAdvancementLevel(UUID playerId);

    /**
     * 获取玩家职业属性加成
     *
     * <p>返回的属性映射包含：</p>
     * <ul>
     *   <li>health - 生命值加成</li>
     *   <li>attack - 攻击力加成</li>
     *   <li>defense - 防御力加成</li>
     *   <li>critChance - 暴击率加成</li>
     *   <li>critDamage - 暴击伤害加成</li>
     *   <li>dodge - 闪避率加成</li>
     *   <li>mana - 魔力值加成</li>
     *   <li>tierMultiplier - 阶位倍率</li>
     *   <li>advancementMultiplier - 转职倍率</li>
     * </ul>
     *
     * @param playerId 玩家UUID
     * @return 属性加成映射，如果未选择职业返回空 Map
     */
    Map<String, Double> getPlayerClassStats(UUID playerId);

    /**
     * 获取玩家指定属性值
     *
     * @param playerId 玩家UUID
     * @param statName 属性名称
     * @return 属性值，如果不存在返回 0.0
     */
    double getPlayerClassStat(UUID playerId, String statName);

    /**
     * 获取玩家经验值
     *
     * @param playerId 玩家UUID
     * @return 当前经验值
     */
    long getPlayerExp(UUID playerId);

    /**
     * 检查服务是否可用
     *
     * @return 如果可用返回 true
     */
    boolean isAvailable();
}
