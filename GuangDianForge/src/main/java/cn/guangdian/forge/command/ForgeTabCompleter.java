package cn.guangdian.forge.command;

import cn.guangdian.forge.GuangDianForge;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Forge 命令 Tab 补全器
 */
public class ForgeTabCompleter implements TabCompleter {
    private final GuangDianForge plugin;

    public ForgeTabCompleter(GuangDianForge plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("forge")) {
            return completeForgeCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("forgeadmin")) {
            return completeAdminCommand(sender, args);
        }
        return new ArrayList<>();
    }

    /**
     * 补全 /forge 命令
     */
    private List<String> completeForgeCommand(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 补全子命令
            String[] subCommands = {"open", "learn", "info", "list", "help"};
            String input = args[0].toLowerCase();

            for (String cmd : subCommands) {
                if (cmd.startsWith(input)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "open" -> {
                    // 补全已学图纸ID
                    if (sender instanceof Player player) {
                        var data = plugin.getPlayerDataManager().get(player.getUniqueId());
                        String input = args[1].toLowerCase();

                        for (var recipe : plugin.getRecipeManager().getAllRecipes()) {
                            if (data.hasLearned(recipe.getId()) && recipe.getId().toLowerCase().startsWith(input)) {
                                completions.add(recipe.getId());
                            }
                        }
                    }
                }
                case "learn" -> {
                    // 补全未学图纸ID
                    if (sender instanceof Player player) {
                        var data = plugin.getPlayerDataManager().get(player.getUniqueId());
                        String input = args[1].toLowerCase();

                        for (var recipe : plugin.getRecipeManager().getAllRecipes()) {
                            if (!data.hasLearned(recipe.getId()) && recipe.getId().toLowerCase().startsWith(input)) {
                                completions.add(recipe.getId());
                            }
                        }
                    }
                }
                case "info" -> {
                    // 补全在线玩家名（需要管理员权限）
                    if (sender.hasPermission("guangdian.forge.admin")) {
                        String input = args[1].toLowerCase();
                        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                            if (onlinePlayer.getName().toLowerCase().startsWith(input)) {
                                completions.add(onlinePlayer.getName());
                            }
                        }
                    }
                }
                case "list" -> {
                    // 补全筛选选项
                    String[] filters = {"all", "learned", "unlearned"};
                    String input = args[1].toLowerCase();

                    for (String filter : filters) {
                        if (filter.startsWith(input)) {
                            completions.add(filter);
                        }
                    }
                }
            }
        }

        return completions;
    }

    /**
     * 补全 /forgeadmin 命令
     */
    private List<String> completeAdminCommand(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("guangdian.forge.admin")) {
            return completions;
        }

        if (args.length == 1) {
            // 补全管理子命令
            String[] subCommands = {"give", "setlevel", "addexp", "reset", "reload", "stats", "help"};
            String input = args[0].toLowerCase();

            for (String cmd : subCommands) {
                if (cmd.startsWith(input)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "give", "setlevel", "addexp", "reset" -> {
                    // 补全在线玩家名
                    String input = args[1].toLowerCase();
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (onlinePlayer.getName().toLowerCase().startsWith(input)) {
                            completions.add(onlinePlayer.getName());
                        }
                    }
                }
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "give" -> {
                    // 补全所有图纸ID
                    String input = args[2].toLowerCase();
                    for (var recipe : plugin.getRecipeManager().getAllRecipes()) {
                        if (recipe.getId().toLowerCase().startsWith(input)) {
                            completions.add(recipe.getId());
                        }
                    }
                }
                case "setlevel" -> {
                    // 补全等级（包括 max）
                    String input = args[2].toLowerCase();
                    if ("max".startsWith(input)) {
                        completions.add("max");
                    }

                    // 补全数字等级
                    var thresholds = getLevelThresholds();
                    for (int level : thresholds.keySet()) {
                        String levelStr = String.valueOf(level);
                        if (levelStr.startsWith(input)) {
                            completions.add(levelStr);
                        }
                    }
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            // 补全数量（1-64）
            String input = args[3];
            for (int i = 1; i <= 64; i++) {
                String numStr = String.valueOf(i);
                if (numStr.startsWith(input)) {
                    completions.add(numStr);
                }
            }
        }

        return completions;
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<Integer, Long> getLevelThresholds() {
        java.util.Map<Integer, Long> thresholds = new java.util.TreeMap<>();
        var section = plugin.getConfig().getConfigurationSection("level-thresholds");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    long exp = section.getLong(key);
                    thresholds.put(level, exp);
                } catch (NumberFormatException ignored) {}
            }
        }
        return thresholds;
    }
}
