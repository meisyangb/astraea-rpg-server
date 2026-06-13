package cn.guangdian.dynamicview.command;

import cn.guangdian.dynamicview.DynamicViewPlugin;
import cn.guangdian.dynamicview.data.PlayerViewData;
import cn.guangdian.dynamicview.data.PlayerViewData.ViewTier;
import cn.guangdian.dynamicview.util.ConfigManager;
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

/**
 * 动态视距命令处理器
 */
public class DynamicViewCommand implements CommandExecutor, TabCompleter {

    private final DynamicViewPlugin plugin;

    public DynamicViewCommand(DynamicViewPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "stats" -> handleStats(sender);
            case "info" -> handleInfo(sender, args);
            case "help" -> sendHelp(sender);
            default -> {
                sender.sendMessage(Component.text("未知命令: " + args[0]).color(NamedTextColor.RED));
                sendHelp(sender);
            }
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("guangdiandynamicview.reload")) {
            sender.sendMessage(Component.text("你没有权限执行此命令").color(NamedTextColor.RED));
            return;
        }
        plugin.reload();
        sender.sendMessage(Component.text("配置已重载").color(NamedTextColor.GREEN));
    }

    private void handleStats(CommandSender sender) {
        if (!sender.hasPermission("guangdiandynamicview.stats")) {
            sender.sendMessage(Component.text("你没有权限执行此命令").color(NamedTextColor.RED));
            return;
        }

        ConfigManager cfg = plugin.getConfigManager();

        sender.sendMessage(Component.text("========== 动态视距统计 ==========").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("在线玩家: ").color(NamedTextColor.YELLOW)
            .append(Component.text(Bukkit.getOnlinePlayers().size()).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("调整次数: ").color(NamedTextColor.YELLOW)
            .append(Component.text(plugin.getViewDistanceManager().getTotalAdjustments()).color(NamedTextColor.WHITE)));

        // 全局默认挡位
        sender.sendMessage(Component.text("--- 全局默认挡位 ---").color(NamedTextColor.AQUA));
        sender.sendMessage(Component.text("跑图: ").color(NamedTextColor.YELLOW)
            .append(Component.text(cfg.getDefaultTierViewDistance(ViewTier.EXPLORING) + "").color(NamedTextColor.GREEN))
            .append(Component.text("  战斗: ").color(NamedTextColor.YELLOW))
            .append(Component.text(cfg.getDefaultTierViewDistance(ViewTier.COMBAT) + "").color(NamedTextColor.RED))
            .append(Component.text("  挂机: ").color(NamedTextColor.YELLOW))
            .append(Component.text(cfg.getDefaultTierViewDistance(ViewTier.AFK) + "").color(NamedTextColor.GRAY)));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        Player target;
        if (args.length < 2) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("请指定玩家名称").color(NamedTextColor.RED));
                return;
            }
            target = player;
        } else {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("玩家不在线: " + args[1]).color(NamedTextColor.RED));
                return;
            }
        }

        ConfigManager cfg = plugin.getConfigManager();
        String worldName = target.getWorld().getName();

        sender.sendMessage(Component.text("========== 玩家信息: " + target.getName() + " ==========").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("当前视距: ").color(NamedTextColor.YELLOW)
            .append(Component.text(target.getViewDistance() + " 区块").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("所在世界: ").color(NamedTextColor.YELLOW)
            .append(Component.text(worldName).color(NamedTextColor.WHITE)));

        // 世界配置
        if (cfg.isWorldFixed(worldName)) {
            sender.sendMessage(Component.text("世界模式: ").color(NamedTextColor.YELLOW)
                .append(Component.text("固定视距 " + cfg.getWorldFixedView(worldName)).color(NamedTextColor.WHITE)));
        } else {
            sender.sendMessage(Component.text("世界模式: ").color(NamedTextColor.YELLOW)
                .append(Component.text("动态挡位").color(NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("  跑图: ").color(NamedTextColor.GREEN)
                .append(Component.text(cfg.getWorldTierViewDistance(worldName, ViewTier.EXPLORING) + "").color(NamedTextColor.WHITE))
                .append(Component.text("  战斗: ").color(NamedTextColor.RED))
                .append(Component.text(cfg.getWorldTierViewDistance(worldName, ViewTier.COMBAT) + "").color(NamedTextColor.WHITE))
                .append(Component.text("  挂机: ").color(NamedTextColor.GRAY))
                .append(Component.text(cfg.getWorldTierViewDistance(worldName, ViewTier.AFK) + "").color(NamedTextColor.WHITE)));
        }

        // 当前挡位
        PlayerViewData data = plugin.getViewDistanceManager().getPlayerData(target.getUniqueId());
        if (data != null) {
            ViewTier tier = data.getCurrentTier();
            String tierName = switch (tier) {
                case EXPLORING -> "跑图";
                case COMBAT -> "战斗";
                case AFK -> "挂机";
            };
            NamedTextColor tierColor = switch (tier) {
                case EXPLORING -> NamedTextColor.GREEN;
                case COMBAT -> NamedTextColor.RED;
                case AFK -> NamedTextColor.GRAY;
            };
            sender.sendMessage(Component.text("当前挡位: ").color(NamedTextColor.YELLOW)
                .append(Component.text(tierName).color(tierColor)));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("========== 动态视距帮助 ==========").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/dview reload").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 重载配置").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/dview stats").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 查看统计信息").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/dview info [玩家]").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 查看玩家挡位信息").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/dview help").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 显示帮助").color(NamedTextColor.WHITE)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList("reload", "stats", "info", "help");
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                .toList();
        }
        return new ArrayList<>();
    }
}
