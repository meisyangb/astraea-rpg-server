package cn.guangdian.dungeon.command;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.session.DungeonSession;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DungeonAdminCommand implements CommandExecutor, TabCompleter {

    private final GuangDianDungeon plugin;

    public DungeonAdminCommand(GuangDianDungeon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("guangdian.dungeon.admin")) {
            sender.sendMessage(plugin.color("<red>你没有权限执行此操作"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            case "list":
                return handleList(sender);
            case "info":
                return handleInfo(sender, args);
            case "stop":
                return handleStop(sender, args);
            case "fail":
                return handleFail(sender, args);
            case "cleanup":
                return handleCleanup(sender);
            case "stats":
                return handleStats(sender);
            case "tp":
                return handleTeleport(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadConfigs();
        sender.sendMessage(plugin.color("<green>[副本] 配置已重载"));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(plugin.color("<gold>========== 活跃副本会话 =========="));

        int count = 0;
        for (DungeonSession session : plugin.getSessionManager().getActiveSessions()) {
            String info = String.format("<yellow>%s <gray>- <white>%s <gray>(队伍: %d人, 状态: %s, 阶段: %d/%d)",
                session.getSessionId(),
                session.getDungeonId(),
                session.getParty().getMembers().size(),
                session.getState().name(),
                session.getCurrentStageIndex() + 1,
                session.getStages().size());
            sender.sendMessage(plugin.color(info));
            count++;
        }

        if (count == 0) {
            sender.sendMessage(plugin.color("<gray>暂无活跃副本"));
        }

        sender.sendMessage(plugin.color("<gold>================================"));
        sender.sendMessage(plugin.color("<gray>总计: <white>" + count + " <gray>个会话"));
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.color("<red>用法: /dungeonadmin info <会话ID>"));
            return true;
        }

        String sessionId = args[1];
        DungeonSession session = plugin.getSessionManager().getSession(sessionId);

        if (session == null) {
            sender.sendMessage(plugin.color("<red>会话不存在: " + sessionId));
            return true;
        }

        sender.sendMessage(plugin.color("<gold>========== 会话详情 =========="));
        sender.sendMessage(plugin.color("<gray>会话ID: <white>" + session.getSessionId()));
        sender.sendMessage(plugin.color("<gray>副本: <white>" + session.getDungeonId()));
        sender.sendMessage(plugin.color("<gray>难度: <white>" + session.getDifficulty()));
        sender.sendMessage(plugin.color("<gray>状态: <white>" + session.getState().name()));
        sender.sendMessage(plugin.color("<gray>用时: <white>" + (session.getElapsedTime() / 1000) + "秒"));
        sender.sendMessage(plugin.color("<gray>击杀: <white>" + session.getTotalKills()));
        sender.sendMessage(plugin.color("<gray>死亡: <white>" + session.getTotalDeaths()));
        sender.sendMessage(plugin.color("<gray>阶段: <white>" + (session.getCurrentStageIndex() + 1) + "/" + session.getStages().size()));

        sender.sendMessage(plugin.color("<gray>队伍成员:"));
        for (var member : session.getParty().getMembers()) {
            Player player = Bukkit.getPlayer(member.getPlayerId());
            String status = player != null ? "<green>在线" : "<red>离线";
            sender.sendMessage(plugin.color("  <white>- " + (player != null ? player.getName() : member.getPlayerId()) + " " + status));
        }

        if (session.getInstanceWorldName() != null) {
            sender.sendMessage(plugin.color("<gray>世界: <white>" + session.getInstanceWorldName()));
        }

        sender.sendMessage(plugin.color("<gold>================================"));
        return true;
    }

    private boolean handleStop(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.color("<red>用法: /dungeonadmin stop <会话ID>"));
            return true;
        }

        String sessionId = args[1];
        DungeonSession session = plugin.getSessionManager().getSession(sessionId);

        if (session == null) {
            sender.sendMessage(plugin.color("<red>会话不存在: " + sessionId));
            return true;
        }

        plugin.getSessionManager().forceCompleteSession(sessionId);

        sender.sendMessage(plugin.color("<green>[副本] 已强制完成会话: " + sessionId));
        return true;
    }

    private boolean handleFail(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.color("<red>用法: /dungeonadmin fail <会话ID> [原因]"));
            return true;
        }

        String sessionId = args[1];
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "管理员强制失败";

        DungeonSession session = plugin.getSessionManager().getSession(sessionId);

        if (session == null) {
            sender.sendMessage(plugin.color("<red>会话不存在: " + sessionId));
            return true;
        }

        plugin.getSessionManager().forceFailSession(sessionId, reason);

        sender.sendMessage(plugin.color("<red>[副本] 已强制失败会话: " + sessionId));
        return true;
    }

    private boolean handleCleanup(CommandSender sender) {
        int cleaned = plugin.getSessionManager().cleanupExpired(0); // 0 = 清理所有非活跃会话
        sender.sendMessage(plugin.color("<green>[副本] 清理了 " + cleaned + " 个过期/已完成会话"));
        return true;
    }

    private boolean handleStats(CommandSender sender) {
        sender.sendMessage(plugin.color("<gold>========== 副本系统统计 =========="));
        sender.sendMessage(plugin.color("<gray>活跃会话: <white>" + plugin.getSessionManager().getActiveSessionCount()));
        sender.sendMessage(plugin.color("<gray>活跃队伍: <white>" + plugin.getPartyManager().getAllParties().size()));
        sender.sendMessage(plugin.color("<gray>已加载模板: <white>" + plugin.getTemplateLoader().getTemplateCount()));
        sender.sendMessage(plugin.color("<gray>最大实例数: <white>" + plugin.getMaxInstances()));

        int runningSessions = (int) plugin.getSessionManager().getAllSessions().stream()
            .filter(s -> s.getState() == DungeonSession.SessionState.RUNNING)
            .count();
        sender.sendMessage(plugin.color("<gray>运行中会话: <white>" + runningSessions));

        sender.sendMessage(plugin.color("<gold>================================"));
        return true;
    }

    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.color("<red>此命令只能由玩家执行"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.color("<red>用法: /dungeonadmin tp <会话ID>"));
            return true;
        }

        Player player = (Player) sender;
        String sessionId = args[1];

        DungeonSession session = plugin.getSessionManager().getSession(sessionId);

        if (session == null) {
            sender.sendMessage(plugin.color("<red>会话不存在: " + sessionId));
            return true;
        }

        if (session.getInstanceWorld() == null) {
            sender.sendMessage(plugin.color("<red>会话世界已卸载"));
            return true;
        }

        player.teleport(session.getInstanceWorld().getSpawnLocation());

        sender.sendMessage(plugin.color("<green>[副本] 已传送到会话: " + sessionId));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.color("<gold>========== 副本管理帮助 =========="));
        sender.sendMessage(plugin.color("<yellow>/dungeonadmin reload <gray>- 重载配置"));
        sender.sendMessage(plugin.color("<yellow>/dungeonadmin list <gray>- 查看活跃会话"));
        sender.sendMessage(plugin.color("<yellow>/dungeonadmin info <会话ID> <gray>- 查看会话详情"));
        sender.sendMessage(plugin.color("<yellow>/dungeonadmin stop <会话ID> <gray>- 强制完成会话"));
        sender.sendMessage(plugin.color("<yellow>/dungeonadmin fail <会话ID> [原因] <gray>- 强制失败会话"));
        sender.sendMessage(plugin.color("<yellow>/dungeonadmin cleanup <gray>- 清理过期会话"));
        sender.sendMessage(plugin.color("<yellow>/dungeonadmin stats <gray>- 查看系统统计"));
        sender.sendMessage(plugin.color("<yellow>/dungeonadmin tp <会话ID> <gray>- 传送到会话世界"));
        sender.sendMessage(plugin.color("<gold>================================"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("reload", "list", "info", "stop", "fail", "cleanup", "stats", "tp"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("info") || subCommand.equals("stop") || subCommand.equals("fail") || subCommand.equals("tp")) {
                completions.addAll(plugin.getSessionManager().getAllSessions().stream()
                    .map(DungeonSession::getSessionId)
                    .collect(Collectors.toList()));
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(c -> c.toLowerCase().startsWith(lastArg))
            .collect(Collectors.toList());
    }
}
