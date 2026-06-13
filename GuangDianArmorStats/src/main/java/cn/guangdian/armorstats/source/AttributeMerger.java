package cn.guangdian.armorstats.source;

import cn.guangdian.armorstats.data.AttributeValue;
import cn.guangdian.armorstats.data.PlayerStats;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 属性合并器
 * 管理多个属性来源，计算玩家最终属性
 * 
 * 修复：使用 Logger 替代 System.err.println
 */
public class AttributeMerger {

    private final Map<String, AttributeSource> sources = new ConcurrentHashMap<>();
    private final List<AttributeSource> sortedSources = new ArrayList<>();
    private Logger logger;

    /**
     * 设置日志器
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * 注册属性来源
     * @param source 属性来源
     */
    public void registerSource(AttributeSource source) {
        sources.put(source.getName(), source);
        rebuildSortedList();
    }

    /**
     * 注销属性来源
     * @param name 来源名称
     */
    public void unregisterSource(String name) {
        sources.remove(name);
        rebuildSortedList();
    }

    /**
     * 获取属性来源
     * @param name 来源名称
     * @return 属性来源
     */
    public AttributeSource getSource(String name) {
        return sources.get(name);
    }

    /**
     * 获取所有属性来源
     * @return 属性来源集合
     */
    public Collection<AttributeSource> getAllSources() {
        return Collections.unmodifiableCollection(sources.values());
    }

    /**
     * 重建排序列表
     */
    private void rebuildSortedList() {
        sortedSources.clear();
        sortedSources.addAll(sources.values());
        sortedSources.sort(Comparator.comparingInt(AttributeSource::getPriority).reversed());
    }

    /**
     * 计算玩家最终属性
     * 按优先级从高到低依次合并属性
     * 
     * @param player 玩家
     * @return 最终属性统计
     */
    public PlayerStats calculateFinalStats(Player player) {
        PlayerStats finalStats = new PlayerStats();

        // 按优先级排序（高优先级优先）
        for (AttributeSource source : sortedSources) {
            if (!source.isEnabled()) {
                continue;
            }

            try {
                PlayerStats sourceStats = source.getPlayerStats(player);
                finalStats.addPlayerStats(sourceStats);
            } catch (Exception e) {
                // 记录错误但不影响其他属性源
                if (logger != null) {
                    logger.warning("属性源 " + source.getName() + " 计算失败: " + e.getMessage());
                }
            }
        }

        return finalStats;
    }

    /**
     * 计算玩家最终属性（Map 形式）
     * 
     * @param player 玩家
     * @return 最终属性映射
     */
    public Map<String, Double> calculateFinalAttributes(Player player) {
        Map<String, Double> finalAttrs = new HashMap<>();

        // 按优先级排序（高优先级优先）
        for (AttributeSource source : sortedSources) {
            if (!source.isEnabled()) {
                continue;
            }

            try {
                Map<String, Double> sourceAttrs = source.getAttributes(player);
                for (Map.Entry<String, Double> entry : sourceAttrs.entrySet()) {
                    finalAttrs.merge(entry.getKey(), entry.getValue(), Double::sum);
                }
            } catch (Exception e) {
                // 记录错误但不影响其他属性源
                if (logger != null) {
                    logger.warning("属性源 " + source.getName() + " 计算失败: " + e.getMessage());
                }
            }
        }

        return finalAttrs;
    }

    /**
     * 清空所有属性来源
     */
    public void clear() {
        sources.clear();
        sortedSources.clear();
    }

    /**
     * 获取属性来源数量
     * @return 数量
     */
    public int getSourceCount() {
        return sources.size();
    }
}
