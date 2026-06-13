package cn.guangdian.raid.model;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RaidPhaseConfig {

    private int duration;
    private List<RaidObjective> objectives;

    public RaidPhaseConfig() {
        this.objectives = new ArrayList<>();
    }

    public static RaidPhaseConfig fromConfig(ConfigurationSection section) {
        RaidPhaseConfig config = new RaidPhaseConfig();
        if (section == null) return config;

        config.duration = section.getInt("duration", 180);

        List<Map<?, ?>> objectivesList = section.getMapList("objectives");
        for (int i = 0; i < objectivesList.size(); i++) {
            RaidObjective objective = RaidObjective.fromMap(i, objectivesList.get(i));
            if (objective != null) {
                config.objectives.add(objective);
            }
        }

        return config;
    }

    public int getDuration() { return duration; }
    public List<RaidObjective> getObjective() { return objectives; }
}
