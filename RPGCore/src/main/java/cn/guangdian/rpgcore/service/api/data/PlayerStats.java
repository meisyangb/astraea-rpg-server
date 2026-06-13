package cn.guangdian.rpgcore.service.api.data;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * 玩家属性数据接口
 *
 * <p>定义玩家RPG属性的基本结构，由具体插件实现。</p>
 *
 * @author Astraea RPG Team
 * @since 1.2.0
 */
public interface PlayerStats {

    /**
     * 获取玩家UUID
     *
     * @return 玩家UUID
     */
    @NotNull UUID getPlayerId();

    /**
     * 获取基础攻击力
     *
     * @return 基础攻击力
     */
    double getBaseAttack();

    /**
     * 获取总攻击力（包含装备加成）
     *
     * @return 总攻击力
     */
    double getTotalAttack();

    /**
     * 获取基础防御力
     *
     * @return 基础防御力
     */
    double getBaseDefense();

    /**
     * 获取总防御力（包含装备加成）
     *
     * @return 总防御力
     */
    double getTotalDefense();

    /**
     * 获取基础生命值
     *
     * @return 基础生命值
     */
    double getBaseHealth();

    /**
     * 获取最大生命值
     *
     * @return 最大生命值
     */
    double getMaxHealth();

    /**
     * 获取暴击率（0.0 - 1.0）
     *
     * @return 暴击率
     */
    double getCritRate();

    /**
     * 获取暴击伤害倍率
     *
     * @return 暴击伤害倍率（例如 1.5 表示 150%）
     */
    double getCritDamage();

    /**
     * 获取闪避率（0.0 - 1.0）
     *
     * @return 闪避率
     */
    double getDodgeRate();

    /**
     * 获取命中率（0.0 - 1.0）
     *
     * @return 命中率
     */
    double getHitRate();

    /**
     * 获取移动速度加成
     *
     * @return 速度加成百分比
     */
    double getSpeedBonus();

    /**
     * 获取属性值
     *
     * @param attribute 属性名称
     * @return 属性值，如果不存在返回 0
     */
    double getAttribute(@NotNull String attribute);

    /**
     * 获取所有属性
     *
     * @return 属性名称 -> 属性值
     */
    @NotNull Map<String, Double> getAllAttributes();

    /**
     * 获取玩家等级
     *
     * @return 等级
     */
    int getLevel();

    /**
     * 获取玩家经验值
     *
     * @return 经验值
     */
    long getExperience();

    /**
     * 获取属性点
     *
     * @return 可用属性点
     */
    int getAttributePoints();

    /**
     * 重新计算属性
     *
     * <p>当装备变化或等级提升时调用。</p>
     */
    void recalculate();

    /**
     * 创建属性快照
     *
     * @return 当前属性的不可变副本
     */
    @NotNull PlayerStats snapshot();
}
