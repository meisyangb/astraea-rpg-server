package cn.guangdian.points.command;

import cn.guangdian.points.GuangDianPoints;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 积分命令处理器 - 使用统一命令框架
 * 
 * <p>示例展示如何迁移到RPGCore统一命令框架。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class PointsCommandHandler {

    private final GuangDianPoints plugin;

    public PointsCommandHandler(GuangDianPoints plugin) {
        this.plugin = plugin;
    }

    /**
     * 查看余额命令
     */
    public void handleBalance(CommandSender sender, String[] args) {
        if (args.length == 0) {
            // 查看自己余额
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("只有玩家可以查看自己的余额！", NamedTextColor.RED));
                return;
            }
            long balance = plugin.getBalance(player.getUniqueId());
            sender.sendMessage(Component.text("你的点券余额: ", NamedTextColor.GREEN)
                .append(Component.text(balance, NamedTextColor.GOLD)));
        } else {
            // 查看他人余额（需要管理员权限）
            if (!sender.hasPermission("guangdian.points.admin")) {
                sender.sendMessage(Component.text("你没有权限查看他人余额！", NamedTextColor.RED));
                return;
            }
            String targetName = args[0];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            long balance = plugin.getBalance(target.getUniqueId());
            sender.sendMessage(Component.text(targetName + " 的点券余额: ", NamedTextColor.GREEN)
                .append(Component.text(balance, NamedTextColor.GOLD)));
        }
    }

    /**
     * 给予点券命令
     */
    public void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.points.admin")) {
            sender.sendMessage(Component.text("你没有权限执行此命令！", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /points give <玩家> <数量>", NamedTextColor.RED));
            return;
        }

        String targetName = args[0];
        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("无效的数量！", NamedTextColor.RED));
            return;
        }

        if (amount <= 0) {
            sender.sendMessage(Component.text("数量必须大于0！", NamedTextColor.RED));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        long oldBalance = plugin.getBalance(target.getUniqueId());
        plugin.setBalance(target.getUniqueId(), oldBalance + amount, "admin-give:" + sender.getName());

        sender.sendMessage(Component.text("已给予 ", NamedTextColor.GREEN)
            .append(Component.text(targetName, NamedTextColor.YELLOW))
            .append(Component.text(" " + amount + " 点券！", NamedTextColor.GREEN)));

        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(Component.text("你收到了 ", NamedTextColor.GREEN)
                .append(Component.text(amount + " 点券！", NamedTextColor.GOLD)));
        }
    }

    /**
     * 扣除点券命令
     */
    public void handleTake(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.points.admin")) {
            sender.sendMessage(Component.text("你没有权限执行此命令！", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /points take <玩家> <数量>", NamedTextColor.RED));
            return;
        }

        String targetName = args[0];
        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("无效的数量！", NamedTextColor.RED));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        long oldBalance = plugin.getBalance(target.getUniqueId());
        
        if (oldBalance < amount) {
            sender.sendMessage(Component.text("玩家余额不足！", NamedTextColor.RED));
            return;
        }

        plugin.setBalance(target.getUniqueId(), oldBalance - amount, "admin-take:" + sender.getName());

        sender.sendMessage(Component.text("已扣除 ", NamedTextColor.GREEN)
            .append(Component.text(targetName, NamedTextColor.YELLOW))
            .append(Component.text(" " + amount + " 点券！", NamedTextColor.GREEN)));
    }

    /**
     * 重载配置命令
     */
    public void handleReload(CommandSender sender) {
        if (!sender.hasPermission("guangdian.points.admin")) {
            sender.sendMessage(Component.text("你没有权限执行此命令！", NamedTextColor.RED));
            return;
        }

        plugin.reloadConfig();
        sender.sendMessage(Component.text("配置已重载！", NamedTextColor.GREEN));
    }

    /**
     * 帮助命令
     */
    public void handleHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== 光点点券帮助 ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/points [玩家] - 查看余额", NamedTextColor.YELLOW));
        
        if (sender.hasPermission("guangdian.points.admin")) {
            sender.sendMessage(Component.text("/points give <玩家> <数量> - 给予点券", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/points take <玩家> <数量> - 扣除点券", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/points reload - 重载配置", NamedTextColor.YELLOW));
        }
    }

    /**
     * Tab补全
     */
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            
            // 基础命令
            if ("balance".startsWith(partial)) completions.add("balance");
            if ("help".startsWith(partial)) completions.add("help");
            
            // 管理员命令
            if (sender.hasPermission("guangdian.points.admin")) {
                if ("give".startsWith(partial)) completions.add("give");
                if ("take".startsWith(partial)) completions.add("take");
                if ("reload".startsWith(partial)) completions.add("reload");
            }
        } else if (args.length == 2) {
            // 玩家名称补全
            String partial = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }
}