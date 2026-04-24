package cn.guangdian.tab;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * 降级 Tab 补全器 - 当 CommandFramework 不可用时使用
 */
public class FallbackTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!command.getName().equalsIgnoreCase("gdtab")) {
            return completions;
        }

        if (args.length == 1) {
            completions.add("reload");
            completions.add("info");
            completions.add("cache");
            completions.add("help");
        }

        return completions;
    }
}
