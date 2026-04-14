package cn.guangdian.rpgcore.display;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AdventureBossBarService {

    private static AdventureBossBarService instance;

    private final Map<UUID, BossBar> playerBossBars = new ConcurrentHashMap<>();

    private AdventureBossBarService() {}

    public static synchronized AdventureBossBarService getInstance() {
        if (instance == null) {
            instance = new AdventureBossBarService();
        }
        return instance;
    }

    public void showBossBar(Player player, Component title, double progress) {
        showBossBar(player, title, progress, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
    }

    public void showBossBar(Player player, Component title, double progress, BossBar.Color color) {
        showBossBar(player, title, progress, color, BossBar.Overlay.PROGRESS);
    }

    public void showBossBar(Player player, Component title, double progress, BossBar.Color color, BossBar.Overlay overlay) {
        hideBossBar(player);
        float progressFloat = (float) Math.max(0.0, Math.min(1.0, progress));
        BossBar bossBar = BossBar.bossBar(title, progressFloat, color, overlay);
        player.showBossBar(bossBar);
        playerBossBars.put(player.getUniqueId(), bossBar);
    }

    public void showBossBar(Player player, Component title, double progress, BossBar.Color color, BossBar.Overlay overlay, Set<BossBar.Flag> flags) {
        hideBossBar(player);
        float progressFloat = (float) Math.max(0.0, Math.min(1.0, progress));
        BossBar bossBar = BossBar.bossBar(title, progressFloat, color, overlay, flags);
        player.showBossBar(bossBar);
        playerBossBars.put(player.getUniqueId(), bossBar);
    }

    public void updateBossBar(Player player, Component title, Float progress, BossBar.Color color, BossBar.Overlay overlay) {
        BossBar existingBar = playerBossBars.get(player.getUniqueId());
        if (existingBar == null) {
            showBossBar(player, title,
                progress != null ? progress : 1.0,
                color != null ? color : BossBar.Color.PURPLE,
                overlay != null ? overlay : BossBar.Overlay.PROGRESS);
            return;
        }
        if (title != null) {
            existingBar = existingBar.name(title);
        }
        if (progress != null) {
            existingBar = existingBar.progress(Math.max(0.0f, Math.min(1.0f, progress)));
        }
        if (color != null) {
            existingBar = existingBar.color(color);
        }
        if (overlay != null) {
            existingBar = existingBar.overlay(overlay);
        }
        playerBossBars.put(player.getUniqueId(), existingBar);
    }

    public void updateTitle(Player player, Component title) {
        BossBar bar = playerBossBars.get(player.getUniqueId());
        if (bar != null && title != null) {
            BossBar newBar = bar.name(title);
            playerBossBars.put(player.getUniqueId(), newBar);
        }
    }

    public void updateProgress(Player player, double progress) {
        BossBar bar = playerBossBars.get(player.getUniqueId());
        if (bar != null) {
            float progressFloat = (float) Math.max(0.0, Math.min(1.0, progress));
            BossBar newBar = bar.progress(progressFloat);
            playerBossBars.put(player.getUniqueId(), newBar);
        }
    }

    public void updateColor(Player player, BossBar.Color color) {
        BossBar bar = playerBossBars.get(player.getUniqueId());
        if (bar != null && color != null) {
            BossBar newBar = bar.color(color);
            playerBossBars.put(player.getUniqueId(), newBar);
        }
    }

    public void updateOverlay(Player player, BossBar.Overlay overlay) {
        BossBar bar = playerBossBars.get(player.getUniqueId());
        if (bar != null && overlay != null) {
            BossBar newBar = bar.overlay(overlay);
            playerBossBars.put(player.getUniqueId(), newBar);
        }
    }

    public void addFlag(Player player, BossBar.Flag flag) {
        BossBar bar = playerBossBars.get(player.getUniqueId());
        if (bar != null && flag != null) {
            Set<BossBar.Flag> flags = new HashSet<>(bar.flags());
            flags.add(flag);
            BossBar newBar = bar.flags(flags);
            playerBossBars.put(player.getUniqueId(), newBar);
        }
    }

    public void removeFlag(Player player, BossBar.Flag flag) {
        BossBar bar = playerBossBars.get(player.getUniqueId());
        if (bar != null && flag != null) {
            Set<BossBar.Flag> flags = new HashSet<>(bar.flags());
            flags.remove(flag);
            BossBar newBar = bar.flags(flags);
            playerBossBars.put(player.getUniqueId(), newBar);
        }
    }

    public void hideBossBar(Player player) {
        BossBar bar = playerBossBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    public void hideAllBossBars() {
        for (Map.Entry<UUID, BossBar> entry : playerBossBars.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                player.hideBossBar(entry.getValue());
            }
        }
        playerBossBars.clear();
    }

    public boolean hasBossBar(Player player) {
        return playerBossBars.containsKey(player.getUniqueId());
    }

    public BossBar getBossBar(Player player) {
        return playerBossBars.get(player.getUniqueId());
    }

    public Collection<BossBar> getAllBossBars() {
        return Collections.unmodifiableCollection(playerBossBars.values());
    }

    public void broadcastBossBar(Component title, double progress, BossBar.Color color, BossBar.Overlay overlay) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            showBossBar(player, title, progress, color, overlay);
        }
    }

    public void broadcastBossBar(Component title, double progress) {
        broadcastBossBar(title, progress, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
    }

    public void broadcastBossBarHide() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            hideBossBar(player);
        }
    }

    public Component getBossBarTitle(Player player) {
        BossBar bar = playerBossBars.get(player.getUniqueId());
        return bar != null ? bar.name() : Component.empty();
    }

    public float getBossBarProgress(Player player) {
        BossBar bar = playerBossBars.get(player.getUniqueId());
        return bar != null ? bar.progress() : 0.0f;
    }

    public BossBar.Color colorFromName(String name, BossBar.Color defaultColor) {
        if (name == null) return defaultColor;
        try {
            return BossBar.Color.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultColor;
        }
    }

    public BossBar.Overlay overlayFromName(String name, BossBar.Overlay defaultOverlay) {
        if (name == null) return defaultOverlay;
        try {
            return BossBar.Overlay.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultOverlay;
        }
    }

    public void shutdown() {
        hideAllBossBars();
    }
}
