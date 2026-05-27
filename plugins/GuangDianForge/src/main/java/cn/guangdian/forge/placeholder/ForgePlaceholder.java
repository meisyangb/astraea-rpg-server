package cn.guangdian.forge.placeholder;

import cn.guangdian.forge.GuangDianForge;
import cn.guangdian.forge.model.PlayerForgeData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Forge 占位符扩展
 * 
 * <p>提供锻造相关占位符：</p>
 * <ul>
 *   <li>%forge_level% - 锻造等级</li>
 *   <li>%forge_exp% - 当前经验</li>
 *   <li>%forge_recipes% - 已学图纸数量</li>
 *   <li>%forge_total% - 总锻造次数</li>
 *   <li>%forge_success% - 成功锻造次数</li>
 *   <li>%forge_rate% - 成功率</li>
 * </ul>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class ForgePlaceholder extends PlaceholderExpansion {

    private final GuangDianForge plugin;

    public ForgePlaceholder(GuangDianForge plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "forge";
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
        if (player == null || plugin.getPlayerDataManager() == null) {
            return "0";
        }

        PlayerForgeData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) {
            return "0";
        }

        String key = params.toLowerCase(Locale.ROOT);
        
        return switch (key) {
            case "level", "等级" -> String.valueOf(data.getForgeLevel());
            case "exp", "经验" -> String.valueOf(data.getForgeExp());
            case "recipes", "图纸数", "已学图纸" -> String.valueOf(data.getLearnedRecipes().size());
            case "total", "总锻造", "总次数" -> String.valueOf(data.getTotalForges());
            case "success", "成功次数" -> String.valueOf(data.getSuccessForges());
            case "rate", "成功率" -> {
                if (data.getTotalForges() == 0) {
                    yield "0%";
                }
                int rate = (int) ((data.getSuccessForges() * 100.0) / data.getTotalForges());
                yield rate + "%";
            }
            case "failed", "失败次数" -> String.valueOf(data.getTotalForges() - data.getSuccessForges());
            default -> null;
        };
    }
}