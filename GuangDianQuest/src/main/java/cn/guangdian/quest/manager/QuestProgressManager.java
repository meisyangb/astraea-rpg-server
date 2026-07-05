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

        // 立即保存玩家数据（同步）
        savePlayerData(playerId);
    }

    public int incrementProgress(UUID playerId, String questId, int objectiveIndex, int amount) {
        PlayerQuestData data = playerRepository.getPlayerData(playerId);
        Quest quest = questManager.getQuest(questId);

        if (quest == null) return 0;

        QuestObjective obj = quest.getObjective(objectiveIndex);
        if (obj == null) return 0;

        int newProgress = data.incrementProgress(questId, objectiveIndex, amount, obj.getAmount());

        publishProgressEvent(playerId, questId, objectiveIndex, newProgress, obj.getAmount());

        // 发送进度更新消息到聊天框
        sendProgressMessage(playerId, quest, obj, newProgress);

        // 立即保存玩家数据（同步）
        savePlayerData(playerId);

        return newProgress;
    }

    private void savePlayerData(UUID playerId) {
        playerRepository.savePlayerData(playerId);
    }

    private void sendProgressMessage(UUID playerId, Quest quest, QuestObjective obj, int currentProgress) {
        org.bukkit.entity.Player player = plugin.getServer().getPlayer(playerId);
        if (player == null) return;

        int required = obj.getAmount();
        String objDesc = obj.getDescription();
        
        // 使用 MiniMessage 格式
        net.kyori.adventure.text.minimessage.MiniMessage mm = 
            net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();

        if (currentProgress >= required) {
            // 目标完成
            player.sendMessage(mm.deserialize(
                "<green>✔ <gray>" + objDesc + " <green>完成！"
            ));
            player.playSound(player.getLocation(), 
                org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        } else {
            // 进度更新
            int percent = (currentProgress * 100) / required;
            player.sendMessage(mm.deserialize(
                "<yellow>⏳ <gray>" + objDesc + " <white>" + currentProgress + "<gray>/<white>" + required + 
                " <gray>(" + percent + "%)"
            ));
        }
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
