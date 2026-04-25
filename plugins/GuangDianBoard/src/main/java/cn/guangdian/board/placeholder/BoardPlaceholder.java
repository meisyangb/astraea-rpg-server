package cn.guangdian.board.placeholder;

import cn.guangdian.board.GuangDianBoard;
import cn.guangdian.rpgcore.integration.PlaceholderService;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class BoardPlaceholder {

    private final GuangDianBoard plugin;

    public BoardPlaceholder(GuangDianBoard plugin) {
        this.plugin = plugin;
    }

    public void register() {
        PlaceholderService service = PlaceholderService.getInstance();
        if (service == null) return;
        
        service.register("gdboard", (player, params) -> {
            if (player == null) return "";

            String param = params.toLowerCase();

            switch (param) {
                case "enabled":
                    return String.valueOf(plugin.getConfig().getBoolean("enabled", true));
                case "visible":
                    if (player.isOnline()) {
                        Player onlinePlayer = player.getPlayer();
                        if (onlinePlayer != null) {
                            return String.valueOf(plugin.shouldShowBoardPublic(onlinePlayer));
                        }
                    }
                    return "false";
                case "lines":
                    return String.valueOf(plugin.getConfig().getInt("advanced.max-lines", 15));
                case "title":
                    return plugin.getDefaultTitlePublic();
                default:
                    return null;
            }
        });
    }

    public void unregister() {}
}
