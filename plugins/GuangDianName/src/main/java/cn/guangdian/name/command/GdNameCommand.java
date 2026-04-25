package cn.guangdian.name.command;

import cn.guangdian.name.GuangDianName;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.SubCommand;
import cn.guangdian.rpgcore.command.Description;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * gdname 命令 - 使用 RPGCore CommandFramework
 * 管理命令: reload, status, health, refresh, monitor, debug
 */
@CommandInfo(name = "gdname", description = "GuangDianName 管理命令", permission = "guangdian.name.admin")
public class GdNameCommand extends BaseCommand {

    private final GuangDianName plugin;

    public GdNameCommand(GuangDianName plugin) {
        this.plugin = plugin;
    }

    /**
     * 显示帮助信息
     */
    @SubCommand(name = "")
    @Description("显示帮助信息")
    public void showHelp(CommandContext ctx) {
        CommandSender sender = ctx.getSender();
        ctx.sendMessage("<gold>=== GuangDianName 命令 ===");
        ctx.sendMessage("<green>/gdname reload <gray>- 重载配置文件");
        ctx.sendMessage("<green>/gdname status <gray>- 显示插件状态");
        ctx.sendMessage("<green>/gdname health <gray>- 显示所有玩家血量");
        ctx.sendMessage("<green>/gdname refresh <gray>- 刷新所有玩家显示");
        ctx.sendMessage("<green>/gdname monitor <gray>- 开关实时血量监控日志");
        ctx.sendMessage("<green>/gdname debug <gray>- 开关详细调试日志");
    }

    /**
     * 重载配置
     */
    @SubCommand(name = "reload")
    @Description("重载配置文件")
    public void reload(CommandContext ctx) {
        CommandSender sender = ctx.getSender();
        ctx.sendMessage("<yellow>正在重载配置文件...");

        plugin.reloadConfig();
        plugin.getNameDisplayManager().loadSettings();

        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getNameDisplayManager().removeAllDisplays(player);
            plugin.getNameDisplayManager().initPlayer(player);
        }

        ctx.sendSuccess("配置文件已重载！");
    }

    /**
     * 显示状态
     */
    @SubCommand(name = "status")
    @Description("显示插件状态")
    public void status(CommandContext ctx) {
        CommandSender sender = ctx.getSender();
        ctx.sendMessage("<gold>=== GuangDianName 状态 ===");
        ctx.sendMessage("<green>在线玩家: <white>" + Bukkit.getOnlinePlayers().size());
        String rpgcoreStatus = Bukkit.getPluginManager().isPluginEnabled("RPGCore") ? "<green>已启用" : "<red>未启用";
        ctx.sendMessage("<green>RPGCore: <white>" + rpgcoreStatus);
        ctx.sendMessage("<green>显示模式: <white>全TextDisplay");
    }

    /**
     * 显示血量调试信息
     */
    @SubCommand(name = "health")
    @Description("显示所有玩家血量")
    public void health(CommandContext ctx) {
        CommandSender sender = ctx.getSender();
        ctx.sendMessage("<gold>=== 玩家血量信息 ===");

        for (Player player : Bukkit.getOnlinePlayers()) {
            double health = player.getHealth();
            double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            int displayHealth = (int) Math.ceil(health);

            ctx.sendMessage(String.format("<green>%s: <white>%.1f/%.1f (显示: %d)",
                player.getName(), health, maxHealth, displayHealth));
        }
    }

    /**
     * 刷新所有显示
     */
    @SubCommand(name = "refresh")
    @Description("刷新所有玩家显示")
    public void refresh(CommandContext ctx) {
        CommandSender sender = ctx.getSender();
        ctx.sendMessage("<green>正在刷新所有玩家显示...");

        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getNameDisplayManager().removeAllDisplays(player);
            plugin.getNameDisplayManager().initPlayer(player);
        }

        ctx.sendSuccess("刷新完成！");
    }

    /**
     * 切换监控日志
     */
    @SubCommand(name = "monitor")
    @Description("开关实时血量监控日志")
    public void monitor(CommandContext ctx) {
        CommandSender sender = ctx.getSender();
        boolean currentState = plugin.getHealthMonitor().isEnabled();
        plugin.getHealthMonitor().setEnabled(!currentState);

        String status = !currentState ? "<green>启用" : "<red>禁用";
        ctx.sendMessage("<green>血量监控已" + status);
        ctx.sendMessage("<gray>监控日志将输出到服务器日志文件");
    }

    /**
     * 切换调试模式
     */
    @SubCommand(name = "debug")
    @Description("开关详细调试日志")
    public void debug(CommandContext ctx) {
        CommandSender sender = ctx.getSender();
        boolean currentState = plugin.getNameDisplayManager().isDebug();
        plugin.getNameDisplayManager().setDebug(!currentState);

        String status = !currentState ? "<green>启用" : "<red>禁用";
        ctx.sendMessage("<green>详细调试日志已" + status);
        ctx.sendMessage("<gray>调试日志将输出详细信息");
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        String[] args = context.getArgs();
        String subCommandName = subCommandMethod.getAnnotation(SubCommand.class).name();

        if (subCommandName.isEmpty() && args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            String[] commands = {"reload", "status", "health", "refresh", "monitor", "debug"};
            for (String cmd : commands) {
                if (cmd.startsWith(partial)) {
                    completions.add(cmd);
                }
            }
            return completions;
        }

        return new ArrayList<>();
    }
}
