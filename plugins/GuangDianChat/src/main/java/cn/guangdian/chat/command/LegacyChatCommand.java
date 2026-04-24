package cn.guangdian.chat.command;

import cn.guangdian.chat.GuangDianChat;
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
 * 传统聊天命令实现 (降级处理)
 *
 * <p>当 RPGCore CommandFramework 不可用时使用此实现。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class LegacyChatCommand implements CommandExecutor, TabCompleter {

    private final GuangDianChat plugin;

    public LegacyChatCommand(GuangDianChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("gdchat")) {
            return false;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            showHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("guangdian.chat.admin")) {
                sender.sendMessage(Component.text("没有权限!", NamedTextColor.RED));
                return true;
            }
            plugin.reloadConfig();
            plugin.loadWorldAliases();
            plugin.clearLuckPermsCache();
            sender.sendMessage(Component.text("配置已重新加载!", NamedTextColor.GREEN));
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            if (!sender.hasPermission("guangdian.chat.admin")) {
                sender.sendMessage(Component.text("没有权限!", NamedTextColor.RED));
                return true;
            }
            sender.sendMessage(Component.text("GuangDianChat v" + plugin.getDescription().getVersion(), NamedTextColor.GOLD));
            sender.sendMessage(Component.text("External Services: " + (plugin.isExternalServicesAvailable() ? "connected" : "not connected"), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Chat range: " + plugin.getConfig().getInt("settings.chat-range", 0), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Global prefix: " + plugin.getConfig().getString("settings.global-prefix", "!"), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("LuckPerms cache: " + plugin.getLuckPermsCacheSize() + " entries", NamedTextColor.YELLOW));
            return true;
        }

        if (args[0].equalsIgnoreCase("refresh")) {
            if (!sender.hasPermission("guangdian.chat.admin")) {
                sender.sendMessage(Component.text("没有权限!", NamedTextColor.RED));
                return true;
            }

            if (args.length >= 2) {
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(Component.text("玩家不在线或不存在!", NamedTextColor.RED));
                    return true;
                }
                plugin.refreshPlayerCache(target.getUniqueId());
                sender.sendMessage(Component.text("已刷新玩家 " + target.getName() + " 的缓存", NamedTextColor.GREEN));
            } else {
                if (sender instanceof Player player) {
                    plugin.refreshPlayerCache(player.getUniqueId());
                    sender.sendMessage(Component.text("已刷新你的缓存", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("用法: /gdchat refresh <玩家>", NamedTextColor.YELLOW));
                }
            }
            return true;
        }

        showHelp(sender);
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(Component.text("========== 光点聊天插件 ==========", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/gdchat reload - 重新加载配置", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/gdchat info - 显示插件信息", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/gdchat refresh [玩家] - 刷新缓存", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("==================================", NamedTextColor.GOLD));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            if ("reload".startsWith(partial)) completions.add("reload");
            if ("info".startsWith(partial)) completions.add("info");
            if ("refresh".startsWith(partial)) completions.add("refresh");
            if ("help".startsWith(partial)) completions.add("help");
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("refresh")) {
            String partial = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}
