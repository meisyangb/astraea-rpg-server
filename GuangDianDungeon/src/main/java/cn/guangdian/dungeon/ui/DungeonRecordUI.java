package cn.guangdian.dungeon.ui;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 通关记录界面 - 查看玩家在所有副本的全部通关记录
 */
public class DungeonRecordUI extends AbstractDungeonUI {

    private int page;
    private final int maxPage;

    public DungeonRecordUI(GuangDianDungeon plugin, Player player) {
        super(plugin, player, 54, "<dark_gray>通关记录");
        var playerData = plugin.getPlayerRepository().getPlayerData(player.getUniqueId());
        int recordCount = playerData != null ? playerData.getClearRecords().size() : 0;
        this.maxPage = Math.max(0, (recordCount - 1) / 36);
        this.page = 0;
        refresh();
    }

    @Override
    protected void refresh() {
        inventory.clear();

        // 标题
        inventory.setItem(4, createItem(Material.WRITABLE_BOOK, "<gold><bold>通关记录",
            "<gray>查看你的副本战绩"));

        var playerData = plugin.getPlayerRepository().getPlayerData(player.getUniqueId());
        if (playerData == null || playerData.getClearRecords().isEmpty()) {
            inventory.setItem(22, createItem(Material.BOOK, "<yellow>暂无通关记录",
                "<gray>还没有通关任何副本",
                "<white>快找队友一起挑战吧！"));
            fillAllEmpty();
            inventory.setItem(45, createBackItem());
            inventory.setItem(53, createCloseItem());
            return;
        }

        Map<String, PlayerDungeonData.ClearRecord> records = playerData.getClearRecords();
        List<Map.Entry<String, PlayerDungeonData.ClearRecord>> sorted = new ArrayList<>(records.entrySet());
        sorted.sort(Comparator.comparingLong(e -> -e.getValue().firstClearTime));

        int start = page * 36;
        int end = Math.min(start + 36, sorted.size());

        for (int i = start, slot = 9; i < end && slot < 45; i++, slot++) {
            var entry = sorted.get(i);
            String key = entry.getKey(); // "dungeonId:difficultyId"
            PlayerDungeonData.ClearRecord record = entry.getValue();

            String[] parts = key.split(":", 2);
            String dungeonId = parts[0];
            String diffId = parts.length > 1 ? parts[1] : "unknown";

            DungeonTemplate template = plugin.getTemplateLoader().getTemplate(dungeonId);
            String dungeonName = template != null ? template.getName() : dungeonId;

            Difficulty difficulty = template != null ? template.getDifficulty(diffId) : null;
            String diffDisplay = difficulty != null ? difficulty.getName() : diffId;
            String diffColor = getDiffColor(diffId);

            String timeStr = formatTimeMs(System.currentTimeMillis() - record.firstClearTime);

            inventory.setItem(slot, createItem(Material.MAP,
                "<white>" + dungeonName,
                "<gray>难度: " + diffColor + diffDisplay,
                "<gray>最高分: <gold>" + record.bestScore,
                "<gray>最佳时间: <white>" + formatTimeMs(record.bestTime),
                "<gray>首通: <white>" + timeStr + "前",
                "<gray>通关: <white>" + record.clearCount + " 次"
            ));
        }

        // 翻页
        if (page > 0) {
            inventory.setItem(45, createItem(Material.SPECTRAL_ARROW, "<green>上一页"));
        }
        inventory.setItem(49, createItem(Material.BOOK, "<yellow>第 " + (page + 1) + " / " + (maxPage + 1) + " 页",
            "<gray>共 " + records.size() + " 条记录"));
        if (page < maxPage) {
            inventory.setItem(51, createItem(Material.SPECTRAL_ARROW, "<green>下一页"));
        }

        inventory.setItem(53, createCloseItem());
        fillAllEmpty();
    }

    private String getDiffColor(String id) {
        return switch (id) {
            case "normal" -> "<green>";
            case "hard" -> "<gold>";
            case "heroic" -> "<red>";
            default -> "<white>";
        };
    }

    private String formatTimeMs(long ms) {
        long m = ms / 60000;
        long h = m / 60;
        m = m % 60;
        if (h > 0) return h + "小时" + m + "分钟";
        if (m > 0) return m + "分钟";
        return "不到1分钟";
    }

    @Override
    protected void handleClick(int slot) {
        playClickSound();

        if (slot == 53) { close(); return; }
        if (slot == 45 && page > 0) { page--; refresh(); return; }
        if (slot == 51 && page < maxPage) { page++; refresh(); return; }
    }
}
