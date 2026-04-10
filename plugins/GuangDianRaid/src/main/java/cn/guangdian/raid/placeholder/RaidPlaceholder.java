package cn.guangdian.raid.placeholder;

import cn.guangdian.raid.GuangDianRaid;
import cn.guangdian.raid.instance.RaidInstance;
import cn.guangdian.raid.model.RaidPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Optional;

public class RaidPlaceholder {

    private final GuangDianRaid plugin;
    private Object registeredExpansion;

    public RaidPlaceholder(GuangDianRaid plugin) {
        this.plugin = plugin;
    }

    public void register() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            plugin.getLogger().warning("PlaceholderAPI 未安装，占位符功能已禁用");
            return;
        }

        try {
            Class<?> expansionClass = Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
            Object expansion = java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { expansionClass },
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    return switch (methodName) {
                        case "getIdentifier" -> "gdraid";
                        case "getAuthor" -> "Astraea RPG Team";
                        case "getVersion" -> "1.0.0";
                        case "persist" -> true;
                        case "onRequest" -> onRequest((OfflinePlayer) args[0], (String) args[1]);
                        case "canRegister" -> true;
                        default -> null;
                    };
                }
            );

            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Method registerMethod = papiClass.getMethod("registerExpansion", expansionClass);
            registerMethod.invoke(null, expansion);
            registeredExpansion = expansion;
            plugin.getLogger().info("PlaceholderAPI 占位符已注册");
        } catch (Exception e) {
            plugin.getLogger().warning("注册 PlaceholderAPI 占位符失败: " + e.getMessage());
        }
    }

    public void unregister() {
        if (registeredExpansion == null) return;
        
        try {
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Class<?> expansionClass = Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
            Method unregisterMethod = papiClass.getMethod("unregisterExpansion", expansionClass);
            unregisterMethod.invoke(null, registeredExpansion);
        } catch (Exception e) {
            plugin.getLogger().warning("注销占位符失败: " + e.getMessage());
        }
    }

    private String onRequest(OfflinePlayer offlinePlayer, String identifier) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) return "";

        Player player = offlinePlayer.getPlayer();
        if (player == null) return "";

        Optional<RaidInstance> instanceOpt = plugin.getInstanceManager().getPlayerInstance(player.getUniqueId());
        
        return switch (identifier.toLowerCase()) {
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
    }
}
