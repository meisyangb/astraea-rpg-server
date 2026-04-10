package cn.guangdian.name;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * 调试命令处理器
 */
public class DebugCommand implements CommandExecutor {
    
    private final GuangDianName plugin;
    
    public DebugCommand(GuangDianName plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                reloadConfig(sender);
                break;
            case "status":
                sendStatus(sender);
                break;
            case "health":
                sendHealthDebug(sender);
                break;
            case "scoreboard":
                sendScoreboardDebug(sender);
                break;
            case "cache":
                sendCacheDebug(sender);
                break;
            case "refresh":
                refreshAll(sender);
                break;
            case "monitor":
                toggleMonitor(sender);
                break;
            case "debug":
                toggleDebug(sender);
                break;
            default:
                sendHelp(sender);
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== GuangDianName 命令 ===");
        sender.sendMessage(ChatColor.GREEN + "/gdname reload " + ChatColor.GRAY + "- 重载配置文件");
        sender.sendMessage(ChatColor.GREEN + "/gdname status " + ChatColor.GRAY + "- 显示插件状态");
        sender.sendMessage(ChatColor.GREEN + "/gdname health " + ChatColor.GRAY + "- 显示所有玩家血量");
        sender.sendMessage(ChatColor.GREEN + "/gdname scoreboard " + ChatColor.GRAY + "- 显示 Scoreboard 信息");
        sender.sendMessage(ChatColor.GREEN + "/gdname cache " + ChatColor.GRAY + "- 显示缓存信息");
        sender.sendMessage(ChatColor.GREEN + "/gdname refresh " + ChatColor.GRAY + "- 刷新所有玩家显示");
        sender.sendMessage(ChatColor.GREEN + "/gdname monitor " + ChatColor.GRAY + "- 开关实时血量监控日志");
        sender.sendMessage(ChatColor.GREEN + "/gdname debug " + ChatColor.GRAY + "- 开关详细调试日志");
    }
    
    private void reloadConfig(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "正在重载配置文件...");
        
        plugin.reloadConfig();
        
        plugin.getHealthDisplay().loadConfig();
        plugin.getTitleDisplay().loadSettings();
        plugin.getTextDisplayManager().loadSettings();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getTextDisplayManager().removeTextDisplay(player);
            plugin.getTextDisplayManager().createTextDisplay(player);
            plugin.getTitleDisplay().updateDisplay(player);
        }
        
        sender.sendMessage(ChatColor.GREEN + "配置文件已重载！");
        sender.sendMessage(ChatColor.GRAY + "工会显示高度: " + plugin.getTextDisplayManager().displayHeight);
    }
    
    private void sendStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== GuangDianName 状态 ===");
        sender.sendMessage(ChatColor.GREEN + "在线玩家: " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size());
        sender.sendMessage(ChatColor.GREEN + "缓存数量: " + ChatColor.WHITE + plugin.getHealthDisplay().getCacheSize());
        sender.sendMessage(ChatColor.GREEN + "RPGCore: " + ChatColor.WHITE + (Bukkit.getPluginManager().isPluginEnabled("RPGCore") ? ChatColor.GREEN + "已启用" : ChatColor.RED + "未启用"));
        sender.sendMessage(ChatColor.GREEN + "工会显示高度: " + ChatColor.WHITE + plugin.getTextDisplayManager().displayHeight);
    }
    
    private void sendHealthDebug(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== 玩家血量信息 ===");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            double health = player.getHealth();
            double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            int displayHealth = (int) Math.ceil(health);
            
            sender.sendMessage(String.format(ChatColor.GREEN + "%s: " + ChatColor.WHITE + "%.1f/%.1f (显示: %d)", 
                player.getName(), health, maxHealth, displayHealth));
        }
    }
    
    private void sendScoreboardDebug(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Scoreboard 信息 ===");
        
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard board = viewer.getScoreboard();
            if (board == null) {
                sender.sendMessage(ChatColor.RED + viewer.getName() + ": 无 Scoreboard");
                continue;
            }
            
            Objective objective = board.getObjective("gdnhealth");
            if (objective == null) {
                sender.sendMessage(ChatColor.RED + viewer.getName() + ": 无 gdnhealth Objective");
                continue;
            }
            
            sender.sendMessage(ChatColor.GREEN + viewer.getName() + ":");
            sender.sendMessage("  " + ChatColor.GRAY + "DisplaySlot: " + ChatColor.WHITE + objective.getDisplaySlot());
            sender.sendMessage("  " + ChatColor.GRAY + "DisplayName: " + ChatColor.WHITE + objective.getDisplayName());
            
            for (Player target : Bukkit.getOnlinePlayers()) {
                int score = objective.getScore(target.getName()).getScore();
                sender.sendMessage("  " + ChatColor.GRAY + "- " + target.getName() + ": " + ChatColor.WHITE + score);
            }
        }
    }
    
    private void sendCacheDebug(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== 缓存信息 ===");
        sender.sendMessage(ChatColor.GREEN + "缓存数量: " + ChatColor.WHITE + plugin.getHealthDisplay().getCacheSize());
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            Integer cachedHealth = plugin.getHealthDisplay().getCachedHealth(player.getUniqueId());
            if (cachedHealth != null) {
                sender.sendMessage(ChatColor.GREEN + player.getName() + ": " + ChatColor.WHITE + cachedHealth);
            } else {
                sender.sendMessage(ChatColor.RED + player.getName() + ": 未缓存");
            }
        }
    }
    
    private void refreshAll(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "正在刷新所有玩家显示...");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getHealthDisplay().updateHealth(player);
            plugin.getTitleDisplay().updateDisplay(player);
            plugin.getTextDisplayManager().updatePlayerTextDisplay(player);
        }
        
        sender.sendMessage(ChatColor.GREEN + "刷新完成！");
    }
    
    private void toggleMonitor(CommandSender sender) {
        boolean currentState = plugin.getHealthMonitor().isEnabled();
        plugin.getHealthMonitor().setEnabled(!currentState);
        
        sender.sendMessage(ChatColor.GREEN + "血量监控已" + (!currentState ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        sender.sendMessage(ChatColor.GRAY + "监控日志将输出到服务器日志文件");
    }
    
    private void toggleDebug(CommandSender sender) {
        boolean currentState = plugin.getHealthDisplay().isDebug();
        plugin.getHealthDisplay().setDebug(!currentState);
        plugin.getTitleDisplay().setDebug(!currentState);
        plugin.getTextDisplayManager().setDebug(!currentState);
        
        sender.sendMessage(ChatColor.GREEN + "详细调试日志已" + (!currentState ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        sender.sendMessage(ChatColor.GRAY + "调试日志将输出详细信息");
    }
}
