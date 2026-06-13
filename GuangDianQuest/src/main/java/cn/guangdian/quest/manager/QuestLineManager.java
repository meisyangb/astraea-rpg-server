package cn.guangdian.quest.manager;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.QuestLine;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class QuestLineManager {

    private final GuangDianQuest plugin;
    private final Map<String, QuestLine> questLines;

    public QuestLineManager(GuangDianQuest plugin) {
        this.plugin = plugin;
        this.questLines = new HashMap<>();
    }

    public void loadAll(File file) {
        questLines.clear();
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("questlines");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection lineSection = section.getConfigurationSection(id);
            if (lineSection != null) {
                QuestLine line = QuestLine.fromConfig(id, lineSection);
                questLines.put(id, line);
            }
        }

        plugin.getLogger().info("已加载 " + questLines.size() + " 条任务线");
    }

    public QuestLine getQuestLine(String id) {
        return questLines.get(id);
    }

    public Set<String> getQuestLineIds() {
        return questLines.keySet();
    }

    public Collection<QuestLine> getAllQuestLines() {
        return questLines.values();
    }

    public String getNextQuestId(UUID playerId, String questLineId) {
        QuestLine line = questLines.get(questLineId);
        if (line == null) return null;

        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);
        int progress = data.getQuestLineProgress(questLineId);

        return line.getQuestId(progress + 1);
    }

    public int getQuestLineProgress(UUID playerId, String questLineId) {
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);
        return data.getQuestLineProgress(questLineId);
    }

    public QuestLine getQuestLineByQuestId(String questId) {
        for (QuestLine line : questLines.values()) {
            if (line.getQuestIds().contains(questId)) {
                return line;
            }
        }
        return null;
    }
}
