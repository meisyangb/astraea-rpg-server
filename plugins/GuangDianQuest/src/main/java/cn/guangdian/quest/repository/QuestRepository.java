package cn.guangdian.quest.repository;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.Quest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class QuestRepository {

    private final GuangDianQuest plugin;
    private final Map<String, Quest> quests;

    public QuestRepository(GuangDianQuest plugin, File questsDir) {
        this.plugin = plugin;
        this.quests = new HashMap<>();
    }

    public void loadAll() {
        quests.clear();
        File questsDir = new File(plugin.getDataFolder(), "quests");
        if (!questsDir.exists()) return;

        loadFromDirectory(questsDir, "");
        plugin.getLogger().info("已加载 " + quests.size() + " 个任务");
    }

    private void loadFromDirectory(File dir, String prefix) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                String dirPrefix = prefix.isEmpty() ? file.getName() : prefix + "/" + file.getName();
                loadFromDirectory(file, dirPrefix);
            } else if (file.getName().endsWith(".yml")) {
                try {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    String id = file.getName().replace(".yml", "");
                    Quest quest = Quest.fromConfig(id, config);
                    quests.put(id, quest);
                } catch (Exception e) {
                    plugin.getLogger().warning("加载任务失败: " + file.getName() + " - " + e.getMessage());
                }
            }
        }
    }

    public Quest getQuest(String id) {
        return quests.get(id);
    }

    public boolean exists(String id) {
        return quests.containsKey(id);
    }

    public Collection<Quest> getAllQuests() {
        return quests.values();
    }

    public int getQuestCount() {
        return quests.size();
    }
}
