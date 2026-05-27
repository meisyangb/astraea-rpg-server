package cn.guangdian.raid.command;

import cn.guangdian.raid.GuangDianRaid;
import cn.guangdian.raid.instance.RaidInstance;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RaidAdminCommand implements CommandExecutor, TabCompleter {

    private final GuangDianRaid plugin;

    public RaidAdminCommand(GuangDianRaid plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> handleReload(sender);
            case "list" -> handleListInstances(sender);
            case "forceend" -> handleForceEnd(sender, args);
            case "tp" -> handleTp(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  副本管理命令").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/raidadmin reload").color(NamedTextColor.GREEN)
            .append(Component.text(" - 重载配置").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/raidadmin list").color(NamedTextColor.GREEN)
            .append(Component.text(" - 查看活跃副本").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/raidadmin forceend <实例ID>").color(NamedTextColor.GREEN)
            .append(Component.text(" - 强制结束副本").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/raidadmin tp <实例ID>").color(NamedTextColor.GREEN)
            .append(Component.text(" - 传送到副本").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD));
    }

    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().reload();
        sender.sendMessage(Component.text("配置已重载").color(NamedTextColor.GREEN));
    }

    private void handleListInstances(CommandSender sender) {
        var instances = plugin.getInstanceManager().getAllInstances();
        if (instances.isEmpty()) {
            sender.sendMessage(Component.text("当前没有活跃的副本").color(NamedTextColor.YELLOW));
            return;
        }

        sender.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  活跃副本列表 (" + instances.size() + ")").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD));

        for (RaidInstance instance : instances) {
            sender.sendMessage(Component.text("  " + instance.getInstanceId()).color(NamedTextColor.GREEN)
                .append(Component.text(" - " + instance.getRaid().getName()).color(NamedTextColor.WHITE))
                .append(Component.text(" [" + instance.getCurrentPhase().getDisplayName() + "]")
                    .color(NamedTextColor.GRAY))
                .append(Component.text(" " + instance.getTeam().size() + "人").color(NamedTextColor.AQUA)));
        }
    }

    private void handleForceEnd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /raidadmin forceend <实例ID>").color(NamedTextColor.RED));
            return;
        }

        String instanceId = args[1];
        RaidInstance instance = plugin.getInstanceManager().getInstance(instanceId);
        if (instance == null) {
            sender.sendMessage(Component.text("副本实例不存在: " + instanceId).color(NamedTextColor.RED));
            return;
        }

        instance.fail("管理员强制结束");
        sender.sendMessage(Component.text("已强制结束副本: " + instanceId).color(NamedTextColor.GREEN));
    }

    private void handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c该命令只能由玩家执行");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /raidadmin tp <实例ID>").color(NamedTextColor.RED));
            return;
        }

        String instanceId = args[1];
        RaidInstance instance = plugin.getInstanceManager().getInstance(instanceId);
        if (instance == null) {
            sender.sendMessage(Component.text("副本实例不存在: " + instanceId).color(NamedTextColor.RED));
            return;
        }

        var world = instance.getWorld();
        if (world == null) {
            sender.sendMessage(Component.text("副本世界不存在").color(NamedTextColor.RED));
            return;
        }

        player.teleport(world.getSpawnLocation());
        sender.sendMessage(Component.text("已传送到副本: " + instanceId).color(NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("reload", "list", "forceend", "tp"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("forceend") || args[0].equalsIgnoreCase("tp")) {
                completions.addAll(plugin.getInstanceManager().getAllInstances().stream()
                    .map(RaidInstance::getInstanceId)
                    .collect(Collectors.toList()));
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(lastArg))
            .collect(Collectors.toList());
    }
}
