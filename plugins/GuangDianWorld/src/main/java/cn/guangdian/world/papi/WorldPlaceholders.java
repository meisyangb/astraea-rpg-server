package cn.guangdian.world.papi;

import cn.guangdian.world.GuangDianWorld;
import cn.guangdian.world.model.GDWorld;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WorldPlaceholders extends PlaceholderExpansion {

    private final GuangDianWorld plugin;

    public WorldPlaceholders(GuangDianWorld plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "gdworld";
    }

    @Override
    public @NotNull String getAuthor() {
        return "GuangDian";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) {
            return "";
        }

        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null) {
            return "";
        }

        String worldName = onlinePlayer.getWorld().getName();
        GDWorld world = plugin.getWorldManager().getWorld(worldName);

        return switch (params.toLowerCase()) {
            case "current" -> worldName;
            case "alias" -> world != null ? world.getDisplayName() : worldName;
            case "environment" -> world != null ? world.getEnvironment().name() : "NORMAL";
            case "difficulty" -> world != null ? world.getDifficulty() : "NORMAL";
            case "gamemode" -> world != null ? world.getGamemode() : "SURVIVAL";
            case "pvp" -> world != null ? String.valueOf(world.isPvp()) : "true";
            case "flight" -> world != null ? String.valueOf(world.isAllowFlight()) : "false";
            case "count" -> String.valueOf(plugin.getWorldManager().getWorldCount());
            case "loaded" -> world != null ? String.valueOf(world.isLoaded()) : "false";
            default -> handleWorldSpecificPlaceholder(params, onlinePlayer);
        };
    }

    private String handleWorldSpecificPlaceholder(String params, Player player) {
        if (params.startsWith("name_")) {
            String targetWorld = params.substring(5);
            GDWorld world = plugin.getWorldManager().getWorld(targetWorld);
            return world != null ? world.getDisplayName() : targetWorld;
        }

        if (params.startsWith("exists_")) {
            String targetWorld = params.substring(7);
            return String.valueOf(plugin.getWorldManager().getWorld(targetWorld) != null);
        }

        if (params.startsWith("loaded_")) {
            String targetWorld = params.substring(7);
            GDWorld world = plugin.getWorldManager().getWorld(targetWorld);
            return String.valueOf(world != null && world.isLoaded());
        }

        if (params.startsWith("players_")) {
            String targetWorld = params.substring(8);
            var world = org.bukkit.Bukkit.getWorld(targetWorld);
            return world != null ? String.valueOf(world.getPlayers().size()) : "0";
        }

        return null;
    }
}
