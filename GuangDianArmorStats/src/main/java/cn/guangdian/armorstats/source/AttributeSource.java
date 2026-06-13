package cn.guangdian.armorstats.source;

import cn.guangdian.armorstats.data.PlayerStats;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * 属性来源接口
 * 定义统一的属性获取接口，支持多种属性来源
 */
public interface AttributeSource {

    /**
     * 获取属性来源名称
     * @return 来源名称（如：装备、职业、Buff等）
     */
    String getName();

    /**
     * 获取属性来源优先级
     * 数值越大优先级越高
     * 
     * 推荐优先级：
     * - EQUIPMENT: 100 (装备属性)
     * - CLASS: 80 (职业属性)
     * - BUFF: 60 (Buff属性)
     * - PASSIVE: 40 (被动技能属性)
     * - BASE: 20 (基矗属性)
     * 
     * @return 优先级
     */
    int getPriority();

    /**
     * 获取玩家的属性
     * @param player 玩家
     * @return 属性映射
     */
    Map<String, Double> getAttributes(Player player);

    /**
     * 获取玩家的完整属性统计
     * @param player 玩家
     * @return 属性统计对象
     */
    PlayerStats getPlayerStats(Player player);

    /**
     * 是否启用
     * @return 是否启用
     */
    boolean isEnabled();

    /**
     * 设置启用状态
     * @param enabled 是否启用
     */
    void setEnabled(boolean enabled);
}
