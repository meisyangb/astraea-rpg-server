package cn.guangdian.menu.command;

import cn.guangdian.menu.GuangDianMenu;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.SubCommand;
import cn.guangdian.rpgcore.command.Description;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单管理命令 - 使用 RPGCore CommandFramework
 */
@CommandInfo(name = "guangdianmenu", description = "菜单插件管理", permission = "guangdian.menu.admin")
public class GuangDianMenuCommand extends BaseCommand {

    private final GuangDianMenu plugin;

    public GuangDianMenuCommand(GuangDianMenu plugin) {
        this.plugin = plugin;
    }

    /**
     * 显示帮助信息
     */
    @SubCommand(name = "")
    @Description("显示帮助信息")
    public void showHelp(CommandContext ctx) {
        // 调用父类的帮助显示方法
        super.showHelp(ctx.getSender());
    }

    /**
     * 重新加载配置
     */
    @SubCommand(name = "reload", permission = "guangdian.menu.admin")
    @Description("重新加载菜单配置")
    public void reload(CommandContext ctx) {
        CommandSender sender = ctx.getSender();

        plugin.reloadConfig();
        plugin.reloadMenus();

        String message = plugin.getConfig().getString("messages.config-reloaded", "<green>菜单配置已重新加载!");
        msg.send(sender, message);
    }

    /**
     * 给予玩家菜单物品
     */
    @SubCommand(name = "give", permission = "guangdian.menu.admin", minArgs = 0, maxArgs = 1)
    @Description("给予玩家菜单物品")
    public void give(CommandContext ctx) {
        CommandSender sender = ctx.getSender();
        String[] args = ctx.getArgs();

        if (args.length >= 1) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                msg.sendError(sender, "玩家不在线或不存在!");
                return;
            }
            plugin.giveMenuItem(target, false);
            msg.sendSuccess(sender, "已发放主菜单物品给玩家: <yellow>" + target.getName());
            return;
        }

        if (sender instanceof Player player) {
            plugin.giveMenuItem(player, false);
            msg.sendSuccess(sender, "已发放主菜单物品!");
            return;
        }

        msg.sendWarning(sender, "用法: /guangdianmenu give <玩家>");
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        String[] args = context.getArgs();
        String subCommandName = subCommandMethod.getAnnotation(SubCommand.class).name();

        if (subCommandName.equals("give") && args.length == 1) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
