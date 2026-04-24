package cn.guangdian.guild.command;

import cn.guangdian.guild.GuangDianGuild;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

/**
 * 传统管理员命令执行器 - 降级使用
 *
 * <p>当 RPGCore CommandFramework 不可用时，使用此传统命令处理器。</p>
 *
 * @author Astraea RPG Team
 * @since 1.2.0
 */
public class LegacyAdminCommandExecutor implements CommandExecutor {

    private final GuangDianGuild plugin;

    public LegacyAdminCommandExecutor(GuangDianGuild plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("guangdian.guild.admin")) { sender.sendMessage(plugin.getMsg("no-permission")); return true; }
        if (args.length == 0) {
            sender.sendMessage(Component.text("用法: /guildadmin <reload|delete>").color(NamedTextColor.RED));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(Component.text("配置已重新加载!").color(NamedTextColor.GREEN));
                return true;
            case "delete":
                if (args.length < 2) { sender.sendMessage(Component.text("用法: /guildadmin delete <工会名>").color(NamedTextColor.RED)); return true; }
                String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                if (plugin.disbandGuild(name)) sender.sendMessage(Component.text("工会已删除: " + name).color(NamedTextColor.GREEN));
                else sender.sendMessage(plugin.getMsg("guild-not-found"));
                return true;
            default:
                sender.sendMessage(Component.text("用法: /guildadmin <reload|delete>").color(NamedTextColor.RED));
                return true;
        }
    }
}
