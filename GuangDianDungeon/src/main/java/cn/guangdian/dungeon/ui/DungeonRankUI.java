package cn.guangdian.dungeon.ui;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.DungeonTemplate;
import cn.guangdian.dungeon.model.Difficulty;
import cn.guangdian.dungeon.repository.PlayerDungeonRepository;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 副本排行榜 - 基于玩家通关数据实时排名
 */
public class DungeonRankUI extends AbstractDungeonUI {

    private String selectedDungeonId;
    private String selectedDifficulty;
    private final List<String> dungeonIds;
    private RankData rankData;

    public DungeonRankUI(GuangDianDungeon plugin, Player player) {
        super(plugin, player, 54, "<dark_gray>副本排行榜");
        this.dungeonIds = new ArrayList<>(plugin.getTemplateLoader().getTemplateIds());
        this.selectedDungeonId = dungeonIds.isEmpty() ? null : dungeonIds.get(0);
        this.selectedDifficulty = "normal";
        loadRankData();
        refresh();
    }

    private void loadRankData() {
        if (selectedDungeonId == null) {
            rankData = null;
            return;
        }
        rankData = RankData.loadFromRepository(
            plugin.getPlayerRepository(), selectedDungeonId, selectedDifficulty);
    }

    @Override
    protected void refresh() {
        inventory.clear();

        // 第一行 - 副本选择
        inventory.setItem(0, createItem(Material.ARROW, "<yellow>上一个副本", "<gray>切换查看的副本"));
        inventory.setItem(4, createItem(Material.GOLDEN_APPLE, "<gold><bold>排行榜",
            selectedDungeonId != null ? "<gray>副本: <white>" + selectedDungeonId +
            " <gray>难度: " + getDiffDisplay() : "<gray>请选择副本"));
        inventory.setItem(8, createItem(Material.ARROW, "<yellow>下一个副本", "<gray>切换查看的副本"));

        // 难度切换
        inventory.setItem(2, createItem(Material.GREEN_CONCRETE,
            "<green>普通", selectedDifficulty.equals("normal") ? "<white>当前" : "<gray>点击切换"));
        inventory.setItem(6, createItem(Material.ORANGE_CONCRETE,
            "<gold>困难", selectedDifficulty.equals("hard") ? "<white>当前" : "<gray>点击切换"));

        // 第二行 - 分隔
        fillRange(9, 17);

        // 排行内容
        if (selectedDungeonId == null) {
            inventory.setItem(22, createItem(Material.BARRIER, "<red>暂无可用副本"));
            fillAllEmpty();
            return;
        }

        if (rankData == null || rankData.entries.isEmpty()) {
            inventory.setItem(22, createItem(Material.BOOK, "<yellow>暂无通关记录",
                "<gray>还没有玩家通关此副本难度",
                "<white>快来做第一个通关的人吧！"));
            fillAllEmpty();
            return;
        }

        // 前3名特殊展示
        List<RankEntry> entries = rankData.entries;
        if (entries.size() >= 1) {
            inventory.setItem(20, createRankItem(Material.GOLD_BLOCK, "<gold><bold>第 1 名", entries.get(0)));
        }
        if (entries.size() >= 2) {
            inventory.setItem(22, createRankItem(Material.IRON_BLOCK, "<white><bold>第 2 名", entries.get(1)));
        }
        if (entries.size() >= 3) {
            inventory.setItem(24, createRankItem(Material.COPPER_BLOCK, "<yellow><bold>第 3 名", entries.get(2)));
        }

        // 4-10名
        int[] rankSlots = {30, 31, 32, 33, 34, 35, 36};
        for (int i = 0; i < Math.min(7, entries.size() - 3); i++) {
            int rank = i + 4;
            RankEntry entry = entries.get(i + 3);
            inventory.setItem(rankSlots[i], createItem(Material.PAPER,
                "<white>第 " + rank + " 名: <yellow>" + entry.playerName,
                "<gray>评分: <gold>" + entry.bestScore,
                "<gray>最快: <white>" + formatTimeMs(entry.bestTime),
                "<gray>通关: <white>" + entry.clearCount + " 次"));
        }

        // 我的排名
        inventory.setItem(49, createItem(Material.PLAYER_HEAD, "<aqua>我的排名",
            "<gray>评分: <gold>" + rankData.myBestScore,
            "<gray>最佳: <white>" + formatTimeMs(rankData.myBestTime),
            "<gray>排名: <yellow>" + (rankData.myRank > 0 ? "第 " + rankData.myRank + " 名" : "暂无")));

        // 底部导航
        inventory.setItem(45, createBackItem());
        inventory.setItem(53, createCloseItem());

        fillAllEmpty();
    }

    private org.bukkit.inventory.ItemStack createRankItem(Material material, String title, RankEntry entry) {
        return createItem(material, title,
            "<white>" + entry.playerName,
            "<gray>评分: <gold>" + entry.bestScore,
            "<gray>时间: <white>" + formatTimeMs(entry.bestTime),
            "<gray>通关: <white>" + entry.clearCount + " 次");
    }

    private String formatTimeMs(long ms) {
        if (ms <= 0) return "无";
        int seconds = (int) (ms / 1000);
        int m = seconds / 60;
        int s = seconds % 60;
        return m > 0 ? m + "分" + s + "秒" : s + "秒";
    }

    private String getDiffDisplay() {
        return switch (selectedDifficulty) {
            case "hard" -> "<gold>困难";
            case "heroic" -> "<red>英雄";
            default -> "<green>普通";
        };
    }

    @Override
    protected void handleClick(int slot) {
        playClickSound();

        // 上一个副本
        if (slot == 0 && selectedDungeonId != null) {
            int idx = dungeonIds.indexOf(selectedDungeonId);
            selectedDungeonId = dungeonIds.get(idx > 0 ? idx - 1 : dungeonIds.size() - 1);
            loadRankData();
            refresh();
            return;
        }

        // 下一个副本
        if (slot == 8 && selectedDungeonId != null) {
            int idx = dungeonIds.indexOf(selectedDungeonId);
            selectedDungeonId = dungeonIds.get(idx < dungeonIds.size() - 1 ? idx + 1 : 0);
            loadRankData();
            refresh();
            return;
        }

        // 普通难度
        if (slot == 2) {
            selectedDifficulty = "normal";
            loadRankData();
            refresh();
            return;
        }

        // 困难难度
        if (slot == 6) {
            selectedDifficulty = "hard";
            loadRankData();
            refresh();
            return;
        }

        // 返回
        if (slot == 45) {
            close();
            new DungeonMainMenuUI(plugin, player).open();
            return;
        }

        // 关闭
        if (slot == 53) {
            close();
        }
    }

    // ========== 排行数据结构 ==========

    static class RankData {
        List<RankEntry> entries = new ArrayList<>();
        int myRank = 0;
        long myBestTime = 0;
        int myBestScore = 0;

        static RankData loadFromRepository(PlayerDungeonRepository repo, String dungeonId, String difficulty) {
            RankData data = new RankData();
            File playerDataDir = new File(repo.toString()); // not ideal, fallback
            Map<UUID, RankEntry> playerScores = new HashMap<>();

            // 收集所有玩家数据
            for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
                var pd = repo.getPlayerData(offline.getUniqueId());
                if (pd == null) continue;

                String key = dungeonId + ":" + difficulty;
                if (!pd.hasCleared(key)) continue;

                int score = pd.getBestScore(key);
                long time = pd.getBestTime(key);
                int count = pd.getClearCount(key);

                playerScores.put(offline.getUniqueId(),
                    new RankEntry(offline.getName() != null ? offline.getName() : "Unknown",
                        score, time, count));
            }

            // 按评分降序排列
            data.entries = playerScores.values().stream()
                .sorted(Comparator.comparingInt((RankEntry e) -> e.bestScore).reversed()
                    .thenComparingLong(e -> e.bestTime))
                .limit(10)
                .collect(Collectors.toList());

            return data;
        }
    }

    static class RankEntry {
        String playerName;
        int bestScore;
        long bestTime;
        int clearCount;

        RankEntry(String playerName, int bestScore, long bestTime, int clearCount) {
            this.playerName = playerName;
            this.bestScore = bestScore;
            this.bestTime = bestTime;
            this.clearCount = clearCount;
        }
    }
}
