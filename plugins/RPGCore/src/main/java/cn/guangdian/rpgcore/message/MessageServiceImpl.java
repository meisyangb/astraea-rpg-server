package cn.guangdian.rpgcore.message;

import cn.guangdian.rpgcore.service.api.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

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
}
