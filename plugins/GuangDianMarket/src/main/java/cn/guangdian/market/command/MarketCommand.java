package cn.guangdian.market.command;

import cn.guangdian.market.GuangDianMarket;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.SubCommand;
import cn.guangdian.rpgcore.message.MessageServiceImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 全球市场命令 - 使用 RPGCore CommandFramework
 */
@CommandInfo(name = "market", description = "全球市场系统", permission = "guangdian.market.use")
public class MarketCommand extends BaseCommand {

    private final GuangDianMarket plugin;
    private final MessageServiceImpl msg;

    public MarketCommand(GuangDianMarket plugin) {
        this.plugin = plugin;
        this.msg = MessageServiceImpl.getInstance();
    }

    /**
     * 打开市场主界面
     */
    @SubCommand(name = "", playerOnly = true)
    public void openMarket(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        plugin.openMarketGUI(player, 1);
    }

    /**
     * 上架物品
     */
    @SubCommand(name = "sell", playerOnly = true, minArgs = 1, maxArgs = 2)
    public void sell(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        String[] args = ctx.getArgs();

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            msg.sendError(player, "请手持要出售的物品!");
            return;
        }

        try {
            long price = parseAmount(args[0]);

            // 解析货币类型
            GuangDianMarket.CurrencyType currencyType = GuangDianMarket.CurrencyType.POINTS;
            if (args.length >= 2) {
                String typeArg = args[1].toLowerCase();
                if (typeArg.equals("eco") || typeArg.equals("economy") || typeArg.equals("金币") || typeArg.equals("金")) {
                    if (!plugin.getConfig().getBoolean("currency.economy.enabled", true)) {
                        msg.sendError(player, "经济系统未启用，无法使用金币上架!");
                        return;
                    }
                    currencyType = GuangDianMarket.CurrencyType.ECONOMY;
                } else if (typeArg.equals("points") || typeArg.equals("point") || typeArg.equals("点券") || typeArg.equals("点")) {
                    if (!plugin.getConfig().getBoolean("currency.points.enabled", true)) {
                        msg.sendError(player, "点券系统未启用，无法使用点券上架!");
                        return;
                    }
                    currencyType = GuangDianMarket.CurrencyType.POINTS;
                }
            } else {
                // 默认使用点券
                if (!plugin.getConfig().getBoolean("currency.points.enabled", true) &&
                    plugin.getConfig().getBoolean("currency.economy.enabled", true)) {
                    currencyType = GuangDianMarket.CurrencyType.ECONOMY;
                    msg.sendWarning(player, "点券系统未启用，自动使用金币作为货币");
                }
            }

            plugin.listItem(player, item, price, currencyType);
        } catch (NumberFormatException e) {
            msg.sendError(player, "无效的价格!");
        }
    }

    /**
     * 查看我的上架
     */
    @SubCommand(name = "my", playerOnly = true)
    public void myListings(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        plugin.openMyListingsGUI(player);
    }

    /**
     * 查看帮助
     */
    @SubCommand(name = "help")
    public void help(CommandContext ctx) {
        CommandSender sender = ctx.getSender();
        msg.send(sender, "<gold>===== 全球市场帮助 =====");
        msg.send(sender, "<yellow>/market <gray>- 打开市场");
        msg.send(sender, "<yellow>/market sell <价格> [货币] <gray>- 上架手持物品");
        msg.send(sender, "<yellow>/market my <gray>- 查看我的上架");
        msg.send(sender, "<gray>货币: points(点券) / eco(金币)");
    }

    /**
     * 重新加载配置（管理员）
     */
    @SubCommand(name = "reload", permission = "guangdian.market.admin")
    public void reload(CommandContext ctx) {
        CommandSender sender = ctx.getSender();
        plugin.reloadConfig();
        msg.sendSuccess(sender, "配置已重新加载!");
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        String[] args = context.getArgs();
        String subCommandName = subCommandMethod.getAnnotation(SubCommand.class).name();

        if (subCommandName.equals("sell")) {
            if (args.length == 1) {
                // 价格建议
                List<String> suggestions = new ArrayList<>();
                suggestions.add("100");
                suggestions.add("1000");
                suggestions.add("10000");
                return suggestions;
            } else if (args.length == 2) {
                // 货币类型建议
                List<String> suggestions = new ArrayList<>();
                suggestions.add("points");
                suggestions.add("eco");
                return suggestions;
            }
        }

        return new ArrayList<>();
    }

    /**
     * 解析金额（支持 k, m, w 后缀）
     */
    private long parseAmount(String str) throws NumberFormatException {
        str = str.toLowerCase().replace(",", "");
        if (str.endsWith("k")) {
            return (long) (Double.parseDouble(str.substring(0, str.length() - 1)) * 1000);
        } else if (str.endsWith("m")) {
            return (long) (Double.parseDouble(str.substring(0, str.length() - 1)) * 1000000);
        } else if (str.endsWith("w") || str.endsWith("万")) {
            return (long) (Double.parseDouble(str.substring(0, str.length() - 1)) * 10000);
        }
        return Long.parseLong(str);
    }
}
