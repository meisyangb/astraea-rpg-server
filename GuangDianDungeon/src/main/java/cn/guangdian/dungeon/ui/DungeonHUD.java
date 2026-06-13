package cn.guangdian.dungeon.ui;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.DungeonParty;
import cn.guangdian.dungeon.model.PartyMember;
import cn.guangdian.dungeon.model.session.DungeonSession;
import cn.guangdian.dungeon.model.stage.Stage;
import cn.guangdian.dungeon.model.stage.Wave;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * 副本战斗 HUD - 副本中进行时打开的状态面板
 * 展示实时阶段、波次、击杀进度、队伍状态
 */
public class DungeonHUD extends AbstractDungeonUI {

    private final DungeonSession session;
    private long updateTaskId = -1;

    public DungeonHUD(GuangDianDungeon plugin, Player player, DungeonSession session) {
        super(plugin, player, 27, "<dark_red>副本战斗面板"); // 27格紧凑面板
        this.session = session;
        refresh();
        startAutoRefresh();
    }

    @Override
    protected void refresh() {
        inventory.clear();

        if (session == null || session.getState() == DungeonSession.SessionState.COMPLETED
            || session.getState() == DungeonSession.SessionState.FAILED) {
            inventory.setItem(13, createItem(Material.BARRIER, "<red>副本已结束"));
            inventory.setItem(22, createCloseItem());
            fillAllEmpty();
            stopAutoRefresh();
            return;
        }

        Stage currentStage = session.getCurrentStage();
        Wave currentWave = session.getCurrentWave();
        DungeonParty party = session.getParty();
        long elapsedSec = session.getElapsedTime() / 1000;
        long remainingSec = session.getTimeLimit() > 0 ?
            session.getTimeLimit() - elapsedSec : -1;

        // 第一行: 副本信息
        String timeDisplay = remainingSec >= 0 ?
            String.format("<white>剩余 <gold>%02d:%02d", remainingSec / 60, remainingSec % 60) :
            String.format("<white>已用 <aqua>%02d:%02d", elapsedSec / 60, elapsedSec % 60);
        inventory.setItem(4, createItem(Material.CLOCK,
            "<gold><bold>" + session.getDungeonId(),
            timeDisplay,
            "<gray>难度: <white>" + session.getDifficulty(),
            "<gray>进度: <green>" + session.getTotalKills() + "击杀 <red>" + session.getTotalDeaths() + "死亡"));

        // 第二行: 分隔
        fillRange(9, 9);
        fillRange(15, 15);

        // 阶段/波次信息
        if (currentStage != null) {
            String stageType = currentStage.getType() == Stage.StageType.BOSS ?
                "<red>BOSS战" : "<gold>战斗";
            inventory.setItem(10, createItem(Material.NETHER_STAR,
                "<yellow>当前阶段",
                "<white>" + currentStage.getName(),
                "<gray>类型: " + stageType));
        }

        if (currentWave != null) {
            int killCount = currentWave.getKillCount();
            int totalCount = currentWave.getSpawnedMobCount();
            String progress = totalCount > 0 ?
                killCount + "/" + totalCount + " (" + (int)((double)killCount / totalCount * 100) + "%)" :
                "准备中";

            inventory.setItem(13, createItem(Material.IRON_SWORD,
                "<green>当前波次",
                "<white>击杀: " + progress,
                currentWave.getTimeLimit() > 0 ?
                    "<gray>时限: <white>" + currentWave.getTimeLimit() + "秒" : ""));
        }

        // 经验
        if (currentWave != null && currentWave.getExpReward() > 0) {
            inventory.setItem(14, createItem(Material.EXPERIENCE_BOTTLE,
                "<aqua>波次经验", "<white>" + currentWave.getExpReward() + " EXP"));
        }

        // 队员状态 (slot 18-22)
        if (party != null) {
            int memberSlot = 18;
            for (PartyMember member : party.getMembers()) {
                if (memberSlot > 22) break;
                Player memberPlayer = Bukkit.getPlayer(member.getPlayerId());
                if (memberPlayer == null) continue;

                double health = memberPlayer.getHealth();
                double maxHealth = memberPlayer.getMaxHealth();
                int healthPercent = (int) ((health / maxHealth) * 100);
                String healthColor = healthPercent > 60 ? "<green>" :
                    healthPercent > 30 ? "<gold>" : "<red>";

                inventory.setItem(memberSlot, createItem(
                    member.isLeader() ? Material.GOLDEN_HELMET : Material.PLAYER_HEAD,
                    (member.isLeader() ? "<gold>" : "<white>") + memberPlayer.getName(),
                    healthColor + "❤ " + healthPercent + "% (" + (int)health + "/" + (int)maxHealth + ")",
                    memberPlayer.isDead() ? "<red>已死亡" : "<green>存活"
                ));
                memberSlot++;
            }
        }

        // 底部
        inventory.setItem(26, createCloseItem());

        fillAllEmpty();
    }

    private void startAutoRefresh() {
        updateTaskId = plugin.getScheduler().runSyncRepeating(() -> {
            if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() == this) {
                refresh();
            } else {
                stopAutoRefresh();
            }
        }, 20L, 20L);
    }

    private void stopAutoRefresh() {
        if (updateTaskId != -1) {
            plugin.getScheduler().cancelTask(updateTaskId);
            updateTaskId = -1;
        }
    }

    @Override
    protected void handleClick(int slot) {
        if (slot == 26) {
            close();
        }
    }

    @Override
    public void close() {
        stopAutoRefresh();
        super.close();
    }
}
