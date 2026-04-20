package cn.guangdian.world.papi;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.integration.PlaceholderService;
import cn.guangdian.world.GuangDianWorld;
import cn.guangdian.world.model.GDWorld;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * 世界管理占位符扩展
 *
 * <p>使用 RPGCore PlaceholderService 统一注册占位符。</p>
 * <p>所有占位符前缀为: %rpg_world_xxx%</p>
 *
 * <h3>支持的占位符:</h3>
 * <ul>
 *   <li>%rpg_world_current% - 当前世界名称</li>
 *   <li>%rpg_world_alias% - 当前世界别名</li>
 *   <li>%rpg_world_environment% - 当前世界环境</li>
 *   <li>%rpg_world_difficulty% - 当前世界难度</li>
 *   <li>%rpg_world_gamemode% - 当前世界游戏模式</li>
 *   <li>%rpg_world_pvp% - 是否开启PVP</li>
 *   <li>%rpg_world_flight% - 是否允许飞行</li>
 *   <li>%rpg_world_count% - 世界总数</li>
 *   <li>%rpg_world_loaded% - 当前世界是否已加载</li>
 *   <li>%rpg_world_name_<世界名>% - 获取指定世界的别名</li>
 *   <li>%rpg_world_exists_<世界名>% - 指定世界是否存在</li>
 *   <li>%rpg_world_loaded_<世界名>% - 指定世界是否已加载</li>
 *   <li>%rpg_world_players_<世界名>% - 指定世界的玩家数量</li>
 * </ul>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class WorldPlaceholders {

    private final GuangDianWorld plugin;
    private final PlaceholderService placeholderService;
    private final Map<String, BiFunction<Player, String, String>> registeredPlaceholders;

    public WorldPlaceholders(GuangDianWorld plugin) {
        this.plugin = plugin;
        this.registeredPlaceholders = new HashMap<>();
        this.placeholderService = PlaceholderService.getInstance();
    }

    /**
     * 注册所有世界管理相关占位符
     */
    public void register() {
        if (!placeholderService.isAvailable()) {
            plugin.getLogger().warning("PlaceholderAPI 未安装，占位符功能不可用");
            return;
        }

        // 基础占位符
        registerPlaceholder("world_current", (player, params) -> player.getWorld().getName());
        registerPlaceholder("world_alias", this::getWorldAlias);
        registerPlaceholder("world_environment", this::getWorldEnvironment);
        registerPlaceholder("world_difficulty", this::getWorldDifficulty);
        registerPlaceholder("world_gamemode", this::getWorldGamemode);
        registerPlaceholder("world_pvp", this::getWorldPvp);
        registerPlaceholder("world_flight", this::getWorldFlight);
        registerPlaceholder("world_count", (player, params) -> String.valueOf(plugin.getWorldManager().getWorldCount()));
        registerPlaceholder("world_loaded", this::getWorldLoaded);

        // 带参数的世界特定占位符
        registerPlaceholder("world_name", this::getWorldNameByParam);
        registerPlaceholder("world_exists", this::getWorldExists);
        registerPlaceholder("world_loaded_check", this::getWorldLoadedByParam);
        registerPlaceholder("world_players", this::getWorldPlayers);

        plugin.getLogger().info("已注册 " + registeredPlaceholders.size() + " 个世界管理占位符到 RPGCore");
    }

    /**
     * 注销所有已注册的占位符
     */
    public void unregister() {
        for (String identifier : registeredPlaceholders.keySet()) {
            placeholderService.unregister(identifier);
        }
        registeredPlaceholders.clear();
        plugin.getLogger().info("已注销所有世界管理占位符");
    }

    /**
     * 注册单个占位符
     */
    private void registerPlaceholder(String identifier, BiFunction<Player, String, String> handler) {
        placeholderService.register(identifier, handler);
        registeredPlaceholders.put(identifier, handler);
    }

    // ==================== 占位符处理器 ====================

    private String getWorldAlias(Player player, String params) {
        GDWorld world = getGDWorld(player);
        return world != null ? world.getDisplayName() : player.getWorld().getName();
    }

    private String getWorldEnvironment(Player player, String params) {
        GDWorld world = getGDWorld(player);
        return world != null ? world.getEnvironment().name() : "NORMAL";
    }

    private String getWorldDifficulty(Player player, String params) {
        GDWorld world = getGDWorld(player);
        return world != null ? world.getDifficulty() : "NORMAL";
    }

    private String getWorldGamemode(Player player, String params) {
        GDWorld world = getGDWorld(player);
        return world != null ? world.getGamemode() : "SURVIVAL";
    }

    private String getWorldPvp(Player player, String params) {
        GDWorld world = getGDWorld(player);
        return world != null ? String.valueOf(world.isPvp()) : "true";
    }

    private String getWorldFlight(Player player, String params) {
        GDWorld world = getGDWorld(player);
        return world != null ? String.valueOf(world.isAllowFlight()) : "false";
    }

    private String getWorldLoaded(Player player, String params) {
        GDWorld world = getGDWorld(player);
        return world != null ? String.valueOf(world.isLoaded()) : "false";
    }

    private String getWorldNameByParam(Player player, String params) {
        if (params.isEmpty()) return "";
        GDWorld world = plugin.getWorldManager().getWorld(params);
        return world != null ? world.getDisplayName() : params;
    }

    private String getWorldExists(Player player, String params) {
        if (params.isEmpty()) return "false";
        return String.valueOf(plugin.getWorldManager().getWorld(params) != null);
    }

    private String getWorldLoadedByParam(Player player, String params) {
        if (params.isEmpty()) return "false";
        GDWorld world = plugin.getWorldManager().getWorld(params);
        return String.valueOf(world != null && world.isLoaded());
    }

    private String getWorldPlayers(Player player, String params) {
        if (params.isEmpty()) return "0";
        var world = Bukkit.getWorld(params);
        return world != null ? String.valueOf(world.getPlayers().size()) : "0";
    }

    // ==================== 辅助方法 ====================

    private GDWorld getGDWorld(Player player) {
        String worldName = player.getWorld().getName();
        return plugin.getWorldManager().getWorld(worldName);
    }
}
