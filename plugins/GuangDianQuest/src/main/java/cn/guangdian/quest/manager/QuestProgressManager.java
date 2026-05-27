package cn.guangdian.quest.manager;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.Quest;
import cn.guangdian.quest.model.QuestObjective;
import cn.guangdian.quest.repository.PlayerQuestRepository;

import java.util.UUID;

public class QuestProgressManager {

    private final GuangDianQuest plugin;
    private final QuestManager questManager;
    private final PlayerQuestRepository playerRepository;

    public QuestProgressManager(GuangDianQuest plugin, QuestManager questManager, PlayerQuestRepository playerRepository) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.playerRepository = playerRepository;
    }

    public PlayerQuestData getPlayerData(UUID playerId) {
        return playerRepository.getPlayerData(playerId);
    }

    public void updateProgress(UUID playerId, String questId, int objectiveIndex, int progress) {
        PlayerQuestData data = playerRepository.getPlayerData(playerId);
        data.updateProgress(questId, objectiveIndex, progress);

        Quest quest = questManager.getQuest(questId);
        QuestObjective obj = quest != null ? quest.getObjective(objectiveIndex) : null;
        int required = obj != null ? obj.getAmount() : 0;
        publishProgressEvent(playerId, questId, objectiveIndex, progress, required);
    }

    public int incrementProgress(UUID playerId, String questId, int objectiveIndex, int amount) {
        PlayerQuestData data = playerRepository.getPlayerData(playerId);
        Quest quest = questManager.getQuest(questId);

        if (quest == null) return 0;

        QuestObjective obj = quest.getObjective(objectiveIndex);
        if (obj == null) return 0;

        int newProgress = data.incrementProgress(questId, objectiveIndex, amount, obj.getAmount());

        publishProgressEvent(playerId, questId, objectiveIndex, newProgress, obj.getAmount());

        return newProgress;
    }

    private void publishProgressEvent(UUID playerId, String questId, int objectiveIndex, int currentProgress, int requiredProgress) {
        try {
            cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
            if (rpgCore == null) return;
            cn.guangdian.rpgcore.api.EventBus eventBus = rpgCore.getEventBus();
            if (eventBus == null) return;

            Quest quest = questManager.getQuest(questId);
            String questName = quest != null ? quest.getName() : questId;
            String questType = quest != null ? quest.getType().name() : "UNKNOWN";

            cn.guangdian.rpgcore.event.events.RpgQuestEvent.Progress event =
                new cn.guangdian.rpgcore.event.events.RpgQuestEvent.Progress(
                    playerId, questId, questName, questType, objectiveIndex, currentProgress, requiredProgress);
            eventBus.publish(event);
        } catch (Exception ignored) {}
    }
}
