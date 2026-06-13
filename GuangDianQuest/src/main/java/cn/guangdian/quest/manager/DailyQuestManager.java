package cn.guangdian.quest.manager;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.Quest;
import cn.guangdian.quest.model.QuestType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DailyQuestManager {

    private final GuangDianQuest plugin;
    private final QuestManager questManager;
    private final Random random;
    private final Map<UUID, List<String>> playerDailyQuests;
    private final Map<UUID, Long> lastDailyAssign;

    public DailyQuestManager(GuangDianQuest plugin, QuestManager questManager) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.random = new Random();
        this.playerDailyQuests = new ConcurrentHashMap<>();
        this.lastDailyAssign = new ConcurrentHashMap<>();
    }

    public List<String> getDailyQuests(UUID playerId) {
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);
        if (data.needsDailyReset()) {
            data.resetDaily(questManager.getDailyQuestIds());
            playerDailyQuests.remove(playerId);
            lastDailyAssign.remove(playerId);
        }

        if (!playerDailyQuests.containsKey(playerId)) {
            assignDailyQuests(playerId);
        }

        return playerDailyQuests.getOrDefault(playerId, Collections.emptyList());
    }

    private void assignDailyQuests(UUID playerId) {
        List<String> allDaily = new ArrayList<>();
        for (Quest quest : questManager.getQuestRepository().getAllQuests()) {
            if (quest.getType() == QuestType.DAILY) {
                for (int i = 0; i < quest.getDailyWeight(); i++) {
                    allDaily.add(quest.getId());
                }
            }
        }

        int limit = plugin.getDailyQuestLimit();
        List<String> assigned = new ArrayList<>();
        Set<String> used = new HashSet<>();

        Collections.shuffle(allDaily, random);

        for (String questId : allDaily) {
            if (assigned.size() >= limit) break;
            if (!used.contains(questId)) {
                assigned.add(questId);
                used.add(questId);
            }
        }

        playerDailyQuests.put(playerId, assigned);
        lastDailyAssign.put(playerId, System.currentTimeMillis());
    }

    public String getRandomDailyQuest() {
        List<String> dailyQuests = new ArrayList<>();
        for (Quest quest : questManager.getQuestRepository().getAllQuests()) {
            if (quest.getType() == QuestType.DAILY) {
                for (int i = 0; i < quest.getDailyWeight(); i++) {
                    dailyQuests.add(quest.getId());
                }
            }
        }
        if (dailyQuests.isEmpty()) return null;
        return dailyQuests.get(random.nextInt(dailyQuests.size()));
    }

    public void resetPlayerDaily(UUID playerId) {
        playerDailyQuests.remove(playerId);
        lastDailyAssign.remove(playerId);
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);
        data.resetDaily(questManager.getDailyQuestIds());
    }

    public void resetAllDaily() {
        playerDailyQuests.clear();
        lastDailyAssign.clear();
    }
}
