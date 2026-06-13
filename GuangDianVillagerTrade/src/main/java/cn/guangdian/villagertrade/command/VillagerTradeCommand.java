package cn.guangdian.villagertrade.command;

import cn.guangdian.villagertrade.GuangDianVillagerTrade;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * 村民兑换命令处理器
 */
public class VillagerTradeCommand implements CommandExecutor, TabCompleter {

    private final GuangDianVillagerTrade plugin;

    public VillagerTradeCommand(GuangDianVillagerTrade plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();
        
        if (cmdName.equals("villagertrade") || cmdName.equals("vtrade") || cmdName.equals("vt")) {
            return handleTradeCommand(sender, args);
        } else if (cmdName.equals("villagertradeadmin") || cmdName.equals("vtadmin") || cmdName.equals("vta")) {
            return handleAdminCommand(sender, args);
        }
        
        return false;
    }

    /**
     * 处理玩家兑换命令
     */
    private boolean handleTradeCommand(CommandSender sender, String[] args) {
        // 检查是否是玩家
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.red("该命令只能由玩家执行"));
            return true;
        }

        // 检查权限
        if (!player.hasPermission("villagertrade.use")) {
            player.sendMessage(plugin.red("你没有权限使用此命令"));
            return true;
        }

        // 检查参数
        if (args.length < 1) {
            player.sendMessage(plugin.yellow("用法: /vtrade <配方名称/配方组名称>"));
            player.sendMessage(plugin.gray("可用配方: " + String.join(", ", plugin.getRecipeManager().getRecipeNames())));
            player.sendMessage(plugin.gray("可用配方组: " + String.join(", ", plugin.getRecipeManager().getRecipeGroupNames())));
            return true;
        }

        String name = args[0];
        
        // 优先检查配方组
        if (plugin.getRecipeManager().hasRecipeGroup(name)) {
            // 打开配方组界面
            if (plugin.openRecipeGroupGUI(player, name)) {
                plugin.getLogger().info("玩家 " + player.getName() + " 打开了配方组界面: " + name);
            }
        } else if (plugin.getRecipeManager().hasRecipe(name)) {
            // 打开单个配方界面
            if (plugin.openTradeGUI(player, name)) {
                plugin.getLogger().info("玩家 " + player.getName() + " 打开了兑换界面: " + name);
            }
        } else {
            player.sendMessage(plugin.red("找不到配方或配方组: " + name));
        }
        
        return true;
    }

    /**
     * 处理管理员命令
     */
    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        // 检查权限
        if (!sender.hasPermission("villagertrade.admin")) {
            sender.sendMessage(plugin.red("你没有权限使用此命令"));
            return true;
        }

        // 检查参数
        if (args.length < 1) {
            sender.sendMessage(plugin.yellow("用法: /vtadmin <reload/list/open> [参数]"));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                return handleReloadCommand(sender);
            case "list":
                return handleListCommand(sender);
            case "open":
                return handleOpenCommand(sender, args);
            default:
                sender.sendMessage(plugin.red("未知子命令: " + subCommand));
                sender.sendMessage(plugin.yellow("可用子命令: reload, list, open"));
                return true;
        }
    }

    /**
     * 处理重载命令
     */
    private boolean handleReloadCommand(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getRecipeManager().reload();
        
        sender.sendMessage(plugin.green("村民兑换系统配置已重载"));
        sender.sendMessage(plugin.gray("已加载 " + plugin.getRecipeManager().getRecipeCount() + " 个配方, " 
            + plugin.getRecipeManager().getRecipeGroupCount() + " 个配方组"));
        
        plugin.getLogger().info("配置已由 " + sender.getName() + " 重载");
        return true;
    }

    /**
     * 处理列表命令
     */
    private boolean handleListCommand(CommandSender sender) {
        Set<String> recipeNames = plugin.getRecipeManager().getRecipeNames();
        Set<String> groupNames = plugin.getRecipeManager().getRecipeGroupNames();
        
        sender.sendMessage(plugin.green("===== 可用兑换配方 ====="));
        
        if (recipeNames.isEmpty()) {
            sender.sendMessage(plugin.gray("暂无配方"));
        } else {
            for (String name : recipeNames) {
                sender.sendMessage(plugin.yellow("- " + name));
            }
        }
        
        sender.sendMessage(plugin.gray("共 " + recipeNames.size() + " 个配方"));
        
        sender.sendMessage(plugin.green("===== 可用配方组 ====="));
        
        if (groupNames.isEmpty()) {
            sender.sendMessage(plugin.gray("暂无配方组"));
        } else {
            for (String name : groupNames) {
                sender.sendMessage(plugin.aqua("- " + name));
            }
        }
        
        sender.sendMessage(plugin.gray("共 " + groupNames.size() + " 个配方组"));
        return true;
    }

    /**
     * 处理打开命令
     */
    private boolean handleOpenCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.yellow("用法: /vtadmin open <玩家> <配方名称/配方组名称>"));
            return true;
        }

        String playerName = args[1];
        String name = args[2];

        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage(plugin.red("玩家不在线: " + playerName));
            return true;
        }

        // 优先检查配方组
        if (plugin.getRecipeManager().hasRecipeGroup(name)) {
            if (plugin.openRecipeGroupGUI(target, name)) {
                sender.sendMessage(plugin.green("已为 " + playerName + " 打开配方组界面: " + name));
            }
        } else if (plugin.getRecipeManager().hasRecipe(name)) {
            if (plugin.openTradeGUI(target, name)) {
                sender.sendMessage(plugin.green("已为 " + playerName + " 打开兑换界面: " + name));
            }
        } else {
            sender.sendMessage(plugin.red("找不到配方或配方组: " + name));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmdName = command.getName().toLowerCase();
        
        if (cmdName.equals("villagertrade") || cmdName.equals("vtrade") || cmdName.equals("vt")) {
            return handleTradeTabComplete(sender, args);
        } else if (cmdName.equals("villagertradeadmin") || cmdName.equals("vtadmin") || cmdName.equals("vta")) {
            return handleAdminTabComplete(sender, args);
        }
        
        return new ArrayList<>();
    }

    /**
     * 处理玩家命令的Tab补全
     */
    private List<String> handleTradeTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 返回所有配方名称和配方组名称
            String input = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            
            completions.addAll(plugin.getRecipeManager().getRecipeNames().stream()
                .filter(name -> name.toLowerCase().startsWith(input))
                .collect(Collectors.toList()));
            
            completions.addAll(plugin.getRecipeManager().getRecipeGroupNames().stream()
                .filter(name -> name.toLowerCase().startsWith(input))
                .collect(Collectors.toList()));
            
            return completions;
        }
        
        return new ArrayList<>();
    }

    /**
     * 处理管理员命令的Tab补全
     */
    private List<String> handleAdminTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 返回子命令
            List<String> subCommands = List.of("reload", "list", "open");
            String input = args[0].toLowerCase();
            return subCommands.stream()
                .filter(cmd -> cmd.startsWith(input))
                .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            // 返回在线玩家列表
            String input = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("open")) {
            // 返回配方名称和配方组名称
            String input = args[2].toLowerCase();
            List<String> completions = new ArrayList<>();
            
            completions.addAll(plugin.getRecipeManager().getRecipeNames().stream()
                .filter(name -> name.toLowerCase().startsWith(input))
                .collect(Collectors.toList()));
            
            completions.addAll(plugin.getRecipeManager().getRecipeGroupNames().stream()
                .filter(name -> name.toLowerCase().startsWith(input))
                .collect(Collectors.toList()));
            
            return completions;
        }
        
        return new ArrayList<>();
    }
}
