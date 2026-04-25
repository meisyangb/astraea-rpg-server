package cn.guangdian.aggro.placeholder;

import cn.guangdian.aggro.GuangDianAggro;
import cn.guangdian.aggro.manager.AggroManager;
import cn.guangdian.rpgcore.integration.PlaceholderService;
import org.bukkit.entity.Player;

public class AggroPlaceholder {

    private final GuangDianAggro plugin;
    private final AggroManager aggroManager;

    public AggroPlaceholder(GuangDianAggro plugin, AggroManager aggroManager) {
        this.plugin = plugin;
        this.aggroManager = aggroManager;
    }

    public void register() {
        PlaceholderService service = PlaceholderService.getInstance();
        if (service == null) return;
        
        service.register("gdaggro", (player, params) -> {
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
        });
    }

    public void unregister() {}

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
