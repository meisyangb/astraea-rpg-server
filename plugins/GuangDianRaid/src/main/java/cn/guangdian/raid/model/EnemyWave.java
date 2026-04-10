package cn.guangdian.raid.model;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EnemyWave {

    public enum WaveTrigger {
        PHASE_START,
        INTEL_COLLECT,
        OBJECTIVE_COMPLETE,
        PLAYER_DEATH,
        TIME_ELAPSED
    }

    private final String id;
    private WaveTrigger trigger;
    private int triggerValue;
    private List<EnemySpawn> spawns;

    public EnemyWave(String id) {
        this.id = id;
        this.trigger = WaveTrigger.PHASE_START;
        this.triggerValue = 0;
        this.spawns = new ArrayList<>();
    }

    public static EnemyWave fromConfig(String id, ConfigurationSection section) {
        EnemyWave wave = new EnemyWave(id);
        if (section == null) return wave;

        String triggerStr = section.getString("trigger", "PHASE_START");
        try {
            wave.trigger = WaveTrigger.valueOf(triggerStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            wave.trigger = WaveTrigger.PHASE_START;
        }

        wave.triggerValue = section.getInt("trigger_value", 0);

        List<Map<?, ?>> spawnsList = section.getMapList("spawns");
        for (Map<?, ?> spawnMap : spawnsList) {
            EnemySpawn spawn = EnemySpawn.fromMap(spawnMap);
            if (spawn != null) {
                wave.spawns.add(spawn);
            }
        }

        return wave;
    }

    public String getId() { return id; }
    public WaveTrigger getTrigger() { return trigger; }
    public int getTriggerValue() { return triggerValue; }
    public List<EnemySpawn> getSpawns() { return spawns; }
}
