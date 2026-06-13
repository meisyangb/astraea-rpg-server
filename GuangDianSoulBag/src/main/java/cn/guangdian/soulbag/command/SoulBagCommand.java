package cn.guangdian.soulbag.command;

import cn.guangdian.soulbag.GuangDianSoulBag;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SoulBagCommand implements CommandExecutor, TabCompleter {
    
    private final GuangDianSoulBag plugin;
    
    public SoulBagCommand(GuangDianSoulBag plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("soulbag")) {
            return handleSoulBagCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("soulbagadmin")) {
            return handleAdminCommand(sender, args);
        }
        return false;
    }
    
    private boolean handleSoulBagCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("§c该命令只能由玩家执行！"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("soulbag.use")) {
            String msg = plugin.getConfig().getString("messages.no-permission", "&c你没有权限执行此操作！");
            player.sendMessage(Component.text(msg.replace("&", "§")));
            return true;
        }
        
        if (args.length == 0) {
            plugin.getBagGUI().openBag(player);
            return true;
        }
        
        if (args.length == 1) {
            if (!player.hasPermission("soulbag.admin")) {
                String msg = plugin.getConfig().getString("messages.no-permission", "&c你没有权限执行此操作！");
                player.sendMessage(Component.text(msg.replace("&", "§")));
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                String msg = plugin.getConfig().getString("messages.player-not-found", "&c玩家不存在或未在线！");
                player.sendMessage(Component.text(msg.replace("&", "§")));
                return true;
            }
            
            plugin.getBagGUI().openBag(player, target.getUniqueId());
            return true;
        }
        
        return false;
    }
    
    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("soulbag.admin")) {
            sender.sendMessage(Component.text("§c你没有权限执行此操作！"));
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage(Component.text("§e用法: /soulbagadmin <reload|clear|view> [player]"));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                plugin.reloadConfig();
                plugin.getBagManager().loadConfiguration();
                String reloadMsg = plugin.getConfig().getString("messages.reload-success", "&a配置文件已重新加载！");
                sender.sendMessage(Component.text(reloadMsg.replace("&", "§")));
                break;
                
            case "clear":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("§c用法: /soulbagadmin clear <player>"));
                    return true;
                }
                
                Player clearTarget = Bukkit.getPlayer(args[1]);
                if (clearTarget == null) {
                    String msg = plugin.getConfig().getString("messages.player-not-found", "&c玩家不存在或未在线！");
                    sender.sendMessage(Component.text(msg.replace("&", "§")));
                    return true;
                }
                
                plugin.getBagManager().clearBag(clearTarget.getUniqueId());
                String clearMsg = plugin.getConfig().getString("messages.clear-success", "&a灵魂背包已清空！");
                sender.sendMessage(Component.text(clearMsg.replace("&", "§")));
                break;
                
            case "view":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("§c用法: /soulbagadmin view <player>"));
                    return true;
                }
                
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Component.text("§c该命令只能由玩家执行！"));
                    return true;
                }
                
                Player viewer = (Player) sender;
                Player viewTarget = Bukkit.getPlayer(args[1]);
                if (viewTarget == null) {
                    String msg = plugin.getConfig().getString("messages.player-not-found", "&c玩家不存在或未在线！");
                    viewer.sendMessage(Component.text(msg.replace("&", "§")));
                    return true;
                }
                
                plugin.getBagGUI().openBag(viewer, viewTarget.getUniqueId());
                break;
                
            default:
                sender.sendMessage(Component.text("§c未知的子命令！"));
                sender.sendMessage(Component.text("§e用法: /soulbagadmin <reload|clear|view> [player]"));
                break;
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("soulbag")) {
            if (args.length == 1 && sender.hasPermission("soulbag.admin")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            }
        } else if (command.getName().equalsIgnoreCase("soulbagadmin")) {
            if (args.length == 1) {
                List<String> subCommands = Arrays.asList("reload", "clear", "view");
                for (String sub : subCommands) {
                    if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(sub);
                    }
                }
            } else if (args.length == 2 && (args[0].equalsIgnoreCase("clear") || args[0].equalsIgnoreCase("view"))) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            }
        }
        
        return completions;
    }
}
