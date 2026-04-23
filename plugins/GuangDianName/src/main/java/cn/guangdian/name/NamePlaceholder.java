package cn.guangdian.name;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI 扩展
 * 
 * 提供占位符：
 * - %gdname_show_title% - 称号显示状态
 * - %gdname_show_guild% - 工会显示状态
 * - %gdname_show_marriage% - 婚姻显示状态
 * - %gdname_show_health% - 血量显示状态
 */
public class NamePlaceholder extends PlaceholderExpansion {

    private final GuangDianName plugin;
    private final NameDisplayManager displayManager;

    public NamePlaceholder(GuangDianName plugin, NameDisplayManager displayManager) {
        this.plugin = plugin;
        this.displayManager = displayManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "gdname";
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
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

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
    }
}