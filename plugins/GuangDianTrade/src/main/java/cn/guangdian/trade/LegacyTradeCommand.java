package cn.guangdian.trade;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 传统交易命令处理器 (降级处理 - 当 RPGCore 不可用时使用)
 */
public class LegacyTradeCommand implements CommandExecutor, TabCompleter {

    private final GuangDianTrade plugin;

    public LegacyTradeCommand(GuangDianTrade plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c该命令只能由玩家执行!");
            return true;
        }

        if (!player.hasPermission("guangdian.trade.use")) {
            player.sendMessage("§c你没有权限使用交易功能!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§6===== 光点交易系统 =====");
            player.sendMessage("§e蹲下 + 右键玩家 §7- 发送交易请求");
            player.sendMessage("§e对方蹲下 + 右键你 §7- 接受交易请求");
            player.sendMessage("§e点击确认按钮 §7- 确认交易");
            player.sendMessage("§e/trade cancel §7- 取消交易");
            player.sendMessage("§7双方确认后等待倒计时完成交易");
            return true;
        }

        if (args[0].equalsIgnoreCase("cancel")) {
            if (plugin.isInTradeAPI(player.getUniqueId())) {
                plugin.cancelTradeAPI(player.getUniqueId());
            } else {
                player.sendMessage("§c你没有正在进行的交易!");
            }
            return true;
        }

        player.sendMessage("§e用法: /trade [cancel]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            
            if ("cancel".startsWith(partial)) {
                completions.add("cancel");
            }
            
            return completions;
        }
        
        return new ArrayList<>();
    }
}
