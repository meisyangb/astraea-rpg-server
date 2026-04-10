package cn.guangdian.menu.placeholder;

import cn.guangdian.menu.GuangDianMenu;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 菜单占位符扩展
 *
 * <p>提供菜单相关的 PlaceholderAPI 占位符。</p>
 *
 * <h3>可用占位符：</h3>
 * <ul>
 *   <li>%gdmenu_opened% - 玩家当前打开的菜单名称 (无则返回空)</li>
 *   <li>%gdmenu_count% - 服务器菜单总数</li>
 *   <li>%gdmenu_has_open% - 玩家是否打开了菜单 (true/false)</li>
 * </ul>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class MenuPlaceholder extends PlaceholderExpansion {

    private final GuangDianMenu plugin;

    public MenuPlaceholder(GuangDianMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "gdmenu";
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
        String param = params.toLowerCase();

        // 不需要玩家对象的占位符
        switch (param) {
            case "count":
            case "菜单总数":
            case "total":
                // 服务器菜单总数
                return String.valueOf(plugin.getMenuCountAPI());

            case "menus":
            case "菜单列表":
                // 菜单名称列表 (逗号分隔)
                return String.join(", ", plugin.getMenuNamesAPI());

            case "default":
            case "默认菜单":
                // 默认菜单名称
                return plugin.getConfig().getString("default-menu", "main");
        }

        // 需要玩家对象的占位符
        if (player == null) return "";

        UUID playerId = player.getUniqueId();

        switch (param) {
            case "opened":
            case "当前菜单":
                // 玩家当前打开的菜单名称
                String menuName = plugin.getPlayerMenu(playerId);
                return menuName != null ? menuName : "";

            case "has_open":
            case "是否打开":
                // 玩家是否打开了菜单
                return plugin.getPlayerMenu(playerId) != null ? "true" : "false";

            default:
                // 检查特定菜单是否存在: %gdmenu_exists_main%
                if (param.startsWith("exists_")) {
                    String targetMenu = param.substring(7);
                    return plugin.hasMenuAPI(targetMenu) ? "true" : "false";
                }

                return null;
        }
    }
}