package cn.guangdian.soulbind.command;

import cn.guangdian.soulbind.GuangDianSoulBind;
import cn.guangdian.soulbind.api.SoulBindService;
import cn.guangdian.soulbind.manager.ConfigManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SoulBindCommand implements CommandExecutor, TabCompleter {

    private final GuangDianSoulBind plugin;
    private final SoulBindService service;
    private final ConfigManager config;

    public SoulBindCommand(GuangDianSoulBind plugin) {
        this.plugin = plugin;
        this.service = plugin.getService();
        this.config = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "bind" -> handleBind(sender, args);
            case "unbind" -> handleUnbind(sender, args);
            case "info" -> handleInfo(sender, args);
            case "reload" -> handleReload(sender);
            case "help" -> sendHelp(sender);
            default -> sendMessage(sender, "<red>未知的子命令！使用 /soulbind help 查看帮助");
        }

        return true;
    }

    private void handleBind(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.soulbind.admin")) {
            sendMessage(sender, config.getMessage("no-permission"));
            return;
        }

        if (args.length < 2) {
            sendMessage(sender, "<red>用法: /soulbind bind <玩家> [物品ID]");
            sendMessage(sender, "<gray>如果不指定物品ID，则绑定目标玩家手持的物品");
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            sendMessage(sender, config.getMessage("player-not-found"));
            return;
        }

        ItemStack item;
        if (args.length >= 3) {
            // 通过物品ID给予并绑定
            String itemId = args[2];
            item = plugin.getMythicMobsHook().getMythicItem(itemId);
            if (item == null || item.getType() == Material.AIR) {
                sendMessage(sender, "<red>无法找到物品: " + itemId);
                return;
            }
        } else {
            // 绑定目标玩家手持的物品
            item = targetPlayer.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR) {
                sendMessage(sender, "<red>目标玩家必须手持需要绑定的物品！");
                return;
            }
        }

        if (service.isBound(item)) {
            sendMessage(sender, config.getMessage("already-bound"));
            return;
        }

        if (service.bindItem(item, targetPlayer)) {
            if (args.length >= 3) {
                // 给予新物品
                targetPlayer.getInventory().addItem(item);
                sendMessage(sender, "<green>已成功给予并绑定物品给玩家 <yellow>" + targetPlayer.getName());
                sendMessage(targetPlayer, config.getMessage("bind-success"));
            } else {
                sendMessage(sender, "<green>已成功绑定物品到玩家 <yellow>" + targetPlayer.getName());
                sendMessage(targetPlayer, config.getMessage("bind-success"));
            }
        }
    }

    private void handleUnbind(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.soulbind.admin")) {
            sendMessage(sender, config.getMessage("no-permission"));
            return;
        }

        if (args.length < 2) {
            sendMessage(sender, "<red>用法: /soulbind unbind <玩家>");
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            sendMessage(sender, config.getMessage("player-not-found"));
            return;
        }

        ItemStack item = targetPlayer.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            sendMessage(sender, "<red>目标玩家必须手持需要解绑的物品！");
            return;
        }

        if (!service.isBound(item)) {
            sendMessage(sender, config.getMessage("not-bound"));
            return;
        }

        if (service.unbindItem(item)) {
            sendMessage(sender, "<green>已成功解绑物品！");
            sendMessage(targetPlayer, "<yellow>你手持的物品已被管理员解绑");
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.soulbind.admin")) {
            sendMessage(sender, config.getMessage("no-permission"));
            return;
        }

        if (args.length < 2) {
            sendMessage(sender, "<red>用法: /soulbind info <玩家>");
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            sendMessage(sender, config.getMessage("player-not-found"));
            return;
        }

        ItemStack item = targetPlayer.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            sendMessage(sender, "<red>目标玩家必须手持需要查看的物品！");
            return;
        }

        sendMessage(sender, "<yellow>========== 物品绑定信息 ==========");
        sendMessage(sender, "<yellow>玩家: <white>" + targetPlayer.getName());
        sendMessage(sender, "<yellow>物品: <white>" + item.getType().name());
        sendMessage(sender, "<yellow>是否为MythicMobs物品: <white>" + (service.isMythicMobsItem(item) ? "是" : "否"));

        if (service.isBound(item)) {
            String boundName = service.getBoundPlayerName(item);
            sendMessage(sender, "<yellow>绑定状态: <green>已绑定");
            sendMessage(sender, "<yellow>绑定玩家: <white>" + boundName);
        } else {
            sendMessage(sender, "<yellow>绑定状态: <red>未绑定");
        }
        sendMessage(sender, "<yellow>================================");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("guangdian.soulbind.admin")) {
            sendMessage(sender, config.getMessage("no-permission"));
            return;
        }

        config.reloadConfig();
        sendMessage(sender, "<green>配置文件已重新加载！");
    }

    private void sendHelp(CommandSender sender) {
        sendMessage(sender, "<yellow>========== 灵魂绑定帮助 ==========");
        if (sender.hasPermission("guangdian.soulbind.admin")) {
            sendMessage(sender, "<yellow>/soulbind bind <玩家> [物品ID] <gray>- 绑定玩家手持物品或给予新物品并绑定");
            sendMessage(sender, "<yellow>/soulbind unbind <玩家> <gray>- 解绑玩家手持物品");
            sendMessage(sender, "<yellow>/soulbind info <玩家> <gray>- 查看玩家手持物品绑定信息");
            sendMessage(sender, "<yellow>/soulbind reload <gray>- 重载配置");
        }
        sendMessage(sender, "<yellow>/soulbind help <gray>- 显示此帮助");
        sendMessage(sender, "<yellow>================================");
    }

    private void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        Component component = plugin.getMiniMessage().colorize(message);
        sender.sendMessage(component);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("guangdian.soulbind.admin")) {
            return completions;
        }

        if (args.length == 1) {
            completions.addAll(Arrays.asList("bind", "unbind", "info", "reload", "help"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "bind", "unbind", "info" ->
                    Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(lastArg));

        return completions;
    }
}
