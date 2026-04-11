package cn.guangdian.board;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 光点侧边栏插件 - GuangDianBoard
 * 
 * 显示玩家侧边栏计分板，支持PlaceholderAPI和LuckPerms
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
    private final Map<String, String> worldAliases = new HashMap<>();
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
    private Set<String> styleSeparatorPositions = new HashSet<>();

    private static final int MAX_SCOREBOARD_ENTRY_LENGTH = 40;
    private static final int UNIQUE_SUFFIX_LENGTH = 4;
    private static final int MAX_DISPLAY_LENGTH = MAX_SCOREBOARD_ENTRY_LENGTH - UNIQUE_SUFFIX_LENGTH;
    private static final char[] SUFFIX_CHAR_POOL = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    
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

    private int cachedMaxLines;
    private double[] cachedTps = new double[]{20.0, 20.0, 20.0};
    private long lastTpsUpdate = 0;
    private static final long TPS_UPDATE_INTERVAL = 1000;

    @Override
    protected void onPluginEnable() {
        instance = this;
        
        loadConfig();
        loadWorldAliases();
        registerEvents();
        startTasks();
        
        // 注册 RPGCore 服务适配器
        serviceAdapter = new BoardServiceAdapter(this);
        if (serviceAdapter.isUsingRPGCore()) {
            getLogger().info("已集成 RPGCore 服务系统!");
        }

        if (externalServices != null && externalServices.isPlaceholderAPIEnabled()) {
            getLogger().info("已连接到 PlaceholderAPI!");
            new BoardPlaceholder(this).register();
            getLogger().info("已注册 PlaceholderAPI 扩展!");
        }

        getLogger().info("光点侧边栏插件已启用! 版本: " + getDescription().getVersion());
        getLogger().info("作者: Gumin | QQ: 2271257344");
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
        // 注销玩家生命周期处理器
        if (dataHandler != null) {
            dataHandler.unregister();
        }
        
        // 注销 RPGCore 服务
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        
        // 取消所有任务
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
        
        stopTasks();
        clearAllBoards();
        
        getLogger().info("光点侧边栏插件已禁用!");
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
        defaultTitle = config.getString("title", "&6&l光点RPG");
        debug = config.getBoolean("advanced.debug", false);
        
        titleAnimationEnabled = config.getBoolean("title-animation.enabled", false);
        titleFrames = config.getStringList("title-animation.frames");
        cachedMaxLines = Math.max(1, config.getInt("advanced.max-lines", 15));
        loadStyleSettings();
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
            getLogger().info("已注册到 RPGCore PlayerLifecycleManager");
        } else {
            getLogger().warning("RPGCore 未启用，使用传统事件监听");
        }
    }

    private void startTasks() {
        stopTasks();
        
        if (scheduler == null) {
            getLogger().warning("Scheduler not available, using fallback");
            return;
        }
        
        final long refreshPeriod = Math.max(1L, refreshInterval / 50);
        refreshTaskId = scheduler.runSyncRepeating(() -> {
            if (!config.getBoolean("enabled", true)) return;
            
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
    }

    public void createBoard(Player player) {
        if (!config.getBoolean("enabled", true)) return;
        
        if (!shouldShowBoard(player)) return;
        
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        player.setScoreboard(board);
        
        playerBoards.put(player.getUniqueId(), board);
        
        String objectiveName = "guangdianboard";
        
        String title = getCurrentTitle(player);
        Objective objective = board.registerNewObjective(objectiveName, "dummy", translateColors(title));
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
        
        objective.setDisplayName(translateColors(getCurrentTitle(player)));
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
                line = ChatColor.RESET.toString();
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
        return line + ChatColor.RESET + ChatColor.COLOR_CHAR + suffixChar;
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

        if (config.getBoolean("advanced.remember-toggle-state", true)) {
            Boolean toggleState = boardToggleState.get(player.getUniqueId());
            if (toggleState != null && !toggleState) {
                return false;
            }
        }

        List<String> hiddenWorlds = config.getStringList("advanced.hidden-worlds");
        if (hiddenWorlds.contains(player.getWorld().getName())) {
            return false;
        }

        if (config.getBoolean("advanced.hide-when-sneaking", false) && player.isSneaking()) {
            return false;
        }

        if (config.getBoolean("advanced.hide-in-combat", false) && isPlayerInCombat(player)) {
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
        if (!config.getBoolean("smart-refresh.enabled", true)) return;
        
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
        Scoreboard board = playerBoards.get(player.getUniqueId());
        if (board != null) {
            Objective obj = board.getObjective("guangdianboard");
            if (obj != null) {
                obj.unregister();
            }
        }
        playerBoards.remove(player.getUniqueId());
        boardCache.remove(player.getUniqueId());
        lineHashCache.remove(player.getUniqueId());
        lastBoardEntries.remove(player.getUniqueId());
    }

    public void toggleBoard(Player player) {
        boolean currentState = boardToggleState.getOrDefault(player.getUniqueId(), 
                config.getBoolean("advanced.show-by-default", true));
        boolean newState = !currentState;
        
        boardToggleState.put(player.getUniqueId(), newState);
        
        if (newState) {
            createBoard(player);
            sendMessage(player, config.getString("messages.board-enabled", "&a侧边栏已启用!"));
        } else {
            removeBoard(player);
            sendMessage(player, config.getString("messages.board-disabled", "&c侧边栏已关闭!"));
        }
    }

    private String processPlaceholders(Player player, String text) {
        String world = player.getWorld().getName();
        String worldAlias = worldAliases.getOrDefault(world, world);

        if (debug) {
            getLogger().info("[DEBUG] 原始文本: " + text);
            getLogger().info("[DEBUG] externalServices: " + (externalServices != null ? "已初始化" : "null"));
        }

        if (externalServices == null && rpgCore != null) {
            externalServices = rpgCore.getExternalServices();
            if (externalServices != null) {
                getLogger().warning("[GuangDianBoard] 重新获取 externalServices 成功");
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
            text = text.replace("%luckperms_prefix%", prefix != null ? prefix : "");
        }

        if (text.contains("%luckperms_suffix%")) {
            String suffix = "";
            if (externalServices != null) {
                suffix = externalServices.getPlayerSuffix(player);
            }
            text = text.replace("%luckperms_suffix%", suffix != null ? suffix : "");
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
                getLogger().info("[DEBUG] PlaceholderAPI 解析: " + before + " -> " + text);
            }
        }

        if (debug) {
            getLogger().info("[DEBUG] 最终文本: " + text);
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

    private String translateColors(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private void sendMessage(org.bukkit.command.CommandSender sender, String text) {
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
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
        
        if (config.getBoolean("advanced.remember-toggle-state", true)) {
            if (!boardToggleState.containsKey(player.getUniqueId())) {
                boardToggleState.put(player.getUniqueId(), config.getBoolean("advanced.show-by-default", true));
            }
        }
        
        if (shouldShowBoard(player)) {
            createBoard(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerBoards.remove(player.getUniqueId());
        boardToggleState.remove(player.getUniqueId());
        boardCache.remove(player.getUniqueId());
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
        if (!config.getBoolean("advanced.hide-when-sneaking", false)) return;
        
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
            getServer().getScheduler().runTaskLater(this, task, delay);
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
                player.sendMessage(translateColors(config.getString("messages.no-permission", "&c您没有权限执行此操作!")));
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
                    sender.sendMessage(translateColors("&c未知的命令! 使用 /gdboard help 查看帮助"));
                    return true;
            }
        }
        
        return true;
    }

    private void sendHelp(org.bukkit.command.CommandSender sender) {
        sender.sendMessage(translateColors("&6===== 光点侧边栏插件 ====="));
        sender.sendMessage(translateColors("&e/gdboard reload &7- 重新加载配置"));
        sender.sendMessage(translateColors("&e/gdboard toggle &7- 切换侧边栏显示"));
        sender.sendMessage(translateColors("&e/gdboard info &7- 显示插件信息"));
        sender.sendMessage(translateColors("&e/gdboard help &7- 显示帮助信息"));
        sender.sendMessage(translateColors("&e/toggleboard &7- 快速切换侧边栏"));
        sender.sendMessage(translateColors("&7作者: Gumin | QQ: 2271257344"));
    }

    private boolean handleReload(org.bukkit.command.CommandSender sender) {
        if (!sender.hasPermission("guangdian.board.reload")) {
            sender.sendMessage(translateColors(config.getString("messages.no-permission", "&c您没有权限执行此操作!")));
            return true;
        }
        
        reloadConfig();
        config = getConfig();
        loadWorldAliases();
        loadStyleSettings();
        refreshInterval = config.getLong("refresh-interval", 1000L);
        titleRefreshInterval = config.getLong("title-refresh-interval", 3000L);
        defaultTitle = config.getString("title", "&6&l光点RPG");
        titleAnimationEnabled = config.getBoolean("title-animation.enabled", false);
        titleFrames = config.getStringList("title-animation.frames");
        cachedMaxLines = Math.max(1, config.getInt("advanced.max-lines", 15));
        
        stopTasks();
        startTasks();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (shouldShowBoard(player)) {
                createBoard(player);
            }
        }
        
        sender.sendMessage(translateColors(config.getString("messages.config-reloaded", "&a侧边栏配置已重新加载!")));
        return true;
    }

    private boolean handleToggle(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("该命令只能由玩家执行!");
            return true;
        }
        
        Player player = (Player) sender;
        if (!player.hasPermission("guangdian.board.toggle")) {
            player.sendMessage(translateColors(config.getString("messages.no-permission", "&c您没有权限执行此操作!")));
            return true;
        }
        
        toggleBoard(player);
        return true;
    }

    private boolean handleInfo(org.bukkit.command.CommandSender sender) {
        sender.sendMessage(translateColors("&6===== 光点侧边栏插件信息 ====="));
        sender.sendMessage(translateColors("&e版本: &f" + getDescription().getVersion()));
        sender.sendMessage(translateColors("&e作者: &fGumin"));
        sender.sendMessage(translateColors("&eQQ: &f2271257344"));
        sender.sendMessage(translateColors("&e状态: &a已启用"));
        sender.sendMessage(translateColors("&e刷新间隔: &f" + refreshInterval + " ms"));
        sender.sendMessage(translateColors("&e标题动画: &f" + (titleAnimationEnabled ? "启用" : "禁用")));
        sender.sendMessage(translateColors("&e外部服务: &f" + (externalServices != null ? externalServices.getExternalServiceStatus() : "&c未初始化")));
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
