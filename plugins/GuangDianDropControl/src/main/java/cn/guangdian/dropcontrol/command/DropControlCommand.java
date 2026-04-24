package cn.guangdian.dropcontrol.command;

import cn.guangdian.dropcontrol.GuangDianDropControl;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.Description;
import cn.guangdian.rpgcore.command.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 丢弃控制命令 - 使用 RPGCore CommandFramework
 *
 * @author Astraea RPG Team
 * @since 1.2.0
 */
@CommandInfo(name = "gddrop", description = "丢弃控制", permission = "gddrop.use")
public class DropControlCommand extends BaseCommand {
    private final GuangDianDropControl plugin;

    public DropControlCommand(GuangDianDropControl plugin) {
        this.plugin = plugin;
    }

    /**
     * 显示帮助信息（默认子命令）
     */
    @SubCommand(name = "")
    @Description("显示帮助信息")
    public void showHelpDefault(CommandContext ctx) {
        showHelp(ctx.getSender());
    }

    /**
     * 切换自己的丢弃状态
     */
    @SubCommand(name = "toggle", playerOnly = true)
    @Description("切换丢弃功能")
    public void toggle(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        UUID playerUuid = player.getUniqueId();

        Boolean currentStatus = plugin.getPlayerDropStatus().get(playerUuid);
        if (currentStatus == null) {
            currentStatus = plugin.isPlayerDefaultEnabled();
        }

        boolean newStatus = !currentStatus;
        plugin.getPlayerDropStatus().put(playerUuid, newStatus);

        String statusText = newStatus ? "<green>启用" : "<red>禁用";
        ctx.sendSuccess("你已将丢弃功能切换为: " + statusText);
    }

    /**
     * 查看自己的丢弃状态
     */
    @SubCommand(name = "status", playerOnly = true)
    @Description("查看丢弃状态")
    public void status(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        UUID uuid = player.getUniqueId();
        Boolean status = plugin.getPlayerDropStatus().get(uuid);
        String statusText;
        if (status != null) {
            statusText = status ? "<green>启用" : "<red>禁用";
        } else {
            statusText = plugin.isPlayerDefaultEnabled() ? "<green>启用 (默认)" : "<red>禁用 (默认)";
        }
        ctx.sendMessage("<yellow>当前丢弃状态: " + statusText);
    }

    /**
     * 启用全局丢弃（管理员）
     */
    @SubCommand(name = "enable", permission = "gddrop.admin")
    @Description("启用全局丢弃")
    public void enable(CommandContext ctx) {
        plugin.setDropEnabled(true);
        Bukkit.getServer().broadcast(msg.colorize("<green>物品丢弃已启用!"));
    }

    /**
     * 禁用全局丢弃（管理员）
     */
    @SubCommand(name = "disable", permission = "gddrop.admin")
    @Description("禁用全局丢弃")
    public void disable(CommandContext ctx) {
        plugin.setDropEnabled(false);
        Bukkit.getServer().broadcast(msg.colorize("<red>物品丢弃已禁用!"));
    }

    /**
     * 切换玩家的丢弃状态（管理员）
     */
    @SubCommand(name = "player", permission = "gddrop.admin", minArgs = 1)
    @Description("切换玩家的丢弃状态")
    public void player(CommandContext ctx) {
        String targetName = ctx.getStringArg(0);
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            ctx.sendError("玩家不存在或不在线!");
            return;
        }

        UUID targetUuid = target.getUniqueId();
        Boolean targetStatus = plugin.getPlayerDropStatus().get(targetUuid);

        if (targetStatus == null) {
            targetStatus = plugin.isDropEnabled();
        }

        boolean newTargetStatus = !targetStatus;
        plugin.getPlayerDropStatus().put(targetUuid, newTargetStatus);

        if (newTargetStatus) {
            ctx.sendSuccess("已允许 " + target.getName() + " 丢弃物品");
        } else {
            ctx.sendSuccess("已禁止 " + target.getName() + " 丢弃物品");
        }
    }

    /**
     * 重新加载配置（管理员）
     */
    @SubCommand(name = "reload", permission = "gddrop.admin")
    @Description("重新加载配置")
    public void reload(CommandContext ctx) {
        plugin.reloadConfig();
        ctx.sendSuccess("配置已重新加载!");
    }

    /**
     * 查看全局丢弃状态（管理员）
     */
    @SubCommand(name = "globalstatus", permission = "gddrop.admin")
    @Description("查看全局丢弃状态")
    public void globalStatus(CommandContext ctx) {
        String status = plugin.isDropEnabled() ? "<green>启用" : "<red>禁用";
        ctx.sendMessage("<yellow>当前全局丢弃状态: " + status);
    }

    /**
     * 显示帮助
     */
    @SubCommand(name = "help")
    @Description("显示帮助信息")
    public void help(CommandContext ctx) {
        showHelp(ctx.getSender());
    }

    @Override
    public void showHelp(org.bukkit.command.CommandSender sender) {
        sender.sendMessage(msg.colorize("<gold><bold>===== 丢弃控制 ====="));
        sender.sendMessage(msg.colorize("<yellow>/gddrop toggle <gray>- 切换丢弃功能"));
        sender.sendMessage(msg.colorize("<yellow>/gddrop status <gray>- 查看丢弃状态"));
        sender.sendMessage(msg.colorize("<yellow>/gddrop help <gray>- 显示帮助信息"));

        if (sender.hasPermission("gddrop.admin")) {
            sender.sendMessage(msg.colorize("<yellow>/gddrop enable <gray>- 启用全局丢弃"));
            sender.sendMessage(msg.colorize("<yellow>/gddrop disable <gray>- 禁用全局丢弃"));
            sender.sendMessage(msg.colorize("<yellow>/gddrop player <玩家> <gray>- 切换玩家的丢弃状态"));
            sender.sendMessage(msg.colorize("<yellow>/gddrop reload <gray>- 重新加载配置"));
            sender.sendMessage(msg.colorize("<yellow>/gddrop globalstatus <gray>- 查看全局丢弃状态"));
        }
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        List<String> completions = new ArrayList<>();
        String subCommandName = subCommandMethod.getAnnotation(SubCommand.class).name();

        if (context.getArgCount() == 0) {
            // 普通玩家可以看到 toggle 和 status
            completions.addAll(List.of("toggle", "status", "help"));
            // 管理员可以看到所有命令
            if (context.hasPermission("gddrop.admin")) {
                completions.addAll(List.of("enable", "disable", "player", "reload", "globalstatus"));
            }
        } else if (subCommandName.equals("player") && context.hasPermission("gddrop.admin")) {
            if (context.getArgCount() == 1) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    completions.add(online.getName());
                }
            }
        }

        String lastArg = context.getStringArgOrDefault(context.getArgCount() - 1, "").toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(lastArg))
            .collect(Collectors.toList());
    }
}
