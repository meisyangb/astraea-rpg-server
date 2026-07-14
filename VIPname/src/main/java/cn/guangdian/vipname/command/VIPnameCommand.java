package cn.guangdian.vipname.command;

import cn.guangdian.vipname.VIPname;
import cn.guangdian.vipname.model.Title;
import cn.guangdian.vipname.model.PlayerTitle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * VIPname 命令处理
 */
public class VIPnameCommand implements CommandExecutor, TabCompleter {

    private final VIPname plugin;

    public VIPnameCommand(VIPname plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "list":
                return handleList(sender);
            case "set":
                return handleSet(sender, args);
            case "clear":
                return handleClear(sender);
            case "grant":
                return handleGrant(sender, args);
            case "remove":
                return handleRemove(sender, args);
            case "reload":
                return handleReload(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("========== VIPname 称号系统 ==========", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/vipname list", NamedTextColor.YELLOW)
            .append(Component.text(" - 查看可用称号", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/vipname set <称号>", NamedTextColor.YELLOW)
            .append(Component.text(" - 设置当前称号", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/vipname clear", NamedTextColor.YELLOW)
            .append(Component.text(" - 清除当前称号", NamedTextColor.WHITE)));
        if (sender.hasPermission("vipname.admin")) {
            sender.sendMessage(Component.text("/vipname grant <玩家> <称号>", NamedTextColor.YELLOW)
                .append(Component.text(" - 授予称号", NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("/vipname remove <玩家> <称号>", NamedTextColor.YELLOW)
                .append(Component.text(" - 移除称号", NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("/vipname reload", NamedTextColor.YELLOW)
                .append(Component.text(" - 重载配置", NamedTextColor.WHITE)));
        }
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(Component.text("========== 可用称号 ==========", NamedTextColor.GOLD));
        
        if (sender instanceof Player player) {
            PlayerTitle pt = plugin.getTitleManager().getPlayerData(player.getUniqueId());
            
            for (Title title : plugin.getTitleManager().getAllTitles()) {
                boolean owned = pt.hasTitle(title.getId());
                boolean current = title.getId().equalsIgnoreCase(pt.getCurrentTitle());
                
                Component line = Component.text("  - ", NamedTextColor.GRAY)
                    .append(VIPname.color(title.getFullDisplay()))
                    .append(Component.text(" (" + title.getId() + ")", NamedTextColor.DARK_GRAY));
                
                if (current) {
                    line = line.append(Component.text(" [当前]", NamedTextColor.GREEN));
                } else if (owned) {
                    line = line.append(Component.text(" [已获得]", NamedTextColor.YELLOW));
                } else {
                    line = line.append(Component.text(" [未获得]", NamedTextColor.RED));
                }
                
                sender.sendMessage(line);
            }
        } else {
            for (Title title : plugin.getTitleManager().getAllTitles()) {
                sender.sendMessage(Component.text("  - ", NamedTextColor.GRAY)
                    .append(VIPname.color(title.getFullDisplay()))
                    .append(Component.text(" (" + title.getId() + ")", NamedTextColor.DARK_GRAY)));
            }
        }
        
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该命令只能由玩家执行", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /vipname set <称号ID>", NamedTextColor.RED));
            return true;
        }
        
        String titleId = args[1];
        Title title = plugin.getTitleManager().getTitle(titleId);
        
        if (title == null) {
            sender.sendMessage(Component.text("称号不存在: " + titleId, NamedTextColor.RED));
            return true;
        }
        
        if (plugin.getTitleManager().setCurrentTitle(player.getUniqueId(), titleId)) {
            sender.sendMessage(Component.text("已设置称号为: ", NamedTextColor.GREEN)
                .append(VIPname.color(title.getFullDisplay())));
        } else {
            sender.sendMessage(Component.text("你还没有获得该称号", NamedTextColor.RED));
        }
        
        return true;
    }

    private boolean handleClear(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该命令只能由玩家执行", NamedTextColor.RED));
            return true;
        }
        
        plugin.getTitleManager().setCurrentTitle(player.getUniqueId(), null);
        sender.sendMessage(Component.text("已清除称号", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleGrant(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vipname.admin")) {
            sender.sendMessage(Component.text("没有权限", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(Component.text("用法: /vipname grant <玩家> <称号ID>", NamedTextColor.RED));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("玩家不在线", NamedTextColor.RED));
            return true;
        }
        
        String titleId = args[2];
        Title title = plugin.getTitleManager().getTitle(titleId);
        if (title == null) {
            sender.sendMessage(Component.text("称号不存在: " + titleId, NamedTextColor.RED));
            return true;
        }
        
        plugin.getTitleManager().grantTitle(target.getUniqueId(), titleId);
        sender.sendMessage(Component.text("已授予 " + target.getName() + " 称号: ", NamedTextColor.GREEN)
            .append(VIPname.color(title.getFullDisplay())));
        target.sendMessage(Component.text("你获得了称号: ", NamedTextColor.GREEN)
            .append(VIPname.color(title.getFullDisplay())));
        
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vipname.admin")) {
            sender.sendMessage(Component.text("没有权限", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(Component.text("用法: /vipname remove <玩家> <称号ID>", NamedTextColor.RED));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("玩家不在线", NamedTextColor.RED));
            return true;
        }
        
        String titleId = args[2];
        
        if (plugin.getTitleManager().removeTitle(target.getUniqueId(), titleId)) {
            sender.sendMessage(Component.text("已移除 " + target.getName() + " 的称号: " + titleId, NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("玩家没有该称号", NamedTextColor.RED));
        }
        
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("vipname.admin")) {
            sender.sendMessage(Component.text("没有权限", NamedTextColor.RED));
            return true;
        }
        
        plugin.reload();
        sender.sendMessage(Component.text("配置已重载", NamedTextColor.GREEN));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("list");
            completions.add("set");
            completions.add("clear");
            if (sender.hasPermission("vipname.admin")) {
                completions.add("grant");
                completions.add("remove");
                completions.add("reload");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            if (sender instanceof Player player) {
                PlayerTitle pt = plugin.getTitleManager().getPlayerData(player.getUniqueId());
                completions.addAll(pt.getOwnedTitles());
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("grant") || args[0].equalsIgnoreCase("remove"))) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("grant") || args[0].equalsIgnoreCase("remove"))) {
            completions.addAll(plugin.getTitleManager().getAllTitles().stream()
                .map(Title::getId)
                .collect(Collectors.toList()));
        }
        
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}