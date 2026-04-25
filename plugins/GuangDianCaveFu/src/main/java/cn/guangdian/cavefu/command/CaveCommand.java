package cn.guangdian.cavefu.command;

import cn.guangdian.cavefu.GuangDianCaveFu;
import cn.guangdian.cavefu.cave.Cave;
import cn.guangdian.cavefu.cave.CaveLevel;
import cn.guangdian.cavefu.cave.CaveManager;
import cn.guangdian.cavefu.cave.CaveMember;
import cn.guangdian.cavefu.config.ConfigManager;
import cn.guangdian.cavefu.permission.PermissionType;
import cn.guangdian.cavefu.upgrade.UpgradeManager;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.Description;
import cn.guangdian.rpgcore.command.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 玩家洞府命令 - 使用 RPGCore CommandFramework
 *
 * <p>基于注解驱动的命令系统，替代传统的 onCommand 方式。</p>
 *
 * @author Astraea RPG Team
 * @since 1.2.0
 */
@CommandInfo(name = "cave", description = "洞府系统", permission = "guangdian.cave.use")
public class CaveCommand extends BaseCommand {
    private final GuangDianCaveFu plugin;
    private final CaveManager caveManager;
    private final ConfigManager configManager;
    private final UpgradeManager upgradeManager;

    public CaveCommand(GuangDianCaveFu plugin) {
        this.plugin = plugin;
        this.caveManager = plugin.getCaveManager();
        this.configManager = plugin.getConfigManager();
        this.upgradeManager = plugin.getUpgradeManager();
    }

    /**
     * 显示帮助信息
     */
    @SubCommand(name = "")
    @Description("显示帮助信息")
    public void showHelpDefault(CommandContext ctx) {
        showHelp(ctx.getSender());
    }

    /**
     * 创建洞府
     */
    @SubCommand(name = "create", playerOnly = true)
    @Description("创建洞府")
    public void create(CommandContext ctx) {
        Player player = ctx.requirePlayer();

        if (!player.hasPermission("guangdian.cave.create")) {
            ctx.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        Cave existing = caveManager.getPlayerCave(player.getUniqueId());
        if (existing != null) {
            ctx.sendMessage(configManager.getMessage("already-have-cave"));
            return;
        }

        Cave cave = caveManager.createCave(player);
        if (cave == null) {
            ctx.sendMessage(configManager.getMessage("cave-full"));
            return;
        }

        ctx.sendMessage(configManager.getMessage("cave-created"));
        player.teleport(cave.getHomeLocation());
    }

    /**
     * 回到洞府
     */
    @SubCommand(name = "home", playerOnly = true)
    @Description("回到洞府")
    public void home(CommandContext ctx) {
        Player player = ctx.requirePlayer();

        if (!player.hasPermission("guangdian.cave.home")) {
            ctx.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        Cave cave = caveManager.getPlayerCave(player.getUniqueId());
        if (cave == null) {
            ctx.sendMessage(configManager.getMessage("no-cave"));
            return;
        }

        ctx.sendMessage(configManager.getMessage("teleporting"));
        caveManager.teleportHome(player);
    }

    /**
     * 传送到洞府世界
     */
    @SubCommand(name = "world", playerOnly = true)
    @Description("传送到洞府世界")
    public void world(CommandContext ctx) {
        Player player = ctx.requirePlayer();

        if (!player.hasPermission("guangdian.cave.world")) {
            ctx.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        org.bukkit.Location spawnLoc = plugin.getWorldManager().getWorldSpawnLocation();
        if (spawnLoc == null) {
            ctx.sendError("洞府世界未加载！");
            return;
        }

        ctx.sendMessage(configManager.getMessage("teleporting"));
        player.teleport(spawnLoc);
    }

    /**
     * 设置传送点
     */
    @SubCommand(name = "sethome", playerOnly = true)
    @Description("设置传送点")
    public void setHome(CommandContext ctx) {
        Player player = ctx.requirePlayer();

        if (!player.hasPermission("guangdian.cave.sethome")) {
            ctx.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        Cave cave = caveManager.getPlayerCave(player.getUniqueId());
        if (cave == null) {
            ctx.sendMessage(configManager.getMessage("no-cave"));
            return;
        }

        if (!cave.isInside(player.getLocation())) {
            ctx.sendMessage(configManager.getMessage("not-in-cave"));
            return;
        }

        if (caveManager.setHome(player)) {
            ctx.sendMessage(configManager.getMessage("home-set"));
        }
    }

    /**
     * 查看洞府信息
     */
    @SubCommand(name = "info", playerOnly = true, minArgs = 0, maxArgs = 1)
    @Description("查看洞府信息")
    public void info(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        Cave cave;

        if (ctx.getArgCount() >= 1) {
            // 查看他人洞府
            String targetName = ctx.getStringArg(0);
            Player target = plugin.getServer().getPlayer(targetName);
            if (target != null) {
                cave = caveManager.getPlayerCave(target.getUniqueId());
            } else {
                // 离线玩家查找
                cave = plugin.getDataManager().getCavesByOwner().values().stream()
                    .filter(c -> c.getOwnerName().equalsIgnoreCase(targetName))
                    .findFirst().orElse(null);
            }
        } else {
            cave = caveManager.getPlayerCave(player.getUniqueId());
        }

        if (cave == null) {
            ctx.sendMessage(configManager.getMessage("no-cave"));
            return;
        }

        CaveLevel level = configManager.getLevel(cave.getLevel());

        ctx.sendMessage("<gold>========== 洞府信息 ==========");
        ctx.sendMessage("<yellow>洞主: <white>" + cave.getOwnerName());
        ctx.sendMessage("<yellow>等级: <white>" + (level != null ? level.getName() : cave.getLevel()));
        ctx.sendMessage("<yellow>大小: <white>" + (level != null ? level.getSize() + "x" + level.getSize() : "未知"));
        ctx.sendMessage("<yellow>成员: <white>" + cave.getMembers().size() + "/" + configManager.getMaxMembers());
        ctx.sendMessage("<gold>==============================");
    }

    /**
     * 升级洞府
     */
    @SubCommand(name = "upgrade", playerOnly = true)
    @Description("升级洞府")
    public void upgrade(CommandContext ctx) {
        Player player = ctx.requirePlayer();

        if (!player.hasPermission("guangdian.cave.upgrade")) {
            ctx.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        Cave cave = caveManager.getOwnerCave(player.getUniqueId());
        if (cave == null) {
            ctx.sendMessage(configManager.getMessage("no-cave"));
            return;
        }

        CaveLevel nextLevel = configManager.getNextLevel(cave.getLevel());
        if (nextLevel == null) {
            ctx.sendMessage(configManager.getMessage("upgrade-max"));
            return;
        }

        if (!upgradeManager.canUpgrade(player, cave)) {
            String cost = upgradeManager.getUpgradeCostDescription(nextLevel.getLevel());
            ctx.sendMessage(configManager.getMessage("upgrade-failed") + " 需要: " + cost);
            return;
        }

        if (upgradeManager.upgrade(player, cave)) {
            ctx.sendMessage(configManager.getMessage("upgrade-success",
                "level", String.valueOf(nextLevel.getLevel()),
                "name", nextLevel.getName()));
        }
    }

    /**
     * 邀请成员
     */
    @SubCommand(name = "invite", playerOnly = true, minArgs = 1)
    @Description("邀请成员")
    public void invite(CommandContext ctx) {
        Player player = ctx.requirePlayer();

        if (!player.hasPermission("guangdian.cave.invite")) {
            ctx.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        String targetName = ctx.getStringArg(0);
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) {
            ctx.sendMessage(configManager.getMessage("player-not-online"));
            return;
        }

        Cave cave = caveManager.getOwnerCave(player.getUniqueId());
        if (cave == null) {
            ctx.sendMessage(configManager.getMessage("not-owner"));
            return;
        }

        if (cave.isMember(target.getUniqueId())) {
            ctx.sendMessage(configManager.getMessage("player-already-member"));
            return;
        }

        if (caveManager.inviteMember(player, target)) {
            ctx.sendMessage(configManager.getMessage("invite-sent", "player", target.getName()));
            target.sendMessage(msg.colorize(configManager.getMessage("invite-received", "player", player.getName())));
        } else {
            ctx.sendError("邀请失败！");
        }
    }

    /**
     * 踢出成员
     */
    @SubCommand(name = "kick", playerOnly = true, minArgs = 1)
    @Description("踢出成员")
    public void kick(CommandContext ctx) {
        Player player = ctx.requirePlayer();

        if (!player.hasPermission("guangdian.cave.kick")) {
            ctx.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        String targetName = ctx.getStringArg(0);

        Cave cave = caveManager.getOwnerCave(player.getUniqueId());
        if (cave == null) {
            ctx.sendMessage(configManager.getMessage("not-owner"));
            return;
        }

        // 通过名字找UUID
        java.util.UUID targetUuid = null;
        for (Map.Entry<java.util.UUID, CaveMember> entry : cave.getMembers().entrySet()) {
            if (entry.getValue().getName().equalsIgnoreCase(targetName)) {
                targetUuid = entry.getKey();
                break;
            }
        }

        if (targetUuid == null) {
            ctx.sendMessage(configManager.getMessage("player-not-member"));
            return;
        }

        if (caveManager.kickMember(player, targetUuid)) {
            ctx.sendSuccess("已移除成员: " + targetName);
            Player target = plugin.getServer().getPlayer(targetUuid);
            if (target != null) {
                target.sendMessage(msg.colorize(configManager.getMessage("member-kicked")));
            }
        }
    }

    /**
     * 查看成员
     */
    @SubCommand(name = "members", playerOnly = true)
    @Description("查看成员")
    public void members(CommandContext ctx) {
        Player player = ctx.requirePlayer();

        if (!player.hasPermission("guangdian.cave.members")) {
            ctx.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        Cave cave = caveManager.getPlayerCave(player.getUniqueId());
        if (cave == null) {
            ctx.sendMessage(configManager.getMessage("no-cave"));
            return;
        }

        ctx.sendMessage("<gold>========== 洞府成员 ==========");
        for (CaveMember member : cave.getMembers().values()) {
            String prefix = member.getPermission() == PermissionType.OWNER ? "<red>[洞主]" :
                "<green>[成员]";
            ctx.sendMessage(prefix + " <white>" + member.getName());
        }
        ctx.sendMessage("<gold>==============================");
    }

    /**
     * 离开洞府
     */
    @SubCommand(name = "leave", playerOnly = true)
    @Description("离开洞府")
    public void leave(CommandContext ctx) {
        Player player = ctx.requirePlayer();

        if (!player.hasPermission("guangdian.cave.leave")) {
            ctx.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        Cave cave = caveManager.getPlayerCave(player.getUniqueId());
        if (cave == null) {
            ctx.sendMessage(configManager.getMessage("no-cave"));
            return;
        }

        if (cave.getOwnerUuid().equals(player.getUniqueId())) {
            ctx.sendError("洞主不能直接离开，请先转让洞主或解散洞府！");
            return;
        }

        if (caveManager.leaveCave(player.getUniqueId())) {
            ctx.sendSuccess("你已离开洞府！");
        }
    }

    /**
     * 转让洞主
     */
    @SubCommand(name = "transfer", playerOnly = true, minArgs = 1)
    @Description("转让洞主")
    public void transfer(CommandContext ctx) {
        Player player = ctx.requirePlayer();

        if (!player.hasPermission("guangdian.cave.transfer")) {
            ctx.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        String targetName = ctx.getStringArg(0);
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) {
            ctx.sendMessage(configManager.getMessage("player-not-online"));
            return;
        }

        if (caveManager.transferOwner(player, target)) {
            ctx.sendMessage(configManager.getMessage("transfer-success", "player", target.getName()));
            target.sendMessage(msg.colorize("<green>你已成为新的洞主！"));
        } else {
            ctx.sendError("转让失败！");
        }
    }

    /**
     * 访问洞府
     */
    @SubCommand(name = "visit", playerOnly = true, minArgs = 1)
    @Description("访问洞府")
    public void visit(CommandContext ctx) {
        Player player = ctx.requirePlayer();

        if (!player.hasPermission("guangdian.cave.visit")) {
            ctx.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        String targetName = ctx.getStringArg(0);

        // 查找目标洞府
        Cave targetCave = null;
        for (Cave cave : plugin.getDataManager().getAllCaves()) {
            if (cave.getOwnerName().equalsIgnoreCase(targetName) || cave.isMember(plugin.getServer().getOfflinePlayer(targetName).getUniqueId())) {
                targetCave = cave;
                break;
            }
        }

        if (targetCave == null) {
            ctx.sendMessage(configManager.getMessage("target-no-cave"));
            return;
        }

        if (targetCave.isMember(player.getUniqueId())) {
            // 成员直接传送
            ctx.sendMessage(configManager.getMessage("teleporting"));
            player.teleport(targetCave.getHomeLocation());
        } else if (configManager.isAllowVisitor()) {
            // 访客模式
            ctx.sendMessage(configManager.getMessage("teleporting"));
            ctx.sendMessage(configManager.getMessage("visitor-allowed"));
            player.teleport(targetCave.getHomeLocation());
        } else {
            ctx.sendMessage(configManager.getMessage("visitor-denied"));
        }
    }

    /**
     * 显示帮助
     */
    @SubCommand(name = "help")
    @Description("显示帮助信息")
    public void help(CommandContext ctx) {
        showHelp(ctx.getSender());
    }

    @Override
    public void showHelp(org.bukkit.command.CommandSender sender) {
        sender.sendMessage(msg.colorize("<gold>========== 洞府帮助 =========="));
        sender.sendMessage(msg.colorize("<yellow>/cave create <gray>- 创建洞府"));
        sender.sendMessage(msg.colorize("<yellow>/cave home <gray>- 回到洞府"));
        sender.sendMessage(msg.colorize("<yellow>/cave world <gray>- 传送到洞府世界"));
        sender.sendMessage(msg.colorize("<yellow>/cave sethome <gray>- 设置传送点"));
        sender.sendMessage(msg.colorize("<yellow>/cave info <gray>- 查看洞府信息"));
        sender.sendMessage(msg.colorize("<yellow>/cave upgrade <gray>- 升级洞府"));
        sender.sendMessage(msg.colorize("<yellow>/cave invite <玩家> <gray>- 邀请成员"));
        sender.sendMessage(msg.colorize("<yellow>/cave kick <玩家> <gray>- 移除成员"));
        sender.sendMessage(msg.colorize("<yellow>/cave members <gray>- 查看成员"));
        sender.sendMessage(msg.colorize("<yellow>/cave leave <gray>- 离开洞府"));
        sender.sendMessage(msg.colorize("<yellow>/cave transfer <玩家> <gray>- 转让洞主"));
        sender.sendMessage(msg.colorize("<yellow>/cave visit <玩家> <gray>- 访问洞府"));
        sender.sendMessage(msg.colorize("<gold>=============================="));
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        List<String> completions = new ArrayList<>();
        String subCommandName = subCommandMethod.getAnnotation(SubCommand.class).name();

        if (subCommandName.equals("invite") || subCommandName.equals("kick") || subCommandName.equals("transfer")) {
            if (context.getArgCount() == 1) {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
        } else if (subCommandName.equals("visit") || subCommandName.equals("info")) {
            if (context.getArgCount() == 1) {
                for (Cave cave : plugin.getDataManager().getAllCaves()) {
                    completions.add(cave.getOwnerName());
                }
            }
        }

        String lastArg = context.getStringArgOrDefault(context.getArgCount() - 1, "").toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(lastArg))
            .collect(Collectors.toList());
    }
}
