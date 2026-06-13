package cn.guangdian.rpgcore.service.api;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

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

    // ==================== 扩展方法 (从 UnifiedMessageService 迁移) ====================

    boolean sendMessage(UUID playerId, String message);

    void sendMessage(Collection<? extends Player> players, String message);

    void broadcastFiltered(String message, Predicate<Player> filter);

    void sendActionBar(Player player, String message);

    void sendActionBar(Collection<? extends Player> players, String message);

    void showTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    void showTitle(Collection<? extends Player> players, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    // ==================== 统一占位符方法 (支持 %xxx% 和 {xxx} 格式) ====================

    Component parseUnified(String text, Map<String, String> placeholders);

    Component parseUnified(String text, String... keyValues);

    String replaceUnified(String text, Map<String, String> placeholders);

    String replaceUnified(String text, String... keyValues);

    void sendUnified(Player player, String text, Map<String, String> placeholders);

    void sendUnified(Player player, String text, String... keyValues);
}
