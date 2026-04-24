package cn.guangdian.raid.placeholder;

import cn.guangdian.raid.GuangDianRaid;
import cn.guangdian.raid.instance.RaidInstance;
import cn.guangdian.raid.model.RaidPlayer;
import cn.guangdian.rpgcore.integration.PlaceholderService;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Optional;

public class RaidPlaceholderExpansion {

    private final GuangDianRaid plugin;

    public RaidPlaceholderExpansion(GuangDianRaid plugin) {
        this.plugin = plugin;
    }

    public void register() {
        PlaceholderService service = PlaceholderService.getInstance();
        if (service == null) return;
        
        service.register("gdraid", (player, params) -> {
            if (player == null || !player.isOnline()) {
                return "";
            }

            Player onlinePlayer = player.getPlayer();
            if (onlinePlayer == null) {
                return "";
            }

            Optional<RaidInstance> instanceOpt = plugin.getInstanceManager().getPlayerInstance(player.getUniqueId());

            if (params == null) {
                return null;
            }

            return switch (params.toLowerCase()) {
                case "current" -> instanceOpt.map(i -> i.getRaid().getName()).orElse("");
                case "phase" -> instanceOpt.map(i -> i.getCurrentPhase().getDisplayName()).orElse("");
                case "time" -> {
                    if (instanceOpt.isEmpty()) yield "";
                    RaidInstance instance = instanceOpt.get();
                    long elapsed = (System.currentTimeMillis() - instance.getStartTime()) / 1000;
                    int remaining = instance.getRaid().getTotalTimeLimit() - (int) elapsed;
                    int mins = remaining / 60;
                    int secs = remaining % 60;
                    yield String.format("%02d:%02d", mins, secs);
                }
                case "intel" -> instanceOpt.map(i -> String.valueOf(i.getCollectedIntel())).orElse("0");
                case "kills" -> {
                    if (instanceOpt.isEmpty()) yield "0";
                    RaidInstance instance = instanceOpt.get();
                    RaidPlayer rp = instance.getTeam().getMember(player.getUniqueId());
                    yield rp != null ? String.valueOf(rp.getKills()) : "0";
                }
                case "extraction" -> {
                    if (instanceOpt.isEmpty()) yield "";
                    RaidInstance instance = instanceOpt.get();
                    yield instance.isExtractionActive() ? "撤离中" : "";
                }
                case "alive" -> {
                    if (instanceOpt.isEmpty()) yield "";
                    RaidInstance instance = instanceOpt.get();
                    long aliveCount = instance.getTeam().getMembers().stream()
                        .filter(p -> p.getState() == cn.guangdian.raid.model.RaidPlayerState.ALIVE)
                        .count();
                    yield aliveCount + "/" + instance.getTeam().size();
                }
                default -> null;
            };
        });
    }

    public void unregister() {
    }
}
