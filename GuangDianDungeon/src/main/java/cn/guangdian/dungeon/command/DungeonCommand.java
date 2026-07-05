package cn.guangdian.dungeon.command;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.manager.WorldInstanceManager;
import cn.guangdian.dungeon.model.*;
import cn.guangdian.dungeon.model.session.DungeonSession;
import cn.guangdian.dungeon.ui.DungeonDetailUI;
import cn.guangdian.dungeon.ui.DungeonHUD;
import cn.guangdian.dungeon.ui.DungeonListUI;
import cn.guangdian.dungeon.ui.DungeonMainMenuUI;
import cn.guangdian.dungeon.ui.DungeonRankUI;
import cn.guangdian.dungeon.ui.DungeonRecordUI;
import cn.guangdian.dungeon.ui.PartyUI;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class DungeonCommand implements CommandExecutor, TabCompleter {

    private final GuangDianDungeon plugin;

    public DungeonCommand(GuangDianDungeon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                DungeonMainMenuUI ui = new DungeonMainMenuUI(plugin, (Player) sender);
                ui.open();
                return true;
            }
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                return handleList(sender);
            case "info":
                return handleInfo(sender, args);
            case "enter":
            case "join":
                return handleEnter(sender, args);
            case "leave":
                return handleLeave(sender);
            case "status":
                return handleStatus(sender);
            case "hud":
            case "panel":
                return handleHUD(sender);
            case "top":
            case "rank":
                return handleRank(sender);
            case "reload":
                return handleReload(sender);
            case "open":
            case "menu":
                return handleOpen(sender);
            case "party":
                return handlePartyUI(sender);
            case "detail":
                return handleDetail(sender, args);
            case "records":
                return handleRecords(sender);
            case "test":
                return handleTest(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleList(CommandSender sender) {
        if (sender instanceof Player) {
            new DungeonListUI(plugin, (Player) sender).open();
            return true;
        }

        sender.sendMessage(plugin.color("<gold>========== 可用副本 =========="));

        for (DungeonTemplate template : plugin.getTemplateLoader().getAllTemplates()) {
            String info = String.format("<yellow>%s <gray>- <white>%s <gray>(人数: %d-%d)",
                template.getId(), template.getName(),
                template.getSettings().getMinPlayers(),
                template.getSettings().getMaxPlayers());
            sender.sendMessage(plugin.color(info));
        }

        sender.sendMessage(plugin.color("<gold>================================"));
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.color("<red>用法: /dungeon info <副本ID>"));
            return true;
        }

        String dungeonId = args[1];
        DungeonTemplate template = plugin.getTemplateLoader().getTemplate(dungeonId);

        if (template == null) {
            sender.sendMessage(plugin.color("<red>副本不存在: " + dungeonId));
            return true;
        }

        sender.sendMessage(plugin.color("<gold>========== " + template.getName() + " =========="));
        sender.sendMessage(plugin.color("<gray>ID: <white>" + template.getId()));
        sender.sendMessage(plugin.color("<gray>描述: <white>" + template.getDescription()));
        sender.sendMessage(plugin.color("<gray>人数: <white>" + template.getSettings().getMinPlayers() +
            " - " + template.getSettings().getMaxPlayers()));
        sender.sendMessage(plugin.color("<gray>时限: <white>" + template.getSettings().getTimeLimit() + "秒"));

        StringBuilder difficulties = new StringBuilder();
        for (Difficulty diff : template.getDifficulties()) {
            difficulties.append(diff.getName()).append(" ");
        }
        sender.sendMessage(plugin.color("<gray>难度: <white>" + difficulties.toString().trim()));
        sender.sendMessage(plugin.color("<gold>================================"));

        return true;
    }

    private boolean handleDetail(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.color("<red>此命令只能由玩家执行"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.color("<red>用法: /dungeon detail <副本ID>"));
            return true;
        }

        Player player = (Player) sender;
        new DungeonDetailUI(plugin, player, args[1]).open();
        return true;
    }

    /**
     * 进入副本 - 基于 DungeonSession 新体系
     */
    private boolean handleEnter(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.color("<red>此命令只能由玩家执行"));
            return true;
        }

        Player player = (Player) sender;

        if (plugin.getSessionManager().isInDungeon(player.getUniqueId())) {
            player.sendMessage(plugin.color("<red>你已在副本中"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.color("<red>用法: /dungeon enter <副本ID> [难度]"));
            return true;
        }

        String dungeonId = args[1];
        String difficultyId = args.length > 2 ? args[2] : "normal";

        DungeonTemplate template = plugin.getTemplateLoader().getTemplate(dungeonId);
        if (template == null) {
            player.sendMessage(plugin.color("<red>副本不存在: " + dungeonId));
            return true;
        }

        Difficulty difficulty = template.getDifficulty(difficultyId);
        if (difficulty == null) {
            player.sendMessage(plugin.color("<red>难度不存在: " + difficultyId));
            return true;
        }

        // 检查冷却
        var playerData = plugin.getPlayerRepository().getPlayerData(player.getUniqueId());
        if (playerData != null && playerData.isOnCooldown(dungeonId)) {
            long remaining = playerData.getRemainingCooldown(dungeonId);
            player.sendMessage(plugin.color("<red>副本冷却中，剩余: <yellow>" + (remaining / 1000) + "秒"));
            return true;
        }

        // 创建或获取队伍
        DungeonParty party = plugin.getPartyManager().getPlayerParty(player).orElse(null);
        if (party == null) {
            party = plugin.getPartyManager().createParty(player, template);
            if (party == null) {
                player.sendMessage(plugin.color("<red>创建队伍失败"));
                return true;
            }
        } else if (!party.isLeader(player)) {
            player.sendMessage(plugin.color("<red>只有队长可以开始副本"));
            return true;
        }

        // 使用 SessionManager 体系创建副本
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        String templateWorldName = template.getWorldTemplate();

        player.sendMessage(plugin.color("<yellow>正在创建副本实例..."));

        WorldInstanceManager.InstanceInfo instanceInfo =
            plugin.getWorldInstanceManager().createInstance(templateWorldName, dungeonId, sessionId);

        if (instanceInfo == null) {
            player.sendMessage(plugin.color("<red>创建副本实例失败"));
            return true;
        }

        World instanceWorld = Bukkit.getWorld(instanceInfo.getInstanceWorldName());
        if (instanceWorld == null) {
            player.sendMessage(plugin.color("<red>加载实例世界失败"));
            return true;
        }

        // 从配置加载阶段/波次
        DungeonSession session = plugin.getStageLoader().loadDungeonConfig(dungeonId, instanceWorld);
        if (session == null) {
            player.sendMessage(plugin.color("<red>加载副本配置失败"));
            plugin.getWorldInstanceManager().unloadAndDeleteInstance(instanceInfo.getInstanceWorldName());
            return true;
        }

        session.setSessionId(sessionId);
        session.setParty(party);
        session.setDifficulty(difficultyId);
        session.setTimeLimit(template.getSettings().getTimeLimit() + difficulty.getTimeLimitModifier());

        // 注册到 SessionManager
        plugin.getSessionManager().getSessions().put(sessionId, session);
        for (PartyMember member : party.getMembers()) {
            plugin.getSessionManager().getPlayerSessions().put(member.getPlayerId(), sessionId);
        }
        party.setActiveSessionId(sessionId);
        party.setState(PartyState.IN_DUNGEON);

        // 传送所有队员 - 从配置文件读取入口坐标
        org.bukkit.configuration.file.YamlConfiguration dungeonConfig =
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File(plugin.getDataFolder(), "dungeons/" + dungeonId + ".yml"));
        double entranceX = dungeonConfig.getDouble("teleports.entrance.x", 0);
        double entranceY = dungeonConfig.getDouble("teleports.entrance.y", 64);
        double entranceZ = dungeonConfig.getDouble("teleports.entrance.z", 0);

        for (PartyMember member : party.getMembers()) {
            Player p = Bukkit.getPlayer(member.getPlayerId());
            if (p != null) {
                plugin.getWorldInstanceManager().addPlayerToInstance(
                    instanceInfo.getInstanceWorldName(), member.getPlayerId());

                Location entrance = new Location(instanceWorld, entranceX, entranceY, entranceZ);
                p.teleport(entrance);
            }
        }

        // 倒计时开始
        startDungeonWithAnnouncement(session, instanceInfo.getInstanceWorldName());

        return true;
    }

    private boolean handleTest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.color("<red>此命令只能由玩家执行"));
            return true;
        }

        Player player = (Player) sender;
        String dungeonId = args.length > 1 ? args[1] : "trial_ground";

        String templateWorldName = "dungeon_flat";
        World templateWorld = Bukkit.getWorld(templateWorldName);
        if (templateWorld == null) {
            player.sendMessage(plugin.color("<red>模板世界 " + templateWorldName + " 不存在"));
            return true;
        }

        DungeonParty party = plugin.getPartyManager().getPlayerParty(player).orElse(null);
        if (party == null) {
            party = plugin.getPartyManager().createParty(player,
                plugin.getTemplateLoader().getTemplate(dungeonId));
            if (party == null) {
                player.sendMessage(plugin.color("<red>创建队伍失败"));
                return true;
            }
        }

        if (!party.isLeader(player)) {
            player.sendMessage(plugin.color("<red>只有队长可以开始副本"));
            return true;
        }

        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        player.sendMessage(plugin.color("<yellow>正在创建副本实例..."));

        WorldInstanceManager.InstanceInfo instanceInfo =
            plugin.getWorldInstanceManager().createInstance(templateWorldName, dungeonId, sessionId);

        if (instanceInfo == null) {
            player.sendMessage(plugin.color("<red>创建副本实例失败"));
            return true;
        }

        World instanceWorld = Bukkit.getWorld(instanceInfo.getInstanceWorldName());
        if (instanceWorld == null) {
            player.sendMessage(plugin.color("<red>加载实例世界失败"));
            return true;
        }

        DungeonSession session = plugin.getStageLoader().loadDungeonConfig(dungeonId, instanceWorld);
        if (session == null) {
            player.sendMessage(plugin.color("<red>加载副本配置失败"));
            plugin.getWorldInstanceManager().unloadAndDeleteInstance(instanceInfo.getInstanceWorldName());
            return true;
        }

        session.setSessionId(sessionId);
        session.setParty(party);
        session.setDifficulty("normal");

        // 从配置文件读取入口坐标
        org.bukkit.configuration.file.YamlConfiguration testDungeonConfig =
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File(plugin.getDataFolder(), "dungeons/" + dungeonId + ".yml"));
        double testEntranceX = testDungeonConfig.getDouble("teleports.entrance.x", 0);
        double testEntranceY = testDungeonConfig.getDouble("teleports.entrance.y", 64);
        double testEntranceZ = testDungeonConfig.getDouble("teleports.entrance.z", 0);

        for (PartyMember member : party.getMembers()) {
            Player p = Bukkit.getPlayer(member.getPlayerId());
            if (p != null) {
                plugin.getWorldInstanceManager().addPlayerToInstance(
                    instanceInfo.getInstanceWorldName(), member.getPlayerId());

                Location entrance = new Location(instanceWorld, testEntranceX, testEntranceY, testEntranceZ);
                p.teleport(entrance);
            }
        }

        startDungeonWithAnnouncement(session, instanceInfo.getInstanceWorldName());

        return true;
    }

    private void startDungeonWithAnnouncement(DungeonSession session, String instanceWorldName) {
        session.setState(DungeonSession.SessionState.STARTING);
        session.setInstanceWorldName(instanceWorldName);

        plugin.getSessionManager().getSessions().put(session.getSessionId(), session);

        for (PartyMember member : session.getParty().getMembers()) {
            plugin.getSessionManager().getPlayerSessions().put(member.getPlayerId(), session.getSessionId());
        }

        for (PartyMember member : session.getParty().getMembers()) {
            Player player = Bukkit.getPlayer(member.getPlayerId());
            if (player != null) {
                showTitle(player, "<yellow><bold>试炼之地", "<white>副本即将开始...", 500, 2000, 500);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                player.sendMessage(plugin.color("<yellow><bold>[副本]</bold> <white>你已进入 <gold>试炼之地"));
                player.sendMessage(plugin.color("<gray>实例世界: <white>" + instanceWorldName));
            }
        }

        plugin.getScheduler().runSyncLater(() -> {
            for (PartyMember member : session.getParty().getMembers()) {
                Player player = Bukkit.getPlayer(member.getPlayerId());
                if (player != null) {
                    showTitle(player, "<red><bold>3", null, 250, 1000, 250);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                }
            }
        }, 20L);

        plugin.getScheduler().runSyncLater(() -> {
            for (PartyMember member : session.getParty().getMembers()) {
                Player player = Bukkit.getPlayer(member.getPlayerId());
                if (player != null) {
                    showTitle(player, "<gold><bold>2", null, 250, 1000, 250);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                }
            }
        }, 40L);

        plugin.getScheduler().runSyncLater(() -> {
            for (PartyMember member : session.getParty().getMembers()) {
                Player player = Bukkit.getPlayer(member.getPlayerId());
                if (player != null) {
                    showTitle(player, "<yellow><bold>1", null, 250, 1000, 250);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                }
            }
        }, 60L);

        plugin.getScheduler().runSyncLater(() -> {
            for (PartyMember member : session.getParty().getMembers()) {
                Player player = Bukkit.getPlayer(member.getPlayerId());
                if (player != null) {
                    showTitle(player, "<green><bold>开始!", "<white>击败所有敌人!", 500, 1500, 500);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
            }

            plugin.getSessionManager().startSession(session);
        }, 80L);
    }

    private void showTitle(Player player, String title, String subtitle, long fadeInMs, long stayMs, long fadeOutMs) {
        Title.Times times = Title.Times.times(
            Duration.ofMillis(fadeInMs),
            Duration.ofMillis(stayMs),
            Duration.ofMillis(fadeOutMs)
        );
        Title adventureTitle = Title.title(
            plugin.color(title),
            subtitle != null ? plugin.color(subtitle) : net.kyori.adventure.text.Component.empty(),
            times
        );
        player.showTitle(adventureTitle);
    }

    private boolean handleLeave(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.color("<red>此命令只能由玩家执行"));
            return true;
        }

        Player player = (Player) sender;

        DungeonSession session = plugin.getSessionManager().getPlayerSession(player.getUniqueId());
        if (session == null) {
            player.sendMessage(plugin.color("<red>你不在副本中"));
            return true;
        }

        plugin.getWorldInstanceManager().teleportToExitWorld(player);
        player.sendMessage(plugin.color("<yellow>你已离开副本"));

        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.color("<red>此命令只能由玩家执行"));
            return true;
        }

        Player player = (Player) sender;

        DungeonSession session = plugin.getSessionManager().getPlayerSession(player.getUniqueId());
        if (session == null) {
            player.sendMessage(plugin.color("<gray>你当前不在副本中"));
            return true;
        }

        DungeonTemplate template = plugin.getTemplateLoader().getTemplate(session.getDungeonId());

        sender.sendMessage(plugin.color("<gold>========== 副本状态 =========="));
        sender.sendMessage(plugin.color("<gray>副本: <white>" + (template != null ? template.getName() : session.getDungeonId())));
        sender.sendMessage(plugin.color("<gray>难度: <white>" + session.getDifficulty()));

        var stage = session.getCurrentStage();
        sender.sendMessage(plugin.color("<gray>阶段: <white>" + (stage != null ? stage.getName() : "未开始")));

        sender.sendMessage(plugin.color("<gray>用时: <white>" + (session.getElapsedTime() / 1000) + "秒"));

        long remaining = session.getTimeLimit() - session.getElapsedTime() / 1000;
        sender.sendMessage(plugin.color("<gray>剩余: <white>" + Math.max(0, remaining) + "秒"));

        sender.sendMessage(plugin.color("<gray>击杀: <white>" + session.getTotalKills()));
        sender.sendMessage(plugin.color("<gray>死亡: <white>" + session.getTotalDeaths()));
        sender.sendMessage(plugin.color("<gray>状态: <white>" + session.getState().name()));
        sender.sendMessage(plugin.color("<gold>================================"));

        return true;
    }

    private boolean handleHUD(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.color("<red>此命令只能由玩家执行"));
            return true;
        }

        Player player = (Player) sender;
        DungeonSession session = plugin.getSessionManager().getPlayerSession(player.getUniqueId());

        if (session == null) {
            player.sendMessage(plugin.color("<red>你不在副本中"));
            return true;
        }

        new DungeonHUD(plugin, player, session).open();
        return true;
    }

    private boolean handleRecords(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.color("<red>此命令只能由玩家执行"));
            return true;
        }
        new DungeonRecordUI(plugin, (Player) sender).open();
        return true;
    }

    private boolean handleRank(CommandSender sender) {
        if (sender instanceof Player) {
            new DungeonRankUI(plugin, (Player) sender).open();
            return true;
        }
        sender.sendMessage(plugin.color("<gold>========== 副本排行 =========="));
        sender.sendMessage(plugin.color("<gray>暂无排行数据"));
        sender.sendMessage(plugin.color("<gold>================================"));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("guangdian.dungeon.admin")) {
            sender.sendMessage(plugin.color("<red>你没有权限执行此操作"));
            return true;
        }

        plugin.reloadConfigs();
        sender.sendMessage(plugin.color("<green>配置已重载"));
        return true;
    }

    private boolean handleOpen(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.color("<red>此命令只能由玩家执行"));
            return true;
        }

        Player player = (Player) sender;
        DungeonMainMenuUI ui = new DungeonMainMenuUI(plugin, player);
        ui.open();
        return true;
    }

    private boolean handlePartyUI(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.color("<red>此命令只能由玩家执行"));
            return true;
        }

        Player player = (Player) sender;
        PartyUI ui = new PartyUI(plugin, player);
        ui.open();
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.color("<gold>========== 副本帮助 =========="));
        sender.sendMessage(plugin.color("<yellow>/dungeon <gray>- 打开副本主菜单"));
        sender.sendMessage(plugin.color("<yellow>/dungeon list <gray>- 打开副本列表"));
        sender.sendMessage(plugin.color("<yellow>/dungeon detail <副本> <gray>- 查看副本详情"));
        sender.sendMessage(plugin.color("<yellow>/dungeon info <副本> <gray>- 查看副本信息(聊天)"));
        sender.sendMessage(plugin.color("<yellow>/dungeon enter <副本> [难度] <gray>- 进入副本"));
        sender.sendMessage(plugin.color("<yellow>/dungeon test [副本] <gray>- 测试副本(自动开始)"));
        sender.sendMessage(plugin.color("<yellow>/dungeon leave <gray>- 离开副本"));
        sender.sendMessage(plugin.color("<yellow>/dungeon status <gray>- 查看副本状态"));
        sender.sendMessage(plugin.color("<yellow>/dungeon hud <gray>- 打开战斗面板(副本中)"));
        sender.sendMessage(plugin.color("<yellow>/dungeon records <gray>- 查看通关记录"));
        sender.sendMessage(plugin.color("<yellow>/dungeon party <gray>- 打开队伍界面"));
        sender.sendMessage(plugin.color("<yellow>/dungeon rank <gray>- 打开排行榜"));
        sender.sendMessage(plugin.color("<yellow>/dungeon reload <gray>- 重载配置(管理员)"));
        sender.sendMessage(plugin.color("<gold>================================"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("list", "info", "detail", "enter", "leave", "status", "hud", "panel", "records", "rank", "top", "reload", "open", "menu", "party", "test"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("info") || subCommand.equals("enter") || subCommand.equals("detail") || subCommand.equals("top") || subCommand.equals("test")) {
                completions.addAll(plugin.getTemplateLoader().getTemplateIds());
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("enter")) {
                String dungeonId = args[1];
                DungeonTemplate template = plugin.getTemplateLoader().getTemplate(dungeonId);
                if (template != null) {
                    completions.addAll(template.getDifficulties().stream()
                        .map(Difficulty::getId)
                        .collect(Collectors.toList()));
                }
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(c -> c.toLowerCase().startsWith(lastArg))
            .collect(Collectors.toList());
    }
}
