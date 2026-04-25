package cn.guangdian.forge.placeholder;

import cn.guangdian.forge.GuangDianForge;
import cn.guangdian.forge.model.PlayerForgeData;
import cn.guangdian.rpgcore.integration.PlaceholderService;
import org.bukkit.OfflinePlayer;

import java.util.Locale;

public class ForgePlaceholder {

    private final GuangDianForge plugin;

    public ForgePlaceholder(GuangDianForge plugin) {
        this.plugin = plugin;
    }

    public void register() {
        PlaceholderService service = PlaceholderService.getInstance();
        if (service == null) return;
        
        service.register("forge", (player, params) -> {
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
        });
    }

    public void unregister() {
    }
}
