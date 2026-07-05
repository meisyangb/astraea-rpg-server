package cn.guangdian.quest.model;

import java.util.*;

public class PlayerQuestData {

    private final UUID playerId;
    private final Map<String, int[]> activeQuests;
    private final Map<String, Long> completedQuests;
    private int dailyCompletedCount;
    private long dailyResetTime;
    private final Map<String, Integer> questLineProgress;
    private int totalCompletedCount;
    private int achievementPoints;

    public PlayerQuestData(UUID playerId) {
        this.playerId = playerId;
        this.activeQuests = new HashMap<>();
        this.completedQuests = new HashMap<>();
        this.questLineProgress = new HashMap<>();
        this.dailyCompletedCount = 0;
        this.dailyResetTime = System.currentTimeMillis();
        this.totalCompletedCount = 0;
        this.achievementPoints = 0;
    }

    public void acceptQuest(String questId, int objectiveCount) {
        activeQuests.put(questId, new int[objectiveCount]);
    }

    public void abandonQuest(String questId) {
        activeQuests.remove(questId);
    }

    public void completeQuest(String questId, QuestType type) {
        activeQuests.remove(questId);
        completedQuests.put(questId, System.currentTimeMillis());
        totalCompletedCount++;
        if (type == QuestType.DAILY) {
            dailyCompletedCount++;
        } else if (type == QuestType.ACHIEVEMENT) {
            achievementPoints++;
        }
    }

    public boolean isQuestActive(String questId) {
        return activeQuests.containsKey(questId);
    }

    public boolean isQuestCompleted(String questId) {
        return completedQuests.containsKey(questId);
    }

    /**
     * 检查每日任务今天是否已完成
     */
    public boolean isDailyQuestCompleted(String questId) {
        Long completionTime = completedQuests.get(questId);
        if (completionTime == null) return false;

        Calendar completionCal = Calendar.getInstance();
        completionCal.setTimeInMillis(completionTime);
        Calendar nowCal = Calendar.getInstance();

        return completionCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
            && completionCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR);
    }

    public int[] getProgress(String questId) {
        return activeQuests.get(questId);
    }

    public void updateProgress(String questId, int objectiveIndex, int progress) {
        int[] progressArray = activeQuests.get(questId);
        if (progressArray != null && objectiveIndex >= 0 && objectiveIndex < progressArray.length) {
            progressArray[objectiveIndex] = progress;
        }
    }

    public int incrementProgress(String questId, int objectiveIndex, int amount, int max) {
        int[] progressArray = activeQuests.get(questId);
        if (progressArray != null && objectiveIndex >= 0 && objectiveIndex < progressArray.length) {
            progressArray[objectiveIndex] = Math.min(progressArray[objectiveIndex] + amount, max);
            return progressArray[objectiveIndex];
        }
        return 0;
    }

    public int getActiveQuestCount() {
        return activeQuests.size();
    }

    public Set<String> getActiveQuestIds() {
        return activeQuests.keySet();
    }

    public int getCompletedQuestCount() {
        return completedQuests.size();
    }

    public Long getCompletionTime(String questId) {
        return completedQuests.get(questId);
    }

    public Map<String, Long> getCompletedQuests() {
        return completedQuests;
    }

    public int getDailyCompletedCount() {
        return dailyCompletedCount;
    }

    public boolean needsDailyReset() {
        long now = System.currentTimeMillis();
        Calendar lastCal = Calendar.getInstance();
        lastCal.setTimeInMillis(dailyResetTime);
        Calendar nowCal = Calendar.getInstance();
        nowCal.setTimeInMillis(now);
        return lastCal.get(Calendar.DAY_OF_YEAR) != nowCal.get(Calendar.DAY_OF_YEAR)
            || lastCal.get(Calendar.YEAR) != nowCal.get(Calendar.YEAR);
    }

    public void resetDaily(Set<String> dailyQuestIds) {
        dailyCompletedCount = 0;
        dailyResetTime = System.currentTimeMillis();
        activeQuests.keySet().removeAll(dailyQuestIds);
    }

    public int getQuestLineProgress(String questLineId) {
        return questLineProgress.getOrDefault(questLineId, -1);
    }

    public void updateQuestLineProgress(String questLineId, int progress) {
        questLineProgress.put(questLineId, progress);
    }

    public int getTotalCompletedCount() { return totalCompletedCount; }
    public int getAchievementPoints() { return achievementPoints; }
    public long getDailyResetTime() { return dailyResetTime; }
    public UUID getPlayerId() { return playerId; }

    // SQLite 存储辅助方法
    public void setTotalCompletedCount(int v) { this.totalCompletedCount = v; }
    public void setAchievementPoints(int v) { this.achievementPoints = v; }
    public void setDailyCompletedCount(int v) { this.dailyCompletedCount = v; }
    public void setDailyResetTime(long v) { this.dailyResetTime = v; }

    public void loadActiveQuest(String questId, int[] progress) {
        this.activeQuests.put(questId, progress);
    }

    public void loadCompletedQuest(String questId, long completionTime) {
        this.completedQuests.put(questId, completionTime);
    }

    public void loadQuestLineProgress(String lineId, int progress) {
        this.questLineProgress.put(lineId, progress);
    }

    public Map<String, Integer> getQuestLineProgressMap() {
        return questLineProgress;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("playerId", playerId.toString());
        map.put("totalCompletedCount", totalCompletedCount);
        map.put("achievementPoints", achievementPoints);
        map.put("dailyCompletedCount", dailyCompletedCount);
        map.put("dailyResetTime", dailyResetTime);

        Map<String, List<Integer>> active = new LinkedHashMap<>();
        for (Map.Entry<String, int[]> entry : activeQuests.entrySet()) {
            List<Integer> progressList = new ArrayList<>();
            for (int v : entry.getValue()) {
                progressList.add(v);
            }
            active.put(entry.getKey(), progressList);
        }
        map.put("activeQuests", active);

        map.put("completedQuests", new LinkedHashMap<>(completedQuests));
        map.put("questLineProgress", new LinkedHashMap<>(questLineProgress));

        return map;
    }

    @SuppressWarnings("unchecked")
    public static PlayerQuestData fromMap(Map<String, Object> map) {
        UUID playerId = UUID.fromString((String) map.get("playerId"));
        PlayerQuestData data = new PlayerQuestData(playerId);

        data.totalCompletedCount = ((Number) map.getOrDefault("totalCompletedCount", 0)).intValue();
        data.achievementPoints = ((Number) map.getOrDefault("achievementPoints", 0)).intValue();
        data.dailyCompletedCount = ((Number) map.getOrDefault("dailyCompletedCount", 0)).intValue();
        data.dailyResetTime = ((Number) map.getOrDefault("dailyResetTime", System.currentTimeMillis())).longValue();

        Object activeObj = map.get("activeQuests");
        if (activeObj instanceof Map) {
            Map<String, Object> activeMap = (Map<String, Object>) activeObj;
            for (Map.Entry<String, Object> entry : activeMap.entrySet()) {
                String questId = entry.getKey();
                Object val = entry.getValue();
                if (val instanceof List) {
                    List<?> list = (List<?>) val;
                    int[] progress = new int[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        progress[i] = ((Number) list.get(i)).intValue();
                    }
                    data.activeQuests.put(questId, progress);
                }
            }
        }

        Object completedObj = map.get("completedQuests");
        if (completedObj instanceof Map) {
            Map<String, Object> completedMap = (Map<String, Object>) completedObj;
            for (Map.Entry<String, Object> entry : completedMap.entrySet()) {
                long timestamp = ((Number) entry.getValue()).longValue();
                data.completedQuests.put(entry.getKey(), timestamp);
            }
        }

        Object lineObj = map.get("questLineProgress");
        if (lineObj instanceof Map) {
            Map<String, Object> lineMap = (Map<String, Object>) lineObj;
            for (Map.Entry<String, Object> entry : lineMap.entrySet()) {
                int progress = ((Number) entry.getValue()).intValue();
                data.questLineProgress.put(entry.getKey(), progress);
            }
        }

        return data;
    }
}
