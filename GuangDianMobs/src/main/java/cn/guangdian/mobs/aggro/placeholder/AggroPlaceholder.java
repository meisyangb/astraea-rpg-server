package cn.guangdian.mobs.aggro.placeholder;

import cn.guangdian.mobs.GuangDianMobs;
import cn.guangdian.mobs.aggro.manager.AggroManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AggroPlaceholder extends PlaceholderExpansion {

    private final GuangDianMobs plugin;
    private final AggroManager aggroManager;

    public AggroPlaceholder(GuangDianMobs plugin, AggroManager aggroManager) {
        this.plugin = plugin;
        this.aggroManager = aggroManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "gdaggro";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Astraea RPG Team";
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
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        String[] args = params.split("_", 2);
        String action = args[0];

        switch (action.toLowerCase()) {
            case "top":
                return getTopAggroTarget(args);
            case "value":
                return getAggroValue(player, args);
            case "rank":
                return getAggroRank(player, args);
            case "total":
                return getTotalAggro(args);
            case "has":
                return hasAggro(player, args);
            default:
                return "";
        }
    }

    private String getTopAggroTarget(String[] args) {
        return "";
    }

    private String getAggroValue(Player player, String[] args) {
        return String.valueOf(0);
    }

    private String getAggroRank(Player player, String[] args) {
        return "-1";
    }

    private String getTotalAggro(String[] args) {
        return "0";
    }

    private String hasAggro(Player player, String[] args) {
        return "false";
    }
}
