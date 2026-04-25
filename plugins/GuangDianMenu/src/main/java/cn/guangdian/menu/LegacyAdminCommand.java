package cn.guangdian.menu;

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
 * 传统管理命令处理器 (降级处理 - 当 RPGCore 不可用时使用)
 */
public class LegacyAdminCommand implements CommandExecutor, TabCompleter {

    private final GuangDianMenu plugin;

    public LegacyAdminCommand(GuangDianMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("guangdian.menu.admin")) {
            sender.sendMessage("§c您没有权限执行此操作!");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.reloadMenus();
            sender.sendMessage("§a菜单配置已重新加载!");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
            if (args.length >= 2) {
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§c玩家不在线或不存在!");
                    return true;
                }
                plugin.giveMenuItem(target, false);
                sender.sendMessage("§a已发放主菜单物品给玩家: §e" + target.getName());
                return true;
            }

            if (sender instanceof Player player) {
                plugin.giveMenuItem(player, false);
                sender.sendMessage("§a已发放主菜单物品!");
                return true;
            }

            sender.sendMessage("§e用法: /guangdianmenu give <玩家>");
            return true;
        }

        sender.sendMessage("§e用法: /guangdianmenu reload|give [玩家]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("reload");
            completions.add("give");
            return completions.stream()
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String partial = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
