package cn.guangdian.signin.command;

import cn.guangdian.signin.GuangDianSignIn;
import cn.guangdian.signin.api.SignInService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SignInCommand implements CommandExecutor, TabCompleter {
    
    private final GuangDianSignIn plugin;
    
    public SignInCommand(GuangDianSignIn plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("该命令只能由玩家执行")
                    .color(NamedTextColor.RED));
                return true;
            }
            
            Player player = (Player) sender;
            SignInService service = plugin.getSignInService();
            
            if (!service.canSignIn(player.getUniqueId())) {
                player.sendMessage(Component.text("你今天已经签到过了！")
                    .color(NamedTextColor.RED));
                return true;
            }
            
            service.signIn(player.getUniqueId());
            return true;
        }
        
        if (args[0].equalsIgnoreCase("info")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("该命令只能由玩家执行")
                    .color(NamedTextColor.RED));
                return true;
            }
            
            Player player = (Player) sender;
            SignInService service = plugin.getSignInService();
            
            int consecutive = service.getConsecutiveDays(player.getUniqueId());
            int total = service.getTotalDays(player.getUniqueId());
            
            player.sendMessage(Component.text("===== 签到信息 =====")
                .color(NamedTextColor.GOLD));
            player.sendMessage(Component.text("连续签到: ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(consecutive)
                    .color(NamedTextColor.GREEN))
                .append(Component.text(" 天")));
            player.sendMessage(Component.text("累计签到: ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(total)
                    .color(NamedTextColor.GREEN))
                .append(Component.text(" 天")));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("guangdian.signin.admin")) {
                sender.sendMessage(Component.text("你没有权限执行此命令")
                    .color(NamedTextColor.RED));
                return true;
            }
            
            plugin.getConfigManager().loadConfig();
            sender.sendMessage(Component.text("配置已重新加载")
                .color(NamedTextColor.GREEN));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("guangdian.signin.admin")) {
                sender.sendMessage(Component.text("你没有权限执行此命令")
                    .color(NamedTextColor.RED));
                return true;
            }
            
            if (args.length < 2) {
                sender.sendMessage(Component.text("用法: /signin reset <玩家>")
                    .color(NamedTextColor.RED));
                return true;
            }
            
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            SignInService service = plugin.getSignInService();
            service.resetConsecutiveDays(target.getUniqueId());
            
            sender.sendMessage(Component.text("已重置玩家 ")
                .color(NamedTextColor.GREEN)
                .append(Component.text(args[1])
                    .color(NamedTextColor.YELLOW))
                .append(Component.text(" 的连续签到天数")));
            return true;
        }
        
        sender.sendMessage(Component.text("未知命令，使用 /signin 查看帮助")
            .color(NamedTextColor.RED));
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("info");
            if (sender.hasPermission("guangdian.signin.admin")) {
                completions.add("reload");
                completions.add("reset");
            }
            return completions;
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
            return completions;
        }
        
        return completions;
    }
}
