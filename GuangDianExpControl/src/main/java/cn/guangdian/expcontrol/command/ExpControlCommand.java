package cn.guangdian.expcontrol.command;

import cn.guangdian.expcontrol.GuangDianExpControl;
import cn.guangdian.expcontrol.api.ExpControlService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 经验控制命令处理器
 */
public class ExpControlCommand implements CommandExecutor, TabCompleter {
    
    private final GuangDianExpControl plugin;
    private final ExpControlService expService;
    
    public ExpControlCommand(GuangDianExpControl plugin, ExpControlService expService) {
        this.plugin = plugin;
        this.expService = expService;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "give" -> handleGive(sender, args);
            case "set" -> handleSet(sender, args);
            case "add" -> handleAdd(sender, args);
            case "take" -> handleTake(sender, args);
            case "reset" -> handleReset(sender, args);
            case "info" -> handleInfo(sender, args);
            case "reload" -> handleReload(sender);
            case "toggle" -> handleToggle(sender);
            case "help" -> sendHelp(sender);
            default -> sender.sendMessage(Component.text("未知命令! 使用 /expcontrol help 查看帮助", NamedTextColor.RED));
        }
        
        return true;
    }
    
    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.expcontrol.admin")) {
            sender.sendMessage(getMessage("no-permission"));
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage(Component.text("用法: /expcontrol give <玩家> <数量>", NamedTextColor.RED));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found"));
            return;
        }
        
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(getMessage("invalid-amount"));
            return;
        }
        
        if (amount <= 0) {
            sender.sendMessage(Component.text("经验数量必须大于0!", NamedTextColor.RED));
            return;
        }
        
        expService.giveExp(target, amount, "command:" + sender.getName());
        sender.sendMessage(getMessage("exp-given", 
            "player", target.getName(),
            "amount", String.valueOf(amount)));
    }
    
    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.expcontrol.admin")) {
            sender.sendMessage(getMessage("no-permission"));
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage(Component.text("用法: /expcontrol set <玩家> <经验值>", NamedTextColor.RED));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found"));
            return;
        }
        
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(getMessage("invalid-amount"));
            return;
        }
        
        if (amount < 0) {
            sender.sendMessage(Component.text("经验值不能为负数!", NamedTextColor.RED));
            return;
        }
        
        expService.setExp(target, amount);
        sender.sendMessage(getMessage("exp-set", 
            "player", target.getName(),
            "amount", String.valueOf(amount)));
    }
    
    private void handleAdd(CommandSender sender, String[] args) {
        // add 是 give 的别名
        handleGive(sender, args);
    }
    
    private void handleTake(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.expcontrol.admin")) {
            sender.sendMessage(getMessage("no-permission"));
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage(Component.text("用法: /expcontrol take <玩家> <数量>", NamedTextColor.RED));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found"));
            return;
        }
        
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(getMessage("invalid-amount"));
            return;
        }
        
        if (amount <= 0) {
            sender.sendMessage(Component.text("数量必须大于0!", NamedTextColor.RED));
            return;
        }
        
        int taken = expService.takeExp(target, amount);
        sender.sendMessage(Component.text("成功从 " + target.getName() + " 扣除 " + taken + " 点经验!", NamedTextColor.GREEN));
    }
    
    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.expcontrol.admin")) {
            sender.sendMessage(getMessage("no-permission"));
            return;
        }
        
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(Component.text("请指定玩家!", NamedTextColor.RED));
            return;
        }
        
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found"));
            return;
        }
        
        expService.resetExp(target);
        sender.sendMessage(getMessage("exp-reset", "player", target.getName()));
    }
    
    private void handleInfo(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(Component.text("请指定玩家!", NamedTextColor.RED));
            return;
        }
        
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found"));
            return;
        }
        
        int exp = expService.getExp(target);
        int level = expService.getLevel(target);
        
        sender.sendMessage(getMessage("exp-info", 
            "player", target.getName(),
            "exp", String.valueOf(exp),
            "level", String.valueOf(level)));
    }
    
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("guangdian.expcontrol.admin")) {
            sender.sendMessage(getMessage("no-permission"));
            return;
        }
        
        plugin.reloadConfiguration();
        sender.sendMessage(getMessage("reload-success"));
    }
    
    private void handleToggle(CommandSender sender) {
        if (!sender.hasPermission("guangdian.expcontrol.admin")) {
            sender.sendMessage(getMessage("no-permission"));
            return;
        }
        
        boolean newState = !plugin.isBlockAllExp();
        plugin.setBlockAllExp(newState);
        
        sender.sendMessage(Component.text("经验拦截已" + (newState ? "启用" : "禁用") + "!", NamedTextColor.GREEN));
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("===== 经验控制插件帮助 =====", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/expcontrol give <玩家> <数量> - 给玩家发放经验", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/expcontrol set <玩家> <经验值> - 设置玩家的经验", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/expcontrol take <玩家> <数量> - 扣除玩家的经验", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/expcontrol reset [玩家] - 重置玩家的经验", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/expcontrol info [玩家] - 查看玩家经验信息", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/expcontrol toggle - 切换经验拦截状态", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/expcontrol reload - 重载配置", NamedTextColor.YELLOW));
    }
    
    private Component getMessage(String key, String... placeholders) {
        String prefix = plugin.getConfig().getString("messages.prefix", "<gold>[经验控制]</gold> ");
        String message = plugin.getConfig().getString("messages." + key, key);
        
        // 替换占位符
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace("%" + placeholders[i] + "%", placeholders[i + 1]);
            }
        }
        
        return Component.text()
            .append(parseMiniMessage(prefix))
            .append(parseMiniMessage(message))
            .build();
    }
    
    private Component parseMiniMessage(String text) {
        try {
            return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(text);
        } catch (Exception e) {
            return Component.text(text.replace("<", "").replace(">", ""));
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("give", "set", "take", "reset", "info", "toggle", "reload", "help"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (Arrays.asList("give", "set", "take", "reset", "info").contains(subCommand)) {
                // 玩家名补全
                Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (Arrays.asList("give", "set", "take").contains(subCommand)) {
                completions.addAll(Arrays.asList("10", "50", "100", "500", "1000"));
            }
        }
        
        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(lastArg))
            .collect(Collectors.toList());
    }
}
