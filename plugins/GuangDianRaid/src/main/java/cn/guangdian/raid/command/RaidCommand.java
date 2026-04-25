package cn.guangdian.raid.command;

import cn.guangdian.raid.GuangDianRaid;
import cn.guangdian.raid.instance.RaidInstance;
import cn.guangdian.raid.model.Raid;
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
import java.util.Optional;
import java.util.stream.Collectors;

public class RaidCommand implements CommandExecutor, TabCompleter {

    private final GuangDianRaid plugin;

    public RaidCommand(GuangDianRaid plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(net.kyori.adventure.text.Component.text("该命令只能由玩家执行").color(net.kyori.adventure.text.format.NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "join" -> handleJoin(player, args);
            case "leave" -> handleLeave(player);
            case "list" -> handleList(player);
            case "info" -> handleInfo(player, args);
            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("  搜打撤副本系统").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/raid list").color(NamedTextColor.GREEN)
            .append(Component.text(" - 查看可用副本").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/raid join <副本ID>").color(NamedTextColor.GREEN)
            .append(Component.text(" - 加入副本").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/raid leave").color(NamedTextColor.GREEN)
            .append(Component.text(" - 离开副本").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/raid info <副本ID>").color(NamedTextColor.GREEN)
            .append(Component.text(" - 查看副本信息").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD));
    }

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("用法: /raid join <副本ID>").color(NamedTextColor.RED));
            return;
        }

        Optional<RaidInstance> currentRaid = plugin.getInstanceManager().getPlayerInstance(player.getUniqueId());
        if (currentRaid.isPresent()) {
            player.sendMessage(Component.text("你已在副本中！").color(NamedTextColor.RED));
            return;
        }

        String raidId = args[1];
        Raid raid = plugin.getConfigManager().getRaid(raidId);
        if (raid == null) {
            player.sendMessage(Component.text("副本不存在: " + raidId).color(NamedTextColor.RED));
            return;
        }

        List<Player> players = new ArrayList<>();
        players.add(player);

        plugin.getInstanceManager().createInstance(raidId, players);
    }

    private void handleLeave(Player player) {
        Optional<RaidInstance> instance = plugin.getInstanceManager().getPlayerInstance(player.getUniqueId());
        if (instance.isEmpty()) {
            player.sendMessage(Component.text("你不在任何副本中！").color(NamedTextColor.RED));
            return;
        }

        instance.get().fail("玩家主动退出");
        plugin.getInstanceManager().removePlayer(player.getUniqueId());
        plugin.getRaidBoard().removeBoard(player);

        player.sendMessage(Component.text("已离开副本").color(NamedTextColor.YELLOW));
    }

    private void handleList(Player player) {
        List<String> raidIds = plugin.getConfigManager().getRaidIds();
        if (raidIds.isEmpty()) {
            player.sendMessage(Component.text("当前没有可用的副本").color(NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("  可用副本列表").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD));

        for (String raidId : raidIds) {
            Raid raid = plugin.getConfigManager().getRaid(raidId);
            if (raid != null) {
                player.sendMessage(Component.text("  " + raidId).color(NamedTextColor.GREEN)
                    .append(Component.text(" - " + raid.getName()).color(NamedTextColor.WHITE))
                    .append(Component.text(" [" + raid.getMinPlayers() + "-" + raid.getMaxPlayers() + "人]")
                        .color(NamedTextColor.GRAY)));
            }
        }

        player.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD));
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("用法: /raid info <副本ID>").color(NamedTextColor.RED));
            return;
        }

        String raidId = args[1];
        Raid raid = plugin.getConfigManager().getRaid(raidId);
        if (raid == null) {
            player.sendMessage(Component.text("副本不存在: " + raidId).color(NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("  " + raid.getName()).color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD));

        for (String desc : raid.getDescription()) {
            player.sendMessage(Component.text(desc));
        }

        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("人数限制: ").color(NamedTextColor.GRAY)
            .append(Component.text(raid.getMinPlayers() + "-" + raid.getMaxPlayers() + " 人").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("时间限制: ").color(NamedTextColor.GRAY)
            .append(Component.text((raid.getTotalTimeLimit() / 60) + " 分钟").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("join", "leave", "list", "info"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("info")) {
                completions.addAll(plugin.getConfigManager().getRaidIds());
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(lastArg))
            .collect(Collectors.toList());
    }
}
