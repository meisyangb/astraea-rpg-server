package cn.guangdian.dungeon.command;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.*;
import cn.guangdian.dungeon.model.session.DungeonSession;
import cn.guangdian.dungeon.ui.*;
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
import java.util.*;
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
                new DungeonMainMenuUI(plugin, (Player) sender).open();
                return true;
            }
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list": return handleList(sender);
            case "info": return handleInfo(sender, args);
            case "enter":
            case "join": return handleEnter(sender, args);
            case "leave": return handleLeave(sender);
            case "status": return handleStatus(sender);
            case "hud":
            case "panel": return handleHUD(sender);
            case "top":
            case "rank": return handleRank(sender);
            case "reload": return handleReload(sender);
            case "open":
            case "menu": return handleOpen(sender);
            case "party": return handlePartyUI(sender);
            case "detail": return handleDetail(sender, args);
            case "records": return handleRecords(sender);
            case "test": return handleTest(sender, args);
            default: sendHelp(sender); return true;
        }
    }

    private boolean handleList(CommandSender sender) {
        if (sender instanceof Player) {
            new DungeonListUI(plugin, (Player) sender).open();
            return true;
        }
        sender.sendMessage(plugin.color("<gold>========== 可用副本 =========="));
        for (DungeonTemplate template : plugin.getTemplateLoader().getAllTemplates()) {
            String info = String.format("<yellow>%s <gray>- <white>%s <gray>(人数: %d-%d  |  地图: %s)",
                template.getId(), template.getName(),
                template.getSettings().getMinPlayers(), template.getSettings().getMaxPlayers(),
                template.getMapName());
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
        DungeonTemplate template = plugin.getTemplateLoader().getTemplate(args[1]);
        if (template == null) {
            sender.sendMessage(plugin.color("<red>副本不存在: " + args[1]));
            return true;
        }
        sender.sendMessage(plugin.color("<gold>========== " + template.getName() + " =========="));
        sender.sendMessage(plugin.color("<gray>ID: <white>" + template.getId()));
        sender.sendMessage(plugin.color("<gray>描述: <white>" + template.getDescription()));
        sender.sendMessage(plugin.color("<gray>地图: <white>" + template.getMapName()));
        sender.sendMessage(plugin.color("<gray>当前实例: <white>" +
            plugin.getMapInstanceManager().getActiveInstanceCount(template.getMapName()) + "/3"));
        StringBuilder difficulties = new StringBuilder();
        for (Difficulty diff : template.getDifficulties()) difficulties.append(diff.getName()).append(" ");
        sender.sendMessage(plugin.color("<gray>难度: <white>" + difficulties.toString().trim()));
        sender.sendMessage(plugin.color("<gold>================================"));
        return true;
    }

    private boolean handleDetail(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.color("<red>此命令只能由玩家执行")); return true; }
        if (args.length < 2) { sender.sendMessage(plugin.color("<red>用法: /dungeon detail <副本ID>")); return true; }
        new DungeonDetailUI(plugin, (Player) sender, args[1]).open();
        return true;
    }

    private boolean handleEnter(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.color("<red>此命令只能由玩家执行")); return true; }
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
        if (template == null) { player.sendMessage(plugin.color("<red>副本不存在: " + dungeonId)); return true; }

        Difficulty difficulty = template.getDifficulty(difficultyId);
        if (difficulty == null) { player.sendMessage(plugin.color("<red>难度不存在: " + difficultyId)); return true; }

        var playerData = plugin.getPlayerRepository().getPlayerData(player.getUniqueId());
        if (playerData != null && playerData.isOnCooldown(dungeonId)) {
            player.sendMessage(plugin.color("<red>副本冷却中，剩余: <yellow>" + (playerData.getRemainingCooldown(dungeonId) / 1000) + "秒"));
            return true;
        }

        DungeonParty party = plugin.getPartyManager().getPlayerParty(player).orElse(null);
        if (party == null) {
            party = plugin.getPartyManager().createParty(player, template);
            if (party == null) { player.sendMessage(plugin.color("<red>创建队伍失败")); return true; }
        } else if (!party.isLeader(player)) {
            player.sendMessage(plugin.color("<red>只有队长可以开始副本"));
            return true;
        }

        // 使用 MapInstanceManager 创建世界实例
        String mapName = template.getMapName();
        player.sendMessage(plugin.color("<yellow>正在创建副本实例..."));

        String instanceWorldName = plugin.getMapInstanceManager().createInstance(mapName);
        if (instanceWorldName == null) {
            player.sendMessage(plugin.color("<red>副本已满！当前地图 " + mapName + " 已达到最大实例数 (3)"));
            return true;
        }

        World instanceWorld = Bukkit.getWorld(instanceWorldName);
        if (instanceWorld == null) {
            player.sendMessage(plugin.color("<red>加载副本世界失败"));
            plugin.getMapInstanceManager().destroyInstance(instanceWorldName);
            return true;
        }

        // 使用 SessionManager.createSession 创建会话
        DungeonSession session = plugin.getSessionManager().createSession(dungeonId, party, instanceWorld);
        session.setInstanceWorldName(instanceWorldName);
        session.setDifficulty(difficultyId);
        session.setTimeLimit(template.getSettings().getTimeLimit() + difficulty.getTimeLimitModifier());

        DungeonSession loadedSession = plugin.getStageLoader().loadDungeonConfig(dungeonId, instanceWorld);
        if (loadedSession != null) {
            session.setStages(loadedSession.getStages());
            session.setSpawnPoints(loadedSession.getSpawnPoints());
        }

        // 传送所有队员到入口
        org.bukkit.configuration.file.YamlConfiguration dungeonConfig =
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File(plugin.getDataFolder(), "dungeons/" + dungeonId + ".yml"));
        double entranceX = dungeonConfig.getDouble("teleports.entrance.x", 0);
        double entranceY = dungeonConfig.getDouble("teleports.entrance.y", 64);
        double entranceZ = dungeonConfig.getDouble("teleports.entrance.z", 0);

        for (PartyMember member : party.getMembers()) {
            Player p = Bukkit.getPlayer(member.getPlayerId());
            if (p != null) {
                p.teleport(new Location(instanceWorld, entranceX, entranceY, entranceZ));
            }
        }

        startDungeonWithAnnouncement(session);
        return true;
    }

    private boolean handleTest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.color("<red>此命令只能由玩家执行")); return true; }
        Player player = (Player) sender;
        String dungeonId = args.length > 1 ? args[1] : "example_dungeon";

        DungeonTemplate template = plugin.getTemplateLoader().getTemplate(dungeonId);
        if (template == null) { player.sendMessage(plugin.color("<red>副本不存在: " + dungeonId)); return true; }

        DungeonParty party = plugin.getPartyManager().getPlayerParty(player).orElse(null);
        if (party == null) {
            party = plugin.getPartyManager().createParty(player, template);
            if (party == null) { player.sendMessage(plugin.color("<red>创建队伍失败")); return true; }
        }
        if (!party.isLeader(player)) { player.sendMessage(plugin.color("<red>只有队长可以开始副本")); return true; }

        String mapName = template.getMapName();
        player.sendMessage(plugin.color("<yellow>正在创建副本实例..."));

        String instanceWorldName = plugin.getMapInstanceManager().createInstance(mapName);
        if (instanceWorldName == null) {
            player.sendMessage(plugin.color("<red>副本已满！地图 " + mapName + " 已达到最大实例数 (3)"));
            return true;
        }

        World instanceWorld = Bukkit.getWorld(instanceWorldName);
        if (instanceWorld == null) {
            player.sendMessage(plugin.color("<red>加载副本世界失败"));
            plugin.getMapInstanceManager().destroyInstance(instanceWorldName);
            return true;
        }

        DungeonSession session = plugin.getSessionManager().createSession(dungeonId, party, instanceWorld);
        session.setInstanceWorldName(instanceWorldName);
        session.setDifficulty("normal");

        DungeonSession loadedSession = plugin.getStageLoader().loadDungeonConfig(dungeonId, instanceWorld);
        if (loadedSession != null) {
            session.setStages(loadedSession.getStages());
            session.setSpawnPoints(loadedSession.getSpawnPoints());
        }

        org.bukkit.configuration.file.YamlConfiguration testConfig =
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File(plugin.getDataFolder(), "dungeons/" + dungeonId + ".yml"));
        double x = testConfig.getDouble("teleports.entrance.x", 0);
        double y = testConfig.getDouble("teleports.entrance.y", 64);
        double z = testConfig.getDouble("teleports.entrance.z", 0);

        for (PartyMember member : party.getMembers()) {
            Player p = Bukkit.getPlayer(member.getPlayerId());
            if (p != null) p.teleport(new Location(instanceWorld, x, y, z));
        }

        startDungeonWithAnnouncement(session);
        return true;
    }

    private void startDungeonWithAnnouncement(DungeonSession session) {
        session.setState(DungeonSession.SessionState.STARTING);

        for (PartyMember member : session.getParty().getMembers()) {
            Player player = Bukkit.getPlayer(member.getPlayerId());
            if (player != null) {
                showTitle(player, "<yellow><bold>试炼之地", "<white>副本即将开始...", 500, 2000, 500);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                player.sendMessage(plugin.color("<yellow><bold>[副本]</bold> <white>你已进入 <gold>试炼之地"));
                player.sendMessage(plugin.color("<gray>实例世界: <white>" + session.getInstanceWorldName()));
            }
        }

        long[] delays = {20L, 40L, 60L};
        String[] titles = {"<red><bold>3", "<gold><bold>2", "<yellow><bold>1"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (PartyMember member : session.getParty().getMembers()) {
                    Player player = Bukkit.getPlayer(member.getPlayerId());
                    if (player != null) {
                        showTitle(player, titles[idx], null, 250, 1000, 250);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                    }
                }
            }, delays[i]);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
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

    private void showTitle(Player player, String title, String subtitle, long fadeIn, long stay, long fadeOut) {
        Title.Times times = Title.Times.times(Duration.ofMillis(fadeIn), Duration.ofMillis(stay), Duration.ofMillis(fadeOut));
        player.showTitle(Title.title(plugin.color(title),
            subtitle != null ? plugin.color(subtitle) : net.kyori.adventure.text.Component.empty(), times));
    }

    private boolean handleLeave(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.color("<red>此命令只能由玩家执行")); return true; }
        Player player = (Player) sender;
        DungeonSession session = plugin.getSessionManager().getPlayerSession(player.getUniqueId());
        if (session == null) { player.sendMessage(plugin.color("<red>你不在副本中")); return true; }
        plugin.getMapInstanceManager().teleportToExitWorld(player);
        player.sendMessage(plugin.color("<yellow>你已离开副本"));
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.color("<red>此命令只能由玩家执行")); return true; }
        Player player = (Player) sender;
        DungeonSession session = plugin.getSessionManager().getPlayerSession(player.getUniqueId());
        if (session == null) { player.sendMessage(plugin.color("<gray>你当前不在副本中")); return true; }
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
        sender.sendMessage(plugin.color("<gold>================================"));
        return true;
    }

    private boolean handleHUD(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.color("<red>此命令只能由玩家执行")); return true; }
        Player player = (Player) sender;
        DungeonSession session = plugin.getSessionManager().getPlayerSession(player.getUniqueId());
        if (session == null) { player.sendMessage(plugin.color("<red>你不在副本中")); return true; }
        new DungeonHUD(plugin, player, session).open();
        return true;
    }

    private boolean handleRecords(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.color("<red>此命令只能由玩家执行")); return true; }
        new DungeonRecordUI(plugin, (Player) sender).open();
        return true;
    }

    private boolean handleRank(CommandSender sender) {
        if (sender instanceof Player) { new DungeonRankUI(plugin, (Player) sender).open(); return true; }
        sender.sendMessage(plugin.color("<gold>========== 副本排行 =========="));
        sender.sendMessage(plugin.color("<gray>暂无排行数据"));
        sender.sendMessage(plugin.color("<gold>================================"));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("guangdian.dungeon.admin")) { sender.sendMessage(plugin.color("<red>你没有权限执行此操作")); return true; }
        plugin.reloadConfigs();
        sender.sendMessage(plugin.color("<green>配置已重载"));
        return true;
    }

    private boolean handleOpen(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.color("<red>此命令只能由玩家执行")); return true; }
        new DungeonMainMenuUI(plugin, (Player) sender).open();
        return true;
    }

    private boolean handlePartyUI(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.color("<red>此命令只能由玩家执行")); return true; }
        new PartyUI(plugin, (Player) sender).open();
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
            String sub = args[0].toLowerCase();
            if (sub.equals("info") || sub.equals("enter") || sub.equals("detail") || sub.equals("top") || sub.equals("test")) {
                completions.addAll(plugin.getTemplateLoader().getTemplateIds());
            }
        } else if (args.length == 3 && args[0].toLowerCase().equals("enter")) {
            DungeonTemplate template = plugin.getTemplateLoader().getTemplate(args[1]);
            if (template != null) completions.addAll(template.getDifficulties().stream().map(Difficulty::getId).collect(Collectors.toList()));
        }
        return completions.stream().filter(c -> c.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
    }
}