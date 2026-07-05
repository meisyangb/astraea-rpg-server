package cn.guangdian.quest.repository;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 玩家任务数据仓库 - SQLite 存储
 * <p>参考 GuangDianPoints 的架构：主类 → Repository → PlayerQuestStorage → SQLite</p>
 */
public class PlayerQuestRepository {

    private final PlayerQuestStorage storage;

    public PlayerQuestRepository(JavaPlugin plugin) {
        this.storage = new PlayerQuestStorage(plugin);
    }

    public boolean initialize() {
        return storage.initialize();
    }

    public boolean isEnabled() {
        return storage.isEnabled();
    }

    public PlayerQuestData getPlayerData(UUID playerId) {
        return storage.getCached(playerId);
    }

    /** 同步保存（操作后立即调用） */
    public void savePlayerData(UUID playerId) {
        PlayerQuestData data = storage.getCached(playerId);
        if (data != null) {
            storage.savePlayerSync(playerId, data);
        }
    }

    /** 异步保存（定时 + 退出） */
    public CompletableFuture<Void> savePlayerDataAsync(UUID playerId) {
        PlayerQuestData data = storage.getCached(playerId);
        if (data != null) {
            return storage.savePlayerAsync(playerId, data);
        }
        return CompletableFuture.completedFuture(null);
    }

    /** 同步全量保存（关闭时） */
    public void saveAll() {
        storage.saveAll();
    }

    /** 异步全量保存（定时器） */
    public CompletableFuture<Void> saveAllAsync() {
        return storage.saveAllAsync();
    }

    public void removePlayerData(UUID playerId) {
        PlayerQuestData data = storage.getCached(playerId);
        if (data != null) {
            storage.savePlayerSync(playerId, data); // 先保存
        }
        storage.removeCached(playerId);
    }

    public void close() {
        storage.close();
    }

    public boolean isLoaded(UUID playerId) {
        return storage.isCached(playerId);
    }
}
