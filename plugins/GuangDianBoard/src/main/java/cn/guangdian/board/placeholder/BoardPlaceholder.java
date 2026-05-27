package cn.guangdian.board.placeholder;

import cn.guangdian.board.GuangDianBoard;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Board PlaceholderAPI 扩展
 * 
 * <p>提供侧边栏相关的占位符。</p>
 * 
 * <h3>可用占位符：</h3>
 * <ul>
 *   <li>%gdboard_enabled% - 侧边栏是否启用</li>
 *   <li>%gdboard_visible% - 玩家是否能看到侧边栏</li>
 *   <li>%gdboard_lines% - 侧边栏行数</li>
 *   <li>%gdboard_title% - 侧边栏标题</li>
 * </ul>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class BoardPlaceholder extends PlaceholderExpansion {

    private final GuangDianBoard plugin;

    public BoardPlaceholder(GuangDianBoard plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "gdboard";
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

        // 侧边栏是否启用
        if (param.equals("enabled")) {
            return String.valueOf(plugin.getConfig().getBoolean("enabled", true));
        }

        // 玩家是否能看到侧边栏
        if (param.equals("visible")) {
            if (player.isOnline()) {
                Player onlinePlayer = player.getPlayer();
                if (onlinePlayer != null) {
                    return String.valueOf(plugin.shouldShowBoardPublic(onlinePlayer));
                }
            }
            return "false";
        }

        // 侧边栏行数
        if (param.equals("lines")) {
            return String.valueOf(plugin.getConfig().getInt("advanced.max-lines", 15));
        }

        // 侧边栏标题
        if (param.equals("title")) {
            return plugin.getDefaultTitlePublic();
        }

        return null;
    }
}
