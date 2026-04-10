package cn.guangdian.soulbind.command;

import cn.guangdian.soulbind.GuangDianSoulBind;
import cn.guangdian.soulbind.api.SoulBindService;
import cn.guangdian.soulbind.manager.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
            case "info" -> handleInfo(sender);
            case "reload" -> handleReload(sender);
            case "help" -> sendHelp(sender);
            default -> sendMessage(sender, "&c未知的子命令！使用 /soulbind help 查看帮助");
        }

        return true;
    }

    private void handleBind(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "&c该命令只能由玩家执行！");
            return;
        }

        if (!sender.hasPermission("guangdian.soulbind.bind")) {
            sendMessage(sender, config.getMessage("no-permission"));
            return;
        }

        Player targetPlayer = player;
        if (args.length >= 2 && sender.hasPermission("guangdian.soulbind.admin")) {
            targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                sendMessage(sender, config.getMessage("player-not-found"));
                return;
            }
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            sendMessage(sender, "&c请手持需要绑定的物品！");
            return;
        }

        if (service.isBound(item)) {
            sendMessage(sender, config.getMessage("already-bound"));
            return;
        }

        if (config.isMythicMobsOnly() && !service.isMythicMobsItem(item)) {
            sendMessage(sender, config.getMessage("not-mythic-item"));
            return;
        }

        if (service.bindItem(item, targetPlayer)) {
            sendMessage(sender, config.getMessage("bind-success"));
        }
    }

    private void handleUnbind(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "&c该命令只能由玩家执行！");
            return;
        }

        if (!sender.hasPermission("guangdian.soulbind.admin")) {
            sendMessage(sender, config.getMessage("no-permission"));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            sendMessage(sender, "&c请手持需要解绑的物品！");
            return;
        }

        if (!service.isBound(item)) {
            sendMessage(sender, config.getMessage("not-bound"));
            return;
        }

        if (service.unbindItem(item)) {
            sendMessage(sender, "&a物品已成功解绑！");
        }
    }

    private void handleInfo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "&c该命令只能由玩家执行！");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            sendMessage(sender, "&c请手持需要查看的物品！");
            return;
        }

        sendMessage(sender, "&e========== 物品绑定信息 ==========");
        sendMessage(sender, "&e是否为MythicMobs物品: &f" + (service.isMythicMobsItem(item) ? "是" : "否"));

        if (service.isBound(item)) {
            String boundName = service.getBoundPlayerName(item);
            sendMessage(sender, "&e绑定状态: &a已绑定");
            sendMessage(sender, "&e绑定玩家: &f" + boundName);
        } else {
            sendMessage(sender, "&e绑定状态: &c未绑定");
        }
        sendMessage(sender, "&e================================");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("guangdian.soulbind.admin")) {
            sendMessage(sender, config.getMessage("no-permission"));
            return;
        }

        config.reloadConfig();
        sendMessage(sender, "&a配置文件已重新加载！");
    }

    private void sendHelp(CommandSender sender) {
        sendMessage(sender, "&e========== 灵魂绑定帮助 ==========");
        sendMessage(sender, "&e/soulbind bind [玩家] &7- 绑定手持物品");
        sendMessage(sender, "&e/soulbind unbind &7- 解绑手持物品(管理员)");
        sendMessage(sender, "&e/soulbind info &7- 查看物品绑定信息");
        sendMessage(sender, "&e/soulbind reload &7- 重载配置(管理员)");
        sendMessage(sender, "&e/soulbind help &7- 显示此帮助");
        sendMessage(sender, "&e================================");
    }

    private void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        sender.sendMessage(component);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("bind", "unbind", "info", "reload", "help"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("bind") && sender.hasPermission("guangdian.soulbind.admin")) {
            Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
        }

        String lastArg = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(lastArg));

        return completions;
    }
}
