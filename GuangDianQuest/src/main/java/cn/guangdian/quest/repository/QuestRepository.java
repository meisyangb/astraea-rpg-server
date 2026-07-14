package cn.guangdian.quest.repository;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.Quest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class QuestRepository {

    private final GuangDianQuest plugin;
    private final Map<String, Quest> quests = new HashMap<>();
    
    // 中文宗门名 -> 英文 key 映射
    private static final Map<String, String> SECT_FOLDER_MAP = new HashMap<>();
    static {
        SECT_FOLDER_MAP.put("common", null);       // 通用任务
        SECT_FOLDER_MAP.put("鬼王宗", "guiwang");
        SECT_FOLDER_MAP.put("青云宗", "qingyun");
        SECT_FOLDER_MAP.put("合欢宗", "hehuan");
        SECT_FOLDER_MAP.put("天音寺", "tianyin");
        SECT_FOLDER_MAP.put("焚香谷", "fenxiang");
        SECT_FOLDER_MAP.put("长生堂", "changsheng");
    }

    public QuestRepository(GuangDianQuest plugin, File questsDir) {
        this.plugin = plugin;
    }

    public void loadAll() {
        quests.clear();
        File questsDir = new File(plugin.getDataFolder(), "quests");
        if (!questsDir.exists()) return;

        // 遍历宗门文件夹
        File[] sectFolders = questsDir.listFiles(File::isDirectory);
        if (sectFolders == null) return;

        for (File sectFolder : sectFolders) {
            String folderName = sectFolder.getName();
            String sectKey = SECT_FOLDER_MAP.getOrDefault(folderName, folderName.toLowerCase());
            
            if ("common".equals(folderName) || "common".equals(sectKey)) {
                // 通用任务文件夹，不设 sect
                loadFromDirectory(sectFolder, null);
            } else {
                // 宗门专属任务，从文件夹名继承 sect
                loadFromDirectory(sectFolder, sectKey);
            }
        }

        plugin.getLogger().info("已加载 " + quests.size() + " 个任务");
    }

    /**
     * 递归加载目录下的任务文件
     * @param dir 目录
     * @param sectOverride 宗门覆盖（null=通用，非null=强制设置该宗门）
     */
    private void loadFromDirectory(File dir, String sectOverride) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // 子目录递归（如 main/side/daily）
                loadFromDirectory(file, sectOverride);
            } else if (file.getName().endsWith(".yml")) {
                try {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    
                    // 判断格式：根节点有 "type" 字段 = 单任务文件（旧格式），否则 = 多任务文件
                    if (config.contains("type") && !config.isConfigurationSection("type")) {
                        // 旧格式：文件名就是任务ID，根节点内容就是任务数据
                        String id = file.getName().replace(".yml", "");
                        Quest quest = Quest.fromConfig(id, config);
                        if (quest.getSect() == null && sectOverride != null) {
                            quest.setSect(sectOverride);
                        }
                        quests.put(id, quest);
                    } else {
                        // 新格式：遍历 YAML 中的每个顶层 key（每个 key 是一个任务）
                        for (String questId : config.getKeys(false)) {
                            if (config.isConfigurationSection(questId)) {
                                ConfigurationSection questSection = config.getConfigurationSection(questId);
                                if (questSection != null) {
                                    Quest quest = Quest.fromConfig(questId, questSection);
                                    if (quest.getSect() == null && sectOverride != null) {
                                        quest.setSect(sectOverride);
                                    }
                                    quests.put(questId, quest);
                                }
                            }
                        }
                    }
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
