package cn.guangdian.name;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.integration.PlaceholderService;
import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.entity.Player;

public class NamePlaceholder {

    private final GuangDianName plugin;
    private final NameDisplayManager displayManager;

    public NamePlaceholder(GuangDianName plugin, NameDisplayManager displayManager) {
        this.plugin = plugin;
        this.displayManager = displayManager;
    }

    public void register() {
        PlaceholderService service = PlaceholderService.getInstance();
        if (service == null) return;
        
        service.register("gdname", (player, params) -> {
            if (player == null) return "";

            String result;
            switch (params.toLowerCase()) {
                case "show_title":
                    result = displayManager.getShowTitleStatus(player);
                    break;
                case "show_guild":
                    result = displayManager.getShowGuildStatus(player);
                    break;
                case "show_marriage":
                    result = displayManager.getShowMarriageStatus(player);
                    break;
                case "show_health":
                    result = displayManager.getShowHealthStatus(player);
                    break;
                default:
                    return null;
            }

            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                MiniMessageService mm = rpgCore.getMiniMessageService();
                return mm.serialize(mm.colorize(result));
            }
            return result;
        });
    }

    public void unregister() {
    }
}
