package cn.guangdian.dungeon.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DungeonTemplate {

    private final String id;
    private final String name;
    private final String description;
    private final String worldTemplate;
    private final DungeonSettings settings;
    private final List<Difficulty> difficulties;
    private final Map<String, Difficulty> difficultyMap;
    private final Map<String, RewardPool> rewardPools;
    private final List<RewardDefinition> firstClearRewards;
    private final List<ScoreReward> scoreRewards;

    public DungeonTemplate(String id, String name, String description, String worldTemplate,
                          DungeonSettings settings, List<Difficulty> difficulties,
                          Map<String, RewardPool> rewardPools,
                          List<RewardDefinition> firstClearRewards, List<ScoreReward> scoreRewards) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.worldTemplate = worldTemplate;
        this.settings = settings;
        this.difficulties = difficulties;
        this.difficultyMap = difficulties.stream().collect(Collectors.toMap(Difficulty::getId, d -> d));
        this.rewardPools = rewardPools;
        this.firstClearRewards = firstClearRewards;
        this.scoreRewards = scoreRewards;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getWorldTemplate() { return worldTemplate; }
    public DungeonSettings getSettings() { return settings; }
    public List<Difficulty> getDifficulties() { return difficulties; }
    public Map<String, RewardPool> getRewardPools() { return rewardPools; }
    public List<RewardDefinition> getFirstClearRewards() { return firstClearRewards; }
    public List<ScoreReward> getScoreRewards() { return scoreRewards; }

    public Difficulty getDifficulty(String difficultyId) {
        return difficultyMap != null ? difficultyMap.get(difficultyId) : null;
    }

    public Difficulty getDefaultDifficulty() {
        return difficulties.isEmpty() ? null : difficulties.get(0);
    }

    public int getTotalPhases() {
        // 阶段数量现在由 StageLoader 从 dungeons/*.yml 动态加载
        return 0;
    }
}
