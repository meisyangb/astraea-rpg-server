package cn.guangdian.rpgcore.message;

import cn.guangdian.rpgcore.service.api.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public class MessageServiceImpl implements MessageService {

    private static MessageServiceImpl instance;
    private final MiniMessageService miniMessage;

    public MessageServiceImpl() {
        this.miniMessage = MiniMessageService.getInstance();
    }

    public static MessageServiceImpl getInstance() {
        if (instance == null) {
            instance = new MessageServiceImpl();
        }
        return instance;
    }

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
    public void sendBroadcast(String message) {
        Bukkit.broadcast(miniMessage.colorize(message));
    }

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

    @Override
    public void sendHoverable(CommandSender sender, String text, String hoverText) {
        if (sender == null || text == null) {
            return;
        }
        Component component = colorize(text, hoverText);
        sender.sendMessage(component);
    }

    @Override
    public void sendClickable(CommandSender sender, String text, String clickAction, String clickValue) {
        if (sender == null || text == null) {
            return;
        }
        Component component = colorize(text, null, clickAction, clickValue);
        sender.sendMessage(component);
    }

    @Override
    public void sendKeyable(CommandSender sender, String text, String suggestCommand) {
        if (sender == null || text == null) {
            return;
        }
        Component component = colorize(text, null, ClickEvent.Action.SUGGEST_COMMAND.name(), suggestCommand);
        sender.sendMessage(component);
    }

    @Override
    public Component colorize(String text) {
        return miniMessage.colorize(text);
    }

    @Override
    public Component colorize(String text, @Nullable String hoverText) {
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
    public Component colorize(String text, @Nullable String hoverText, @Nullable String clickAction, @Nullable String clickValue) {
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
            component = component.clickEvent(ClickEvent.suggestCommand(clickValue));
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

    // ==================== 扩展方法实现 ====================

    @Override
    public boolean sendMessage(UUID playerId, String message) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.sendMessage(miniMessage.colorize(message));
            return true;
        }
        return false;
    }

    @Override
    public void sendMessage(Collection<? extends Player> players, String message) {
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
    public void broadcastFiltered(String message, Predicate<Player> filter) {
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
    public void showTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) {
            return;
        }
        AudienceService.getInstance().showTitle(
            player,
            miniMessage.colorize(title),
            miniMessage.colorize(subtitle),
            fadeIn, stay, fadeOut
        );
    }

    @Override
    public void showTitle(Collection<? extends Player> players, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (title == null) {
            return;
        }
        Component titleComp = miniMessage.colorize(title);
        Component subtitleComp = subtitle != null ? miniMessage.colorize(subtitle) : Component.empty();
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                AudienceService.getInstance().showTitle(player, titleComp, subtitleComp, fadeIn, stay, fadeOut);
            }
        }
    }

    // ==================== 统一占位符方法实现 ====================

    @Override
    public Component parseUnified(String text, Map<String, String> placeholders) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return miniMessage.parseUnified(text, placeholders);
    }

    @Override
    public Component parseUnified(String text, String... keyValues) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return miniMessage.parseUnified(text, keyValues);
    }

    @Override
    public String replaceUnified(String text, Map<String, String> placeholders) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return miniMessage.replaceUnified(text, placeholders);
    }

    @Override
    public String replaceUnified(String text, String... keyValues) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return miniMessage.replaceUnified(text, keyValues);
    }

    @Override
    public void sendUnified(Player player, String text, Map<String, String> placeholders) {
        if (player == null || text == null) {
            return;
        }
        player.sendMessage(parseUnified(text, placeholders));
    }

    @Override
    public void sendUnified(Player player, String text, String... keyValues) {
        if (player == null || text == null) {
            return;
        }
        player.sendMessage(parseUnified(text, keyValues));
    }
}
