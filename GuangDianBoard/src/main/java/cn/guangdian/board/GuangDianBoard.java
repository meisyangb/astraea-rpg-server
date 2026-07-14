package cn.guangdian.board;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.api.GameLogger;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import cn.guangdian.board.adapter.BoardServiceAdapter;
import cn.guangdian.board.lifecycle.BoardDataHandler;
import cn.guangdian.board.placeholder.BoardPlaceholder;
import me.clip.placeholderapi.PlaceholderAPI;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 光点侧边栏插件 - GuangDianBoard
 *
 * <p>RPGCore 服务集成:
 * <ul>
 *   <li>MiniMessageService: 使用 RPGCore 统一消息服务进行文本格式化</li>
 *   <li>GameLogger: 使用 RPGCore 统一日志服务</li>
 *   <li>SyncScheduler: 使用 RPGCore 同步任务调度器</li>
 *   <li>ExternalServiceIntegration: 使用 RPGCore 外部服务集成</li>
 * </ul>
 *
 * <p>优先级模式: 优先使用 RPGCore 服务，不可用则降级到本地实现
 *
 * <p>显示玩家侧边栏计分板，支持PlaceholderAPI和LuckPerms
 *
 * @author Gumin
 * @QQ 2271257344
 * @version 1.0.0
 */
public class GuangDianBoard extends AbstractRPGPlugin implements Listener {

    private static GuangDianBoard instance;
    private FileConfiguration config;
    private boolean debug = false;
    
    private final Map<UUID, Boolean> boardToggleState = new ConcurrentHashMap<>();
    private final Map<UUID, Scoreboard> playerBoards = new ConcurrentHashMap<>();
    private final Map<String, String> worldAliases = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> boardCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastRefreshTime = new ConcurrentHashMap<>();
    
    // 存储每个玩家上一次的侧边栏 entries，用于精确清除
    private final Map<UUID, Set<String>> lastBoardEntries = new ConcurrentHashMap<>();
    
    // ==================== 高性能优化: 脏标记机制 ====================
    /**
     * 脏玩家集合 - 属性变化时标记，刷新后清除
     * 避免全量定时刷新，只在需要时刷新
     */
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();
    
    /**
     * 行哈希缓存 - 用于快速比较内容变化
     * 替代完整的List比较
     */
    private final Map<UUID, int[]> lineHashCache = new ConcurrentHashMap<>();
    
    /**
     * 是否启用脏标记优化
     */
    private boolean dirtyFlagEnabled = true;
    
    private long eventCooldown = 1000;
    private boolean showStyleSeparator;
    private String styleSeparatorLine;
    private Set<String> styleSeparatorPositions = ConcurrentHashMap.newKeySet();

    private static final int MAX_SCOREBOARD_ENTRY_LENGTH = 40;
    private static final int UNIQUE_SUFFIX_LENGTH = 4;
    private static final int MAX_DISPLAY_LENGTH = MAX_SCOREBOARD_ENTRY_LENGTH - UNIQUE_SUFFIX_LENGTH;
    private static final char[] SUFFIX_CHAR_POOL = "abcdefghijklmnopqrstuvwxyz".toCharArray();

    private static final String[] COLOR_TAGS = {
        "<black>",      // 0
        "<dark_blue>",  // 1
        "<dark_green>", // 2
        "<dark_aqua>",  // 3
        "<dark_red>",   // 4
        "<dark_purple>",// 5
        "<gold>",       // 6
        "<gray>",       // 7
        "<dark_gray>",  // 8
        "<blue>",       // 9
        "<green>",      // a
        "<aqua>",       // b
        "<red>",        // c
        "<light_purple>",// d
        "<yellow>",     // e
        "<white>"       // f
    };

    private long refreshInterval;
    private long titleRefreshInterval;
    private int titleFrameIndex = 0;
    private boolean titleAnimationEnabled;
    private List<String> titleFrames;
    private String defaultTitle;
    
    private long refreshTaskId = -1;
    private long titleAnimationTaskId = -1;
    
    // RPGCore 服务适配器
    private BoardServiceAdapter serviceAdapter;
    private BoardDataHandler dataHandler;
    private BoardPlaceholder boardPlaceholder;

    // RPGCore 服务引用 - 优先使用 RPGCore，本地实现作为降级
    private MiniMessageService miniMessage;
    private MiniMessage miniMessageParser;
    private GameLogger gameLogger;

    private int cachedMaxLines;
    private boolean cachedRememberToggleState;
    private boolean cachedShowByDefault;
    private Set<String> cachedHiddenWorlds;
    private boolean cachedHideWhenSneaking;
    private boolean cachedHideInCombat;
    private boolean cachedSmartRefreshEnabled;
    private double[] cachedTps = new double[]{20.0, 20.0, 20.0};
    private long lastTpsUpdate = 0;
    private static final long TPS_UPDATE_INTERVAL = 1000;

    @Override
    protected void onPluginEnable() {
        instance = this;

        // 初始化 RPGCore 服务（优先使用 RPGCore，本地实现作为降级）
        initRPGCoreServices();

        loadConfig();
        loadWorldAliases();
        registerEvents();
        startTasks();

        // 注册 RPGCore 服务适配器
        serviceAdapter = new BoardServiceAdapter(this);
        if (serviceAdapter.isUsingRPGCore()) {
            logInfo("已集成 RPGCore 服务系统!");
        }

        if (externalServices != null && externalServices.isPlaceholderAPIEnabled()) {
            logInfo("已连接到 PlaceholderAPI!");
            boardPlaceholder = new BoardPlaceholder(this);
            boardPlaceholder.register();
            logInfo("已注册 PlaceholderAPI 扩展!");
        }

        logInfo("光点侧边栏插件已启用! 版本: " + getDescription().getVersion());
        logInfo("作者: Gumin | QQ: 2271257344");
    }

    /**
     * 初始化 RPGCore 核心服务
     * 优先使用 RPGCore 统一服务，本地实现作为降级方案
     */
    private void initRPGCoreServices() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            miniMessage = rpgCore.getMiniMessageService();
            if (miniMessage != null) {
                miniMessageParser = miniMessage.getMiniMessage();
                logInfo("已连接到 RPGCore MiniMessageService");
            }
            // 初始化 GameLogger
            gameLogger = rpgCore.getGameLogger();
            if (gameLogger != null) {
                logInfo("已连接到 RPGCore GameLogger");
            }
        }

        // 如果 RPGCore 服务不可用，初始化本地降级服务
        if (miniMessage == null) {
            miniMessage = MiniMessageService.getInstance();
            miniMessageParser = miniMessage.getMiniMessage();
            logInfo("使用本地 MiniMessageService（降级）");
        }
        if (gameLogger == null) {
            logInfo("使用 Bukkit Logger（降级）");
        }
    }

    /**
     * 日志辅助方法 - 优先使用 RPGCore GameLogger
     */
    public void logInfo(String message) {
        if (gameLogger != null) {
            gameLogger.info(message);
        } else {
            getLogger().info(message);
        }
    }

    public void logWarning(String message) {
        if (gameLogger != null) {
            gameLogger.warning(message);
        } else {
            getLogger().warning(message);
        }
    }

    public void logSevere(String message) {
        if (gameLogger != null) {
            gameLogger.severe(message);
        } else {
            getLogger().severe(message);
        }
    }

    public void logDebug(String message) {
        if (!debug) return;
        if (gameLogger != null) {
            gameLogger.debug(message);
        } else {
            getLogger().info("[DEBUG] " + message);
        }
    }

    private boolean shouldShowTopSeparator() {
        return styleSeparatorPositions.contains("top") || styleSeparatorPositions.contains("both") ||
            styleSeparatorPositions.contains("all");
    }

    private boolean shouldShowBottomSeparator() {
        return styleSeparatorPositions.contains("bottom") || styleSeparatorPositions.contains("both") ||
            styleSeparatorPositions.contains("all");
    }

    private boolean shouldShowBetweenSeparator() {
        return styleSeparatorPositions.contains("between") || styleSeparatorPositions.contains("all");
    }

    private List<String> applyStyleSeparators(List<String> lines) {
        if (!showStyleSeparator || styleSeparatorLine == null || styleSeparatorLine.isEmpty()) {
            return lines;
        }

        List<String> styled = new ArrayList<>();
        if (shouldShowTopSeparator()) {
            styled.add(styleSeparatorLine);
        }

        for (String line : lines) {
            if (line == null) continue;
            if (line.trim().isEmpty() && shouldShowBetweenSeparator()) {
                styled.add(styleSeparatorLine);
                continue;
            }
            styled.add(line);
        }

        if (shouldShowBottomSeparator()) {
            styled.add(styleSeparatorLine);
        }

        return styled;
    }

    @Override
    protected void onPluginDisable() {
        // 取消所有调度任务
        cancelAllTasks();
        
        // 注销玩家生命周期处理器
        if (dataHandler != null) {
            dataHandler.unregister();
        }
        
        // 注销 RPGCore 服务
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        
        // 注销 PlaceholderAPI 扩展
        if (boardPlaceholder != null) {
            PlaceholderAPI.unregisterExpansion(boardPlaceholder);
        }
        
        stopTasks();
        clearAllBoards();

        logInfo("光点侧边栏插件已禁用!");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianBoard";
    }

    private void loadConfig() {
        saveDefaultConfig();
        config = getConfig();
        
        refreshInterval = config.getLong("refresh-interval", 5000L);
        titleRefreshInterval = config.getLong("title-refresh-interval", 3000L);
        eventCooldown = config.getLong("smart-refresh.event-cooldown", 1000L);
        defaultTitle = config.getString("title", "<gold><bold>光点RPG");
        debug = config.getBoolean("advanced.debug", false);
        
        titleAnimationEnabled = config.getBoolean("title-animation.enabled", false);
        titleFrames = config.getStringList("title-animation.frames");
        cachedMaxLines = Math.max(1, config.getInt("advanced.max-lines", 15));
        loadStyleSettings();
        cacheConfigValues();
    }

    private void cacheConfigValues() {
        cachedRememberToggleState = config.getBoolean("advanced.remember-toggle-state", true);
        cachedShowByDefault = config.getBoolean("advanced.show-by-default", true);
        cachedHiddenWorlds = new HashSet<>(config.getStringList("advanced.hidden-worlds"));
        cachedHideWhenSneaking = config.getBoolean("advanced.hide-when-sneaking", false);
        cachedHideInCombat = config.getBoolean("advanced.hide-in-combat", false);
        cachedSmartRefreshEnabled = config.getBoolean("smart-refresh.enabled", true);
    }

    private void loadWorldAliases() {
        worldAliases.clear();
        if (config.contains("world-aliases")) {
            for (String key : config.getConfigurationSection("world-aliases").getKeys(false)) {
                worldAliases.put(key, config.getString("world-aliases." + key));
            }
        }
    }

    private void loadStyleSettings() {
        ConfigurationSection styleSection = config.getConfigurationSection("style");
        if (styleSection == null) {
            showStyleSeparator = false;
            styleSeparatorLine = "";
            styleSeparatorPositions = Set.of("top", "bottom");
            return;
        }

        showStyleSeparator = styleSection.getBoolean("show-separator", false);
        styleSeparatorLine = styleSection.getString("separator", "");
        String position = styleSection.getString("separator-position", "top-bottom").toLowerCase(Locale.ROOT);

        styleSeparatorPositions = new HashSet<>();
        for (String token : position.split("[,\\s-]+")) {
            if (!token.isBlank()) {
                styleSeparatorPositions.add(token);
            }
        }

        if (styleSeparatorPositions.isEmpty()) {
            styleSeparatorPositions = Set.of("top", "bottom");
        }
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(this, this);
        
        // 注册玩家生命周期处理器
        if (getServer().getPluginManager().isPluginEnabled("RPGCore")) {
            dataHandler = new BoardDataHandler(this);
            dataHandler.register();
            logInfo("已注册到 RPGCore PlayerLifecycleManager");
        } else {
            logWarning("RPGCore 未启用，使用传统事件监听");
        }
    }

    private void startTasks() {
        stopTasks();

        if (scheduler == null) {
            logWarning("Scheduler not available, using fallback");
            return;
        }
        
        // 节流机制：最小刷新间隔不低于 500ms (10tick)，避免过于频繁刷新
        final long refreshPeriod = Math.max(10L, refreshInterval / 50);
        
        refreshTaskId = scheduler.runSyncRepeating(() -> {
            if (!config.getBoolean("enabled", true)) return;
            
            // 节流优化：如果脏标记机制启用，只刷新脏玩家
            if (dirtyFlagEnabled && !dirtyPlayers.isEmpty()) {
                // 只刷新标记为脏的玩家，减少不必要的刷新
                for (UUID playerId : dirtyPlayers) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null && player.isOnline() && shouldShowBoard(player)) {
                        updateBoard(player);
                    }
                }
                return;
            }
            
            // 脏标记机制禁用或没有脏玩家时，执行全量刷新（作为兜底）
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (shouldShowBoard(player)) {
                    updateBoard(player);
                }
            }
        }, 0L, refreshPeriod);
        
        if (titleAnimationEnabled && !titleFrames.isEmpty()) {
            final long titlePeriod = titleRefreshInterval / 50;
            titleAnimationTaskId = scheduler.runSyncRepeating(() -> {
                titleFrameIndex = (titleFrameIndex + 1) % titleFrames.size();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (shouldShowBoard(player)) {
                        updateBoardTitle(player);
                    }
                }
            }, titlePeriod, titlePeriod);
        }
    }

    private void stopTasks() {
        if (scheduler != null) {
            if (refreshTaskId >= 0) {
                scheduler.cancelTask(refreshTaskId);
                refreshTaskId = -1;
            }
            if (titleAnimationTaskId >= 0) {
                scheduler.cancelTask(titleAnimationTaskId);
                titleAnimationTaskId = -1;
            }
        }
    }

    private void clearAllBoards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeBoard(player);
        }
        playerBoards.clear();
        boardCache.clear();
        lineHashCache.clear();
        lastBoardEntries.clear();
        dirtyPlayers.clear();
        lastRefreshTime.clear();
    }

    public void createBoard(Player player) {
        if (!config.getBoolean("enabled", true)) return;
        
        if (!shouldShowBoard(player)) return;
        
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        player.setScoreboard(board);
        
        playerBoards.put(player.getUniqueId(), board);
        
        String objectiveName = "guangdianboard";

        String title = getCurrentTitle(player);
        // 使用 MiniMessage 解析标题为 Component
        Component titleComponent = translateColorsToComponent(title);
        Objective objective = board.registerNewObjective(objectiveName, "dummy", titleComponent);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        updateBoardContent(player, objective);
    }

    public void updateBoard(Player player) {
        if (!config.getBoolean("enabled", true)) return;
        
        if (!shouldShowBoard(player)) {
            removeBoard(player);
            return;
        }
        
        Scoreboard board = playerBoards.get(player.getUniqueId());
        if (board == null) {
            createBoard(player);
            return;
        }
        
        Objective objective = board.getObjective("guangdianboard");
        if (objective == null) {
            createBoard(player);
            return;
        }

        updateBoardContent(player, objective);
    }

    public void updateBoardTitle(Player player) {
        Scoreboard board = playerBoards.get(player.getUniqueId());
        if (board == null) return;
        
        Objective objective = board.getObjective("guangdianboard");
        if (objective == null) return;
        
        // 使用 MiniMessage 解析标题为 Component
        objective.displayName(translateColorsToComponent(getCurrentTitle(player)));
    }

    private void updateBoardContent(Player player, Objective objective) {
        List<String> displayLines = buildDisplayLines(player);
        
        // ==================== 高性能优化: 增量哈希比较 ====================
        // 使用int[]比较替代List.equals()
        int[] newHashes = computeLineHashes(displayLines);
        int[] oldHashes = lineHashCache.get(player.getUniqueId());
        
        if (oldHashes != null && Arrays.equals(newHashes, oldHashes)) {
            return;  // 内容未变化，跳过更新
        }
        
        // 更新缓存
        lineHashCache.put(player.getUniqueId(), newHashes);
        boardCache.put(player.getUniqueId(), List.copyOf(displayLines));
        
        // 清除脏标记
        dirtyPlayers.remove(player.getUniqueId());

        int maxLines = getMaxLines();
        
        Scoreboard board = objective.getScoreboard();
        UUID playerId = player.getUniqueId();
        
        // 只清除上一次侧边栏使用的 entries，不清除其他 Objective（如血量显示）的分数
        Set<String> oldEntries = lastBoardEntries.get(playerId);
        if (oldEntries != null) {
            for (String entry : oldEntries) {
                board.resetScores(entry);
            }
        }
        
        // 记录本次设置的 entries
        Set<String> newEntries = new HashSet<>();

        int score = Math.min(displayLines.size(), maxLines);
        for (int i = 0; i < displayLines.size() && i < maxLines; i++) {
            String line = displayLines.get(i);
            if (line.isEmpty()) {
                // 使用空字符串替代 ChatColor.RESET
                line = " ";
            }

            String entry = appendUniqueSuffix(line, i);
            newEntries.add(entry);
            Score scoreObj = objective.getScore(entry);
            scoreObj.setScore(score--);
        }
        
        // 保存本次 entries
        lastBoardEntries.put(playerId, newEntries);
    }
    
    /**
     * 计算行哈希数组（用于快速比较）
     */
    private int[] computeLineHashes(List<String> lines) {
        int[] hashes = new int[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            hashes[i] = line != null ? line.hashCode() : 0;
        }
        return hashes;
    }
    
    /**
     * 标记玩家为脏（属性变化时调用）
     * 通过BoardServiceAdapter的事件订阅调用
     */
    public void markDirty(UUID playerId) {
        if (dirtyFlagEnabled) {
            dirtyPlayers.add(playerId);
        }
    }
    
    /**
     * 检查玩家是否是脏状态
     */
    public boolean isDirty(UUID playerId) {
        return dirtyPlayers.contains(playerId);
    }

    private List<String> buildDisplayLines(Player player) {
        List<String> rawLines = getLinesForPlayer(player);
        List<String> styledLines = applyStyleSeparators(rawLines);
        List<String> output = new ArrayList<>();
        int maxLines = getMaxLines();

        for (String rawLine : styledLines) {
            if (output.size() >= maxLines) break;
            if (rawLine == null) continue;

            String processedLine = processPlaceholders(player, rawLine);
            processedLine = translateColors(processedLine);
            processedLine = fixLineLength(processedLine);

            output.add(processedLine);
        }

        return output;
    }

    private int getMaxLines() {
        return cachedMaxLines;
    }

    private String appendUniqueSuffix(String line, int index) {
        char suffixChar = SUFFIX_CHAR_POOL[index % SUFFIX_CHAR_POOL.length];
        // 使用 § 字符替代 ChatColor.COLOR_CHAR
        return line + "§r§" + suffixChar;
    }

    private String fixLineLength(String line) {
        if (line.length() > MAX_DISPLAY_LENGTH) {
            return line.substring(0, MAX_DISPLAY_LENGTH);
        }
        return line;
    }

    private String getCurrentTitle(Player player) {
        BoardTemplate template = resolveBoardTemplate(player);
        String title;
        if (titleAnimationEnabled && !titleFrames.isEmpty()) {
            title = titleFrames.get(titleFrameIndex);
        } else {
            title = template.title == null || template.title.isBlank() ? defaultTitle : template.title;
        }
        return processPlaceholders(player, title);
    }

    private List<String> getLinesForPlayer(Player player) {
        return resolveBoardTemplate(player).lines;
    }

    private String getPrimaryGroup(Player player) {
        if (externalServices == null) return null;
        return externalServices.getPlayerPrimaryGroup(player);
    }

    public boolean shouldShowBoard(Player player) {
        if (!player.hasPermission("guangdian.board.use")) {
            return false;
        }

        if (cachedRememberToggleState) {
            Boolean toggleState = boardToggleState.get(player.getUniqueId());
            if (toggleState != null && !toggleState) {
                return false;
            }
        }

        if (cachedHiddenWorlds.contains(player.getWorld().getName())) {
            return false;
        }

        if (cachedHideWhenSneaking && player.isSneaking()) {
            return false;
        }

        if (cachedHideInCombat && isPlayerInCombat(player)) {
            return false;
        }

        return true;
    }

    /**
     * 智能刷新请求 - 高性能优化版
     * 
     * 特性:
     * 1. 脏标记检查 - 只刷新标记为脏的玩家
     * 2. 防抖机制 - 避免短时间内重复刷新
     * 3. 异步执行 - 不阻塞主线程
     */
    public void requestSmartRefresh(Player player) {
        if (!cachedSmartRefreshEnabled) return;
        
        UUID playerId = player.getUniqueId();
        
        // 脏标记优化: 检查是否需要刷新
        if (dirtyFlagEnabled && !dirtyPlayers.contains(playerId)) {
            // 非脏玩家，跳过刷新（定时任务会处理）
            return;
        }
        
        // 防抖检查
        long now = System.currentTimeMillis();
        Long lastTime = lastRefreshTime.get(playerId);
        
        if (lastTime != null && (now - lastTime) < eventCooldown) {
            return;
        }
        
        lastRefreshTime.put(playerId, now);
        
        // 延迟1tick执行，确保属性已完全更新
        if (scheduler != null) {
            scheduler.runSyncLater(() -> {
                if (player.isOnline()) {
                    updateBoard(player);
                }
            }, 1L);
        }
    }

    public boolean shouldShowBoardPublic(Player player) {
        return shouldShowBoard(player);
    }

    public String getDefaultTitlePublic() {
        return defaultTitle;
    }

    public void removeBoard(Player player) {
        UUID playerId = player.getUniqueId();
        Scoreboard board = playerBoards.get(playerId);
        if (board != null) {
            Objective obj = board.getObjective("guangdianboard");
            if (obj != null) {
                obj.unregister();
            }
        }
        playerBoards.remove(playerId);
        boardCache.remove(playerId);
        lineHashCache.remove(playerId);
        lastBoardEntries.remove(playerId);
        dirtyPlayers.remove(playerId);
        lastRefreshTime.remove(playerId);
    }

    public void toggleBoard(Player player) {
        boolean currentState = boardToggleState.getOrDefault(player.getUniqueId(), cachedShowByDefault);
        boolean newState = !currentState;
        
        boardToggleState.put(player.getUniqueId(), newState);
        
        if (newState) {
            createBoard(player);
            sendMessage(player, config.getString("messages.board-enabled", "<green>侧边栏已启用!"));
        } else {
            removeBoard(player);
            sendMessage(player, config.getString("messages.board-disabled", "<red>侧边栏已关闭!"));
        }
    }

    private String processPlaceholders(Player player, String text) {
        String world = player.getWorld().getName();
        String worldAlias = worldAliases.getOrDefault(world, world);

        if (debug) {
            logDebug("原始文本: " + text);
            logDebug("externalServices: " + (externalServices != null ? "已初始化" : "null"));
        }

        if (externalServices == null && rpgCore != null) {
            externalServices = rpgCore.getExternalServices();
            if (externalServices != null) {
                logWarning("[GuangDianBoard] 重新获取 externalServices 成功");
            }
        }

        text = text.replace("%player%", player.getName());
        text = text.replace("%player_name%", player.getName());
        text = text.replace("%world%", worldAlias);
        text = text.replace("%player_world%", worldAlias);
        text = text.replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        text = text.replace("%server_online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        text = text.replace("%max_players%", String.valueOf(Bukkit.getMaxPlayers()));
        text = text.replace("%server_max_players%", String.valueOf(Bukkit.getMaxPlayers()));
        text = text.replace("%player_level%", String.valueOf(player.getLevel()));
        text = text.replace("%player_health%", String.valueOf((int) Math.ceil(player.getHealth())));
        text = text.replace("%player_max_health%", String.valueOf((int) player.getMaxHealth()));
        text = text.replace("%player_food%", String.valueOf(player.getFoodLevel()));
        text = text.replace("%player_exp%", String.valueOf((int) (player.getExp() * 100)));
        text = text.replace("%player_ping%", String.valueOf(player.getPing()));

        text = processServerTpsPlaceholders(text);

        if (text.contains("%luckperms_prefix%")) {
            String prefix = "";
            if (externalServices != null) {
                prefix = externalServices.getPlayerPrefix(player);
            }
            // 将 & 颜色代码转换为 MiniMessage 格式
            prefix = prefix != null ? convertLegacyColorsToMiniMessage(prefix) : "";
            text = text.replace("%luckperms_prefix%", prefix);
        }

        if (text.contains("%luckperms_suffix%")) {
            String suffix = "";
            if (externalServices != null) {
                suffix = externalServices.getPlayerSuffix(player);
            }
            // 将 & 颜色代码转换为 MiniMessage 格式
            suffix = suffix != null ? convertLegacyColorsToMiniMessage(suffix) : "";
            text = text.replace("%luckperms_suffix%", suffix);
        }

        if (text.contains("%luckperms_primary_group_name%")) {
            String group = "default";
            if (externalServices != null) {
                group = externalServices.getPlayerPrimaryGroup(player);
            }
            text = text.replace("%luckperms_primary_group_name%", group != null ? group : "default");
        }

        if (externalServices != null) {
            String before = text;
            text = externalServices.parsePlaceholders(player, text);
            if (debug && !before.equals(text)) {
                logDebug("PlaceholderAPI 解析: " + before + " -> " + text);
            }
        }

        if (debug) {
            logDebug("最终文本: " + text);
        }

        return text;
    }

    private String processServerTpsPlaceholders(String text) {
        long now = System.currentTimeMillis();
        if (now - lastTpsUpdate > TPS_UPDATE_INTERVAL) {
            cachedTps = Bukkit.getTPS();
            lastTpsUpdate = now;
        }
        text = text.replace("%server_tps%", formatTps(readTps(cachedTps, 0)));
        text = text.replace("%server_tps_1%", formatTps(readTps(cachedTps, 0)));
        text = text.replace("%server_tps_5%", formatTps(readTps(cachedTps, 1)));
        text = text.replace("%server_tps_15%", formatTps(readTps(cachedTps, 2)));
        return text;
    }

    private double readTps(double[] tps, int index) {
        if (tps == null || index < 0 || index >= tps.length) {
            return 20.0D;
        }
        double value = tps[index];
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 20.0D;
        }
        return Math.min(20.0D, value);
    }

    private String formatTps(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private String processLuckPermsPlaceholders(Player player, String text) {
        if (externalServices == null) {
            return text;
        }

        if (text.contains("%luckperms_prefix%")) {
            String prefix = externalServices.parsePlaceholders(player, "%luckperms_prefix%");
            if (prefix != null && !prefix.equals("%luckperms_prefix%")) {
                text = text.replace("%luckperms_prefix%", prefix);
            }
        }

        if (text.contains("%luckperms_suffix%")) {
            String suffix = externalServices.parsePlaceholders(player, "%luckperms_suffix%");
            if (suffix != null && !suffix.equals("%luckperms_suffix%")) {
                text = text.replace("%luckperms_suffix%", suffix);
            }
        }

        if (text.contains("%luckperms_primary_group_name%")) {
            String group = externalServices.parsePlaceholders(player, "%luckperms_primary_group_name%");
            if (group != null && !group.equals("%luckperms_primary_group_name%")) {
                text = text.replace("%luckperms_primary_group_name%", group);
            }
        }

        return text;
    }

    private String getLuckPermsPrefix(Player player) {
        if (externalServices == null) return "";
        return externalServices.getPlayerPrefix(player);
    }

    private String getLuckPermsSuffix(Player player) {
        if (externalServices == null) return "";
        return externalServices.getPlayerSuffix(player);
    }

    private String processRoundedPlaceholder(String text, Player player, String placeholder) {
        String fullPlaceholder = "%" + placeholder + "%";
        if (!text.contains(fullPlaceholder)) {
            return text;
        }

        String value = externalServices != null ? externalServices.parsePlaceholders(player, fullPlaceholder) : fullPlaceholder;
        if (value == null || value.equals(fullPlaceholder)) {
            return text;
        }

        try {
            String numericPart = value.replaceAll("[^0-9.\\-]", "");
            if (numericPart.isEmpty()) {
                return text.replace(fullPlaceholder, value);
            }
            double numValue = Double.parseDouble(numericPart);
            int roundedValue = (int) Math.round(numValue);
            return text.replace(fullPlaceholder, String.valueOf(roundedValue));
        } catch (NumberFormatException e) {
            return text.replace(fullPlaceholder, value);
        }
    }

    private Component translateColorsToComponent(String text) {
        if (text == null) return Component.empty();
        // 使用 MiniMessage 直接解析颜色代码
        return miniMessage.colorize(text);
    }

    /**
     * 将 Legacy & 颜色代码转换为 MiniMessage 格式
     * 例如: <green><bold>称号 -> <green><bold>称号
     */
    private String convertLegacyColorsToMiniMessage(String text) {
        if (text == null || text.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && i + 1 < chars.length) {
                char colorCode = Character.toLowerCase(chars[i + 1]);
                String miniMessageTag = getMiniMessageTag(colorCode);
                if (miniMessageTag != null) {
                    result.append(miniMessageTag);
                    i++; // 跳过颜色代码字符
                    continue;
                }
            }
            result.append(chars[i]);
        }

        return result.toString();
    }

    /**
     * 获取 MiniMessage 标签
     */
    private String getMiniMessageTag(char colorCode) {
        if (colorCode >= '0' && colorCode <= '9') {
            return COLOR_TAGS[colorCode - '0'];
        }
        if (colorCode >= 'a' && colorCode <= 'f') {
            return COLOR_TAGS[colorCode - 'a' + 10];
        }
        return switch (colorCode) {
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }

    /**
     * 将文本转换为带颜色代码的字符串（用于 Scoreboard entry）
     * Scoreboard API 需要 legacy 格式的字符串，但我们使用 MiniMessage 进行解析
     */
    private String translateColors(String text) {
        if (text == null) return "";
        // 使用 MiniMessage 解析，然后序列化为 legacy 格式
        Component component = miniMessage.colorize(text);
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(component);
    }

    private void sendMessage(org.bukkit.command.CommandSender sender, String text) {
        // 使用 RPGCore MiniMessageService 直接发送 Component
        sender.sendMessage(miniMessage.colorize(text));
    }

    private boolean isPlayerInCombat(Player player) {
        if (player == null) return false;
        if (externalServices == null || !externalServices.isPlaceholderAPIEnabled()) return false;
        String value = externalServices.parsePlaceholders(player, "%gdcombat_in_combat%");
        if (value == null) return false;
        value = value.trim().toLowerCase(Locale.ROOT);
        return value.equals("true") || value.equals("1") || value.equals("y") || value.equals("yes");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (cachedRememberToggleState) {
            boardToggleState.putIfAbsent(player.getUniqueId(), cachedShowByDefault);
        }
        
        if (shouldShowBoard(player)) {
            createBoard(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // 完全清理玩家所有缓存数据，确保重新上线时能正常显示面板
        removeBoard(player);
        
        // 注意：boardToggleState 不在此处清除，由配置决定是否保留
        // 如果 remember-toggle-state 为 false，下次上线会重新初始化
        if (!cachedRememberToggleState) {
            boardToggleState.remove(playerId);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        runTaskLaterSafe(() -> {
            if (player.isOnline()) {
                updateBoard(player);
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        
        runTaskLaterSafe(() -> {
            if (player.isOnline()) {
                updateBoard(player);
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!cachedHideWhenSneaking) return;
        
        Player player = event.getPlayer();
        runTaskLaterSafe(() -> {
            if (player.isOnline()) {
                updateBoard(player);
            }
        }, 1L);
    }
    
    private void runTaskLaterSafe(Runnable task, long delay) {
        if (scheduler != null) {
            scheduler.runSyncLater(task, delay);
        } else {
            getLogger().warning("RPGCore 未启用，无法执行延迟任务");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getSlot() < 0 || event.getSlot() > 8) return;
        if (event.getClick().isShiftClick()) return;

        Player player = (Player) event.getWhoClicked();
        requestSmartRefresh(player);
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("toggleboard")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("该命令只能由玩家执行!");
                return true;
            }
            
            Player player = (Player) sender;
            if (!player.hasPermission("guangdian.board.toggle")) {
                player.sendMessage(miniMessage.colorize(config.getString("messages.no-permission", "<red>您没有权限执行此操作!")));
                return true;
            }
            
            toggleBoard(player);
            return true;
        }
        
        if (command.getName().equalsIgnoreCase("gdboard")) {
            if (args.length == 0) {
                sendHelp(sender);
                return true;
            }
            
            switch (args[0].toLowerCase()) {
                case "reload":
                    return handleReload(sender);
                case "toggle":
                    return handleToggle(sender);
                case "info":
                    return handleInfo(sender);
                case "help":
                    sendHelp(sender);
                    return true;
                default:
                    sender.sendMessage(miniMessage.colorize("<red>未知的命令! 使用 /gdboard help 查看帮助"));
                    return true;
            }
        }
        
        return true;
    }

    private void sendHelp(org.bukkit.command.CommandSender sender) {
        sender.sendMessage(miniMessage.colorize("<gold><bold>===== 光点侧边栏插件 ====="));
        sender.sendMessage(miniMessage.colorize("<yellow>/gdboard reload <gray>- 重新加载配置"));
        sender.sendMessage(miniMessage.colorize("<yellow>/gdboard toggle <gray>- 切换侧边栏显示"));
        sender.sendMessage(miniMessage.colorize("<yellow>/gdboard info <gray>- 显示插件信息"));
        sender.sendMessage(miniMessage.colorize("<yellow>/gdboard help <gray>- 显示帮助信息"));
        sender.sendMessage(miniMessage.colorize("<yellow>/toggleboard <gray>- 快速切换侧边栏"));
        sender.sendMessage(miniMessage.colorize("<gray>作者: Gumin | QQ: 2271257344"));
    }

    private boolean handleReload(org.bukkit.command.CommandSender sender) {
        if (!sender.hasPermission("guangdian.board.reload")) {
            sender.sendMessage(miniMessage.colorize(config.getString("messages.no-permission", "<red>您没有权限执行此操作!")));
            return true;
        }
        
        reloadConfig();
        config = getConfig();
        loadWorldAliases();
        loadStyleSettings();
        refreshInterval = config.getLong("refresh-interval", 1000L);
        titleRefreshInterval = config.getLong("title-refresh-interval", 3000L);
        defaultTitle = config.getString("title", "<gold><bold>光点RPG");
        titleAnimationEnabled = config.getBoolean("title-animation.enabled", false);
        titleFrames = config.getStringList("title-animation.frames");
        cachedMaxLines = Math.max(1, config.getInt("advanced.max-lines", 15));
        cacheConfigValues();
        
        stopTasks();
        startTasks();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (shouldShowBoard(player)) {
                createBoard(player);
            }
        }
        
        sender.sendMessage(miniMessage.colorize(config.getString("messages.config-reloaded", "<green>侧边栏配置已重新加载!")));
        return true;
    }

    private boolean handleToggle(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("该命令只能由玩家执行!");
            return true;
        }
        
        Player player = (Player) sender;
        if (!player.hasPermission("guangdian.board.toggle")) {
            player.sendMessage(miniMessage.colorize(config.getString("messages.no-permission", "<red>您没有权限执行此操作!")));
            return true;
        }
        
        toggleBoard(player);
        return true;
    }

    private boolean handleInfo(org.bukkit.command.CommandSender sender) {
        sender.sendMessage(miniMessage.colorize("<gold><bold>===== 光点侧边栏插件信息 ====="));
        sender.sendMessage(miniMessage.colorize("<yellow>版本: <white>" + getDescription().getVersion()));
        sender.sendMessage(miniMessage.colorize("<yellow>作者: <white>Gumin"));
        sender.sendMessage(miniMessage.colorize("<yellow>QQ: <white>2271257344"));
        sender.sendMessage(miniMessage.colorize("<yellow>状态: <green>已启用"));
        sender.sendMessage(miniMessage.colorize("<yellow>刷新间隔: <white>" + refreshInterval + " ms"));
        sender.sendMessage(miniMessage.colorize("<yellow>标题动画: <white>" + (titleAnimationEnabled ? "启用" : "禁用")));
        sender.sendMessage(miniMessage.colorize("<yellow>外部服务: <white>" + (externalServices != null ? externalServices.getExternalServiceStatus() : "<red>未初始化")));
        return true;
    }

    @Override
    public List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("gdboard")) {
            if (args.length == 1) {
                completions.add("reload");
                completions.add("toggle");
                completions.add("info");
                completions.add("help");
            }
        }
        
        return completions;
    }

    public Map<UUID, Boolean> getBoardToggleState() {
        return boardToggleState;
    }
    
    public Map<UUID, Scoreboard> getPlayerBoards() {
        return playerBoards;
    }
    
    public Map<UUID, List<String>> getBoardCache() {
        return boardCache;
    }

    public static GuangDianBoard getInstance() {
        return instance;
    }

    private BoardTemplate resolveBoardTemplate(Player player) {
        String world = player.getWorld().getName();
        String primaryGroup = getPrimaryGroup(player);

        if (primaryGroup != null) {
            String groupWorldPath = "group-world-boards." + primaryGroup + "." + world;
            if (config.contains(groupWorldPath + ".lines") && config.getBoolean(groupWorldPath + ".enabled", true)) {
                return new BoardTemplate(
                    config.getString(groupWorldPath + ".title", defaultTitle),
                    config.getStringList(groupWorldPath + ".lines")
                );
            }
        }

        if (primaryGroup != null) {
            String groupPath = "group-boards." + primaryGroup;
            if (config.contains(groupPath + ".lines") && config.getBoolean(groupPath + ".enabled", true)) {
                return new BoardTemplate(
                    config.getString(groupPath + ".title", defaultTitle),
                    config.getStringList(groupPath + ".lines")
                );
            }
        }

        String worldPath = "world-boards." + world;
        if (config.contains(worldPath + ".lines") && config.getBoolean(worldPath + ".enabled", true)) {
            return new BoardTemplate(
                config.getString(worldPath + ".title", defaultTitle),
                config.getStringList(worldPath + ".lines")
            );
        }

        return new BoardTemplate(defaultTitle, config.getStringList("lines"));
    }

    private static class BoardTemplate {
        private final String title;
        private final List<String> lines;

        private BoardTemplate(String title, List<String> lines) {
            this.title = title;
            this.lines = lines;
        }
    }

}
