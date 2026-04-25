package cn.guangdian.monthlycard.command;

import cn.guangdian.monthlycard.GuangDianMonthlyCard;
import cn.guangdian.monthlycard.data.MonthlyCardData;
import cn.guangdian.monthlycard.data.MonthlyCardType;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.SubCommand;
import cn.guangdian.rpgcore.command.Description;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 月卡命令 - 使用 RPGCore CommandFramework
 */
@CommandInfo(name = "monthlycard", description = "月卡系统管理", permission = "guangdian.monthlycard.use")
public class MonthlyCardCommandFramework extends BaseCommand {

    private final GuangDianMonthlyCard plugin;

    public MonthlyCardCommandFramework(GuangDianMonthlyCard plugin) {
        this.plugin = plugin;
    }

    /**
     * 显示帮助信息
     */
    @SubCommand(name = "")
    @Description("显示帮助信息并打开主菜单")
    public void showHelp(CommandContext ctx) {
        Player player = ctx.getPlayer();
        if (player != null) {
            plugin.getMonthlyCardGUI().openMainMenu(player);
        } else {
            super.showHelp(ctx.getSender());
        }
    }

    /**
     * 查看月卡信息
     */
    @SubCommand(name = "info")
    @Description("查看月卡信息")
    public void info(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        MonthlyCardData data = plugin.getPlayerData(player.getUniqueId());

        ctx.sendMessage("<gold>========== 月卡信息 ==========");

        if (data.hasActiveCard()) {
            Optional<MonthlyCardType> typeOpt = plugin.getCardType(data.getCardType());
            String typeName = typeOpt.map(MonthlyCardType::getDisplayName).orElse(data.getCardType());

            ctx.sendMessage("<yellow>月卡类型: <white>" + typeName);
            ctx.sendMessage("<yellow>剩余天数: <green>" + data.getRemainingDaysInt() + " 天");
            ctx.sendMessage("<yellow>已签到天数: <aqua>" + data.getTotalClaimedDays() + " 天");

            if (data.canClaimToday()) {
                ctx.sendMessage("<yellow>今日奖励: <green>可领取");
            } else {
                ctx.sendMessage("<yellow>今日奖励: <gray>已领取");
            }
        } else {
            ctx.sendMessage("<gray>你还没有激活月卡");
            ctx.sendMessage("<yellow>使用 /monthlycard list 查看可购买的月卡");
        }
    }

    /**
     * 领取每日奖励
     */
    @SubCommand(name = "claim")
    @Description("领取每日月卡奖励")
    public void claim(CommandContext ctx) {
        Player player = ctx.requirePlayer();

        if (!plugin.hasActiveCard(player)) {
            ctx.sendError("你还没有激活月卡");
            return;
        }

        if (!plugin.canClaimToday(player.getUniqueId())) {
            ctx.sendWarning("今日奖励已领取");
            return;
        }

        if (plugin.claimDailyReward(player.getUniqueId())) {
            ctx.sendSuccess("成功领取今日月卡奖励!");
        } else {
            ctx.sendError("领取奖励失败");
        }
    }

    /**
     * 购买月卡
     */
    @SubCommand(name = "buy", minArgs = 1)
    @Description("购买月卡")
    public void buy(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        String cardTypeId = ctx.getStringArg(0).toLowerCase();

        Optional<MonthlyCardType> typeOpt = plugin.getCardType(cardTypeId);
        if (typeOpt.isEmpty()) {
            ctx.sendError("未知的月卡类型: " + cardTypeId);
            return;
        }

        MonthlyCardType type = typeOpt.get();

        ctx.sendMessage("<yellow>正在购买月卡: " + type.getDisplayName());
        ctx.sendMessage("<aqua>价格: " + type.getPrice() + " " + type.getCurrencyType());

        if (plugin.getService().activateCard(player.getUniqueId(), cardTypeId, true)) {
            ctx.sendSuccess("成功激活月卡: " + type.getDisplayName());
        } else {
            ctx.sendError("购买失败，请检查余额是否充足");
        }
    }

    /**
     * 查看可用月卡列表
     */
    @SubCommand(name = "list")
    @Description("查看可用月卡列表")
    public void list(CommandContext ctx) {
        CommandSender sender = ctx.getSender();

        msg.send(sender, "<gold>========== 可用月卡 ==========");

        for (MonthlyCardType type : plugin.getCardManager().getAllCardTypes()) {
            msg.send(sender, "<yellow>- " + type.getId() + " <white>(" + type.getDisplayName() + ")");
            msg.send(sender, "<aqua>  价格: " + type.getPrice() + " " + type.getCurrencyType());
            msg.send(sender, "<gray>  时长: " + type.getDurationDays() + " 天");
        }
    }

    /**
     * 给予玩家月卡 (管理员)
     */
    @SubCommand(name = "give", permission = "guangdian.monthlycard.admin", minArgs = 2)
    @Description("给予玩家月卡")
    public void give(CommandContext ctx) {
        CommandSender sender = ctx.getSender();
        String targetName = ctx.getStringArg(0);
        String cardTypeId = ctx.getStringArg(1).toLowerCase();
        int days = ctx.getArgCount() > 2 ? ctx.getIntArg(2) : 30;

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        Optional<MonthlyCardType> typeOpt = plugin.getCardType(cardTypeId);
        if (typeOpt.isEmpty()) {
            ctx.sendError("未知的月卡类型: " + cardTypeId);
            return;
        }

        plugin.getCardManager().setCard(target.getUniqueId(), cardTypeId, days);
        ctx.sendSuccess("已给予 " + target.getName() + " " + cardTypeId + " 月卡 " + days + " 天");

        if (target.isOnline()) {
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null) {
                msg.send(onlineTarget, "<gold>你获得了月卡: " + typeOpt.get().getDisplayName());
            }
        }
    }

    /**
     * 移除玩家月卡 (管理员)
     */
    @SubCommand(name = "remove", permission = "guangdian.monthlycard.admin", minArgs = 1)
    @Description("移除玩家月卡")
    public void remove(CommandContext ctx) {
        String targetName = ctx.getStringArg(0);
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        plugin.getCardManager().removeCard(target.getUniqueId());
        ctx.sendSuccess("已移除 " + target.getName() + " 的月卡");
    }

    /**
     * 延长玩家月卡 (管理员)
     */
    @SubCommand(name = "extend", permission = "guangdian.monthlycard.admin", minArgs = 2)
    @Description("延长玩家月卡")
    public void extend(CommandContext ctx) {
        String targetName = ctx.getStringArg(0);
        int days = ctx.getIntArg(1);

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        plugin.getCardManager().extendCard(target.getUniqueId(), days);
        ctx.sendSuccess("已延长 " + target.getName() + " 的月卡 " + days + " 天");
    }

    /**
     * 重载配置 (管理员)
     */
    @SubCommand(name = "reload", permission = "guangdian.monthlycard.admin")
    @Description("重载月卡配置")
    public void reload(CommandContext ctx) {
        plugin.reloadConfig();
        plugin.getCardManager().loadCardTypes();
        ctx.sendSuccess("配置已重载");
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        String[] args = context.getArgs();
        String subCommandName = subCommandMethod.getAnnotation(SubCommand.class).name();

        return switch (subCommandName.toLowerCase()) {
            case "buy" -> {
                if (args.length == 1) {
                    String partial = args[0].toLowerCase();
                    yield plugin.getCardManager().getAllCardTypes().stream()
                            .map(MonthlyCardType::getId)
                            .filter(id -> id.toLowerCase().startsWith(partial))
                            .collect(Collectors.toList());
                }
                yield new ArrayList<>();
            }
            case "give" -> {
                if (args.length == 1) {
                    String partial = args[0].toLowerCase();
                    yield Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(partial))
                            .collect(Collectors.toList());
                } else if (args.length == 2) {
                    String partial = args[1].toLowerCase();
                    yield plugin.getCardManager().getAllCardTypes().stream()
                            .map(MonthlyCardType::getId)
                            .filter(id -> id.toLowerCase().startsWith(partial))
                            .collect(Collectors.toList());
                }
                yield new ArrayList<>();
            }
            case "remove", "extend" -> {
                if (args.length == 1) {
                    String partial = args[0].toLowerCase();
                    yield Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(partial))
                            .collect(Collectors.toList());
                }
                yield new ArrayList<>();
            }
            default -> new ArrayList<>();
        };
    }
}
