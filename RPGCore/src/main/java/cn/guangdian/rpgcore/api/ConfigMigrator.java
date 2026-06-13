package cn.guangdian.rpgcore.api;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.function.Consumer;

public interface ConfigMigrator {

    int getCurrentVersion();

    boolean needsMigration();

    boolean migrate();

    boolean migrate(int targetVersion);

    void registerMigration(int fromVersion, int toVersion, MigrationTask task);

    void registerVersion(int version);

    String getMigrationStatus();

    interface MigrationTask {
        void migrate(FileConfiguration config, Consumer<String> log);
    }

    record MigrationInfo(
        int fromVersion,
        int toVersion,
        boolean applied
    ) {}
}
