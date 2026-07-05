package cn.guangdian.dynamicview.manager;

import cn.guangdian.dynamicview.DynamicViewPlugin;
import cn.guangdian.dynamicview.data.PlayerViewData;
import cn.guangdian.dynamicview.data.PlayerViewData.ViewTier;
import cn.guangdian.dynamicview.util.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 视距管理器 - 按世界配置挡位模式
 * 每个世界可以独立配置各挡位的视距值
 * 
 * 支持渐进式视距调整，避免闪烁
 */
public class ViewDistanceManager {

    private final DynamicViewPlugin plugin;
    private final ConfigManager config;

    private final Map<UUID, PlayerViewData> viewDataMap = new ConcurrentHashMap<>();

    // Paper 1.19.4 最低视距为4（1.21.6是3）
    private static final int MIN_VIEW_DISTANCE = 4;

    // 统计
    private int totalAdjustments = 0;

    public ViewDistanceManager(DynamicViewPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    /**
     * 更新所有在线玩家的视距
     * 在主线程执行
     */
    public void updateAllPlayers() {
        if (!config.isEnabled()) {
            return;
        }

        // 必须在主线程执行 setViewDistance
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::updateAllPlayers);
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerViewDistance(player);
        }
    }

    /**
     * 更新单个玩家的视距 - 使用渐进式调整
     */
    public void updatePlayerViewDistance(Player player) {
        String worldName = player.getWorld().getName();

        // 固定视距世界：直接设置固定值
        if (config.isWorldFixed(worldName)) {
            int fixed = clampViewDistance(config.getWorldFixedView(worldName));
            int current = player.getViewDistance();
            if (current != fixed) {
                // 固定视距世界也使用渐进式调整
                gradualAdjust(player, null, fixed);
            }
            return;
        }

        PlayerViewData data = viewDataMap.computeIfAbsent(
            player.getUniqueId(), uuid -> new PlayerViewData(uuid)
        );

        // 初始化玩家数据
        if (!data.isInitialized()) {
            int initialViewDistance = player.getViewDistance();
            data.setCurrentViewDistance(initialViewDistance);
            data.setTargetViewDistance(initialViewDistance);
        }

        // 判断当前应该处于哪个挡位
        ViewTier targetTier = determineTier(data);

        // 从世界配置获取该挡位的视距值
        int targetViewDistance = clampViewDistance(config.getWorldTierViewDistance(worldName, targetTier));

        // 更新目标视距
        data.setTargetViewDistance(targetViewDistance);

        // 更新挡位（用于消息显示）
        ViewTier oldTier = data.getCurrentTier();
        data.setCurrentTier(targetTier);

        // 渐进式调整
        boolean changed = gradualAdjust(player, data, targetViewDistance);

        // 发送消息（只在挡位变化且实际视距变化时发送）
        if (changed && config.isMessagesEnabled() && oldTier != targetTier) {
            sendTierChangeMessage(player, targetTier);
        }
    }

    /**
     * 渐进式视距调整
     * 每次只调整 1 格，避免闪烁
     * 
     * @return 是否进行了调整
     */
    private boolean gradualAdjust(Player player, PlayerViewData data, int targetDistance) {
        int currentDistance = data != null ? data.getCurrentViewDistance() : player.getViewDistance();
        
        // 已经是目标值
        if (currentDistance == targetDistance) {
            return false;
        }

        // 如果禁用渐进式调整，直接跳变
        if (!config.isGradualAdjustEnabled()) {
            player.setViewDistance(targetDistance);
            if (data != null) {
                data.setCurrentViewDistance(targetDistance);
            }
            totalAdjustments++;
            return true;
        }

        long now = System.currentTimeMillis();
        
        // 检查调整间隔（避免过于频繁）
        if (data != null && data.getLastAdjustTime() > 0) {
            long timeSinceLastAdjust = now - data.getLastAdjustTime();
            long minInterval = config.getGradualAdjustInterval();
            if (timeSinceLastAdjust < minInterval) {
                return false; // 还没到调整时间
            }
        }

        // 计算下一步视距（每次只调整 1 格）
        int nextDistance;
        if (currentDistance < targetDistance) {
            // 增加视距
            nextDistance = currentDistance + 1;
        } else {
            // 减少视距
            nextDistance = currentDistance - 1;
        }

        // 确保在有效范围内
        nextDistance = clampViewDistance(nextDistance);

        // 应用视距变化
        player.setViewDistance(nextDistance);
        totalAdjustments++;

        // 更新数据
        if (data != null) {
            data.setCurrentViewDistance(nextDistance);
            data.setLastAdjustTime(now);
        }

        if (config.isDebugEnabled()) {
            plugin.getLogger().info(String.format(
                "[渐进调整] %s: %d -> %d (目标: %d)",
                player.getName(), currentDistance, nextDistance, targetDistance
            ));
        }

        return true;
    }

    /**
     * 确保视距不低于 Paper 最低限制
     */
    private int clampViewDistance(int viewDistance) {
        return Math.max(viewDistance, MIN_VIEW_DISTANCE);
    }

    /**
     * 判断玩家应该处于哪个挡位
     * 优先级: 挂机 > 战斗 > 跑图
     */
    private ViewTier determineTier(PlayerViewData data) {
        long now = System.currentTimeMillis();

        // 1. 检查挂机
        long inactiveTime = now - data.getLastActivityTime();
        long afkTimeout = config.getAFKTimeout() * 1000L;
        if (inactiveTime > afkTimeout) {
            return ViewTier.AFK;
        }

        // 2. 检查战斗（战斗状态有持续时间）
        long combatDuration = config.getCombatDuration() * 1000L;
        if (data.getLastCombatTime() > 0 && (now - data.getLastCombatTime()) < combatDuration) {
            return ViewTier.COMBAT;
        }

        // 3. 默认跑图
        return ViewTier.EXPLORING;
    }

    /**
     * 发送挡位切换消息
     */
    private void sendTierChangeMessage(Player player, ViewTier tier) {
        String message = switch (tier) {
            case AFK -> config.getAFKMessage();
            case COMBAT -> config.getCombatMessage();
            case EXPLORING -> config.getExploringMessage();
        };

        // 将 & 颜色代码转换为 MiniMessage 格式
        String parsed = message
            .replace("&a", "<green>").replace("&b", "<aqua>")
            .replace("&c", "<red>").replace("&d", "<light_purple>")
            .replace("&e", "<yellow>").replace("&f", "<white>")
            .replace("&0", "<black>").replace("&1", "<dark_blue>")
            .replace("&2", "<dark_green>").replace("&3", "<dark_aqua>")
            .replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
            .replace("&6", "<gold>").replace("&7", "<gray>")
            .replace("&8", "<dark_gray>").replace("&9", "<blue>");

        Component component = MiniMessage.miniMessage().deserialize(parsed);
        player.sendMessage(component);
    }

    /**
     * 玩家进入战斗
     */
    public void onPlayerCombat(Player player) {
        PlayerViewData data = viewDataMap.get(player.getUniqueId());
        if (data != null) {
            data.setLastCombatTime(System.currentTimeMillis());
        }
    }

    /**
     * 玩家移动时更新活动时间
     */
    public void onPlayerMove(Player player) {
        PlayerViewData data = viewDataMap.get(player.getUniqueId());
        if (data != null) {
            data.setLastActivityTime(System.currentTimeMillis());
        }
    }

    /**
     * 玩家加入
     */
    public void onPlayerJoin(Player player) {
        PlayerViewData data = new PlayerViewData(player.getUniqueId());
        // 初始化为当前视距
        int currentViewDistance = player.getViewDistance();
        data.setCurrentViewDistance(currentViewDistance);
        data.setTargetViewDistance(currentViewDistance);
        viewDataMap.put(player.getUniqueId(), data);
    }

    /**
     * 玩家退出
     */
    public void onPlayerQuit(Player player) {
        viewDataMap.remove(player.getUniqueId());
    }

    // Getters
    public PlayerViewData getPlayerData(UUID uuid) {
        return viewDataMap.get(uuid);
    }

    public int getTotalAdjustments() {
        return totalAdjustments;
    }
}
