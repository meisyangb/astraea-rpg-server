package cn.guangdian.guild.command;

import cn.guangdian.guild.GuangDianGuild;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 传统 Tab 补全器 - 降级使用
 *
 * <p>当 RPGCore CommandFramework 不可用时，使用此传统 Tab 补全器。</p>
 *
 * @author Astraea RPG Team
 * @since 1.2.0
 */
public class LegacyTabCompleter implements TabCompleter {

    private final GuangDianGuild plugin;

    public LegacyTabCompleter(GuangDianGuild plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();

        if (args.length == 1) {
            list.addAll(Arrays.asList("create", "disband", "invite", "accept", "deny", "join", "leave", "kick", "promote", "demote", "info", "list", "chat", "setprefix", "setdesc"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("invite") || sub.equals("kick") || sub.equals("promote") || sub.equals("demote")) {
                for (Player p : plugin.getServer().getOnlinePlayers()) list.add(p.getName());
            } else if (sub.equals("join") || sub.equals("info")) {
                for (GuangDianGuild.Guild g : plugin.getAllGuilds()) list.add(g.name);
            }
        }

        return list.stream().filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
    }
}
