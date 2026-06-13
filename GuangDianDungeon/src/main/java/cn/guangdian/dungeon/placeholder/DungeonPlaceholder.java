package cn.guangdian.dungeon.placeholder;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.*;
import cn.guangdian.dungeon.model.session.DungeonSession;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DungeonPlaceholder extends PlaceholderExpansion {

    private final GuangDianDungeon plugin;

    public DungeonPlaceholder(GuangDianDungeon plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "dungeon";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Gumin";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) {
            return "";
        }

        Player player = offlinePlayer.getPlayer();
        if (player == null) return "";

        String[] args = params.split("_", 2);
        String subKey = args[0];
        String subValue = args.length > 1 ? args[1] : "";

        switch (subKey.toLowerCase()) {
            case "in":
                return isInDungeon(player);
            case "name":
                return getDungeonName(player);
            case "phase":
                return getCurrentPhase(player);
            case "time":
                return getElapsedTime(player);
            case "remaining":
                return getRemainingTime(player);
            case "deaths":
                return getDeaths(player);
            case "score":
                return getScore(player);
            case "party":
                return handlePartyPlaceholder(player, subValue);
            case "clears":
                return handleClearsPlaceholder(player, subValue);
            case "cooldown":
                return handleCooldownPlaceholder(player, subValue);
            case "top":
                return handleTopPlaceholder(subValue);
            default:
                return "";
        }
    }

    private String isInDungeon(Player player) {
        return plugin.getSessionManager().isInDungeon(player.getUniqueId()) ? "true" : "false";
    }

    private String getDungeonName(Player player) {
        DungeonSession session = plugin.getSessionManager().getPlayerSession(player.getUniqueId());
        if (session == null) return "";
        DungeonTemplate template = plugin.getTemplateLoader().getTemplate(session.getDungeonId());
        return template != null ? template.getName() : session.getDungeonId();
    }

    private String getCurrentPhase(Player player) {
        DungeonSession session = plugin.getSessionManager().getPlayerSession(player.getUniqueId());
        if (session == null) return "";
        var stage = session.getCurrentStage();
        return stage != null ? stage.getName() : "";
    }

    private String getElapsedTime(Player player) {
        DungeonSession session = plugin.getSessionManager().getPlayerSession(player.getUniqueId());
        if (session == null) return "0";
        return String.valueOf(session.getElapsedTime() / 1000);
    }

    private String getRemainingTime(Player player) {
        DungeonSession session = plugin.getSessionManager().getPlayerSession(player.getUniqueId());
        if (session == null || session.getTimeLimit() <= 0) return "0";
        long elapsed = session.getElapsedTime() / 1000;
        long remaining = session.getTimeLimit() - elapsed;
        return String.valueOf(Math.max(0, remaining));
    }

    private String getDeaths(Player player) {
        DungeonSession session = plugin.getSessionManager().getPlayerSession(player.getUniqueId());
        if (session == null) return "0";
        return String.valueOf(session.getTotalDeaths());
    }

    private String getScore(Player player) {
        DungeonSession session = plugin.getSessionManager().getPlayerSession(player.getUniqueId());
        if (session == null) return "0";
        int score = session.getTotalKills() * 10;
        return String.valueOf(score);
    }

    private String handlePartyPlaceholder(Player player, String subValue) {
        if (subValue.isEmpty()) {
            return plugin.getPartyManager().isInParty(player) ? "true" : "false";
        }

        String[] parts = subValue.split("_", 2);
        String action = parts[0];
        String arg = parts.length > 1 ? parts[1] : "";

        switch (action.toLowerCase()) {
            case "size":
                return plugin.getPartyManager().getPlayerParty(player)
                    .map(p -> String.valueOf(p.getMemberCount()))
                    .orElse("0");
            case "max":
                return plugin.getPartyManager().getPlayerParty(player)
                    .map(p -> String.valueOf(p.getMaxMembers()))
                    .orElse("0");
            case "leader":
                return plugin.getPartyManager().getPlayerParty(player)
                    .map(p -> p.getLeader().getName())
                    .orElse("");
            case "ready":
                return plugin.getPartyManager().getPlayerParty(player)
                    .map(p -> p.isReady() ? "true" : "false")
                    .orElse("false");
            default:
                return "";
        }
    }

    private String handleClearsPlaceholder(Player player, String subValue) {
        if (subValue.isEmpty()) {
            var data = plugin.getPlayerRepository().getPlayerData(player.getUniqueId());
            if (data == null) return "0";
            return String.valueOf(data.getClearRecords().size());
        }

        String[] parts = subValue.split("_", 2);
        if (parts.length < 2) return "0";

        String dungeonId = parts[0];
        String action = parts[1];

        var data = plugin.getPlayerRepository().getPlayerData(player.getUniqueId());
        if (data == null) return "0";

        String key = dungeonId;
        if (action.equals("count")) {
            return String.valueOf(data.getClearCount(key));
        } else if (action.equals("besttime")) {
            long time = data.getBestTime(key);
            return time > 0 ? String.valueOf(time / 1000) : "0";
        } else if (action.equals("bestscore")) {
            return String.valueOf(data.getBestScore(key));
        }

        return "0";
    }

    private String handleCooldownPlaceholder(Player player, String subValue) {
        if (subValue.isEmpty()) return "0";

        var data = plugin.getPlayerRepository().getPlayerData(player.getUniqueId());
        if (data == null) return "0";

        long remaining = data.getRemainingCooldown(subValue);
        return String.valueOf(remaining / 1000);
    }

    private String handleTopPlaceholder(String subValue) {
        return "";
    }
}
