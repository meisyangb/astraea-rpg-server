package cn.guangdian.vipname.placeholder;

import cn.guangdian.vipname.VIPname;
import cn.guangdian.vipname.model.Title;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * VIPname PlaceholderAPI 扩展
 * 
 * 占位符：
 * - %vipname_title% - 当前称号ID
 * - %vipname_title_name% - 当前称号名称
 * - %vipname_title_display% - 当前称号显示文本
 * - %vipname_title_prefix% - 当前称号前缀
 * - %vipname_title_suffix% - 当前称号后缀
 * - %vipname_title_full% - 当前称号完整显示
 * - %vipname_displayname% - 带称号的玩家显示名
 */
public class VIPnamePlaceholder extends PlaceholderExpansion {

    private final VIPname plugin;

    public VIPnamePlaceholder(VIPname plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "vipname";
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
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        Title title = plugin.getTitleManager().getCurrentTitle(player.getUniqueId());

        String identifier = params.toLowerCase();

        if (identifier.equals("title")) {
            return title != null ? title.getId() : "";
        }

        if (identifier.equals("title_name")) {
            return title != null ? title.getName() : "";
        }

        if (identifier.equals("title_display")) {
            return title != null ? VIPname.colorToString(title.getDisplay()) : "";
        }

        if (identifier.equals("title_prefix")) {
            return title != null ? VIPname.colorToString(title.getPrefix()) : "";
        }

        if (identifier.equals("title_suffix")) {
            return title != null ? VIPname.colorToString(title.getSuffix()) : "";
        }

        if (identifier.equals("title_full")) {
            return title != null ? VIPname.colorToString(title.getFullDisplay()) : "";
        }

        if (identifier.equals("displayname")) {
            return plugin.getTitleManager().getPlayerDisplayName(player);
        }

        return null;
    }
}