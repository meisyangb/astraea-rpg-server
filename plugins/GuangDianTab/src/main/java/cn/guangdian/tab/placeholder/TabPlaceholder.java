package cn.guangdian.tab.placeholder;

import cn.guangdian.tab.GuangDianTab;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Tab PlaceholderAPI 扩展
 * 
 * <p>提供Tab列表相关的占位符。</p>
 * 
 * <h3>可用占位符：</h3>
 * <ul>
 *   <li>%gdtab_prefix% - 玩家Tab前缀</li>
 *   <li>%gdtab_suffix% - 玩家Tab后缀</li>
 *   <li>%gdtab_group% - 玩家Tab格式组</li>
 *   <li>%gdtab_enabled% - Tab是否启用</li>
 * </ul>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class TabPlaceholder extends PlaceholderExpansion {

    private final GuangDianTab plugin;

    public TabPlaceholder(GuangDianTab plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "gdtab";
    }

    @Override
    public @NotNull String getAuthor() {
        return "GuangDian";
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
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        String param = params.toLowerCase();

        // Tab是否启用
        if (param.equals("enabled")) {
            return String.valueOf(plugin.getConfig().getBoolean("enabled", true));
        }

        // 需要在线玩家的占位符
        if (!player.isOnline()) {
            return "";
        }

        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null) {
            return "";
        }

        // 玩家Tab前缀
        if (param.equals("prefix")) {
            return plugin.getPrefixForPlayer(onlinePlayer);
        }

        // 玩家Tab后缀
        if (param.equals("suffix")) {
            return plugin.getSuffixForPlayer(onlinePlayer);
        }

        // 玩家Tab格式组
        if (param.equals("group")) {
            return plugin.getGroupForPlayer(onlinePlayer);
        }

        return null;
    }
}
