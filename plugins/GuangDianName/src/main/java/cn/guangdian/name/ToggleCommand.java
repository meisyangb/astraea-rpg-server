package cn.guangdian.name;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleCommand implements CommandExecutor {
    
    private final GuangDianName plugin;
    private final TitleDisplay titleDisplay;
    
    public ToggleCommand(GuangDianName plugin, TitleDisplay titleDisplay) {
        this.plugin = plugin;
        this.titleDisplay = titleDisplay;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("该命令只能由玩家执行!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "title":
                handleToggleTitle(player);
                break;
            case "guild":
                handleToggleGuild(player);
                break;
            case "marriage":
                handleToggleMarriage(player);
                break;
            case "status":
                handleStatus(player);
                break;
            case "help":
                sendHelp(player);
                break;
            default:
                player.sendMessage(ChatColor.RED + "未知的子命令! 使用 /gdnametoggle help 查看帮助");
                break;
        }
        
        return true;
    }
    
    private void handleToggleTitle(Player player) {
        boolean newState = titleDisplay.toggleTitle(player);
        String status = newState ? "&a开启" : "&c关闭";
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&e[头顶显示] &7称号显示已" + status));
    }
    
    private void handleToggleGuild(Player player) {
        boolean newState = plugin.getTextDisplayManager().toggleShowGuild(player);
        String status = newState ? "&a开启" : "&c关闭";
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&e[头顶显示] &7工会显示已" + status));
    }
    
    private void handleToggleMarriage(Player player) {
        boolean newState = titleDisplay.toggleMarriage(player);
        String status = newState ? "&a开启" : "&c关闭";
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&e[头顶显示] &7婚姻显示已" + status));
    }
    
    private void handleStatus(Player player) {
        String titleStatus = titleDisplay.getShowTitleStatus(player);
        String guildStatus = plugin.getTextDisplayManager().isShowGuild(player) ? "&a开启" : "&c关闭";
        String marriageStatus = titleDisplay.getShowMarriageStatus(player);
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&6===== 头顶显示状态 ====="));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&e称号显示: " + titleStatus));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&e工会显示: " + guildStatus));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&e婚姻显示: " + marriageStatus));
    }
    
    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&6===== 头顶显示帮助 ====="));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&e/gdnametoggle title &7- 切换称号显示"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&e/gdnametoggle guild &7- 切换工会显示"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&e/gdnametoggle marriage &7- 切换婚姻显示"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&e/gdnametoggle status &7- 查看当前状态"));
    }
}
