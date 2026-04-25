package cn.guangdian.trade.command;

import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.SubCommand;
import cn.guangdian.rpgcore.command.Description;
import cn.guangdian.trade.GuangDianTrade;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 交易命令 - 使用 RPGCore CommandFramework
 */
@CommandInfo(name = "trade", description = "光点交易系统", permission = "guangdian.trade.use", playerOnly = true)
public class TradeCommand extends BaseCommand {

    private final GuangDianTrade plugin;

    public TradeCommand(GuangDianTrade plugin) {
        this.plugin = plugin;
    }

    /**
     * 显示帮助信息
     */
    @SubCommand(name = "")
    @Description("显示交易帮助信息")
    public void showHelp(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        
        player.sendMessage(msg.colorize("<gold>===== 光点交易系统 ====="));
        player.sendMessage(msg.colorize("<yellow>蹲下 + 右键玩家 <gray>- 发送交易请求"));
        player.sendMessage(msg.colorize("<yellow>对方蹲下 + 右键你 <gray>- 接受交易请求"));
        player.sendMessage(msg.colorize("<yellow>点击确认按钮 <gray>- 确认交易"));
        player.sendMessage(msg.colorize("<yellow>/trade cancel <gray>- 取消交易"));
        player.sendMessage(msg.colorize("<gray>双方确认后等待倒计时完成交易"));
    }

    /**
     * 取消当前交易
     */
    @SubCommand(name = "cancel")
    @Description("取消当前进行的交易")
    public void cancelTrade(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        
        if (plugin.isInTradeAPI(player.getUniqueId())) {
            plugin.cancelTradeAPI(player.getUniqueId());
        } else {
            ctx.sendError("你没有正在进行的交易!");
        }
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        String[] args = context.getArgs();
        
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
