package cn.guangdian.dungeon.manager;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.session.DungeonSession;
import cn.guangdian.dungeon.model.stage.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class StageLoader {
    private final GuangDianDungeon plugin;
    
    public StageLoader(GuangDianDungeon plugin) {
        this.plugin = plugin;
    }
    
    public DungeonSession loadDungeonConfig(String dungeonId, World world) {
        File dungeonFile = new File(plugin.getDataFolder(), "dungeons/" + dungeonId + ".yml");
        if (!dungeonFile.exists()) {
            plugin.getLogger().warning("Dungeon config not found: " + dungeonId);
            return null;
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dungeonFile);
        DungeonSession session = new DungeonSession();
        session.setSessionId(UUID.randomUUID().toString().substring(0, 8));
        session.setDungeonId(dungeonId);
        session.setInstanceWorld(world);
        
        int timeLimit = config.getInt("settings.time-limit", 1800);
        session.setTimeLimit(timeLimit);
        plugin.getLogger().info("[DEBUG] Dungeon time limit: " + timeLimit + " seconds");
        
        loadSpawnPoints(session, config);
        loadStages(session, config);
        
        return session;
    }
    
    private void loadSpawnPoints(DungeonSession session, YamlConfiguration config) {
        ConfigurationSection spawnPointsSection = config.getConfigurationSection("spawn-points");
        if (spawnPointsSection == null) return;
        
        for (String pointId : spawnPointsSection.getKeys(false)) {
            ConfigurationSection pointSection = spawnPointsSection.getConfigurationSection(pointId);
            if (pointSection == null) continue;
            
            ConfigurationSection locSection = pointSection.getConfigurationSection("location");
            if (locSection == null) continue;
            
            World world = session.getInstanceWorld();
            double x = locSection.getDouble("x", 0);
            double y = locSection.getDouble("y", 64);
            double z = locSection.getDouble("z", 0);
            
            SpawnPoint spawnPoint = new SpawnPoint(
                pointId,
                new Location(world, x, y, z),
                pointSection.getDouble("radius", 3)
            );
            
            session.getSpawnPoints().put(pointId, spawnPoint);
        }
    }
    
    private void loadStages(DungeonSession session, YamlConfiguration config) {
        List<Map<?, ?>> stagesList = config.getMapList("stages");
        if (stagesList.isEmpty()) return;
        
        for (Map<?, ?> stageMap : stagesList) {
            Stage stage = parseStage(stageMap, session.getInstanceWorld());
            if (stage != null) {
                session.getStages().add(stage);
            }
        }
    }
    
    private Stage parseStage(Map<?, ?> map, World world) {
        Stage stage = new Stage();
        
        stage.setId(getString(map, "id", ""));
        stage.setName(getString(map, "name", ""));
        
        String typeStr = getString(map, "type", "COMBAT");
        try {
            stage.setType(Stage.StageType.valueOf(typeStr));
        } catch (IllegalArgumentException e) {
            stage.setType(Stage.StageType.COMBAT);
        }
        
        Map<?, ?> triggerMap = getMap(map, "trigger");
        if (triggerMap != null) {
            stage.setTrigger(parseTrigger(triggerMap));
        }
        
        List<Map<?, ?>> wavesList = getList(map, "waves");
        if (wavesList != null) {
            for (Map<?, ?> waveMap : wavesList) {
                Wave wave = parseWave(waveMap);
                if (wave != null) {
                    stage.getWaves().add(wave);
                }
            }
        }
        
        List<Map<?, ?>> onCompleteList = getList(map, "on-complete");
        if (onCompleteList != null) {
            for (Map<?, ?> actionMap : onCompleteList) {
                StageAction action = parseAction(actionMap);
                if (action != null) {
                    stage.getOnComplete().add(action);
                }
            }
        }
        
        return stage;
    }
    
    private StageTrigger parseTrigger(Map<?, ?> map) {
        StageTrigger trigger = new StageTrigger();
        
        String typeStr = getString(map, "type", "ON_START");
        try {
            trigger.setType(TriggerType.valueOf(typeStr));
        } catch (IllegalArgumentException e) {
            trigger.setType(TriggerType.ON_START);
        }
        
        trigger.setTargetStage(getString(map, "stage", null));
        trigger.setTargetMob(getString(map, "target-mob", null));
        trigger.setCount(getInt(map, "count", 0));
        trigger.setDelaySeconds(getInt(map, "delay", 0));
        
        return trigger;
    }
    
    private Wave parseWave(Map<?, ?> map) {
        Wave wave = new Wave();
        
        wave.setId(getString(map, "id", ""));
        wave.setTimeLimit(getInt(map, "time-limit", 120));
        wave.setExpReward(getInt(map, "exp-reward", 0));
        wave.setCompletionMessage(getString(map, "completion-message", null));
        wave.setStartMessage(getString(map, "start-message", null));
        
        plugin.getLogger().info("[DEBUG] parseWave: id=" + wave.getId() + ", timeLimit=" + wave.getTimeLimit());
        
        Map<?, ?> triggerMap = getMap(map, "next-wave-trigger");
        if (triggerMap != null) {
            wave.setNextWaveTrigger(parseWaveTrigger(triggerMap));
            plugin.getLogger().info("[DEBUG] Wave trigger set: " + wave.getNextWaveTrigger().getType());
        } else {
            plugin.getLogger().info("[DEBUG] Wave has no trigger, using default ON_KILL_COMPLETE");
        }
        
        List<Map<?, ?>> spawnsList = getList(map, "spawns");
        plugin.getLogger().info("[DEBUG] Wave spawns list: " + (spawnsList != null ? spawnsList.size() : "null"));
        
        if (spawnsList != null) {
            for (Map<?, ?> spawnMap : spawnsList) {
                MobSpawn spawn = parseMobSpawn(spawnMap);
                if (spawn != null) {
                    wave.getSpawns().add(spawn);
                    plugin.getLogger().info("[DEBUG] Added spawn: mob=" + spawn.getMobId() + ", amount=" + spawn.getAmount() + ", spawnPoint=" + spawn.getSpawnPointId());
                }
            }
        }
        
        Map<?, ?> completionMap = getMap(map, "completion");
        if (completionMap != null) {
            wave.setCompletion(parseCompletion(completionMap));
            plugin.getLogger().info("[DEBUG] Wave completion: " + wave.getCompletion().getType());
        }
        
        plugin.getLogger().info("[DEBUG] Wave " + wave.getId() + " total spawns: " + wave.getSpawns().size());
        
        return wave;
    }
    
    private MobSpawn parseMobSpawn(Map<?, ?> map) {
        MobSpawn spawn = new MobSpawn();
        
        spawn.setMobId(getString(map, "mob", ""));
        spawn.setSpawnPointId(getString(map, "spawn-point", ""));
        spawn.setAmount(getInt(map, "amount", 1));
        spawn.setDelaySeconds(getInt(map, "delay", 0));
        spawn.setRadius(getDouble(map, "radius", 3));
        spawn.setBoss(getBoolean(map, "is-boss", false));
        spawn.setHealthBar(getBoolean(map, "health-bar", true));
        spawn.setAnnounceSkills(getBoolean(map, "announce-skills", true));
        spawn.setLevel(getInt(map, "level", 1));

        // 解析 Boss 血量阈值触发器
        List<Map<?, ?>> hpTriggersList = getList(map, "boss-hp-triggers");
        if (hpTriggersList != null) {
            for (Map<?, ?> triggerMap : hpTriggersList) {
                BossHpTrigger trigger = new BossHpTrigger();
                trigger.setHpPercent(getDouble(triggerMap, "hp-percent", 50.0));
                trigger.setMessage(getString(triggerMap, "message", null));

                List<Map<?, ?>> triggerSpawns = getList(triggerMap, "spawns");
                if (triggerSpawns != null) {
                    for (Map<?, ?> sMap : triggerSpawns) {
                        MobSpawn subSpawn = parseMobSpawn(sMap);
                        if (subSpawn != null) {
                            trigger.getSpawns().add(subSpawn);
                        }
                    }
                }
                spawn.getBossHpTriggers().add(trigger);
            }
            plugin.getLogger().info("[DEBUG] Loaded " + spawn.getBossHpTriggers().size() + " boss HP triggers for " + spawn.getMobId());
        }

        return spawn;
    }
    
    private CompletionCondition parseCompletion(Map<?, ?> map) {
        CompletionCondition condition = new CompletionCondition();
        
        String typeStr = getString(map, "type", "KILL_ALL");
        try {
            condition.setType(CompletionType.valueOf(typeStr));
        } catch (IllegalArgumentException e) {
            condition.setType(CompletionType.KILL_ALL);
        }
        
        condition.setTargetCount(getInt(map, "count", 0));
        condition.setTargetMob(getString(map, "target-mob", null));
        
        return condition;
    }
    
    private WaveTrigger parseWaveTrigger(Map<?, ?> map) {
        String typeStr = getString(map, "type", "ON_KILL_COMPLETE");
        WaveTrigger trigger = new WaveTrigger();
        
        try {
            trigger.setType(WaveTriggerType.valueOf(typeStr));
        } catch (IllegalArgumentException e) {
            trigger.setType(WaveTriggerType.ON_KILL_COMPLETE);
        }
        
        trigger.setDelaySeconds(getInt(map, "delay-seconds", 0));
        trigger.setCommand(getString(map, "command", null));
        trigger.setInteractType(getString(map, "interact-type", null));
        trigger.setLocationRadius(getDouble(map, "radius", 5.0));
        
        Map<?, ?> locMap = getMap(map, "location");
        if (locMap != null) {
            double x = getDouble(locMap, "x", 0);
            double y = getDouble(locMap, "y", 64);
            double z = getDouble(locMap, "z", 0);
            String world = getString(locMap, "world", "world");
            org.bukkit.World bukkitWorld = Bukkit.getWorld(world);
            if (bukkitWorld != null) {
                trigger.setTargetLocation(new Location(bukkitWorld, x, y, z));
            }
        }
        
        return trigger;
    }
    
    private StageAction parseAction(Map<?, ?> map) {
        StageAction action = new StageAction();
        
        String typeStr = getString(map, "type", "MESSAGE");
        try {
            action.setType(StageAction.ActionType.valueOf(typeStr));
        } catch (IllegalArgumentException e) {
            action.setType(StageAction.ActionType.MESSAGE);
        }
        
        action.setContent(getString(map, "content", ""));
        action.setSubtitle(getString(map, "subtitle", ""));
        action.setDuration(getInt(map, "duration", 60));
        action.setSound(getString(map, "sound", ""));
        action.setVolume(getFloat(map, "volume", 1.0f));
        action.setSeconds(getInt(map, "seconds", 0));
        
        return action;
    }
    
    private String getString(Map<?, ?> map, String key, String def) {
        Object value = map.get(key);
        return value != null ? value.toString() : def;
    }
    
    private int getInt(Map<?, ?> map, String key, int def) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return def;
    }
    
    private double getDouble(Map<?, ?> map, String key, double def) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return def;
    }
    
    private float getFloat(Map<?, ?> map, String key, float def) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return def;
    }
    
    private boolean getBoolean(Map<?, ?> map, String key, boolean def) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return def;
    }
    
    private Map<?, ?> getMap(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<?, ?>) value;
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private List<Map<?, ?>> getList(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return (List<Map<?, ?>>) value;
        }
        return null;
    }
}
