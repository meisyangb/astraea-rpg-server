package cn.guangdian.mobs.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 迁移报告
 * 记录迁移过程中的统计信息和错误
 */
public class MigrationReport {

    private int mobsMigrated = 0;
    private int skillsMigrated = 0;
    private int spawnersMigrated = 0;
    private int dropsMigrated = 0;

    private final List<MigrationError> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void incrementMobsMigrated() {
        mobsMigrated++;
    }

    public void incrementSkillsMigrated() {
        skillsMigrated++;
    }

    public void incrementSpawnersMigrated() {
        spawnersMigrated++;
    }

    public void incrementDropsMigrated() {
        dropsMigrated++;
    }

    public void addError(String category, String name, String message) {
        errors.add(new MigrationError(category, name, message));
    }

    public void addWarning(String category, String message) {
        warnings.add("[" + category + "] " + message);
    }

    public void clear() {
        mobsMigrated = 0;
        skillsMigrated = 0;
        spawnersMigrated = 0;
        dropsMigrated = 0;
        errors.clear();
        warnings.clear();
    }

    public void printSummary(Logger logger) {
        logger.info("========== 迁移报告 ==========");
        logger.info("怪物迁移: " + mobsMigrated + " 个");
        logger.info("技能迁移: " + skillsMigrated + " 个");
        logger.info("刷新点迁移: " + spawnersMigrated + " 个");
        logger.info("掉落表迁移: " + dropsMigrated + " 个");

        if (!warnings.isEmpty()) {
            logger.warning("警告 (" + warnings.size() + " 个):");
            for (String warning : warnings) {
                logger.warning("  - " + warning);
            }
        }

        if (!errors.isEmpty()) {
            logger.severe("错误 (" + errors.size() + " 个):");
            for (MigrationError error : errors) {
                logger.severe("  - [" + error.category + "] " + error.name + ": " + error.message);
            }
        }

        logger.info("==============================");
    }

    public int getMobsMigrated() {
        return mobsMigrated;
    }

    public int getSkillsMigrated() {
        return skillsMigrated;
    }

    public int getSpawnersMigrated() {
        return spawnersMigrated;
    }

    public int getDropsMigrated() {
        return dropsMigrated;
    }

    public List<MigrationError> getErrors() {
        return new ArrayList<>(errors);
    }

    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * 迁移错误记录
     */
    public static class MigrationError {
        public final String category;
        public final String name;
        public final String message;

        public MigrationError(String category, String name, String message) {
            this.category = category;
            this.name = name;
            this.message = message;
        }
    }
}
