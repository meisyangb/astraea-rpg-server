package cn.guangdian.npccommand;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NPCCommandTabCompleter implements TabCompleter {

    private final GuangDianNPCCommand plugin;
    private final List<String> subCommands = Arrays.asList("add", "remove", "list", "cooldown", "clear", "reload");
    private final List<String> commandTypes = Arrays.asList("console", "player", "op", "command", "no_perms");

    public NPCCommandTabCompleter(GuangDianNPCCommand plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("npcmd.admin")) {
            return completions;
        }

        if (args.length == 1) {
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("add") || subCommand.equals("remove") || 
                subCommand.equals("list") || subCommand.equals("cooldown") || 
                subCommand.equals("clear")) {
                for (NPCCommandData data : plugin.getNPCCommandService().getAllNPCCommandData()) {
                    String id = String.valueOf(data.getNpcId());
                    if (id.startsWith(args[1])) {
                        completions.add(id);
                    }
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("add")) {
                for (String type : commandTypes) {
                    if (type.startsWith(args[2].toLowerCase())) {
                        completions.add(type);
                    }
                }
            } else if (subCommand.equals("remove")) {
                try {
                    int npcId = Integer.parseInt(args[1]);
                    NPCCommandData data = plugin.getNPCCommandService().getNPCCommandData(npcId);
                    if (data != null) {
                        for (int i = 1; i <= data.getCommands().size(); i++) {
                            completions.add(String.valueOf(i));
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return completions;
    }
}
