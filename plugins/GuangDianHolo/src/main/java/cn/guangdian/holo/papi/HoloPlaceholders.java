package cn.guangdian.holo.papi;

import cn.guangdian.holo.GuangDianHolo;
import cn.guangdian.holo.model.Hologram;
import cn.guangdian.rpgcore.integration.PlaceholderService;
import org.bukkit.OfflinePlayer;

public class HoloPlaceholders {

    private final GuangDianHolo plugin;

    public HoloPlaceholders(GuangDianHolo plugin) {
        this.plugin = plugin;
    }

    public void register() {
        PlaceholderService service = PlaceholderService.getInstance();
        if (service == null) return;
        
        service.register("gdholo", (player, params) -> {
            return switch (params.toLowerCase()) {
                case "count" -> String.valueOf(plugin.getHologramManager().getHologramCount());
                case "list" -> String.join(", ", plugin.getHologramManager().getHologramNames());
                default -> handleHoloSpecificPlaceholder(params);
            };
        });
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

    public void unregister() {
    }
}
