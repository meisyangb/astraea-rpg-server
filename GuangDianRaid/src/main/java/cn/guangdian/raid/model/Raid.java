package cn.guangdian.raid.model;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Raid {

    private final String id;
    private String name;
    private List<String> description;
    private int minPlayers;
    private int maxPlayers;
    private int totalTimeLimit;

    private RaidPhaseConfig searchPhase;
    private RaidPhaseConfig combatPhase;
    private RaidPhaseConfig extractPhase;

    private Map<String, ExtractionPoint> extractionPoints;
    private Map<String, Intel> intelItems;
    private List<EnemyWave> enemyWaves;
    private RaidReward baseReward;
    private DifficultyScaling difficulty;

    private String worldName;
    private Location spawnLocation;
    private Location lobbyLocation;

    public Raid(String id) {
        this.id = id;
        this.description = new ArrayList<>();
        this.minPlayers = 1;
        this.maxPlayers = 4;
        this.totalTimeLimit = 600;
        this.extractionPoints = new HashMap<>();
        this.intelItems = new HashMap<>();
        this.enemyWaves = new ArrayList<>();
        this.difficulty = new DifficultyScaling();
    }

    public static Raid fromConfig(String id, ConfigurationSection section) {
        Raid raid = new Raid(id);

        raid.name = section.getString("name", id);
        raid.description = section.getStringList("description");

        ConfigurationSection playersSection = section.getConfigurationSection("players");
        if (playersSection != null) {
            raid.minPlayers = playersSection.getInt("min", 1);
            raid.maxPlayers = playersSection.getInt("max", 4);
        }

        raid.totalTimeLimit = section.getInt("time_limit", 600);

        ConfigurationSection phasesSection = section.getConfigurationSection("phases");
        if (phasesSection != null) {
            raid.searchPhase = RaidPhaseConfig.fromConfig(phasesSection.getConfigurationSection("search"));
            raid.combatPhase = RaidPhaseConfig.fromConfig(phasesSection.getConfigurationSection("combat"));
            raid.extractPhase = RaidPhaseConfig.fromConfig(phasesSection.getConfigurationSection("extract"));
        }

        ConfigurationSection extractionSection = section.getConfigurationSection("extraction_points");
        if (extractionSection != null) {
            for (String pointId : extractionSection.getKeys(false)) {
                ExtractionPoint point = ExtractionPoint.fromConfig(pointId, extractionSection.getConfigurationSection(pointId));
                raid.extractionPoints.put(pointId, point);
            }
        }

        ConfigurationSection intelSection = section.getConfigurationSection("intel_items");
        if (intelSection != null) {
            for (String intelId : intelSection.getKeys(false)) {
                Intel intel = Intel.fromConfig(intelId, intelSection.getConfigurationSection(intelId));
                raid.intelItems.put(intelId, intel);
            }
        }

        ConfigurationSection wavesSection = section.getConfigurationSection("enemy_waves");
        if (wavesSection != null) {
            for (String waveId : wavesSection.getKeys(false)) {
                EnemyWave wave = EnemyWave.fromConfig(waveId, wavesSection.getConfigurationSection(waveId));
                raid.enemyWaves.add(wave);
            }
        }

        ConfigurationSection rewardSection = section.getConfigurationSection("rewards");
        if (rewardSection != null) {
            raid.baseReward = RaidReward.fromConfig(rewardSection);
        } else {
            raid.baseReward = new RaidReward();
        }

        ConfigurationSection difficultySection = section.getConfigurationSection("difficulty");
        if (difficultySection != null) {
            raid.difficulty = DifficultyScaling.fromConfig(difficultySection);
        }

        raid.worldName = section.getString("world", "raid_" + id);

        return raid;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<String> getDescription() { return description; }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getTotalTimeLimit() { return totalTimeLimit; }
    public RaidPhaseConfig getSearchPhase() { return searchPhase; }
    public RaidPhaseConfig getCombatPhase() { return combatPhase; }
    public RaidPhaseConfig getExtractPhase() { return extractPhase; }
    public Map<String, ExtractionPoint> getExtractionPoints() { return extractionPoints; }
    public Map<String, Intel> getIntelItems() { return intelItems; }
    public List<EnemyWave> getEnemyWaves() { return enemyWaves; }
    public RaidReward getBaseReward() { return baseReward; }
    public DifficultyScaling getDifficulty() { return difficulty; }
    public String getWorldName() { return worldName; }
    public Location getSpawnLocation() { return spawnLocation; }
    public Location getLobbyLocation() { return lobbyLocation; }

    public void setSpawnLocation(Location spawnLocation) { this.spawnLocation = spawnLocation; }
    public void setLobbyLocation(Location lobbyLocation) { this.lobbyLocation = lobbyLocation; }
}
