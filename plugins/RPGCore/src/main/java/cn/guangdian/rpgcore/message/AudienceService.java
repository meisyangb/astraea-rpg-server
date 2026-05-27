package cn.guangdian.rpgcore.message;

import cn.guangdian.rpgcore.sound.SoundService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public final class AudienceService {

    private static AudienceService instance;

    private AudienceService() {}

    public static synchronized AudienceService getInstance() {
        if (instance == null) {
            instance = new AudienceService();
        }
        return instance;
    }

    public void sendMessage(@NotNull CommandSender sender, @NotNull Component message) {
        if (sender instanceof Player player) {
            player.sendMessage(message);
        } else {
            sender.sendMessage(MiniMessageService.getInstance().legacySerialize(message));
        }
    }

    public void sendMessage(@NotNull Player player, @NotNull Component message) {
        player.sendMessage(message);
    }

    public void sendMessage(@NotNull Collection<? extends Player> players, @NotNull Component message) {
        for (Player player : players) {
            player.sendMessage(message);
        }
    }

    public void broadcast(@NotNull Component message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    public void send(@NotNull Player player, @NotNull Component message) {
        player.sendMessage(message);
    }

    public void sendToAll(@NotNull Component message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    public void sendToPlayers(@NotNull Collection<? extends Player> players, @NotNull Component message) {
        for (Player player : players) {
            player.sendMessage(message);
        }
    }

    public void sendToFiltered(@NotNull Collection<? extends Player> players,
                               @NotNull Component message,
                               @NotNull Predicate<Player> filter) {
        for (Player player : players) {
            if (filter.test(player)) {
                player.sendMessage(message);
            }
        }
    }

    public void sendActionBar(@NotNull Player player, @NotNull Component message) {
        player.sendActionBar(message);
    }

    public void sendActionBar(@NotNull Collection<? extends Player> players, @NotNull Component message) {
        for (Player player : players) {
            player.sendActionBar(message);
        }
    }

    public void clearActionBar(@NotNull Player player) {
        player.sendActionBar(Component.empty());
    }

    public void clearActionBar(@NotNull Collection<? extends Player> players) {
        for (Player player : players) {
            player.sendActionBar(Component.empty());
        }
    }

    public void showTitle(@NotNull Player player, @NotNull Component title, @NotNull Component subtitle) {
        Title titleObj = Title.title(title, subtitle);
        player.showTitle(titleObj);
    }

    public void showTitle(@NotNull Player player, @NotNull Component title, @NotNull Component subtitle,
                         @NotNull Duration fadeIn, @NotNull Duration stay, @NotNull Duration fadeOut) {
        Title.Times times = Title.Times.times(fadeIn, stay, fadeOut);
        Title titleObj = Title.title(title, subtitle, times);
        player.showTitle(titleObj);
    }

    public void showTitle(@NotNull Player player, @NotNull Component title, @NotNull Component subtitle,
                         long fadeInTicks, long stayTicks, long fadeOutTicks) {
        Title.Times times = Title.Times.times(
            Duration.ofMillis(fadeInTicks * 50),
            Duration.ofMillis(stayTicks * 50),
            Duration.ofMillis(fadeOutTicks * 50)
        );
        Title titleObj = Title.title(title, subtitle, times);
        player.showTitle(titleObj);
    }

    public void showTitle(@NotNull Collection<? extends Player> players, @NotNull Component title, @NotNull Component subtitle) {
        Title titleObj = Title.title(title, subtitle);
        for (Player player : players) {
            player.showTitle(titleObj);
        }
    }

    public void clearTitle(@NotNull Player player) {
        player.clearTitle();
    }

    public void clearTitle(@NotNull Collection<? extends Player> players) {
        for (Player player : players) {
            player.clearTitle();
        }
    }

    public void playSound(@NotNull Player player, @NotNull String soundKey, float volume, float pitch) {
        SoundService.getInstance().playSound(player, soundKey, volume, pitch);
    }

    public void playSound(@NotNull Collection<? extends Player> players, @NotNull String soundKey, float volume, float pitch) {
        SoundService soundService = SoundService.getInstance();
        for (Player player : players) {
            soundService.playSound(player, soundKey, volume, pitch);
        }
    }

    public void stopSound(@NotNull Player player, @NotNull String soundKey) {
        SoundService.getInstance().stopSound(player, soundKey);
    }

    public void stopAllSounds(@NotNull Player player) {
        SoundService.getInstance().stopAllSounds(player);
    }

    public List<Player> filterPlayers(@NotNull Collection<? extends Player> players, @NotNull Predicate<Player> filter) {
        List<Player> result = new ArrayList<>();
        for (Player player : players) {
            if (filter.test(player)) {
                result.add(player);
            }
        }
        return result;
    }

    public List<Player> getOnlinePlayers() {
        return new ArrayList<>(Bukkit.getOnlinePlayers());
    }
}
