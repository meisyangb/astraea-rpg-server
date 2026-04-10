package cn.guangdian.cavefu.placeholder;

import cn.guangdian.cavefu.GuangDianCaveFu;
import cn.guangdian.cavefu.cave.Cave;
import cn.guangdian.cavefu.cave.CaveLevel;
import cn.guangdian.cavefu.cave.CaveMember;
import cn.guangdian.cavefu.permission.PermissionType;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI 扩展
 */
public class CavePlaceholder extends PlaceholderExpansion {
    private final GuangDianCaveFu plugin;

    public CavePlaceholder(GuangDianCaveFu plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "gdcave";
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
        if (player == null) return "";

        Cave cave = plugin.getDataManager().getCaveByMember(player.getUniqueId());
        String param = params.toLowerCase();

        return switch (param) {
            case "owner", "洞主" -> cave != null ? cave.getOwnerName() : "";
            case "level", "等级" -> cave != null ? String.valueOf(cave.getLevel()) : "0";
            case "level_name", "等级名" -> {
                if (cave == null) yield "无";
                CaveLevel level = plugin.getConfigManager().getLevel(cave.getLevel());
                yield level != null ? level.getName() : "未知";
            }
            case "size", "大小" -> {
                if (cave == null) yield "0";
                CaveLevel level = plugin.getConfigManager().getLevel(cave.getLevel());
                yield level != null ? level.getSize() + "x" + level.getSize() : "未知";
            }
            case "members", "成员数" -> cave != null ? String.valueOf(cave.getMembers().size()) : "0";
            case "max_members", "最大成员" -> String.valueOf(plugin.getConfigManager().getMaxMembers());
            case "has_cave", "是否有洞府" -> cave != null ? "是" : "否";
            case "is_owner", "是否洞主" -> {
                if (cave == null) yield "否";
                yield cave.getOwnerUuid().equals(player.getUniqueId()) ? "是" : "否";
            }
            case "permission", "权限" -> {
                if (cave == null) yield "无";
                CaveMember member = cave.getMember(player.getUniqueId());
                yield member != null ? member.getPermission().getDisplayName() : "无";
            }
            case "total_caves", "洞府总数" -> String.valueOf(plugin.getDataManager().getCaveCount());
            default -> null;
        };
    }
}