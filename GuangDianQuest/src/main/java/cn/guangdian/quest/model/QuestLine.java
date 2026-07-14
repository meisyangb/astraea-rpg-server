package cn.guangdian.quest.model;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务线模型
 * 
 * 定义一系列相关联的任务，形成剧情线
 */
public class QuestLine {
    
    private final String id;                  // 任务线ID
    private String name;                      // 任务线名称
    private String description;               // 任务线描述
    private String sect;                      // 门派限制, null = 所有门派
    private List<String> questIds;            // 任务ID列表（按顺序）
    private List<String> chapters;            // 章节名称
    private int currentChapter;               // 当前章节
    
    public QuestLine(String id) {
        this.id = id;
        this.questIds = new ArrayList<>();
        this.chapters = new ArrayList<>();
        this.currentChapter = 0;
    }
    
    /**
     * 从配置加载任务线
     */
    public static QuestLine fromConfig(String id, ConfigurationSection section) {
        QuestLine line = new QuestLine(id);
        
        line.name = section.getString("name", id);
        line.description = section.getString("description", "");
        line.sect = section.getString("sect"); // null = 所有门派
        line.questIds = section.getStringList("quests");
        line.chapters = section.getStringList("chapters");
        
        return line;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getSect() { return sect; }
    public List<String> getQuestIds() { return questIds; }
    public List<String> getChapters() { return chapters; }
    public int getCurrentChapter() { return currentChapter; }
    
    /**
     * 获取任务线长度
     */
    public int getLength() {
        return questIds.size();
    }
    
    /**
     * 根据索引获取任务ID
     */
    public String getQuestId(int index) {
        if (index < 0 || index >= questIds.size()) return null;
        return questIds.get(index);
    }
    
    /**
     * 获取章节名称
     */
    public String getChapterName(int questIndex) {
        if (chapters.isEmpty()) return "第 " + (questIndex + 1) + " 章";
        
        // 根据任务索引计算章节
        int chapterIndex = 0;
        int tasksPerChapter = questIds.size() / chapters.size();
        if (tasksPerChapter > 0) {
            chapterIndex = questIndex / tasksPerChapter;
        }
        
        if (chapterIndex < chapters.size()) {
            return chapters.get(chapterIndex);
        }
        return chapters.get(chapters.size() - 1);
    }
    
    /**
     * 获取进度百分比
     */
    public int getProgressPercent(int currentQuestIndex) {
        if (questIds.isEmpty()) return 0;
        return (currentQuestIndex + 1) * 100 / questIds.size();
    }
}