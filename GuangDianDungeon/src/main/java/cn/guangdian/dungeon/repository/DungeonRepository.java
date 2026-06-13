package cn.guangdian.dungeon.repository;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.DungeonTemplate;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class DungeonRepository {

    private final GuangDianDungeon plugin;
    private final File dungeonsDir;
    private final Map<String, DungeonTemplate> templates;

    public DungeonRepository(GuangDianDungeon plugin, File dungeonsDir) {
        this.plugin = plugin;
        this.dungeonsDir = dungeonsDir;
        this.templates = new HashMap<>();
    }

    public DungeonTemplate getTemplate(String id) {
        return templates.get(id);
    }

    public boolean hasTemplate(String id) {
        return templates.containsKey(id);
    }

    public void addTemplate(DungeonTemplate template) {
        templates.put(template.getId(), template);
    }

    public void removeTemplate(String id) {
        templates.remove(id);
    }

    public void clear() {
        templates.clear();
    }
}
