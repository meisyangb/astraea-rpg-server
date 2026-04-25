package cn.guangdian.cavefu.command;

import cn.guangdian.cavefu.GuangDianCaveFu;
import cn.guangdian.cavefu.cave.Cave;
import cn.guangdian.cavefu.cave.CaveLevel;
import cn.guangdian.cavefu.config.ConfigManager;
import cn.guangdian.cavefu.storage.DataManager;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.Description;
import cn.guangdian.rpgcore.command.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员洞府命令 - 使用 RPGCore CommandFramework
 *
 * @author Astraea RPG Team
 * @since 1.2.0
 */
@CommandInfo(name = "caveadmin", description = "洞府管理", permission = "guangdian.cave.admin")
public class CaveAdminCommand extends BaseCommand {
    private final GuangDianCaveFu plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;

    public CaveAdminCommand(GuangDianCaveFu plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.dataManager = plugin.getDataManager();
    }

    /**
     * 显示帮助
     */
    @SubCommand(name = "")
    @Description("显示管理命令帮助")
    public void showHelpDefault(CommandContext ctx) {
        showHelp(ctx.getSender());
    }

    /**
     * 传送至玩家洞府
     */
    @SubCommand(name = "tp", playerOnly = true, minArgs = 1)
    @Description("传送至玩家洞府")
    public void tp(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        String targetName = ctx.getStringArg(0);
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);

        Cave cave = dataManager.getCaveByOwner(target.getUniqueId());
        if (cave == null) {
            ctx.sendMessage(configManager.getMessage("target-no-cave"));
            return;
        }

        player.teleport(cave.getHomeLocation());
        ctx.sendSuccess("已传送到 " + targetName + " 的洞府");
    }

    /**
     * 删除玩家洞府
     */
    @SubCommand(name = "delete", minArgs = 1)
    @Description("删除玩家洞府")
    public void delete(CommandContext ctx) {
        String targetName = ctx.getStringArg(0);
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);

        Cave cave = dataManager.getCaveByOwner(target.getUniqueId());
        if (cave == null) {
            ctx.sendMessage(configManager.getMessage("target-no-cave"));
            return;
        }

        plugin.getCaveManager().deleteCave(target.getUniqueId());
        ctx.sendSuccess("已删除 " + targetName + " 的洞府");
    }

    /**
     * 设置洞府等级
     */
    @SubCommand(name = "setlevel", minArgs = 2)
    @Description("设置洞府等级")
    public void setLevel(CommandContext ctx) {
        String targetName = ctx.getStringArg(0);
        int level;

        try {
            level = Integer.parseInt(ctx.getStringArg(1));
        } catch (NumberFormatException e) {
            ctx.sendError("等级必须是数字！");
            return;
        }

        OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);

        Cave cave = dataManager.getCaveByOwner(target.getUniqueId());
        if (cave == null) {
            ctx.sendMessage(configManager.getMessage("target-no-cave"));
            return;
        }

        CaveLevel levelConfig = configManager.getLevel(level);
        if (levelConfig == null) {
            ctx.sendError("无效的等级！");
            return;
        }

        cave.setLevel(level);
        dataManager.save();
        ctx.sendSuccess("已将 " + targetName + " 的洞府等级设置为 " + level);
    }

    /**
     * 重新加载配置
     */
    @SubCommand(name = "reload")
    @Description("重新加载配置")
    public void reload(CommandContext ctx) {
        configManager.reload();
        ctx.sendSuccess("配置已重新加载！");
    }

    /**
     * 查看所有洞府
     */
    @SubCommand(name = "list")
    @Description("查看所有洞府")
    public void list(CommandContext ctx) {
        ctx.sendMessage("<gold>========== 洞府列表 ==========");

        int count = 0;
        for (Cave cave : dataManager.getAllCaves()) {
            CaveLevel level = configManager.getLevel(cave.getLevel());
            String levelName = level != null ? level.getName() : "未知";
            ctx.sendMessage("<yellow>" + cave.getOwnerName() + " <gray>- <white>等级: " + levelName + " 成员: " + cave.getMembers().size());
            count++;
        }

        ctx.sendMessage("<gold>总计: <white>" + count + " 个洞府");
        ctx.sendMessage("<gold>================================");
    }

    @Override
    public void showHelp(org.bukkit.command.CommandSender sender) {
        sender.sendMessage(msg.colorize("<gold>========== 洞府管理帮助 =========="));
        sender.sendMessage(msg.colorize("<yellow>/caveadmin tp <玩家> <gray>- 传送至玩家洞府"));
        sender.sendMessage(msg.colorize("<yellow>/caveadmin delete <玩家> <gray>- 删除玩家洞府"));
        sender.sendMessage(msg.colorize("<yellow>/caveadmin setlevel <玩家> <等级> <gray>- 设置洞府等级"));
        sender.sendMessage(msg.colorize("<yellow>/caveadmin reload <gray>- 重载配置"));
        sender.sendMessage(msg.colorize("<yellow>/caveadmin list <gray>- 查看所有洞府"));
        sender.sendMessage(msg.colorize("<gold>================================"));
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        List<String> completions = new ArrayList<>();
        String subCommandName = subCommandMethod.getAnnotation(SubCommand.class).name();

        if (subCommandName.equals("tp") || subCommandName.equals("delete") || subCommandName.equals("setlevel")) {
            if (context.getArgCount() == 1) {
                for (Cave cave : dataManager.getAllCaves()) {
                    completions.add(cave.getOwnerName());
                }
            } else if (subCommandName.equals("setlevel") && context.getArgCount() == 2) {
                for (int i = 1; i <= configManager.getMaxLevel(); i++) {
                    completions.add(String.valueOf(i));
                }
            }
        }

        String lastArg = context.getStringArgOrDefault(context.getArgCount() - 1, "").toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(lastArg))
            .collect(Collectors.toList());
    }
}
