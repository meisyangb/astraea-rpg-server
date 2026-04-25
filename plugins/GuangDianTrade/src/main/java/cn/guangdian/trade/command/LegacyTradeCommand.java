package cn.guangdian.trade.command;

import cn.guangdian.trade.GuangDianTrade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 交易命令 - 传统 Bukkit 实现 (降级方案)
 * 当 RPGCore CommandFramework 不可用时使用
 */
public class LegacyTradeCommand implements CommandExecutor, TabCompleter {

    private final GuangDianTrade plugin;

    public LegacyTradeCommand(GuangDianTrade plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("该命令只能由玩家执行").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            return showHelp(player);
        }

        switch (args[0].toLowerCase()) {
            case "cancel":
                return handleCancel(player);
            case "help":
            default:
                return showHelp(player);
        }
    }

    private boolean showHelp(Player player) {
        player.sendMessage(Component.text("===== 光点交易系统 =====").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("蹲下 + 右键玩家 ").color(NamedTextColor.YELLOW)
                .append(Component.text("- 发送交易请求").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("对方蹲下 + 右键你 ").color(NamedTextColor.YELLOW)
                .append(Component.text("- 接受交易请求").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("点击确认按钮 ").color(NamedTextColor.YELLOW)
                .append(Component.text("- 确认交易").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/trade cancel ").color(NamedTextColor.YELLOW)
                .append(Component.text("- 取消交易").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("双方确认后等待倒计时完成交易").color(NamedTextColor.GRAY));
        return true;
    }

    private boolean handleCancel(Player player) {
        if (plugin.isInTradeAPI(player.getUniqueId())) {
            plugin.cancelTradeAPI(player.getUniqueId());
            player.sendMessage(Component.text("已取消交易!").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("你没有正在进行的交易!").color(NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            if ("cancel".startsWith(partial)) {
                completions.add("cancel");
            }
            if ("help".startsWith(partial)) {
                completions.add("help");
            }
        }

        return completions;
    }
}
