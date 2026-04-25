package cn.guangdian.points.command;

import cn.guangdian.points.GuangDianPoints;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.Description;
import cn.guangdian.rpgcore.command.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 点卷命令 - 使用 RPGCore CommandFramework
 *
 * <p>基于注解驱动的命令系统，替代传统的 onCommand 方式。</p>
 *
 * @author Astraea RPG Team
 * @since 1.2.0
 */
@CommandInfo(name = "points", description = "点卷系统", permission = "guangdian.points.use")
public class PointsCommand extends BaseCommand {

    private final GuangDianPoints plugin;

    public PointsCommand(GuangDianPoints plugin) {
        this.plugin = plugin;
    }

    /**
     * 查看余额
     */
    @SubCommand(name = "balance")
    @Description("查看点卷余额")
    public void balance(CommandContext ctx) {
        if (ctx.getArgCount() == 0) {
            // 查看自己余额
            Player player = ctx.requirePlayer();
            long balance = plugin.getBalance(player.getUniqueId());
            String balanceMsg = plugin.getConfig().getString("messages.balance-display", "<yellow>你当前有 <gold>%balance% <yellow>点卷")
                .replace("%balance%", formatNumber(balance));
            ctx.sendMessage(balanceMsg);
        } else {
            // 查看他人余额（需要管理员权限）
            ctx.requirePermission("guangdian.points.admin");
            String targetName = ctx.getStringArg(0);
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                ctx.sendError("玩家不在线!");
                return;
            }
            long balance = plugin.getBalance(target.getUniqueId());
            ctx.sendMessage("<yellow>" + target.getName() + " 当前有 <gold>" + formatNumber(balance) + " <yellow>点卷");
        }
    }

    /**
     * 给予点卷
     */
    @SubCommand(name = "give", permission = "guangdian.points.admin", minArgs = 2)
    @Description("给予玩家点卷")
    public void give(CommandContext ctx) {
        String targetName = ctx.getStringArg(0);
        long amount;
        try {
            amount = parseAmount(ctx.getStringArg(1));
        } catch (NumberFormatException e) {
            ctx.sendError("无效的数量!");
            return;
        }

        if (amount <= 0) {
            ctx.sendError("数量必须大于0!");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            ctx.sendError(plugin.getConfig().getString("messages.player-not-found", "玩家不在线!"));
            return;
        }

        UUID adminUuid = ctx.getPlayer() != null ? ctx.getPlayer().getUniqueId() : null;
        plugin.adminGive(target.getUniqueId(), amount, adminUuid);

        String giveMsg = plugin.getConfig().getString("messages.give-success", "<green>已给予 %player% %amount% 点卷!")
            .replace("%player%", target.getName()).replace("%amount%", formatNumber(amount));
        ctx.sendMessage(giveMsg);

        String receiveMsg = plugin.getConfig().getString("messages.receive-points", "<yellow>你收到了 <gold>%amount% <yellow>点卷!")
            .replace("%amount%", formatNumber(amount));
        target.sendMessage(msg.colorize(receiveMsg));
    }

    /**
     * 扣除点卷
     */
    @SubCommand(name = "take", permission = "guangdian.points.admin", minArgs = 2)
    @Description("扣除玩家点卷")
    public void take(CommandContext ctx) {
        String targetName = ctx.getStringArg(0);
        long amount;
        try {
            amount = parseAmount(ctx.getStringArg(1));
        } catch (NumberFormatException e) {
            ctx.sendError("无效的数量!");
            return;
        }

        if (amount <= 0) {
            ctx.sendError("数量必须大于0!");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            ctx.sendError(plugin.getConfig().getString("messages.player-not-found", "玩家不在线!"));
            return;
        }

        UUID adminUuid = ctx.getPlayer() != null ? ctx.getPlayer().getUniqueId() : null;
        if (plugin.adminTake(target.getUniqueId(), amount, adminUuid)) {
            ctx.sendSuccess("已扣除 " + target.getName() + " " + formatNumber(amount) + " 点卷!");
            target.sendMessage(msg.colorize("<red>你被扣除了 " + formatNumber(amount) + " 点卷!"));
        } else {
            ctx.sendError(plugin.getConfig().getString("messages.insufficient-funds", "玩家点卷不足!"));
        }
    }

    /**
     * 设置点卷
     */
    @SubCommand(name = "set", permission = "guangdian.points.admin", minArgs = 2)
    @Description("设置玩家点卷数量")
    public void set(CommandContext ctx) {
        String targetName = ctx.getStringArg(0);
        long amount;
        try {
            amount = parseAmount(ctx.getStringArg(1));
        } catch (NumberFormatException e) {
            ctx.sendError("无效的数量!");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            ctx.sendError(plugin.getConfig().getString("messages.player-not-found", "玩家不在线!"));
            return;
        }

        plugin.setBalance(target.getUniqueId(), amount, "管理员设置");
        ctx.sendSuccess("已设置 " + target.getName() + " 的点卷为 " + formatNumber(amount) + "!");
        target.sendMessage(msg.colorize("<yellow>你的点卷已被设置为 <gold>" + formatNumber(amount) + "<yellow>!"));
    }

    /**
     * 转账
     */
    @SubCommand(name = "pay", playerOnly = true, minArgs = 2)
    @Description("转账给其他玩家")
    public void pay(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        String targetName = ctx.getStringArg(0);
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            ctx.sendError(plugin.getConfig().getString("messages.player-not-found", "玩家不在线!"));
            return;
        }

        if (target.equals(player)) {
            ctx.sendError("不能给自己转账!");
            return;
        }

        long amount;
        try {
            amount = parseAmount(ctx.getStringArg(1));
        } catch (NumberFormatException e) {
            ctx.sendError("无效的数量!");
            return;
        }

        if (amount <= 0) {
            ctx.sendError("数量必须大于0!");
            return;
        }

        if (plugin.transferBalance(player.getUniqueId(), target.getUniqueId(), amount)) {
            String payMsg = plugin.getConfig().getString("messages.pay-success", "<green>已转账 %amount% 点卷给 %player%!")
                .replace("%amount%", formatNumber(amount)).replace("%player%", target.getName());
            ctx.sendMessage(payMsg);
            String receiveMsg = plugin.getConfig().getString("messages.receive-transfer", "<yellow>你收到了 %player% 转账的 <gold>%amount% <yellow>点卷!")
                .replace("%player%", player.getName()).replace("%amount%", formatNumber(amount));
            target.sendMessage(msg.colorize(receiveMsg));
        } else {
            ctx.sendError(plugin.getConfig().getString("messages.insufficient-funds", "点卷不足!"));
        }
    }

    /**
     * 排行榜
     */
    @SubCommand(name = "top")
    @Description("查看点卷排行榜")
    public void top(CommandContext ctx) {
        List<Map.Entry<UUID, Long>> sorted = plugin.getAllBalances().entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(Collectors.toList());

        ctx.sendMessage("<gold>===== 点卷排行榜 =====");
        int count = Math.min(10, sorted.size());
        for (int i = 0; i < count; i++) {
            Map.Entry<UUID, Long> entry = sorted.get(i);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());
            String name = offlinePlayer.getName();
            if (name == null) name = entry.getKey().toString().substring(0, 8);
            ctx.sendMessage("<yellow>" + (i + 1) + ". <white>" + name + " <gray>- <gold>" + formatNumber(entry.getValue()));
        }
    }

    /**
     * 重载配置
     */
    @SubCommand(name = "reload", permission = "guangdian.points.admin")
    @Description("重载插件配置")
    public void reload(CommandContext ctx) {
        plugin.reloadConfig();
        ctx.sendSuccess("配置已重新加载!");
    }

    /**
     * 性能监控
     */
    @SubCommand(name = "perfmon", permission = "guangdian.points.admin", minArgs = 1)
    @Description("性能监控命令")
    public void perfmon(CommandContext ctx) {
        if (plugin.getPerformanceMonitor() == null) {
            ctx.sendError("性能监控未启用!");
            return;
        }

        String subCommand = ctx.getStringArg(0).toLowerCase();

        switch (subCommand) {
            case "status":
                ctx.sendMessage("<gold>===== 性能监控状态 =====");
                ctx.sendMessage(plugin.getPerformanceMonitor().getSummary());
                if (plugin.getLockManager() != null) {
                    ctx.sendMessage("<yellow>" + plugin.getLockManager().getStats().toFormattedString());
                }
                break;
            case "report":
                String[] lines = plugin.getPerformanceMonitor().generateReport().toFormattedString().split("\n");
                for (String line : lines) {
                    ctx.sendMessage("<white>" + line);
                }
                break;
            case "reset":
                plugin.getPerformanceMonitor().reset();
                if (plugin.getLockManager() != null) {
                    plugin.getLockManager().getStats().reset();
                }
                ctx.sendSuccess("性能统计已重置!");
                break;
            case "enable":
                plugin.getPerformanceMonitor().enable();
                ctx.sendSuccess("性能监控已启用!");
                break;
            case "disable":
                plugin.getPerformanceMonitor().disable();
                ctx.sendError("性能监控已禁用!");
                break;
            default:
                ctx.sendError("用法: /points perfmon [status|report|reset|enable|disable]");
        }
    }

    /**
     * 帮助
     */
    @SubCommand(name = "help")
    @Description("显示帮助信息")
    public void help(CommandContext ctx) {
        showHelp(ctx.getSender());
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        List<String> completions = new ArrayList<>();

        if (context.getArgCount() == 0) {
            // 玩家名称补全
            String partial = "";
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (context.getArgCount() == 1) {
            String partial = context.getStringArg(0).toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }

    private long parseAmount(String str) throws NumberFormatException {
        str = str.toLowerCase().replace(",", "");
        if (str.endsWith("k")) {
            return (long) (Double.parseDouble(str.substring(0, str.length() - 1)) * 1000);
        } else if (str.endsWith("m")) {
            return (long) (Double.parseDouble(str.substring(0, str.length() - 1)) * 1000000);
        } else if (str.endsWith("w") || str.endsWith("万")) {
            return (long) (Double.parseDouble(str.substring(0, str.length() - 1)) * 10000);
        }
        return Long.parseLong(str);
    }

    private String formatNumber(long num) {
        if (num >= 100000000) {
            return String.format("%.2f亿", num / 100000000.0);
        } else if (num >= 10000) {
            return String.format("%.2f万", num / 10000.0);
        }
        return String.format("%,d", num);
    }
}
