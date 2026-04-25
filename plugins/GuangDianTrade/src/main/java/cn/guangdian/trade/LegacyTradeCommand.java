package cn.guangdian.trade;

import cn.guangdian.rpgcore.message.MiniMessageService;
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
    private final MiniMessageService miniMessage;

    public LegacyTradeCommand(GuangDianTrade plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessageService.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(miniMessage.red("该命令只能由玩家执行!"));
            return true;
        }

        if (!player.hasPermission("guangdian.trade.use")) {
            player.sendMessage(miniMessage.red("你没有权限使用交易功能!"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(miniMessage.gold("===== 光点交易系统 ====="));
            player.sendMessage(miniMessage.yellow("蹲下 + 右键玩家 ").append(miniMessage.colorize("<gray>- 发送交易请求")));
            player.sendMessage(miniMessage.yellow("对方蹲下 + 右键你 ").append(miniMessage.colorize("<gray>- 接受交易请求")));
            player.sendMessage(miniMessage.yellow("点击确认按钮 ").append(miniMessage.colorize("<gray>- 确认交易")));
            player.sendMessage(miniMessage.yellow("/trade cancel ").append(miniMessage.colorize("<gray>- 取消交易")));
            player.sendMessage(miniMessage.colorize("<gray>双方确认后等待倒计时完成交易"));
            return true;
        }

        if (args[0].equalsIgnoreCase("cancel")) {
            if (plugin.isInTradeAPI(player.getUniqueId())) {
                plugin.cancelTradeAPI(player.getUniqueId());
            } else {
                player.sendMessage(miniMessage.red("你没有正在进行的交易!"));
            }
            return true;
        }

        player.sendMessage(miniMessage.yellow("用法: /trade [cancel]"));
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
