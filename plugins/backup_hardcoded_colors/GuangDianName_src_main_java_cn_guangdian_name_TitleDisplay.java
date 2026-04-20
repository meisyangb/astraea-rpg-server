package cn.guangdian.name;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.service.api.GuildService;
import cn.guangdian.rpgcore.service.api.MarriageService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 头顶显示管理器
 * 
 * 使用 Scoreboard Team 实现头顶显示：
 * - 前缀：称号 (LuckPerms prefix)
 * - 后缀：婚姻信息
 * 
 * 在每个玩家的 Scoreboard 中设置所有玩家的 Team 信息
 */
public class TitleDisplay {
    
    private static final String TEAM_PREFIX = "gdn_";
    
    private final JavaPlugin plugin;
    private ExternalServiceIntegration externalServices;
    
    private final Map<UUID, Boolean> showTitle = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> showGuild = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> showMarriage = new ConcurrentHashMap<>();
    
    private final Map<UUID, String> lastPrefix = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastSuffix = new ConcurrentHashMap<>();
    
    private boolean defaultShowTitle = true;
    private boolean defaultShowGuild = true;
    private boolean defaultShowMarriage = true;
    
    private String marriageFormat = "&d♥%s";
    
    private boolean debug = false;
    
    public TitleDisplay(JavaPlugin plugin) {
        this.plugin = plugin;
        hookExternalServices();
        loadSettings();
    }
    
    private void hookExternalServices() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            externalServices = rpgCore.getExternalServices();
            plugin.getLogger().info("[TitleDisplay] 已连接到 RPGCore ExternalServices");
        }
    }
    
    public void loadSettings() {
        defaultShowTitle = plugin.getConfig().getBoolean("title-display.default-show-title", true);
        defaultShowGuild = plugin.getConfig().getBoolean("title-display.default-show-guild", true);
        defaultShowMarriage = plugin.getConfig().getBoolean("title-display.default-show-marriage", true);
        marriageFormat = plugin.getConfig().getString("title-display.marriage-format", "&d♥%s");
    }
    
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    public void initPlayer(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        showTitle.putIfAbsent(playerId, defaultShowTitle);
        showGuild.putIfAbsent(playerId, defaultShowGuild);
        showMarriage.putIfAbsent(playerId, defaultShowMarriage);
        
        Scoreboard board = player.getScoreboard();
        if (board == null) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }
        
        for (Player target : Bukkit.getOnlinePlayers()) {
            createOrGetTeam(board, target);
            updateDisplayFor(board, target);
        }
        
        updateAllPlayersTeams(player);
        
        log("[初始化] " + player.getName());
    }
    
    private Team createOrGetTeam(Scoreboard board, Player target) {
        String teamName = TEAM_PREFIX + target.getName().toLowerCase();
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }
        
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }
        
        team.addEntry(target.getName());
        
        return team;
    }
    
    private void updateAllPlayersTeams(Player newPlayer) {
        String prefix = buildPrefix(newPlayer);
        String suffix = buildSuffix(newPlayer);
        
        if (prefix.length() > 64) {
            prefix = prefix.substring(0, 64);
        }
        if (suffix.length() > 64) {
            suffix = suffix.substring(0, 64);
        }
        
        String teamName = TEAM_PREFIX + newPlayer.getName().toLowerCase();
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }
        
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(newPlayer)) continue;
            Scoreboard board = online.getScoreboard();
            if (board != null) {
                Team team = board.getTeam(teamName);
                if (team == null) {
                    team = board.registerNewTeam(teamName);
                    team.addEntry(newPlayer.getName());
                }
                try {
                    team.setPrefix(translateColors(prefix));
                    team.setSuffix(translateColors(suffix));
                } catch (Exception e) {
                    // 忽略
                }
            }
        }
    }
    
    public void updateDisplay(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        String prefix = buildPrefix(player);
        String suffix = buildSuffix(player);
        
        if (prefix.length() > 64) {
            prefix = prefix.substring(0, 64);
        }
        if (suffix.length() > 64) {
            suffix = suffix.substring(0, 64);
        }
        
        UUID playerId = player.getUniqueId();
        String lastP = lastPrefix.get(playerId);
        String lastS = lastSuffix.get(playerId);
        
        if (prefix.equals(lastP) && suffix.equals(lastS)) {
            return;
        }
        
        lastPrefix.put(playerId, prefix);
        lastSuffix.put(playerId, suffix);
        
        String teamName = TEAM_PREFIX + player.getName().toLowerCase();
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }
        
        for (Player online : Bukkit.getOnlinePlayers()) {
            Scoreboard board = online.getScoreboard();
            if (board != null) {
                Team team = board.getTeam(teamName);
                if (team == null) {
                    team = board.registerNewTeam(teamName);
                    team.addEntry(player.getName());
                }
                try {
                    team.setPrefix(translateColors(prefix));
                    team.setSuffix(translateColors(suffix));
                } catch (Exception e) {
                    // 忽略
                }
            }
        }
        
        log("[更新] " + player.getName() + " 前缀: " + prefix + " 后缀: " + suffix);
    }
    
    private void updateDisplayFor(Scoreboard board, Player target) {
        String prefix = buildPrefix(target);
        String suffix = buildSuffix(target);
        
        if (prefix.length() > 64) {
            prefix = prefix.substring(0, 64);
        }
        if (suffix.length() > 64) {
            suffix = suffix.substring(0, 64);
        }
        
        Team team = createOrGetTeam(board, target);
        try {
            team.setPrefix(translateColors(prefix));
            team.setSuffix(translateColors(suffix));
        } catch (Exception e) {
            // 忽略
        }
    }
    
    private String buildPrefix(Player player) {
        if (!showTitle.getOrDefault(player.getUniqueId(), defaultShowTitle)) {
            return "";
        }
        
        String prefix = getLuckPermsPrefix(player);
        if (prefix == null || prefix.isEmpty()) {
            return "";
        }
        
        return prefix;
    }
    
    private String buildSuffix(Player player) {
        StringBuilder suffix = new StringBuilder();
        UUID playerId = player.getUniqueId();
        
        if (showMarriage.getOrDefault(playerId, defaultShowMarriage)) {
            String partnerName = getPartnerName(player);
            if (partnerName != null && !partnerName.isEmpty()) {
                suffix.append(String.format(marriageFormat, partnerName));
            }
        }
        
        return suffix.toString();
    }
    
    private String getLuckPermsPrefix(Player player) {
        if (externalServices == null) {
            return "";
        }
        
        return externalServices.getPlayerPrefix(player);
    }
    
    private String getPartnerName(Player player) {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) {
            return null;
        }
        
        try {
            MarriageService marriageService = rpgCore.getServiceRegistry().getService(MarriageService.class);
            if (marriageService != null) {
                return marriageService.getPartner(player.getUniqueId());
            }
        } catch (Exception e) {
            if (debug) {
                plugin.getLogger().warning("[TitleDisplay] 获取婚姻失败: " + e.getMessage());
            }
        }
        
        return null;
    }
    
    public boolean toggleTitle(Player player) {
        UUID playerId = player.getUniqueId();
        boolean current = showTitle.getOrDefault(playerId, defaultShowTitle);
        boolean newValue = !current;
        showTitle.put(playerId, newValue);
        
        updateDisplay(player);
        
        return newValue;
    }
    
    public boolean toggleGuild(Player player) {
        UUID playerId = player.getUniqueId();
        boolean current = showGuild.getOrDefault(playerId, defaultShowGuild);
        boolean newValue = !current;
        showGuild.put(playerId, newValue);
        
        updateDisplay(player);
        
        return newValue;
    }
    
    public boolean toggleMarriage(Player player) {
        UUID playerId = player.getUniqueId();
        boolean current = showMarriage.getOrDefault(playerId, defaultShowMarriage);
        boolean newValue = !current;
        showMarriage.put(playerId, newValue);
        
        updateDisplay(player);
        
        return newValue;
    }
    
    public String getShowTitleStatus(Player player) {
        boolean status = showTitle.getOrDefault(player.getUniqueId(), defaultShowTitle);
        return status ? "<green>开启" : "<red>关闭";
    }

    public String getShowGuildStatus(Player player) {
        boolean status = showGuild.getOrDefault(player.getUniqueId(), defaultShowGuild);
        return status ? "<green>开启" : "<red>关闭";
    }

    public String getShowMarriageStatus(Player player) {
        boolean status = showMarriage.getOrDefault(player.getUniqueId(), defaultShowMarriage);
        return status ? "<green>开启" : "<red>关闭";
    }
    
    public void cleanupPlayer(Player player) {
        if (player == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        String teamName = TEAM_PREFIX + player.getName().toLowerCase();
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }
        
        for (Player online : Bukkit.getOnlinePlayers()) {
            Scoreboard board = online.getScoreboard();
            if (board != null) {
                try {
                    Team team = board.getTeam(teamName);
                    if (team != null) {
                        team.unregister();
                    }
                } catch (Exception e) {
                    // 忽略
                }
            }
        }
        
        showTitle.remove(playerId);
        showGuild.remove(playerId);
        showMarriage.remove(playerId);
        lastPrefix.remove(playerId);
        lastSuffix.remove(playerId);
    }
    
    public void clear() {
        showTitle.clear();
        showGuild.clear();
        showMarriage.clear();
        lastPrefix.clear();
        lastSuffix.clear();
    }
    
    private String translateColors(String text) {
        if (text == null) return "";
        // 返回原始字符串，由调用方使用 MiniMessageService 解析
        return text;
    }
    
    private void log(String message) {
        if (debug) {
            plugin.getLogger().info("[TitleDisplay] " + message);
        }
    }
    
    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateDisplay(player);
        }
    }
    
    /**
     * 获取玩家前缀
     */
    public String getPrefix(Player player) {
        return buildPrefix(player);
    }
    
    /**
     * 获取玩家后缀
     */
    public String getSuffix(Player player) {
        return buildSuffix(player);
    }
    
    /**
     * 设置玩家是否启用显示
     */
    public void setDisplayEnabled(Player player, boolean enabled) {
        UUID playerId = player.getUniqueId();
        showTitle.put(playerId, enabled);
        showGuild.put(playerId, enabled);
        showMarriage.put(playerId, enabled);
        updateDisplay(player);
    }
    
    /**
     * 检查玩家是否启用显示
     */
    public boolean isDisplayEnabled(UUID playerId) {
        return showTitle.getOrDefault(playerId, defaultShowTitle);
    }
}
