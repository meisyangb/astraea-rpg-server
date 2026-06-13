package cn.guangdian.dungeon.adapter;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.DungeonParty;
import cn.guangdian.dungeon.model.DungeonTemplate;
import cn.guangdian.dungeon.model.session.DungeonSession;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DungeonServiceAdapter {

    private final GuangDianDungeon plugin;
    private boolean registered = false;

    public DungeonServiceAdapter(GuangDianDungeon plugin) {
        this.plugin = plugin;
        register();
    }

    private void register() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) {
            plugin.getLogger().warning("RPGCore 未启用，服务注册跳过");
            return;
        }

        try {
            ServiceRegistry registry = rpgCore.getServiceRegistry();
            if (registry != null) {
                registry.registerService(DungeonServiceAdapter.class, this);
                registered = true;
                plugin.getLogger().info("DungeonServiceAdapter 已注册到 RPGCore");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("注册服务失败: " + e.getMessage());
        }
    }

    public void unregister() {
        if (!registered) return;

        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) return;

        try {
            ServiceRegistry registry = rpgCore.getServiceRegistry();
            if (registry != null) {
                registry.unregisterService(DungeonServiceAdapter.class);
                registered = false;
                plugin.getLogger().info("DungeonServiceAdapter 已从 RPGCore 注销");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("注销服务失败: " + e.getMessage());
        }
    }

    public boolean isInDungeon(UUID playerId) {
        return plugin.getSessionManager().isInDungeon(playerId);
    }

    public Optional<String> getDungeonId(UUID playerId) {
        DungeonSession session = plugin.getSessionManager().getPlayerSession(playerId);
        return Optional.ofNullable(session).map(DungeonSession::getDungeonId);
    }

    public List<String> getAvailableDungeons() {
        return new ArrayList<>(plugin.getTemplateLoader().getTemplateIds());
    }

    public Optional<Map<String, Object>> getDungeonInfo(String dungeonId) {
        DungeonTemplate template = plugin.getTemplateLoader().getTemplate(dungeonId);
        if (template == null) return Optional.empty();

        Map<String, Object> info = new HashMap<>();
        info.put("id", template.getId());
        info.put("name", template.getName());
        info.put("description", template.getDescription());
        info.put("maxPlayers", template.getSettings().getMaxPlayers());
        info.put("minPlayers", template.getSettings().getMinPlayers());
        info.put("timeLimit", template.getSettings().getTimeLimit());

        return Optional.of(info);
    }

    public boolean hasCleared(UUID playerId, String dungeonId, String difficultyId) {
        var playerData = plugin.getPlayerRepository().getPlayerData(playerId);
        if (playerData == null) return false;

        String key = dungeonId + ":" + difficultyId;
        return playerData.hasCleared(key);
    }

    public int getClearCount(UUID playerId, String dungeonId, String difficultyId) {
        var playerData = plugin.getPlayerRepository().getPlayerData(playerId);
        if (playerData == null) return 0;

        String key = dungeonId + ":" + difficultyId;
        return playerData.getClearCount(key);
    }

    public long getBestTime(UUID playerId, String dungeonId, String difficultyId) {
        var playerData = plugin.getPlayerRepository().getPlayerData(playerId);
        if (playerData == null) return 0;

        String key = dungeonId + ":" + difficultyId;
        return playerData.getBestTime(key);
    }

    public int getBestScore(UUID playerId, String dungeonId, String difficultyId) {
        var playerData = plugin.getPlayerRepository().getPlayerData(playerId);
        if (playerData == null) return 0;

        String key = dungeonId + ":" + difficultyId;
        return playerData.getBestScore(key);
    }

    public boolean isOnCooldown(UUID playerId, String dungeonId) {
        var playerData = plugin.getPlayerRepository().getPlayerData(playerId);
        if (playerData == null) return false;

        return playerData.isOnCooldown(dungeonId);
    }

    public long getRemainingCooldown(UUID playerId, String dungeonId) {
        var playerData = plugin.getPlayerRepository().getPlayerData(playerId);
        if (playerData == null) return 0;

        return playerData.getRemainingCooldown(dungeonId);
    }

    public Optional<DungeonParty> getPlayerParty(UUID playerId) {
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null) return Optional.empty();
        return plugin.getPartyManager().getPlayerParty(player);
    }

    public int getActiveInstanceCount() {
        return plugin.getSessionManager().getActiveSessionCount();
    }

    public int getActivePartyCount() {
        return plugin.getPartyManager().getAllParties().size();
    }

    public CompletableFuture<Boolean> isInDungeonAsync(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> isInDungeon(playerId));
    }

    public boolean isAvailable() {
        return true;
    }

    public boolean isRegistered() {
        return registered;
    }
}
