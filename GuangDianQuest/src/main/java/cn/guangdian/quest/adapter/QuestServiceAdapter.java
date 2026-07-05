package cn.guangdian.quest.adapter;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.manager.DailyQuestManager;
import cn.guangdian.quest.manager.QuestLineManager;
import cn.guangdian.quest.manager.QuestManager;
import cn.guangdian.quest.manager.QuestProgressManager;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.Quest;
import cn.guangdian.rpgcore.service.api.QuestService;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 任务服务适配器
 * 
 * 实现 RPGCore QuestService 接口
 */
public class QuestServiceAdapter implements QuestService {
    
    private final GuangDianQuest plugin;
    private final QuestManager questManager;
    private final QuestProgressManager progressManager;
    private final DailyQuestManager dailyManager;
    private final QuestLineManager questLineManager;
    
    public QuestServiceAdapter(GuangDianQuest plugin) {
        this.plugin = plugin;
        this.questManager = plugin.getQuestManager();
        this.progressManager = plugin.getProgressManager();
        this.dailyManager = plugin.getDailyManager();
        this.questLineManager = plugin.getQuestLineManager();
        
        // 注册到RPGCore
        register();
    }
    
    private void register() {
        try {
            cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getServiceRegistry().registerService(QuestService.class, this);
                plugin.getLogger().info("已注册 QuestService 到 RPGCore");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("注册QuestService失败: " + e.getMessage());
        }
    }
    
    public void unregister() {
        try {
            cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getServiceRegistry().unregisterService(QuestService.class);
            }
        } catch (Exception ignored) {}
    }
    
    // ==================== 任务查询 ====================
    
    @Override
    public List<String> getAvailableQuests(UUID playerId) {
        return questManager.getAvailableQuests(playerId);
    }
    
    @Override
    public List<String> getActiveQuests(UUID playerId) {
        PlayerQuestData data = progressManager.getPlayerData(playerId);
        return new ArrayList<>(data.getActiveQuestIds());
    }
    
    @Override
    public List<String> getCompletedQuests(UUID playerId) {
        PlayerQuestData data = progressManager.getPlayerData(playerId);
        return data.getCompletedQuestCount() > 0 ? 
            List.copyOf(data.getCompletedQuests().keySet()) : List.of();
    }
    
    @Override
    public Optional<Map<String, Object>> getQuestInfo(String questId) {
        Quest quest = questManager.getQuest(questId);
        if (quest == null) return Optional.empty();
        
        Map<String, Object> info = new HashMap<>();
        info.put("id", quest.getId());
        info.put("name", quest.getName());
        info.put("type", quest.getType().name());
        info.put("description", quest.getDescription());
        info.put("objectiveCount", quest.getObjectiveCount());
        info.put("reward", quest.getReward().getSummary());
        
        return Optional.of(info);
    }
    
    @Override
    public boolean questExists(String questId) {
        return questManager.exists(questId);
    }
    
    @Override
    public String getQuestName(String questId) {
        Quest quest = questManager.getQuest(questId);
        return quest != null ? quest.getName() : null;
    }
    
    @Override
    public String getQuestType(String questId) {
        Quest quest = questManager.getQuest(questId);
        return quest != null ? quest.getType().name() : null;
    }
    
    // ==================== 任务操作 ====================
    
    @Override
    public boolean acceptQuest(UUID playerId, String questId) {
        return questManager.acceptQuest(playerId, questId);
    }
    
    @Override
    public boolean completeQuest(UUID playerId, String questId) {
        return questManager.completeQuest(playerId, questId);
    }
    
    @Override
    public boolean abandonQuest(UUID playerId, String questId) {
        return questManager.abandonQuest(playerId, questId);
    }
    
    @Override
    public boolean canComplete(UUID playerId, String questId) {
        return questManager.canComplete(playerId, questId);
    }
    
    @Override
    public boolean canAccept(UUID playerId, String questId) {
        return questManager.canAccept(playerId, questId);
    }
    
    // ==================== 进度查询 ====================
    
    @Override
    public Map<Integer, Integer> getQuestProgress(UUID playerId, String questId) {
        PlayerQuestData data = progressManager.getPlayerData(playerId);
        int[] progress = data.getProgress(questId);
        if (progress == null) return Map.of();
        
        Map<Integer, Integer> result = new HashMap<>();
        for (int i = 0; i < progress.length; i++) {
            result.put(i, progress[i]);
        }
        return result;
    }
    
    @Override
    public int getQuestProgressPercent(UUID playerId, String questId) {
        Quest quest = questManager.getQuest(questId);
        if (quest == null) return 0;
        
        PlayerQuestData data = progressManager.getPlayerData(playerId);
        int[] progress = data.getProgress(questId);
        if (progress == null) return 0;
        
        int completed = 0;
        for (int i = 0; i < quest.getObjectiveCount(); i++) {
            cn.guangdian.quest.model.QuestObjective obj = quest.getObjective(i);
            if (obj != null && progress[i] >= obj.getAmount()) {
                completed++;
            }
        }
        
        return quest.getObjectiveCount() > 0 ? 
            completed * 100 / quest.getObjectiveCount() : 0;
    }
    
    @Override
    public int getObjectiveProgress(UUID playerId, String questId, int objectiveIndex) {
        PlayerQuestData data = progressManager.getPlayerData(playerId);
        int[] progress = data.getProgress(questId);
        if (progress == null || objectiveIndex < 0 || objectiveIndex >= progress.length) {
            return 0;
        }
        return progress[objectiveIndex];
    }
    
    @Override
    public int getObjectiveRequired(String questId, int objectiveIndex) {
        Quest quest = questManager.getQuest(questId);
        if (quest == null) return 0;
        cn.guangdian.quest.model.QuestObjective obj = quest.getObjective(objectiveIndex);
        return obj != null ? obj.getAmount() : 0;
    }
    
    // ==================== 每日任务 ====================
    
    @Override
    public List<String> getDailyQuests(UUID playerId) {
        return dailyManager.getDailyQuests(playerId);
    }
    
    @Override
    public int getDailyCompletedCount(UUID playerId) {
        PlayerQuestData data = progressManager.getPlayerData(playerId);
        return data.getDailyCompletedCount();
    }
    
    @Override
    public int getDailyQuestLimit() {
        return plugin.getDailyQuestLimit();
    }
    
    @Override
    public boolean isDailyReset(UUID playerId) {
        PlayerQuestData data = progressManager.getPlayerData(playerId);
        return data.needsDailyReset();
    }
    
    @Override
    public void resetDailyQuests(UUID playerId) {
        dailyManager.resetPlayerDaily(playerId);
    }
    
    // ==================== 任务线 ====================
    
    @Override
    public List<String> getQuestLines() {
        return new ArrayList<>(questLineManager.getQuestLineIds());
    }
    
    @Override
    public List<String> getQuestLineQuests(String questLineId) {
        cn.guangdian.quest.model.QuestLine line = questLineManager.getQuestLine(questLineId);
        return line != null ? line.getQuestIds() : List.of();
    }
    
    @Override
    public int getQuestLineProgress(UUID playerId, String questLineId) {
        PlayerQuestData data = progressManager.getPlayerData(playerId);
        return data.getQuestLineProgress(questLineId);
    }
    
    @Override
    public Optional<String> getNextQuestInLine(UUID playerId, String questLineId) {
        cn.guangdian.quest.model.QuestLine line = questLineManager.getQuestLine(questLineId);
        if (line == null) return Optional.empty();
        
        PlayerQuestData data = progressManager.getPlayerData(playerId);
        int progress = data.getQuestLineProgress(questLineId);
        
        String nextQuestId = line.getQuestId(progress + 1);
        return Optional.ofNullable(nextQuestId);
    }
    
    // ==================== 统计 ====================
    
    @Override
    public int getTotalCompletedCount(UUID playerId) {
        PlayerQuestData data = progressManager.getPlayerData(playerId);
        return data.getTotalCompletedCount();
    }
    
    @Override
    public int getAchievementPoints(UUID playerId) {
        PlayerQuestData data = progressManager.getPlayerData(playerId);
        return data.getAchievementPoints();
    }
    
    // ==================== 异步API ====================
    
    @Override
    public CompletableFuture<Map<Integer, Integer>> getQuestProgressAsync(UUID playerId, String questId) {
        return CompletableFuture.supplyAsync(() -> getQuestProgress(playerId, questId));
    }
    
    @Override
    public CompletableFuture<Boolean> acceptQuestAsync(UUID playerId, String questId) {
        return CompletableFuture.supplyAsync(() -> acceptQuest(playerId, questId));
    }
    
    @Override
    public CompletableFuture<Boolean> completeQuestAsync(UUID playerId, String questId) {
        return CompletableFuture.supplyAsync(() -> completeQuest(playerId, questId));
    }
    
    @Override
    public CompletableFuture<Void> savePlayerDataAsync(UUID playerId) {
        plugin.getPlayerRepository().savePlayerData(playerId);
        return CompletableFuture.completedFuture(null);
    }
    
    // ==================== 服务状态 ====================
    
    @Override
    public boolean isAvailable() {
        return true;
    }
}