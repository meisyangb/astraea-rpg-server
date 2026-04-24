package cn.guangdian.rpgcore.message;

import cn.guangdian.rpgcore.service.api.MessageService;
import cn.guangdian.rpgcore.sound.SoundService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 统一消息服务实现
 *
 * <p>整合了原 AudienceService 和 MessageService 的功能，
 * 提供统一的消息发送入口。</p>
 *
 * @author GuangDian
 * @since 2.0.0
 */
public class MessageServiceImpl implements MessageService {

    private static MessageServiceImpl instance;
    private final MiniMessageService miniMessage;
    private final SoundService soundService;

    public MessageServiceImpl() {
        this.miniMessage = MiniMessageService.getInstance();
        this.soundService = SoundService.getInstance();
    }

    public static MessageServiceImpl getInstance() {
        if (instance == null) {
            instance = new MessageServiceImpl();
        }
        return instance;
    }

    // ==================== 基础消息发送 ====================

    @Override
    public void send(CommandSender sender, String message) {
        if (sender == null || message == null) {
            return;
        }
        sender.sendMessage(miniMessage.colorize(message));
    }

    @Override
    public void send(Player player, String message) {
        if (player == null || message == null) {
            return;
        }
        player.sendMessage(miniMessage.colorize(message));
    }

    @Override
    public boolean send(UUID playerId, String message) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.sendMessage(miniMessage.colorize(message));
            return true;
        }
        return false;
    }

    @Override
    public void send(Collection<? extends Player> players, String message) {
        if (message == null) {
            return;
        }
        Component component = miniMessage.colorize(message);
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                player.sendMessage(component);
            }
        }
    }

    @Override
    public void broadcast(String message) {
        if (message == null) {
            return;
        }
        Bukkit.broadcast(miniMessage.colorize(message));
    }

    @Override
    public void broadcast(String message, Predicate<Player> filter) {
        if (message == null || filter == null) {
            return;
        }
        Component component = miniMessage.colorize(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (filter.test(player)) {
                player.sendMessage(component);
            }
        }
    }

    // ==================== 快捷消息方法 ====================

    @Override
    public void sendSuccess(CommandSender sender, String message) {
        if (sender == null || message == null) {
            return;
        }
        sender.sendMessage(miniMessage.colorize("<green>" + message));
    }

    @Override
    public void sendError(CommandSender sender, String message) {
        if (sender == null || message == null) {
            return;
        }
        sender.sendMessage(miniMessage.colorize("<red>" + message));
    }

    @Override
    public void sendWarning(CommandSender sender, String message) {
        if (sender == null || message == null) {
            return;
        }
        sender.sendMessage(miniMessage.colorize("<yellow>" + message));
    }

    @Override
    public void sendInfo(CommandSender sender, String message) {
        if (sender == null || message == null) {
            return;
        }
        sender.sendMessage(miniMessage.colorize("<aqua>" + message));
    }

    // ==================== 交互式消息 ====================

    @Override
    public void sendHoverable(CommandSender sender, String text, String hoverText) {
        if (sender == null || text == null) {
            return;
        }
        Component component = parseWithHover(text, hoverText);
        sender.sendMessage(component);
    }

    @Override
    public void sendClickable(CommandSender sender, String text, String clickAction, String clickValue) {
        if (sender == null || text == null) {
            return;
        }
        Component component = parseWithClick(text, null, clickAction, clickValue);
        sender.sendMessage(component);
    }

    @Override
    public void sendSuggestible(CommandSender sender, String text, String suggestCommand) {
        if (sender == null || text == null) {
            return;
        }
        Component component = parseWithClick(text, null, ClickEvent.Action.SUGGEST_COMMAND.name(), suggestCommand);
        sender.sendMessage(component);
    }

    // ==================== ActionBar ====================

    @Override
    public void sendActionBar(Player player, String message) {
        if (player == null || message == null) {
            return;
        }
        player.sendActionBar(miniMessage.colorize(message));
    }

    @Override
    public void sendActionBar(Collection<? extends Player> players, String message) {
        if (message == null) {
            return;
        }
        Component component = miniMessage.colorize(message);
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                player.sendActionBar(component);
            }
        }
    }

    @Override
    public void clearActionBar(Player player) {
        if (player == null) {
            return;
        }
        player.sendActionBar(Component.empty());
    }

    @Override
    public void clearActionBar(Collection<? extends Player> players) {
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                player.sendActionBar(Component.empty());
            }
        }
    }

    // ==================== Title ====================

    @Override
    public void showTitle(Player player, String title, String subtitle) {
        if (player == null) {
            return;
        }
        Component titleComp = title != null ? miniMessage.colorize(title) : Component.empty();
        Component subtitleComp = subtitle != null ? miniMessage.colorize(subtitle) : Component.empty();
        player.showTitle(Title.title(titleComp, subtitleComp));
    }

    @Override
    public void showTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) {
            return;
        }
        Component titleComp = title != null ? miniMessage.colorize(title) : Component.empty();
        Component subtitleComp = subtitle != null ? miniMessage.colorize(subtitle) : Component.empty();
        Title.Times times = Title.Times.times(
            Duration.ofMillis(fadeIn * 50L),
            Duration.ofMillis(stay * 50L),
            Duration.ofMillis(fadeOut * 50L)
        );
        player.showTitle(Title.title(titleComp, subtitleComp, times));
    }

    @Override
    public void showTitle(Collection<? extends Player> players, String title, String subtitle) {
        if (title == null) {
            return;
        }
        Component titleComp = miniMessage.colorize(title);
        Component subtitleComp = subtitle != null ? miniMessage.colorize(subtitle) : Component.empty();
        Title titleObj = Title.title(titleComp, subtitleComp);
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                player.showTitle(titleObj);
            }
        }
    }

    @Override
    public void showTitle(Collection<? extends Player> players, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (title == null) {
            return;
        }
        Component titleComp = miniMessage.colorize(title);
        Component subtitleComp = subtitle != null ? miniMessage.colorize(subtitle) : Component.empty();
        Title.Times times = Title.Times.times(
            Duration.ofMillis(fadeIn * 50L),
            Duration.ofMillis(stay * 50L),
            Duration.ofMillis(fadeOut * 50L)
        );
        Title titleObj = Title.title(titleComp, subtitleComp, times);
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                player.showTitle(titleObj);
            }
        }
    }

    @Override
    public void clearTitle(Player player) {
        if (player == null) {
            return;
        }
        player.clearTitle();
    }

    @Override
    public void clearTitle(Collection<? extends Player> players) {
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                player.clearTitle();
            }
        }
    }

    // ==================== 声音 ====================

    @Override
    public void playSound(Player player, String soundKey, float volume, float pitch) {
        soundService.playSound(player, soundKey, volume, pitch);
    }

    @Override
    public void playSound(Collection<? extends Player> players, String soundKey, float volume, float pitch) {
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                soundService.playSound(player, soundKey, volume, pitch);
            }
        }
    }

    @Override
    public void stopSound(Player player, String soundKey) {
        soundService.stopSound(player, soundKey);
    }

    @Override
    public void stopAllSounds(Player player) {
        soundService.stopAllSounds(player);
    }

    // ==================== 工具方法 ====================

    @Override
    public Component parse(String text) {
        return miniMessage.colorize(text);
    }

    @Override
    public Component parseWithHover(String text, @Nullable String hoverText) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        Component component = miniMessage.colorize(text);
        if (hoverText != null && !hoverText.isEmpty()) {
            component = component.hoverEvent(HoverEvent.showText(
                miniMessage.colorize(hoverText)
            ));
        }
        return component;
    }

    @Override
    public Component parseWithClick(String text, @Nullable String hoverText, @Nullable String clickAction, @Nullable String clickValue) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        Component component = miniMessage.colorize(text);

        if (hoverText != null && !hoverText.isEmpty()) {
            component = component.hoverEvent(HoverEvent.showText(
                miniMessage.colorize(hoverText)
            ));
        }

        if (clickAction != null && clickValue != null) {
            ClickEvent.Action action;
            try {
                action = ClickEvent.Action.valueOf(clickAction.toUpperCase());
            } catch (IllegalArgumentException e) {
                action = ClickEvent.Action.SUGGEST_COMMAND;
            }
            component = component.clickEvent(ClickEvent.clickEvent(action, clickValue));
        }

        return component;
    }

    @Override
    public String replacePlaceholders(String text, String... keyValues) {
        if (text == null || keyValues == null) {
            return text;
        }
        String result = text;
        for (int i = 0; i < keyValues.length - 1; i += 2) {
            String key = keyValues[i];
            String value = keyValues[i + 1];
            if (key != null && value != null) {
                result = result.replace(key, value);
            }
        }
        return result;
    }
}