package cn.guangdian.gearscore;

import cn.guangdian.gearscore.api.GearScoreService;
import cn.guangdian.gearscore.manager.LeaderboardManager;
import cn.guangdian.gearscore.manager.ScoreCalculator;
import cn.guangdian.gearscore.placeholder.GearScorePlaceholder;
import cn.guangdian.gearscore.service.GearScoreServiceAdapter;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GuangDianGearScore extends AbstractRPGPlugin implements Listener, TabCompleter {

    private static GuangDianGearScore instance;
    
    private ScoreCalculator scoreCalculator;
    private LeaderboardManager leaderboardManager;
    private GearScoreServiceAdapter serviceAdapter;
    
    private final Map<UUID, Long> playerScores = new ConcurrentHashMap<>();
    private final Map<UUID, Long> scoreCache = new ConcurrentHashMap<>();
    
    private int updateInterval;
    private int leaderboardSize;
    private int cacheExpiryMinutes;
    private List<String> scorePatterns;
    
    private long updateTaskId = -1;

    @Override
    protected void onPluginEnable() {
        instance = this;
        
        saveDefaultConfig();
        loadConfig();
        
        scoreCalculator = new ScoreCalculator(this);
        leaderboardManager = new LeaderboardManager(this);
        
        registerEvents();
        registerServices();
        registerPlaceholders();
        startUpdateTask();
        
        getLogger().info("装备评分系统已启用! 版本: " + getDescription().getVersion());
    }

    @Override
    protected void onPluginDisable() {
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
        
        playerScores.clear();
        scoreCache.clear();
        
        getLogger().info("装备评分系统已禁用!");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianGearScore";
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        
        updateInterval = config.getInt("settings.update-interval-ticks", 600);
        leaderboardSize = config.getInt("settings.leaderboard-size", 10);
        cacheExpiryMinutes = config.getInt("settings.cache-expiry-minutes", 5);
        scorePatterns = config.getStringList("score-patterns");
        
        if (scorePatterns.isEmpty()) {
            scorePatterns = Arrays.asList("评分", "战力", "装备评分");
        }
        
        if (scoreCalculator != null) {
            scoreCalculator.updatePatterns(scorePatterns);
        }
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("gearscore").setTabCompleter(this);
    }

    private void registerServices() {
        serviceAdapter = new GearScoreServiceAdapter(this);
        
        if (RPGCore.getInstance() != null) {
            getLogger().info("已注册到 RPGCore 服务系统!");
        }
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GearScorePlaceholder(this).register();
            getLogger().info("已注册 PlaceholderAPI 扩展!");
        }
    }

    private void startUpdateTask() {
        if (scheduler != null) {
            updateTaskId = scheduler.runSyncRepeating(() -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayerScore(player);
                }
                leaderboardManager.updateLeaderboard();
            }, updateInterval, updateInterval);
        }
    }

    public void updatePlayerScore(Player player) {
        long score = scoreCalculator.calculateTotalScore(player);
        playerScores.put(player.getUniqueId(), score);
    }

    public long getPlayerScore(UUID uuid) {
        Long cached = playerScores.get(uuid);
        if (cached != null) {
            return cached;
        }
        
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            updatePlayerScore(player);
            return playerScores.getOrDefault(uuid, 0L);
        }
        
        return 0L;
    }

    public long getPlayerScore(Player player) {
        return getPlayerScore(player.getUniqueId());
    }

    public int getPlayerRank(UUID uuid) {
        return leaderboardManager.getRank(uuid);
    }

    public List<Map.Entry<UUID, Long>> getTopPlayers(int count) {
        return leaderboardManager.getTopPlayers(count);
    }

    public String getTopPlayerName(int index) {
        List<Map.Entry<UUID, Long>> top = getTopPlayers(index + 1);
        if (index < top.size()) {
            UUID uuid = top.get(index).getKey();
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            return player.getName() != null ? player.getName() : "未知";
        }
        return "-";
    }

    public long getTopPlayerScore(int index) {
        List<Map.Entry<UUID, Long>> top = getTopPlayers(index + 1);
        if (index < top.size()) {
            return top.get(index).getValue();
        }
        return 0;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        scheduler.runSyncLater(() -> updatePlayerScore(player), 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerScores.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return handleScoreCommand(sender);
        }

        switch (args[0].toLowerCase()) {
            case "top":
                return handleTopCommand(sender);
            case "reload":
                return handleReloadCommand(sender);
            case "help":
                sendHelp(sender);
                return true;
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleScoreCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize("<red>只有玩家可以使用此命令!"));
            return true;
        }

        Player player = (Player) sender;
        long score = getPlayerScore(player);
        int rank = getPlayerRank(player.getUniqueId());

        FileConfiguration config = getConfig();
        if (score > 0) {
            player.sendMessage(colorize(config.getString("messages.score-display", "<yellow>你的装备评分: <gold>%score%")
                .replace("%score%", formatNumber(score))));
            player.sendMessage(colorize(config.getString("messages.rank-display", "<yellow>你的排名: <gold>#%rank%")
                .replace("%rank%", String.valueOf(rank))));
        } else {
            player.sendMessage(colorize(config.getString("messages.no-score", "<red>你没有任何装备评分")));
        }
        return true;
    }

    private boolean handleTopCommand(CommandSender sender) {
        FileConfiguration config = getConfig();
        sender.sendMessage(colorize(config.getString("messages.leaderboard-header", "<gold>===== 装备评分排行榜 =====")));

        List<Map.Entry<UUID, Long>> top = getTopPlayers(leaderboardSize);
        for (int i = 0; i < top.size(); i++) {
            Map.Entry<UUID, Long> entry = top.get(i);
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            String name = player.getName() != null ? player.getName() : "未知";
            
            sender.sendMessage(colorize(config.getString("messages.leaderboard-line", "<yellow>#%rank% <white>%player% <gray>- <gold>%score%")
                .replace("%rank%", String.valueOf(i + 1))
                .replace("%player%", name)
                .replace("%score%", formatNumber(entry.getValue()))));
        }
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("guangdian.gearscore.admin")) {
            sender.sendMessage(colorize(getConfig().getString("messages.no-permission", "<red>没有权限!")));
            return true;
        }

        reloadConfig();
        loadConfig();
        sender.sendMessage(colorize(getConfig().getString("messages.reload-success", "<green>配置已重新加载!")));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(colorize("<gold>===== 装备评分系统帮助 ====="));
        sender.sendMessage(colorize("<yellow>/gearscore <gray>- 查看你的装备评分"));
        sender.sendMessage(colorize("<yellow>/gearscore top <gray>- 查看排行榜"));
        if (sender.hasPermission("guangdian.gearscore.admin")) {
            sender.sendMessage(colorize("<yellow>/gearscore reload <gray>- 重载配置"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.add("top");
            list.add("help");
            if (sender.hasPermission("guangdian.gearscore.admin")) {
                list.add("reload");
            }
        }
        return list;
    }

    private Component colorize(String text) {
        if (text == null) return Component.empty();
        
        MiniMessageService mm = MiniMessageService.getInstance();
        if (mm != null) {
            return mm.colorize(text);
        }
        
        // 回退: 如果 MiniMessageService 不可用,尝试直接解析
        try {
            return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(text);
        } catch (Exception e) {
            return Component.text(text);
        }
    }

    private String formatNumber(long num) {
        if (num >= 100000000) {
            return String.format("%.2f亿", num / 100000000.0);
        } else if (num >= 10000) {
            return String.format("%.2f万", num / 10000.0);
        }
        return String.format("%,d", num);
    }

    public static GuangDianGearScore getInstance() {
        return instance;
    }

    public ScoreCalculator getScoreCalculator() {
        return scoreCalculator;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public Map<UUID, Long> getPlayerScores() {
        return playerScores;
    }

    public List<String> getScorePatterns() {
        return scorePatterns;
    }

    public int getLeaderboardSize() {
        return leaderboardSize;
    }
}
