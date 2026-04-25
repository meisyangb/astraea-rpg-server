package cn.guangdian.monthlycard.command;

import cn.guangdian.monthlycard.GuangDianMonthlyCard;
import cn.guangdian.monthlycard.data.MonthlyCardData;
import cn.guangdian.monthlycard.data.MonthlyCardType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 月卡命令 - 传统 Bukkit 实现 (降级方案)
 * 当 RPGCore CommandFramework 不可用时使用
 */
public class LegacyMonthlyCardCommand implements CommandExecutor, TabCompleter {

    private final GuangDianMonthlyCard plugin;

    public LegacyMonthlyCardCommand(GuangDianMonthlyCard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                plugin.getMonthlyCardGUI().openMainMenu((Player) sender);
                return true;
            }
            return handleInfo(sender);
        }

        switch (args[0].toLowerCase()) {
            case "info":
                return handleInfo(sender);
            case "claim":
            case "sign":
            case "reward":
                return handleClaim(sender);
            case "buy":
            case "purchase":
                return handleBuy(sender, args);
            case "list":
                return handleList(sender);
            case "give":
                return handleGive(sender, args);
            case "remove":
            case "take":
                return handleRemove(sender, args);
            case "extend":
                return handleExtend(sender, args);
            case "reload":
                return handleReload(sender);
            case "help":
            default:
                return handleHelp(sender);
        }
    }

    private boolean handleInfo(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("只有玩家可以查看月卡信息").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        MonthlyCardData data = plugin.getPlayerData(player.getUniqueId());

        player.sendMessage(Component.text("========== 月卡信息 ==========").color(NamedTextColor.GOLD));

        if (data.hasActiveCard()) {
            Optional<MonthlyCardType> typeOpt = plugin.getCardType(data.getCardType());
            String typeName = typeOpt.map(MonthlyCardType::getDisplayName).orElse(data.getCardType());

            player.sendMessage(Component.text("月卡类型: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(typeName).color(NamedTextColor.WHITE)));
            player.sendMessage(Component.text("剩余天数: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(data.getRemainingDaysInt() + " 天").color(NamedTextColor.GREEN)));
            player.sendMessage(Component.text("已签到天数: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(data.getTotalClaimedDays() + " 天").color(NamedTextColor.AQUA)));

            if (data.canClaimToday()) {
                player.sendMessage(Component.text("今日奖励: ").color(NamedTextColor.YELLOW)
                        .append(Component.text("可领取").color(NamedTextColor.GREEN)));
            } else {
                player.sendMessage(Component.text("今日奖励: ").color(NamedTextColor.YELLOW)
                        .append(Component.text("已领取").color(NamedTextColor.GRAY)));
            }
        } else {
            player.sendMessage(Component.text("你还没有激活月卡").color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("使用 /monthlycard list 查看可购买的月卡").color(NamedTextColor.YELLOW));
        }

        return true;
    }

    private boolean handleClaim(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("只有玩家可以领取奖励").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (!plugin.hasActiveCard(player)) {
            player.sendMessage(Component.text("你还没有激活月卡").color(NamedTextColor.RED));
            return true;
        }

        if (!plugin.canClaimToday(player.getUniqueId())) {
            player.sendMessage(Component.text("今日奖励已领取").color(NamedTextColor.YELLOW));
            return true;
        }

        if (plugin.claimDailyReward(player.getUniqueId())) {
            player.sendMessage(Component.text("成功领取今日月卡奖励!").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("领取奖励失败").color(NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleBuy(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("只有玩家可以购买月卡").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /monthlycard buy <月卡类型>").color(NamedTextColor.YELLOW));
            return true;
        }

        String cardTypeId = args[1].toLowerCase();
        Optional<MonthlyCardType> typeOpt = plugin.getCardType(cardTypeId);

        if (typeOpt.isEmpty()) {
            sender.sendMessage(Component.text("未知的月卡类型: " + cardTypeId).color(NamedTextColor.RED));
            return true;
        }

        MonthlyCardType type = typeOpt.get();
        Player player = (Player) sender;

        player.sendMessage(Component.text("正在购买月卡: " + type.getDisplayName()).color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("价格: " + type.getPrice() + " " + type.getCurrencyType()).color(NamedTextColor.AQUA));

        if (plugin.getService().activateCard(player.getUniqueId(), cardTypeId, true)) {
            player.sendMessage(Component.text("成功激活月卡: " + type.getDisplayName()).color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("购买失败，请检查余额是否充足").color(NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(Component.text("========== 可用月卡 ==========").color(NamedTextColor.GOLD));

        for (MonthlyCardType type : plugin.getCardManager().getAllCardTypes()) {
            sender.sendMessage(Component.text("- " + type.getId()).color(NamedTextColor.YELLOW)
                    .append(Component.text(" (" + type.getDisplayName() + ")").color(NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("  价格: " + type.getPrice() + " " + type.getCurrencyType())
                    .color(NamedTextColor.AQUA));
            sender.sendMessage(Component.text("  时长: " + type.getDurationDays() + " 天")
                    .color(NamedTextColor.GRAY));
        }

        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.monthlycard.admin")) {
            sender.sendMessage(Component.text("没有权限").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("用法: /monthlycard give <玩家> <月卡类型> [天数]")
                    .color(NamedTextColor.YELLOW));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String cardTypeId = args[2].toLowerCase();
        int days = args.length > 3 ? Integer.parseInt(args[3]) : 30;

        Optional<MonthlyCardType> typeOpt = plugin.getCardType(cardTypeId);
        if (typeOpt.isEmpty()) {
            sender.sendMessage(Component.text("未知的月卡类型: " + cardTypeId).color(NamedTextColor.RED));
            return true;
        }

        plugin.getCardManager().setCard(target.getUniqueId(), cardTypeId, days);
        sender.sendMessage(Component.text("已给予 " + target.getName() + " " + cardTypeId + " 月卡 " + days + " 天")
                .color(NamedTextColor.GREEN));

        if (target.isOnline()) {
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null) {
                onlineTarget.sendMessage(Component.text("你获得了月卡: " + typeOpt.get().getDisplayName())
                        .color(NamedTextColor.GOLD));
            }
        }

        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.monthlycard.admin")) {
            sender.sendMessage(Component.text("没有权限").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /monthlycard remove <玩家>").color(NamedTextColor.YELLOW));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        plugin.getCardManager().removeCard(target.getUniqueId());
        sender.sendMessage(Component.text("已移除 " + target.getName() + " 的月卡").color(NamedTextColor.GREEN));

        return true;
    }

    private boolean handleExtend(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.monthlycard.admin")) {
            sender.sendMessage(Component.text("没有权限").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("用法: /monthlycard extend <玩家> <天数>").color(NamedTextColor.YELLOW));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        int days = Integer.parseInt(args[2]);

        plugin.getCardManager().extendCard(target.getUniqueId(), days);
        sender.sendMessage(Component.text("已延长 " + target.getName() + " 的月卡 " + days + " 天")
                .color(NamedTextColor.GREEN));

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("guangdian.monthlycard.admin")) {
            sender.sendMessage(Component.text("没有权限").color(NamedTextColor.RED));
            return true;
        }

        plugin.reloadConfig();
        plugin.getCardManager().loadCardTypes();
        sender.sendMessage(Component.text("配置已重载").color(NamedTextColor.GREEN));

        return true;
    }

    private boolean handleHelp(CommandSender sender) {
        sender.sendMessage(Component.text("========== 月卡帮助 ==========").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/monthlycard info - 查看月卡信息").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/monthlycard claim - 领取每日奖励").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/monthlycard buy <类型> - 购买月卡").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/monthlycard list - 查看可用月卡").color(NamedTextColor.YELLOW));

        if (sender.hasPermission("guangdian.monthlycard.admin")) {
            sender.sendMessage(Component.text("========== 管理命令 ==========").color(NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/monthlycard give <玩家> <类型> [天数] - 给予月卡")
                    .color(NamedTextColor.AQUA));
            sender.sendMessage(Component.text("/monthlycard remove <玩家> - 移除月卡")
                    .color(NamedTextColor.AQUA));
            sender.sendMessage(Component.text("/monthlycard extend <玩家> <天数> - 延长月卡")
                    .color(NamedTextColor.AQUA));
            sender.sendMessage(Component.text("/monthlycard reload - 重载配置")
                    .color(NamedTextColor.AQUA));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("info");
            completions.add("claim");
            completions.add("buy");
            completions.add("list");
            completions.add("help");
            if (sender.hasPermission("guangdian.monthlycard.admin")) {
                completions.add("give");
                completions.add("remove");
                completions.add("extend");
                completions.add("reload");
            }
        } else if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("buy")) {
                completions.addAll(plugin.getCardManager().getAllCardTypes().stream()
                        .map(MonthlyCardType::getId)
                        .collect(Collectors.toList()));
            } else if (subCmd.equals("give") || subCmd.equals("remove") || subCmd.equals("extend")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            completions.addAll(plugin.getCardManager().getAllCardTypes().stream()
                    .map(MonthlyCardType::getId)
                    .collect(Collectors.toList()));
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lastArg))
                .collect(Collectors.toList());
    }
}
