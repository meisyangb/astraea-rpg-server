package cn.guangdian.rpgcore.gui.action;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.logging.Level;

public class ActionExecutor {

    public enum ActionType {
        MENU,
        COMMAND,
        CONSOLE,
        MESSAGE,
        CLOSE,
        SOUND,
        BROADCAST,
        NONE
    }

    private final Player player;
    private final MiniMessageService miniMessage;
    private final ExternalServiceIntegration externalServices;
    private final BiFunction<String, Player, String> placeholderResolver;

    private BiConsumer<ActionType, String> actionLogger;

    public ActionExecutor(@NotNull Player player) {
        this.player = player;
        RPGCore rpgCore = RPGCore.getInstance();
        this.miniMessage = rpgCore != null ? MiniMessageService.getInstance() : null;
        this.externalServices = rpgCore != null ? rpgCore.getExternalServices() : null;
        this.placeholderResolver = this::processPlaceholders;
    }

    public ActionExecutor(@NotNull Player player, @NotNull BiFunction<String, Player, String> placeholderResolver) {
        this.player = player;
        this.miniMessage = MiniMessageService.getInstance();
        this.externalServices = RPGCore.getInstance() != null ? RPGCore.getInstance().getExternalServices() : null;
        this.placeholderResolver = placeholderResolver;
    }

    public void execute(String action) {
        if (action == null || action.isEmpty()) {
            return;
        }

        String processedAction = placeholderResolver.apply(action, player);
        ActionType type = parseActionType(processedAction);
        String value = extractActionValue(processedAction);

        if (actionLogger != null) {
            actionLogger.accept(type, value);
        }

        switch (type) {
            case MENU -> executeMenu(value);
            case COMMAND -> executeCommand(value, false);
            case CONSOLE -> executeCommand(value, true);
            case MESSAGE -> executeMessage(value);
            case CLOSE -> executeClose();
            case SOUND -> executeSound(value);
            case BROADCAST -> executeBroadcast(value);
            case NONE -> executeCommand(processedAction, true);
        }
    }

    public void executeAll(List<String> actions) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        for (String action : actions) {
            execute(action);
        }
    }

    private ActionType parseActionType(String action) {
        if (action == null || action.isEmpty()) {
            return ActionType.NONE;
        }
        String lower = action.toLowerCase(Locale.ROOT).trim();
        if (lower.startsWith("menu:")) return ActionType.MENU;
        if (lower.startsWith("command:")) return ActionType.COMMAND;
        if (lower.startsWith("console:")) return ActionType.CONSOLE;
        if (lower.startsWith("message:")) return ActionType.MESSAGE;
        if (lower.startsWith("close")) return ActionType.CLOSE;
        if (lower.startsWith("sound:")) return ActionType.SOUND;
        if (lower.startsWith("broadcast:")) return ActionType.BROADCAST;
        return ActionType.NONE;
    }

    private String extractActionValue(String action) {
        if (action == null) return "";
        int colonIndex = action.indexOf(':');
        if (colonIndex == -1 || colonIndex == action.length() - 1) {
            return "";
        }
        return action.substring(colonIndex + 1).trim();
    }

    private void executeMenu(String menuName) {
        if (menuName == null || menuName.isEmpty()) return;
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null && rpgCore.getMenuService() != null) {
            rpgCore.getMenuService().openMenu(player, menuName);
        }
    }

    private void executeCommand(String command, boolean asConsole) {
        if (command == null || command.isEmpty()) return;
        String[] parts = command.split("&&");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            // 使用 RPGCore SyncScheduler 执行命令
            RPGCore rpgCore = RPGCore.getInstance();
            Runnable commandTask;
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("mm ")) {
                commandTask = () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), trimmed);
            } else {
                commandTask = () -> {
                    if (asConsole) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), trimmed);
                    } else {
                        player.performCommand(trimmed);
                    }
                };
            }

            if (rpgCore != null) {
                rpgCore.getScheduler().runSync(commandTask);
            } else {
                // 降级：使用 Paper GlobalRegionScheduler
                Bukkit.getGlobalRegionScheduler().run(RPGCore.getInstance(), scheduledTask -> commandTask.run());
            }
        }
    }

    private void executeMessage(String message) {
        if (message == null || message.isEmpty()) return;
        if (miniMessage != null) {
            Component component = miniMessage.colorize(message);
            player.sendMessage(component);
        } else {
            // 降级：使用 Bukkit 原生方法，但记录警告
            Bukkit.getLogger().log(Level.WARNING, "[ActionExecutor] MiniMessageService 不可用，使用原始消息格式");
            player.sendMessage(message);
        }
    }

    private void executeClose() {
        player.closeInventory();
    }

    private void executeSound(String soundConfig) {
        if (soundConfig == null || soundConfig.isEmpty()) return;
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null && rpgCore.getSoundService() != null) {
            String[] parts = soundConfig.split(" ");
            String soundName = parts[0];
            float volume = 1.0f;
            float pitch = 1.0f;
            try {
                if (parts.length > 1) {
                    volume = Float.parseFloat(parts[1]);
                }
                if (parts.length > 2) {
                    pitch = Float.parseFloat(parts[2]);
                }
            } catch (NumberFormatException e) {
                Bukkit.getLogger().log(Level.WARNING, "[ActionExecutor] 无效的音效参数: " + soundConfig + " - " + e.getMessage());
                return;
            }
            rpgCore.getSoundService().playSound(player, soundName, volume, pitch);
        }
    }

    private void executeBroadcast(String message) {
        if (message == null || message.isEmpty()) return;
        if (miniMessage != null) {
            Component component = miniMessage.colorize(message);
            Bukkit.broadcast(component);
        } else {
            Bukkit.broadcastMessage(message);
        }
    }

    private String processPlaceholders(String text, Player player) {
        if (text == null) return "";
        text = text.replace("%player%", player.getName())
                .replace("%player_name%", player.getName())
                .replace("%displayname%", player.getDisplayName())
                .replace("%player_level%", String.valueOf(player.getLevel()))
                .replace("%player_health%", String.valueOf((int) player.getHealth()))
                .replace("%player_max_health%", String.valueOf((int) player.getMaxHealth()))
                .replace("%player_food%", String.valueOf(player.getFoodLevel()))
                .replace("%player_world%", player.getWorld().getName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%max_players%", String.valueOf(Bukkit.getMaxPlayers()));

        if (externalServices != null) {
            text = externalServices.parsePlaceholders(player, text);
        }

        return text;
    }

    public ActionExecutor withLogger(BiConsumer<ActionType, String> logger) {
        this.actionLogger = logger;
        return this;
    }

    public Player getPlayer() {
        return player;
    }

    public static void executeAction(Player player, String action) {
        new ActionExecutor(player).execute(action);
    }

    public static void executeActions(Player player, List<String> actions) {
        new ActionExecutor(player).executeAll(actions);
    }
}