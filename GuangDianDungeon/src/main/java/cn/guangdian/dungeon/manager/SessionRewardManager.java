package cn.guangdian.dungeon.manager;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.Difficulty;
import cn.guangdian.dungeon.model.DungeonParty;
import cn.guangdian.dungeon.model.DungeonTemplate;
import cn.guangdian.dungeon.model.PartyMember;
import cn.guangdian.dungeon.model.session.DungeonSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * 副本奖励管理器（基于 DungeonSession 新体系）
 * 负责通关后发放命令奖励和评分奖励
 */
public class SessionRewardManager {

    private final GuangDianDungeon plugin;

    public SessionRewardManager(GuangDianDungeon plugin) {
        this.plugin = plugin;
    }

    /**
     * 副本通关时发放奖励
     */
    public void distributeRewards(DungeonSession session, int score) {
        DungeonParty party = session.getParty();
        if (party == null) return;

        String dungeonId = session.getDungeonId();
        String difficultyId = session.getDifficulty();

        DungeonTemplate template = plugin.getTemplateLoader().getTemplate(dungeonId);
        if (template == null) return;

        Difficulty difficulty = template.getDifficulty(difficultyId);
        if (difficulty == null) return;

        for (PartyMember member : party.getMembers()) {
            Player player = member.getPlayer();
            if (player == null || !player.isOnline()) continue;

            boolean firstClear = checkFirstClear(player, template, difficulty);

            // 通关奖励命令
            executeRewardCommands(player, dungeonId, difficultyId, score, "clear", firstClear);

            // 评分奖励命令
            executeScoreRewardCommands(player, dungeonId, difficultyId, score);

            if (firstClear) {
                player.sendMessage(plugin.color("<gold><bold>★ 首通奖励 ★</bold>"));
                executeRewardCommands(player, dungeonId, difficultyId, score, "first_clear", true);
            }

            player.sendMessage(plugin.color("<green>获得奖励！评分: <gold>" + score));
        }
    }

    private void executeRewardCommands(Player player, String dungeonId, String difficultyId,
                                       int score, String rewardType, boolean firstClear) {
        String basePath = "reward-commands." + dungeonId + "." + difficultyId + "." + rewardType;
        List<String> commands = plugin.getConfig().getStringList(basePath);

        if (commands.isEmpty()) {
            commands = plugin.getConfig().getStringList("reward-commands.default." + rewardType);
        }

        for (String raw : commands) {
            String cmd = raw;
            double chance = 1.0;
            // 支持格式: "命令 | 0.3"  → 30%概率
            int bar = raw.lastIndexOf(" | ");
            if (bar > 0) {
                try {
                    chance = Double.parseDouble(raw.substring(bar + 3).trim());
                    cmd = raw.substring(0, bar).trim();
                } catch (NumberFormatException ignored) {}
            }
            if (Math.random() > chance) continue;
            String parsed = parseCommand(cmd, player, dungeonId, difficultyId, score, firstClear);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }

    private void executeScoreRewardCommands(Player player, String dungeonId, String difficultyId, int score) {
        String basePath = "reward-commands." + dungeonId + "." + difficultyId + ".score-rewards";
        List<Map<?, ?>> scoreRewards = plugin.getConfig().getMapList(basePath);

        if (scoreRewards.isEmpty()) {
            scoreRewards = plugin.getConfig().getMapList("reward-commands.default.score-rewards");
        }

        for (Map<?, ?> reward : scoreRewards) {
            Object minScoreObj = reward.get("min-score");
            if (minScoreObj == null) continue;

            int minScore = minScoreObj instanceof Number ? ((Number) minScoreObj).intValue() : 0;
            if (score < minScore) continue;

            @SuppressWarnings("unchecked")
            List<String> commands = (List<String>) reward.get("commands");
            if (commands == null) continue;

            for (String raw : commands) {
                String cmd = raw;
                double chance = 1.0;
                int bar = raw.lastIndexOf(" | ");
                if (bar > 0) {
                    try {
                        chance = Double.parseDouble(raw.substring(bar + 3).trim());
                        cmd = raw.substring(0, bar).trim();
                    } catch (NumberFormatException ignored) {}
                }
                if (Math.random() > chance) continue;
                String parsed = parseCommand(cmd, player, dungeonId, difficultyId, score, false);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            }
        }
    }

    private String parseCommand(String command, Player player, String dungeonId,
                                String difficultyId, int score, boolean firstClear) {
        return command
            .replace("{player}", player.getName())
            .replace("{dungeon}", dungeonId)
            .replace("{difficulty}", difficultyId)
            .replace("{score}", String.valueOf(score))
            .replace("{first_clear}", String.valueOf(firstClear));
    }

    private boolean checkFirstClear(Player player, DungeonTemplate template, Difficulty difficulty) {
        String key = template.getId() + ":" + difficulty.getId();

        var playerData = plugin.getPlayerRepository().getPlayerData(player.getUniqueId());
        if (playerData == null) return false;

        if (!playerData.hasCleared(key)) {
            playerData.markCleared(key, System.currentTimeMillis());
            return true;
        }
        return false;
    }
}
