package cn.guangdian.holo.papi;

import cn.guangdian.holo.GuangDianHolo;
import cn.guangdian.holo.model.Hologram;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class HoloPlaceholders extends PlaceholderExpansion {

    private final GuangDianHolo plugin;

    public HoloPlaceholders(GuangDianHolo plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "gdholo";
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
        return switch (params.toLowerCase()) {
            case "count" -> String.valueOf(plugin.getHologramManager().getHologramCount());
            case "list" -> String.join(", ", plugin.getHologramManager().getHologramNames());
            default -> handleHoloSpecificPlaceholder(params);
        };
    }

    private String handleHoloSpecificPlaceholder(String params) {
        if (params.startsWith("exists_")) {
            String name = params.substring(7);
            return String.valueOf(plugin.getHologramManager().getHologram(name) != null);
        }

        if (params.startsWith("lines_")) {
            String name = params.substring(6);
            Hologram holo = plugin.getHologramManager().getHologram(name);
            return holo != null ? String.valueOf(holo.getLineCount()) : "0";
        }

        if (params.startsWith("world_")) {
            String name = params.substring(6);
            Hologram holo = plugin.getHologramManager().getHologram(name);
            return holo != null ? holo.getWorldName() : "";
        }

        if (params.startsWith("visible_")) {
            String name = params.substring(8);
            Hologram holo = plugin.getHologramManager().getHologram(name);
            return holo != null ? String.valueOf(holo.isVisible()) : "false";
        }

        return null;
    }
}
