package cn.guangdian.rpgcore.integration;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.service.api.PointsService;
import cn.guangdian.rpgcore.service.api.SkillService;
import cn.guangdian.rpgcore.service.api.StatsService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * RPGCore 服务访问工具类
 * 
 * <p>提供便捷的静态方法访问 RPGCore 服务，用于旧插件迁移。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 检查 RPGCore 是否可用
 * if (RPGCoreHelper.isAvailable()) {
 *     // 触发技能
 *     boolean success = RPGCoreHelper.triggerSkill(player, "fireball");
 *     
 *     // 操作点券
 *     long balance = RPGCoreHelper.getBalance(player.getUniqueId());
 * }
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public final class RPGCoreHelper {

    private RPGCoreHelper() {
        // 工具类，不允许实例化
    }

    /**
     * 检查 RPGCore 是否可用
     * 
     * @return 如果 RPGCore 已加载返回 true
     */
    public static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("RPGCore") && 
               RPGCore.getInstance() != null;
    }

    /**
     * 获取服务注册表
     */
    private static ServiceRegistry getServiceRegistry() {
        RPGCore core = RPGCore.getInstance();
        return core != null ? core.getServiceRegistry() : null;
    }

    // ==================== 技能服务 ====================

    /**
     * 检查技能服务是否可用
     */
    public static boolean isSkillServiceAvailable() {
        ServiceRegistry registry = getServiceRegistry();
        return registry != null && registry.hasService(SkillService.class);
    }

    /**
     * 触发主动技能
     * 
     * @param player 玩家
     * @param skillName 技能名称
     * @return 如果触发成功返回 true
     */
    public static boolean triggerSkill(Player player, String skillName) {
        if (!isAvailable() || player == null || skillName == null) {
            return false;
        }

        ServiceRegistry registry = getServiceRegistry();
        if (registry == null) {
            return false;
        }

        Optional<SkillService> service = registry.getOptionalService(SkillService.class);
        if (service.isEmpty()) {
            return false;
        }

        return service.get().triggerActiveSkill(player, skillName);
    }

    /**
     * 检查玩家是否拥有技能
     */
    public static boolean hasSkill(UUID playerId, String skillName) {
        if (!isAvailable() || playerId == null || skillName == null) {
            return false;
        }

        ServiceRegistry registry = getServiceRegistry();
        if (registry == null) {
            return false;
        }

        Optional<SkillService> service = registry.getOptionalService(SkillService.class);
        return service.map(skillService -> skillService.hasSkill(playerId, skillName)).orElse(false);
    }

    /**
     * 检查技能是否可用（不在冷却中）
     */
    public static boolean isSkillAvailable(UUID playerId, String skillName) {
        if (!isAvailable() || playerId == null || skillName == null) {
            return false;
        }

        ServiceRegistry registry = getServiceRegistry();
        if (registry == null) {
            return false;
        }

        Optional<SkillService> service = registry.getOptionalService(SkillService.class);
        return service.map(skillService -> skillService.isSkillAvailable(playerId, skillName)).orElse(false);
    }

    // ==================== 点券服务 ====================

    /**
     * 检查点券服务是否可用
     */
    public static boolean isPointsServiceAvailable() {
        ServiceRegistry registry = getServiceRegistry();
        return registry != null && registry.hasService(PointsService.class);
    }

    /**
     * 获取玩家点券余额
     */
    public static long getBalance(UUID playerId) {
        if (!isAvailable() || playerId == null) {
            return 0;
        }

        ServiceRegistry registry = getServiceRegistry();
        if (registry == null) {
            return 0;
        }

        Optional<PointsService> service = registry.getOptionalService(PointsService.class);
        return service.map(pointsService -> pointsService.getBalance(playerId)).orElse(0L);
    }

    /**
     * 增加玩家点券
     */
    public static boolean addBalance(UUID playerId, long amount, String reason) {
        if (!isAvailable() || playerId == null || amount <= 0) {
            return false;
        }

        ServiceRegistry registry = getServiceRegistry();
        if (registry == null) {
            return false;
        }

        Optional<PointsService> service = registry.getOptionalService(PointsService.class);
        if (service.isEmpty()) {
            return false;
        }

        service.get().addBalance(playerId, amount, reason);
        return true;
    }

    /**
     * 扣除玩家点券
     */
    public static boolean removeBalance(UUID playerId, long amount, String reason) {
        if (!isAvailable() || playerId == null || amount <= 0) {
            return false;
        }

        ServiceRegistry registry = getServiceRegistry();
        if (registry == null) {
            return false;
        }

        Optional<PointsService> service = registry.getOptionalService(PointsService.class);
        return service.map(pointsService -> pointsService.removeBalance(playerId, amount, reason)).orElse(false);
    }

    /**
     * 检查玩家是否有足够点券
     */
    public static boolean hasBalance(UUID playerId, long amount) {
        if (!isAvailable() || playerId == null || amount <= 0) {
            return false;
        }

        ServiceRegistry registry = getServiceRegistry();
        if (registry == null) {
            return false;
        }

        Optional<PointsService> service = registry.getOptionalService(PointsService.class);
        return service.map(pointsService -> pointsService.hasBalance(playerId, amount)).orElse(false);
    }

    // ==================== 属性服务 ====================

    /**
     * 检查属性服务是否可用
     */
    public static boolean isStatsServiceAvailable() {
        ServiceRegistry registry = getServiceRegistry();
        return registry != null && registry.hasService(StatsService.class);
    }

    /**
     * 获取玩家总攻击力
     */
    public static double getTotalAttack(Player player) {
        if (!isAvailable() || player == null) {
            return 0;
        }

        ServiceRegistry registry = getServiceRegistry();
        if (registry == null) {
            return 0;
        }

        Optional<StatsService> service = registry.getOptionalService(StatsService.class);
        return service.map(statsService -> statsService.getTotalAttack(player)).orElse(0.0);
    }

    /**
     * 获取玩家总防御力
     */
    public static double getTotalDefense(Player player) {
        if (!isAvailable() || player == null) {
            return 0;
        }

        ServiceRegistry registry = getServiceRegistry();
        if (registry == null) {
            return 0;
        }

        Optional<StatsService> service = registry.getOptionalService(StatsService.class);
        return service.map(statsService -> statsService.getTotalDefense(player)).orElse(0.0);
    }

    /**
     * 刷新玩家属性
     */
    public static void refreshPlayerStats(Player player) {
        if (!isAvailable() || player == null) {
            return;
        }

        ServiceRegistry registry = getServiceRegistry();
        if (registry == null) {
            return;
        }

        Optional<StatsService> service = registry.getOptionalService(StatsService.class);
        service.ifPresent(statsService -> statsService.refreshPlayerStats(player));
    }
}