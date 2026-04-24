package cn.guangdian.tab.placeholder;

import cn.guangdian.rpgcore.integration.PlaceholderService;
import cn.guangdian.tab.GuangDianTab;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class TabPlaceholder {

    private final GuangDianTab plugin;

    public TabPlaceholder(GuangDianTab plugin) {
        this.plugin = plugin;
    }

    public void register() {
        PlaceholderService service = PlaceholderService.getInstance();
        if (service == null) return;
        
        service.register("gdtab", (player, params) -> {
            if (player == null) return "";

            String param = params.toLowerCase();

            if (param.equals("enabled")) {
                return String.valueOf(plugin.getConfig().getBoolean("enabled", true));
            }

            if (!player.isOnline()) return "";

            Player onlinePlayer = player.getPlayer();
            if (onlinePlayer == null) return "";

            switch (param) {
                case "prefix":
                    return plugin.getPrefixForPlayer(onlinePlayer);
                case "suffix":
                    return plugin.getSuffixForPlayer(onlinePlayer);
                case "group":
                    return plugin.getGroupForPlayer(onlinePlayer);
                default:
                    return null;
            }
        });
    }

    public void unregister() {}
}
