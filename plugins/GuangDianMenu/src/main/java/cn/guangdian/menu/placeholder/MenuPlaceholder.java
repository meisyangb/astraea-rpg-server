package cn.guangdian.menu.placeholder;

import cn.guangdian.menu.GuangDianMenu;
import cn.guangdian.rpgcore.integration.PlaceholderService;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class MenuPlaceholder {

    private final GuangDianMenu plugin;

    public MenuPlaceholder(GuangDianMenu plugin) {
        this.plugin = plugin;
    }

    public void register() {
        PlaceholderService service = PlaceholderService.getInstance();
        if (service == null) return;
        
        service.register("gdmenu", (player, params) -> {
            String param = params.toLowerCase();

            switch (param) {
                case "count":
                case "菜单总数":
                case "total":
                    return String.valueOf(plugin.getMenuCountAPI());

                case "menus":
                case "菜单列表":
                    return String.join(", ", plugin.getMenuNamesAPI());

                case "default":
                case "默认菜单":
                    return plugin.getConfig().getString("default-menu", "main");
            }

            if (player == null) return "";

            UUID playerId = player.getUniqueId();

            switch (param) {
                case "opened":
                case "当前菜单":
                    String menuName = plugin.getPlayerMenu(playerId);
                    return menuName != null ? menuName : "";

                case "has_open":
                case "是否打开":
                    return plugin.getPlayerMenu(playerId) != null ? "true" : "false";

                default:
                    if (param.startsWith("exists_")) {
                        String targetMenu = param.substring(7);
                        return plugin.hasMenuAPI(targetMenu) ? "true" : "false";
                    }

                    return null;
            }
        });
    }

    public void unregister() {
    }
}
