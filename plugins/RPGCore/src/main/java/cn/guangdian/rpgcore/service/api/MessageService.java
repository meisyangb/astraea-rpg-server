package cn.guangdian.rpgcore.service.api;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface MessageService {

    void send(CommandSender sender, String message);

    void send(Player player, String message);

    void sendBroadcast(String message);

    void sendSuccess(CommandSender sender, String message);

    void sendError(CommandSender sender, String message);

    void sendWarning(CommandSender sender, String message);

    void sendInfo(CommandSender sender, String message);

    void sendHoverable(CommandSender sender, String text, String hoverText);

    void sendClickable(CommandSender sender, String text, String clickAction, String clickValue);

    void sendKeyable(CommandSender sender, String text, String suggestCommand);

    Component colorize(String text);

    Component colorize(String text, @Nullable String hoverText);

    Component colorize(String text, @Nullable String hoverText, @Nullable String clickAction, @Nullable String clickValue);

    String replacePlaceholders(String text, String... keyValues);
}
