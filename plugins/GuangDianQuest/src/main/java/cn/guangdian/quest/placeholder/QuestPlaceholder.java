package cn.guangdian.quest.placeholder;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.Quest;
import cn.guangdian.quest.model.QuestObjective;
import cn.guangdian.rpgcore.integration.PlaceholderService;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuestPlaceholder {

    private final GuangDianQuest plugin;

    public QuestPlaceholder(GuangDianQuest plugin) {
        this.plugin = plugin;
    }

    public void register() {
        PlaceholderService service = PlaceholderService.getInstance();
        if (service == null) return;
        
        service.register("gdquest", (player, params) -> {
            if (player == null) return "";

            UUID playerId = player.getUniqueId();
            String[] parts = params.split("_", 3);
            String mainKey = parts[0].toLowerCase();

            switch (mainKey) {
                case "active" -> {
                    if (parts.length >= 2 && parts[1].equalsIgnoreCase("list")) {
                        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);
                        List<String> names = new ArrayList<>();
                        for (String questId : data.getActiveQuestIds()) {
                            Quest quest = plugin.getQuestManager().getQuest(questId);
                            if (quest != null) names.add(quest.getName());
                        }
                        return String.join(", ", names);
                    }
                    PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);
                    return String.valueOf(data.getActiveQuestCount());
                }

                case "completed" -> {
                    PlayerQuestData cdata = plugin.getProgressManager().getPlayerData(playerId);
                    return String.valueOf(cdata.getCompletedQuestCount());
                }

                case "daily" -> {
                    if (parts.length >= 2 && parts[1].equalsIgnoreCase("limit")) {
                        return String.valueOf(plugin.getDailyQuestLimit());
                    }
                    PlayerQuestData ddata = plugin.getProgressManager().getPlayerData(playerId);
                    return String.valueOf(ddata.getDailyCompletedCount());
                }

                case "progress" -> {
                    if (parts.length >= 2) {
                        String questId = parts[1];
                        return String.valueOf(plugin.getServiceAdapter().getQuestProgressPercent(playerId, questId));
                    }
                    return "0";
                }

                case "name" -> {
                    if (parts.length >= 2) {
                        Quest quest = plugin.getQuestManager().getQuest(parts[1]);
                        if (quest != null) return quest.getName();
                    }
                    return "";
                }

                case "type" -> {
                    if (parts.length >= 2) {
                        Quest quest = plugin.getQuestManager().getQuest(parts[1]);
                        if (quest != null) return quest.getType().getDisplayName();
                    }
                    return "";
                }

                case "objective" -> {
                    if (parts.length >= 3) {
                        String questId = parts[1];
                        try {
                            int objIndex = Integer.parseInt(parts[2]);
                            Quest quest = plugin.getQuestManager().getQuest(questId);
                            if (quest != null) {
                                QuestObjective obj = quest.getObjective(objIndex);
                                if (obj != null) {
                                    PlayerQuestData odata = plugin.getProgressManager().getPlayerData(playerId);
                                    int[] progress = odata.getProgress(questId);
                                    int current = (progress != null && objIndex < progress.length) ? progress[objIndex] : 0;
                                    return obj.getProgressText(current);
                                }
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                    return "";
                }

                case "objprogress" -> {
                    if (parts.length >= 3) {
                        String questId = parts[1];
                        try {
                            int objIndex = Integer.parseInt(parts[2]);
                            PlayerQuestData pdata = plugin.getProgressManager().getPlayerData(playerId);
                            int[] progress = pdata.getProgress(questId);
                            if (progress != null && objIndex < progress.length) {
                                return String.valueOf(progress[objIndex]);
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                    return "0";
                }

                case "objrequired" -> {
                    if (parts.length >= 3) {
                        String questId = parts[1];
                        try {
                            int objIndex = Integer.parseInt(parts[2]);
                            return String.valueOf(plugin.getServiceAdapter().getObjectiveRequired(questId, objIndex));
                        } catch (NumberFormatException ignored) {}
                    }
                    return "0";
                }

                case "total" -> {
                    return String.valueOf(plugin.getQuestManager().getQuestCount());
                }

                case "available" -> {
                    List<String> available = plugin.getQuestManager().getAvailableQuests(playerId);
                    return String.valueOf(available.size());
                }

                case "cancomplete" -> {
                    if (parts.length >= 2) {
                        return String.valueOf(plugin.getQuestManager().canComplete(playerId, parts[1]));
                    }
                    return "false";
                }

                case "questline" -> {
                    if (parts.length >= 2) {
                        String lineId = parts[1];
                        PlayerQuestData ldata = plugin.getProgressManager().getPlayerData(playerId);
                        int progress = ldata.getQuestLineProgress(lineId);
                        cn.guangdian.quest.model.QuestLine line = plugin.getQuestLineManager().getQuestLine(lineId);
                        if (line != null) {
                            return (progress + 1) + "/" + line.getLength();
                        }
                    }
                    return "";
                }

                case "achievement" -> {
                    PlayerQuestData adata = plugin.getProgressManager().getPlayerData(playerId);
                    return String.valueOf(adata.getAchievementPoints());
                }
            }

            return null;
        });
    }

    public void unregister() {
    }
}
