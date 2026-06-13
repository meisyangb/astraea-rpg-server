package cn.guangdian.particleblocker;

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

/**
 * 粒子屏蔽命令
 */
public class ParticleCommand implements CommandExecutor, TabCompleter {

    private final GuangDianParticleBlocker plugin;
    private final ParticleConfig config;

    public ParticleCommand(GuangDianParticleBlocker plugin, ParticleConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help" -> sendHelp(sender);
            case "status" -> sendStatus(sender);
            case "blockall" -> handleBlockAll(sender);
            case "allowall" -> handleAllowAll(sender);
            case "block" -> handleBlock(sender, args);
            case "allow" -> handleAllow(sender, args);
            case "list" -> sendList(sender);
            case "toggle" -> handleToggle(sender);
            case "stats" -> sendStats(sender);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("========== 粒子屏蔽插件 ==========").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/particleblocker help").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 显示帮助").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/particleblocker status").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 查看状态").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/particleblocker blockall").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 屏蔽所有粒子").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/particleblocker allowall").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 允许所有粒子").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/particleblocker block <粒子类型>").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 屏蔽指定粒子").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/particleblocker allow <粒子类型>").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 允许指定粒子").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/particleblocker list").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 查看屏蔽列表").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/particleblocker toggle").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 切换个人屏蔽状态").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/particleblocker stats").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 查看统计信息").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/particleblocker reload").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 重载配置").color(NamedTextColor.WHITE)));
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(Component.text("========== 粒子屏蔽状态 ==========").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("全局屏蔽: ").color(NamedTextColor.YELLOW)
            .append(Component.text(config.isGlobalBlocked() ? "是" : "否")
                .color(config.isGlobalBlocked() ? NamedTextColor.RED : NamedTextColor.GREEN)));
        sender.sendMessage(Component.text("白名单模式: ").color(NamedTextColor.YELLOW)
            .append(Component.text(config.isWhitelistMode() ? "是" : "否")
                .color(config.isWhitelistMode() ? NamedTextColor.AQUA : NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("屏蔽粒子数: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.valueOf(config.getBlockedTypes().size()))
                .color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("白名单粒子数: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.valueOf(config.getWhitelist().size()))
                .color(NamedTextColor.WHITE)));

        if (sender instanceof Player player) {
            boolean blocked = plugin.getListener().isPlayerBlocked(player.getUniqueId());
            sender.sendMessage(Component.text("个人屏蔽: ").color(NamedTextColor.YELLOW)
                .append(Component.text(blocked ? "是" : "否")
                    .color(blocked ? NamedTextColor.RED : NamedTextColor.GREEN)));
        }
    }

    private void handleBlockAll(CommandSender sender) {
        config.setGlobalBlocked(true);
        sender.sendMessage(Component.text("已屏蔽所有粒子!").color(NamedTextColor.RED));
        plugin.getLogger().info("玩家 " + sender.getName() + " 启用了全局粒子屏蔽");
    }

    private void handleAllowAll(CommandSender sender) {
        config.setGlobalBlocked(false);
        config.getBlockedTypes().clear();
        config.save();
        sender.sendMessage(Component.text("已允许所有粒子!").color(NamedTextColor.GREEN));
        plugin.getLogger().info("玩家 " + sender.getName() + " 禁用了全局粒子屏蔽");
    }

    private void handleBlock(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /particleblocker block <粒子类型>")
                .color(NamedTextColor.RED));
            return;
        }

        String particleName = args[1].toUpperCase();
        try {
            org.bukkit.Particle.valueOf(particleName);
            config.addBlockedType(particleName);
            sender.sendMessage(Component.text("已屏蔽粒子: ").color(NamedTextColor.YELLOW)
                .append(Component.text(particleName).color(NamedTextColor.RED)));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("无效的粒子类型: ").color(NamedTextColor.RED)
                .append(Component.text(particleName).color(NamedTextColor.WHITE)));
        }
    }

    private void handleAllow(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /particleblocker allow <粒子类型>")
                .color(NamedTextColor.RED));
            return;
        }

        String particleName = args[1].toUpperCase();
        config.removeBlockedType(particleName);
        sender.sendMessage(Component.text("已允许粒子: ").color(NamedTextColor.YELLOW)
            .append(Component.text(particleName).color(NamedTextColor.GREEN)));
    }

    private void sendList(CommandSender sender) {
        sender.sendMessage(Component.text("========== 屏蔽粒子列表 ==========").color(NamedTextColor.GOLD));

        var blocked = config.getBlockedTypes();
        if (blocked.isEmpty()) {
            sender.sendMessage(Component.text("无屏蔽粒子").color(NamedTextColor.GRAY));
        } else {
            int count = 0;
            for (String type : blocked) {
                sender.sendMessage(Component.text("  - ").color(NamedTextColor.GRAY)
                    .append(Component.text(type).color(NamedTextColor.RED)));
                count++;
                if (count >= 20) {
                    sender.sendMessage(Component.text("  ... 还有 " + (blocked.size() - 20) + " 个")
                        .color(NamedTextColor.GRAY));
                    break;
                }
            }
        }
    }

    private void handleToggle(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("此命令只能由玩家执行").color(NamedTextColor.RED));
            return;
        }

        boolean current = plugin.getListener().isPlayerBlocked(player.getUniqueId());
        plugin.getListener().setPlayerBlocked(player.getUniqueId(), !current);

        sender.sendMessage(Component.text("个人粒子屏蔽: ").color(NamedTextColor.YELLOW)
            .append(Component.text(!current ? "开启" : "关闭")
                .color(!current ? NamedTextColor.RED : NamedTextColor.GREEN)));
    }

    private void sendStats(CommandSender sender) {
        var listener = plugin.getListener();
        sender.sendMessage(Component.text("========== 粒子统计 ==========").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("已屏蔽粒子: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.valueOf(listener.getTotalBlocked()))
                .color(NamedTextColor.RED)));
        sender.sendMessage(Component.text("已允许粒子: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.valueOf(listener.getTotalAllowed()))
                .color(NamedTextColor.GREEN)));

        long total = listener.getTotalBlocked() + listener.getTotalAllowed();
        if (total > 0) {
            double blockRate = (double) listener.getTotalBlocked() / total * 100;
            sender.sendMessage(Component.text("屏蔽率: ").color(NamedTextColor.YELLOW)
                .append(Component.text(String.format("%.2f%%", blockRate))
                    .color(NamedTextColor.AQUA)));
        }
    }

    private void handleReload(CommandSender sender) {
        config.load();
        sender.sendMessage(Component.text("配置已重载!").color(NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList(
                "help", "status", "blockall", "allowall", "block", "allow", "list", "toggle", "stats", "reload"
            );
            return subCommands.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("block") || args[0].equalsIgnoreCase("allow"))) {
            return Arrays.stream(org.bukkit.Particle.values())
                .map(p -> p.name().toLowerCase())
                .filter(s -> s.startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}