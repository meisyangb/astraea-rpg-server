package cn.guangdian.chat.command;

import cn.guangdian.chat.GuangDianChat;
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
 * 聊天插件命令 - 使用 RPGCore CommandFramework
 *
 * <p>提供聊天插件的管理命令，包括重载配置、查看信息等。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
@CommandInfo(name = "gdchat", description = "光点聊天插件管理", permission = "guangdian.chat.admin")
public class ChatCommand extends BaseCommand {

    private final GuangDianChat plugin;

    public ChatCommand(GuangDianChat plugin) {
        this.plugin = plugin;
    }

    /**
     * 显示帮助信息
     */
    @SubCommand(name = "")
    @Description("显示帮助信息")
    public void showHelp(CommandContext ctx) {
        super.showHelp(ctx.getSender());
    }

    /**
     * 重新加载配置
     */
    @SubCommand(name = "reload", permission = "guangdian.chat.admin")
    @Description("重新加载聊天配置")
    public void reload(CommandContext ctx) {
        CommandSender sender = ctx.getSender();

        plugin.reloadConfig();
        plugin.loadWorldAliases();
        plugin.clearLuckPermsCache();

        String message = plugin.getConfig().getString("messages.config-reloaded", "<green>GuangDianChat reloaded.");
        msg.send(sender, message);
    }

    /**
     * 显示插件信息
     */
    @SubCommand(name = "info", permission = "guangdian.chat.admin")
    @Description("显示插件信息")
    public void info(CommandContext ctx) {
        CommandSender sender = ctx.getSender();

        msg.send(sender, "<gold>GuangDianChat <gray>v" + plugin.getDescription().getVersion());

        if (plugin.isExternalServicesAvailable()) {
            msg.send(sender, "<yellow>External Services: <white>connected");
        } else {
            msg.send(sender, "<yellow>External Services: <red>not connected");
        }

        msg.send(sender, "<yellow>Chat range: <white>" + plugin.getConfig().getInt("settings.chat-range", 0));
        msg.send(sender, "<yellow>Global prefix: <white>" + plugin.getConfig().getString("settings.global-prefix", "!"));
        msg.send(sender, "<yellow>LuckPerms cache: <white>" + plugin.getLuckPermsCacheSize() + " entries");
    }

    /**
     * 刷新玩家缓存
     */
    @SubCommand(name = "refresh", permission = "guangdian.chat.admin", minArgs = 0, maxArgs = 1)
    @Description("刷新指定玩家的 LuckPerms 缓存")
    public void refresh(CommandContext ctx) {
        CommandSender sender = ctx.getSender();
        String[] args = ctx.getArgs();

        if (args.length >= 1) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                msg.sendError(sender, "玩家不在线或不存在!");
                return;
            }
            plugin.refreshPlayerCache(target.getUniqueId());
            msg.sendSuccess(sender, "已刷新玩家 <yellow>" + target.getName() + " <green>的缓存");
        } else {
            if (sender instanceof Player player) {
                plugin.refreshPlayerCache(player.getUniqueId());
                msg.sendSuccess(sender, "已刷新你的缓存");
            } else {
                msg.sendWarning(sender, "用法: /gdchat refresh <玩家>");
            }
        }
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        String[] args = context.getArgs();
        String subCommandName = subCommandMethod.getAnnotation(SubCommand.class).name();

        if (subCommandName.equals("refresh") && args.length == 1) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
