package cn.guangdian.raid.manager;

import cn.guangdian.raid.GuangDianRaid;
import cn.guangdian.raid.instance.RaidInstance;
import cn.guangdian.raid.model.ExtractionPoint;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ExtractionManager {

    private final GuangDianRaid plugin;
    private final SyncScheduler scheduler;
    private final Map<UUID, ExtractionProgress> extractionProgress;
    private final Map<String, TextDisplay> countdownDisplays;

    public ExtractionManager(GuangDianRaid plugin) {
        this.plugin = plugin;
        this.scheduler = RPGCore.getInstance().getScheduler();
        this.extractionProgress = new ConcurrentHashMap<>();
        this.countdownDisplays = new ConcurrentHashMap<>();
    }

    public void activateExtractionPoints(RaidInstance instance) {
        for (ExtractionPoint point : instance.getRaid().getExtractionPoints().values()) {
            createExtractionDisplay(instance, point);
        }

        long taskId = scheduler.runSyncRepeating(() -> {
            if (!instance.isActive()) return;
            checkExtractionZones(instance);
        }, 20L, 20L);
    }

    private void createExtractionDisplay(RaidInstance instance, ExtractionPoint point) {
        Location loc = point.getLocation();
        if (loc == null) return;

        World world = instance.getWorld();
        if (world == null) return;

        loc = loc.clone();
        loc.setWorld(world);
        loc.add(0, 3, 0);

        TextDisplay display = world.spawn(loc, TextDisplay.class);
        display.text(Component.text("撤离点").color(NamedTextColor.GREEN));
        display.setBillboard(Display.Billboard.CENTER);
        display.setViewRange(50f);

        countdownDisplays.put(instance.getInstanceId() + "_" + point.getId(), display);
    }

    private void checkExtractionZones(RaidInstance instance) {
        for (ExtractionPoint point : instance.getRaid().getExtractionPoints().values()) {
            Location pointLoc = point.getLocation();
            if (pointLoc == null) continue;

            World world = instance.getWorld();
            if (world == null) continue;

            pointLoc = pointLoc.clone();
            pointLoc.setWorld(world);

            for (Player player : instance.getTeam().getOnlinePlayers()) {
                var rp = instance.getTeam().getMember(player.getUniqueId());
                if (rp == null || rp.getState() != cn.guangdian.raid.model.RaidPlayerState.ALIVE) continue;

                double distance = player.getLocation().distance(pointLoc);
                if (distance <= point.getRadius()) {
                    if (point.isRequiresIntel() && instance.getCollectedIntel() < point.getMinIntelRequired()) {
                        player.sendActionBar(Component.text("情报不足！需要 " + point.getMinIntelRequired() + " 份情报")
                            .color(NamedTextColor.RED));
                        continue;
                    }

                    ExtractionProgress progress = extractionProgress.computeIfAbsent(
                        player.getUniqueId(),
                        k -> new ExtractionProgress(point)
                    );

                    progress.increment();
                    showExtractionCountdown(player, progress, point);

                    if (progress.isComplete()) {
                        completeExtraction(instance, player);
                        extractionProgress.remove(player.getUniqueId());
                    }
                } else {
                    extractionProgress.remove(player.getUniqueId());
                }
            }
        }
    }

    private void showExtractionCountdown(Player player, ExtractionProgress progress, ExtractionPoint point) {
        int remaining = point.getExtractionTime() - progress.getSeconds();

        String text = switch (remaining) {
            case 10 -> "<green><bold>10秒后撤离";
            case 5 -> "<yellow><bold>5秒后撤离";
            case 4 -> "<yellow><bold>4秒后撤离";
            case 3 -> "<red><bold>3秒后撤离";
            case 2 -> "<red><bold>2秒后撤离";
            case 1 -> "<dark_red><bold>1秒后撤离";
            default -> "<aqua><bold>" + remaining + "秒后撤离";
        };

        player.sendActionBar(Component.text(text));

        if (remaining <= 5 && remaining > 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f - (5 - remaining) * 0.1f);
        }
    }

    private void completeExtraction(RaidInstance instance, Player player) {
        instance.onPlayerExtraction(player);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    public void cleanupExtractionDisplays(RaidInstance instance) {
        for (String key : countdownDisplays.keySet()) {
            if (key.startsWith(instance.getInstanceId())) {
                TextDisplay display = countdownDisplays.remove(key);
                if (display != null) {
                    display.remove();
                }
            }
        }
    }

    public void clearPlayerProgress(UUID playerId) {
        extractionProgress.remove(playerId);
    }

    private static class ExtractionProgress {
        private final ExtractionPoint point;
        private int seconds;

        public ExtractionProgress(ExtractionPoint point) {
            this.point = point;
            this.seconds = 0;
        }

        public void increment() {
            seconds++;
        }

        public int getSeconds() {
            return seconds;
        }

        public boolean isComplete() {
            return seconds >= point.getExtractionTime();
        }
    }
}
