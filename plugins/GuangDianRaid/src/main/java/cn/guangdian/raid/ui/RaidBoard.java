package cn.guangdian.raid.ui;

import cn.guangdian.raid.GuangDianRaid;
import cn.guangdian.raid.instance.RaidInstance;
import cn.guangdian.raid.model.RaidPhaseType;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RaidBoard {

    private final GuangDianRaid plugin;
    private final SyncScheduler scheduler;
    private final Map<UUID, Scoreboard> playerBoards;
    private final Map<UUID, Objective> objectives;

    public RaidBoard(GuangDianRaid plugin) {
        this.plugin = plugin;
        this.scheduler = RPGCore.getInstance().getScheduler();
        this.playerBoards = new ConcurrentHashMap<>();
        this.objectives = new ConcurrentHashMap<>();

        startUpdateTask();
    }

    private void startUpdateTask() {
        scheduler.runSyncRepeating(() -> {
            for (RaidInstance instance : plugin.getInstanceManager().getAllInstances()) {
                if (!instance.isActive()) continue;

                for (var rp : instance.getTeam().getMembers()) {
                    Player player = rp.getPlayer();
                    if (player != null && player.isOnline()) {
                        updateBoard(player, instance);
                    }
                }
            }
        }, 0L, 20L);
    }

    public void createBoard(Player player, RaidInstance instance) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        Objective objective = board.registerNewObjective(
            "raid_" + instance.getInstanceId(),
            Criteria.DUMMY,
            Component.text(instance.getRaid().getName()).color(NamedTextColor.GOLD)
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        playerBoards.put(player.getUniqueId(), board);
        objectives.put(player.getUniqueId(), objective);
        player.setScoreboard(board);
    }

    public void updateBoard(Player player, RaidInstance instance) {
        Objective objective = objectives.get(player.getUniqueId());
        if (objective == null) {
            createBoard(player, instance);
            return;
        }

        clearScores(objective);

        Scoreboard board = playerBoards.get(player.getUniqueId());

        int line = 15;

        String phaseText = switch (instance.getCurrentPhase()) {
            case WAITING -> "<white>等待中";
            case INFILTRATION -> "<yellow>潜入阶段";
            case SEARCH -> "<aqua>搜索阶段";
            case COMBAT -> "<red>战斗阶段";
            case EXTRACTION -> "<green>撤离阶段";
            case COMPLETED -> "<green>已完成";
            case FAILED -> "<red>失败";
        };

        objective.getScore("<white>阶段: " + phaseText).setScore(line--);

        long elapsed = (System.currentTimeMillis() - instance.getStartTime()) / 1000;
        int remaining = instance.getRaid().getTotalTimeLimit() - (int) elapsed;
        int mins = remaining / 60;
        int secs = remaining % 60;
        objective.getScore("<white>时间: <aqua>" + String.format("%02d:%02d", mins, secs)).setScore(line--);

        objective.getScore("").setScore(line--);

        objective.getScore("<white>情报: <yellow>" + instance.getCollectedIntel()).setScore(line--);
        objective.getScore("<white>击杀: <red>" + instance.getKillCount()).setScore(line--);

        long aliveCount = instance.getTeam().getMembers().stream()
            .filter(p -> p.getState() == cn.guangdian.raid.model.RaidPlayerState.ALIVE)
            .count();
        objective.getScore("<white>存活: <green>" + aliveCount + "/" + instance.getTeam().size()).setScore(line--);

        if (instance.getCurrentPhase() == RaidPhaseType.EXTRACTION) {
            objective.getScore("  ").setScore(line--);
            objective.getScore("<green><bold>撤离点已激活!").setScore(line--);
        }
    }

    private void clearScores(Objective objective) {
        for (String entry : objective.getScoreboard().getEntries()) {
            objective.getScoreboard().resetScores(entry);
        }
    }

    public void removeBoard(Player player) {
        Scoreboard board = playerBoards.remove(player.getUniqueId());
        objectives.remove(player.getUniqueId());

        if (board != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }
}
