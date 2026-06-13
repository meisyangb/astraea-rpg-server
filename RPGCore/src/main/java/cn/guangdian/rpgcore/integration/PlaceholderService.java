package cn.guangdian.rpgcore.integration;

import cn.guangdian.rpgcore.RPGCore;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.logging.Logger;

/**
 * 统一占位符服务 - RPGCore 核心服务
 *
 * <p>提供统一的 PlaceholderAPI 占位符注册和管理功能。</p>
 * <p>所有插件应通过此服务注册占位符，而非直接创建 PlaceholderExpansion。</p>
 *
 * <h2>使用示例:</h2>
 * <pre>{@code
 * PlaceholderService placeholder = PlaceholderService.getInstance();
 *
 * // 注册简单占位符
 * placeholder.register("points_balance", (player, params) -> {
 *     return String.valueOf(pointsService.getBalance(player.getUniqueId()));
 * });
 *
 * // 注册带参数的占位符
 * placeholder.register("guild_rank", (player, params) -> {
 *     if (params.equals("name")) {
 *         return guildService.getPlayerRankName(player.getUniqueId());
 *     }
 *     return guildService.getPlayerRankLevel(player.getUniqueId());
 * });
 *
 * // 在游戏中使用: %rpg_points_balance%, %rpg_guild_rank_name%
 * }</pre>
 *
 * @author Astraea RPG Team
 * @since 1.1.0
 */
public final class PlaceholderService {

    private static PlaceholderService instance;

    private final String identifier;
    private final Map<String, BiFunction<Player, String, String>> placeholders;
    private PlaceholderExpansion expansion;
    private final Logger logger;
    private boolean registered = false;

    private PlaceholderService() {
        this.identifier = "rpg"; // 统一前缀: %rpg_xxx%
        this.placeholders = new ConcurrentHashMap<>();
        RPGCore rpgCore = RPGCore.getInstance();
        this.logger = rpgCore != null ? rpgCore.getLogger() : Logger.getLogger("PlaceholderService");
    }

    public static synchronized PlaceholderService getInstance() {
        if (instance == null) {
            instance = new PlaceholderService();
        }
        return instance;
    }

    /**
     * 注册占位符
     *
     * @param identifier 占位符标识 (如 "points_balance")
     * @param handler 处理函数 (player, params) -> value
     */
    public void register(@NotNull String identifier, @NotNull BiFunction<Player, String, String> handler) {
        placeholders.put(identifier.toLowerCase(), handler);

        if (!registered) {
            autoRegister();
        }

        logger.fine("[PlaceholderService] 注册占位符: %" + this.identifier + "_" + identifier + "%");
    }

    /**
     * 批量注册占位符
     *
     * @param placeholderMap 占位符映射
     */
    public void registerAll(@NotNull Map<String, BiFunction<Player, String, String>> placeholderMap) {
        placeholderMap.forEach(this::register);
    }

    /**
     * 注销指定占位符
     *
     * @param identifier 占位符标识
     */
    public void unregister(@NotNull String identifier) {
        placeholders.remove(identifier.toLowerCase());
    }

    /**
     * 注销所有占位符
     */
    public void unregisterAll() {
        placeholders.clear();
        if (expansion != null) {
            // PlaceholderAPI 会自动管理 expansion 的生命周期
            // 只需将引用置空即可
            expansion = null;
            registered = false;
            logger.info("[PlaceholderService] 已注销所有占位符");
        }
    }

    /**
     * 检查 PlaceholderAPI 是否可用
     */
    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    /**
     * 获取已注册的占位符数量
     */
    public int getPlaceholderCount() {
        return placeholders.size();
    }

    /**
     * 获取所有已注册的占位符标识
     */
    public @NotNull List<String> getRegisteredPlaceholders() {
        return new ArrayList<>(placeholders.keySet());
    }

    // ==================== 内部方法 ====================

    private void autoRegister() {
        if (!isAvailable()) {
            logger.warning("[PlaceholderService] PlaceholderAPI 未安装，占位符功能不可用");
            return;
        }

        if (registered) {
            return;
        }

        try {
            expansion = new RPGPlaceholderExpansion();
            if (expansion.register()) {
                registered = true;
                logger.info("[PlaceholderService] 占位符扩展已注册 (前缀: %" + identifier + "_xxx%)");
            } else {
                logger.severe("[PlaceholderService] 占位符扩展注册失败");
            }
        } catch (Exception e) {
            logger.severe("[PlaceholderService] 占位符扩展注册异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String handlePlaceholderRequest(@Nullable Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        // 解析参数 (如 "guild_rank_name" -> identifier="guild_rank", params="name")
        String[] parts = identifier.split("_", 2);
        String baseIdentifier = parts[0];
        String params = parts.length > 1 ? parts[1] : "";

        BiFunction<Player, String, String> handler = placeholders.get(baseIdentifier.toLowerCase());
        if (handler != null) {
            try {
                String result = handler.apply(player, params);
                return result != null ? result : "";
            } catch (Exception e) {
                logger.warning("[PlaceholderService] 占位符处理失败: %" + this.identifier + "_" + identifier + "% - " + e.getMessage());
                return "ERROR";
            }
        }

        return null; // 返回 null 表示不处理此占位符
    }

    // ==================== PlaceholderExpansion 实现 ====================

    private class RPGPlaceholderExpansion extends PlaceholderExpansion {

        @Override
        public @NotNull String getIdentifier() {
            return identifier;
        }

        @Override
        public @NotNull String getAuthor() {
            return "Astraea RPG Team";
        }

        @Override
        public @NotNull String getVersion() {
            return RPGCore.getInstance() != null ?
                RPGCore.getInstance().getDescription().getVersion() : "1.0.0";
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
            return handlePlaceholderRequest(player, identifier);
        }

        @Override
        public @NotNull List<String> getPlaceholders() {
            List<String> list = new ArrayList<>();
            for (String key : placeholders.keySet()) {
                list.add("%" + identifier + "_" + key + "%");
            }
            return list;
        }
    }
}
