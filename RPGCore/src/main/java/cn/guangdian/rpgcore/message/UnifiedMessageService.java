package cn.guangdian.rpgcore.message;

import cn.guangdian.rpgcore.service.api.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 统一消息服务 (Facade - 完全委托给 MessageService)
 *
 * <p><strong>已弃用</strong>: 新插件请直接使用 {@link cn.guangdian.rpgcore.service.api.MessageService}</p>
 *
 * <p>此类现在完全委托给 MessageService，不再包含额外逻辑。</p>
 *
 * @deprecated 自 1.2.0 起弃用，将在 2.0.0 中移除。请使用 {@link cn.guangdian.rpgcore.service.api.MessageService}
 * @see MessageService
 * @since 1.1.0
 */
@Deprecated(since = "1.2.0", forRemoval = true)
public final class UnifiedMessageService {

    private static UnifiedMessageService instance;
    private final MessageService messageService;

    private UnifiedMessageService() {
        this.messageService = MessageServiceImpl.getInstance();
    }

    public static synchronized UnifiedMessageService getInstance() {
        if (instance == null) {
            instance = new UnifiedMessageService();
        }
        return instance;
    }

    /**
     * @deprecated 使用 {@link MessageService#colorize(String)}
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    public @NotNull Component colorize(@NotNull String text) {
        return messageService.colorize(text);
    }

    /**
     * @deprecated 使用 {@link MessageService#send(CommandSender, String)}
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    public void sendMessage(@NotNull CommandSender sender, @NotNull String message) {
        messageService.send(sender, message);
    }

    /**
     * @deprecated 使用 {@link MessageService#send(Player, String)}
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    public void sendMessage(@NotNull Player player, @NotNull String message) {
        messageService.send(player, message);
    }

    /**
     * @deprecated 使用 {@link MessageService#sendMessage(UUID, String)}
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    public boolean sendMessage(@NotNull UUID playerId, @NotNull String message) {
        return messageService.sendMessage(playerId, message);
    }

    /**
     * @deprecated 使用 {@link MessageService#sendMessage(Collection, String)}
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    public void sendMessage(@NotNull Collection<? extends Player> players, @NotNull String message) {
        messageService.sendMessage(players, message);
    }

    /**
     * @deprecated 使用 {@link MessageService#sendBroadcast(String)}
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    public void broadcast(@NotNull String message) {
        messageService.sendBroadcast(message);
    }

    /**
     * @deprecated 使用 {@link MessageService#broadcastFiltered(String, Predicate)}
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    public void broadcastFiltered(@NotNull String message, @NotNull Predicate<Player> filter) {
        messageService.broadcastFiltered(message, filter);
    }

    /**
     * @deprecated 使用 {@link MessageService#sendActionBar(Player, String)}
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    public void sendActionBar(@NotNull Player player, @NotNull String message) {
        messageService.sendActionBar(player, message);
    }

    /**
     * @deprecated 使用 {@link MessageService#sendActionBar(Collection, String)}
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    public void sendActionBar(@NotNull Collection<? extends Player> players, @NotNull String message) {
        messageService.sendActionBar(players, message);
    }

    /**
     * @deprecated 使用 {@link MessageService#showTitle(Player, String, String, int, int, int)}
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    public void showTitle(@NotNull Player player, @NotNull String title, @NotNull String subtitle,
                         int fadeIn, int stay, int fadeOut) {
        messageService.showTitle(player, title, subtitle, fadeIn, stay, fadeOut);
    }

    /**
     * @deprecated 使用 {@link MessageService#showTitle(Collection, String, String, int, int, int)}
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    public void showTitle(@NotNull Collection<? extends Player> players, @NotNull String title,
                         @NotNull String subtitle, int fadeIn, int stay, int fadeOut) {
        messageService.showTitle(players, title, subtitle, fadeIn, stay, fadeOut);
    }
}
