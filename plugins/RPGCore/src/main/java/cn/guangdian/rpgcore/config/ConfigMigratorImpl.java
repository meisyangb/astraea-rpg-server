package cn.guangdian.rpgcore.config;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ConfigMigrator;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ConfigMigratorImpl implements ConfigMigrator {

    private final RPGCore plugin;
    private final Map<Integer, MigrationStep> migrations = new ConcurrentHashMap<>();
    private int baseVersion = 0;

    public ConfigMigratorImpl(RPGCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public int getCurrentVersion() {
        FileConfiguration config = plugin.getConfig();
        return config.getInt("config-version", baseVersion);
    }

    @Override
    public boolean needsMigration() {
        int current = getCurrentVersion();
        return !migrations.isEmpty() && current < getLatestVersion();
    }

    @Override
    public boolean migrate() {
        return migrate(getLatestVersion());
    }

    @Override
    public boolean migrate(int targetVersion) {
        int current = getCurrentVersion();
        if (current >= targetVersion) {
            plugin.getLogger().info("[ConfigMigrator] Already at version " + current);
            return true;
        }

        plugin.getLogger().info("[ConfigMigrator] Starting migration from v" + current + " to v" + targetVersion);

        List<String> migrationLogs = new ArrayList<>();
        Consumer<String> logConsumer = migrationLogs::add;

        for (int from = current; from < targetVersion; from++) {
            MigrationStep step = migrations.get(from);
            if (step == null) {
                plugin.getLogger().warning("[ConfigMigrator] No migration path from v" + from);
                return false;
            }

            plugin.getLogger().info("[ConfigMigrator] Applying migration: v" + from + " -> v" + step.toVersion);

            FileConfiguration config = plugin.getConfig();
            try {
                step.task.migrate(config, logConsumer);
                plugin.getConfig().set("config-version", step.toVersion);
                plugin.saveConfig();

                for (String log : migrationLogs) {
                    plugin.getLogger().info("[ConfigMigrator] " + log);
                }
                migrationLogs.clear();

            } catch (Exception e) {
                plugin.getLogger().severe("[ConfigMigrator] Migration failed at v" + from + ": " + e.getMessage());
                return false;
            }
        }

        plugin.getLogger().info("[ConfigMigrator] Migration completed successfully");
        return true;
    }

    @Override
    public void registerMigration(int fromVersion, int toVersion, MigrationTask task) {
        migrations.put(fromVersion, new MigrationStep(fromVersion, toVersion, task));
        plugin.getLogger().info("[ConfigMigrator] Registered migration: v" + fromVersion + " -> v" + toVersion);
    }

    @Override
    public void registerVersion(int version) {
        if (version > baseVersion) {
            baseVersion = version;
        }
    }

    @Override
    public String getMigrationStatus() {
        int current = getCurrentVersion();
        int latest = getLatestVersion();
        if (current >= latest) {
            return "Up to date (v" + current + ")";
        }
        return "Needs migration: v" + current + " -> v" + latest;
    }

    public int getLatestVersion() {
        int latest = baseVersion;
        for (MigrationStep step : migrations.values()) {
            if (step.toVersion > latest) {
                latest = step.toVersion;
            }
        }
        return latest;
    }

    public List<MigrationInfo> getMigrationPath() {
        List<MigrationInfo> path = new ArrayList<>();
        int current = getCurrentVersion();
        for (int from = current; from < getLatestVersion(); from++) {
            MigrationStep step = migrations.get(from);
            if (step != null) {
                path.add(new MigrationInfo(from, step.toVersion, false));
            }
        }
        return path;
    }

    private record MigrationStep(int fromVersion, int toVersion, MigrationTask task) {}
}
