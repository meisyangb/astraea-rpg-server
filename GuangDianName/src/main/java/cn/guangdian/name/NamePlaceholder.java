package cn.guangdian.name;

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
 */
public class NamePlaceholder extends PlaceholderExpansion {

    private final GuangDianName plugin;
    private final TitleDisplay titleDisplay;

    public NamePlaceholder(GuangDianName plugin, TitleDisplay titleDisplay) {
        this.plugin = plugin;
        this.titleDisplay = titleDisplay;
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

        switch (params.toLowerCase()) {
            case "show_title":
                return MiniMessageService.getInstance().legacyColorize(
                        titleDisplay.getShowTitleStatus(player));
            case "show_guild":
                return MiniMessageService.getInstance().legacyColorize(
                        titleDisplay.getShowGuildStatus(player));
            case "show_marriage":
                return MiniMessageService.getInstance().legacyColorize(
                        titleDisplay.getShowMarriageStatus(player));
            default:
                return null;
        }
    }
}
