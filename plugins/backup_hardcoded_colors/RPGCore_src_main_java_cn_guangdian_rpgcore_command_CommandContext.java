package cn.guangdian.rpgcore.command;

import cn.guangdian.rpgcore.message.UnifiedMessageService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 命令上下文 - 封装命令执行时的所有信息
 *
 * <p>提供类型安全的参数解析和便捷的消息发送方法。</p>
 *
 * <h2>使用示例:</h2>
 * <pre>{@code
 * @SubCommand(name="give")
 * public void give(CommandContext ctx) {
 *     // 获取参数
 *     Player target = ctx.getPlayerArg(0);
 *     long amount = ctx.getLongArg(1);
 *
 *     // 发送消息
 *     ctx.sendMessage("&a成功给予 " + target.getName() + " " + amount + " 点数");
 *     ctx.sendTo(target, "&a你获得了 " + amount + " 点数!");
 * }
 * }</pre>
 *
 * @author Astraea RPG Team
 * @since 1.1.0
 */
public final class CommandContext {

    private final CommandSender sender;
    private final String[] args;
    private final UnifiedMessageService msg;

    public CommandContext(@NotNull CommandSender sender, @NotNull String[] args) {
        this.sender = sender;
        this.args = args;
        this.msg = UnifiedMessageService.getInstance();
    }

    // ==================== 基本信息 ====================

    /**
     * 获取命令发送者
     */
    public @NotNull CommandSender getSender() {
        return sender;
    }

    /**
     * 获取玩家发送者 (如果不是玩家返回 null)
     */
    public @Nullable Player getPlayer() {
        return sender instanceof Player ? (Player) sender : null;
    }

    /**
     * 获取必需的玩家发送者 (如果不是玩家抛出异常)
     */
    public @NotNull Player requirePlayer() {
        if (!(sender instanceof Player)) {
            throw new CommandException("此命令只能由玩家执行!");
        }
        return (Player) sender;
    }

    /**
     * 获取参数数组
     */
    public @NotNull String[] getArgs() {
        return args;
    }

    /**
     * 获取参数数量
     */
    public int getArgCount() {
        return args.length;
    }

    // ==================== 参数解析 ====================

    /**
     * 获取原始参数
     */
    public @NotNull String getStringArg(int index) {
        if (index < 0 || index >= args.length) {
            throw new CommandException("缺少参数 #" + (index + 1));
        }
        return args[index];
    }

    /**
     * 获取可选的字符串参数
     */
    public @Nullable String getStringArgOrDefault(int index, String defaultValue) {
        if (index < 0 || index >= args.length) {
            return defaultValue;
        }
        return args[index];
    }

    /**
     * 获取整数参数
     */
    public int getIntArg(int index) {
        try {
            return Integer.parseInt(getStringArg(index));
        } catch (NumberFormatException e) {
            throw new CommandException("参数 #" + (index + 1) + " 必须是整数!");
        }
    }

    /**
     * 获取长整数参数
     */
    public long getLongArg(int index) {
        try {
            return Long.parseLong(getStringArg(index));
        } catch (NumberFormatException e) {
            throw new CommandException("参数 #" + (index + 1) + " 必须是整数!");
        }
    }

    /**
     * 获取浮点数参数
     */
    public double getDoubleArg(int index) {
        try {
            return Double.parseDouble(getStringArg(index));
        } catch (NumberFormatException e) {
            throw new CommandException("参数 #" + (index + 1) + " 必须是数字!");
        }
    }

    /**
     * 获取布尔参数 (true/yes/on/1 = true)
     */
    public boolean getBooleanArg(int index) {
        String value = getStringArg(index).toLowerCase();
        return value.equals("true") || value.equals("yes") || value.equals("on") || value.equals("1");
    }

    /**
     * 获取玩家参数 (在线玩家)
     */
    public @NotNull Player getPlayerArg(int index) {
        String name = getStringArg(index);
        Player player = Bukkit.getPlayer(name);
        if (player == null) {
            throw new CommandException("找不到在线玩家: " + name);
        }
        return player;
    }

    /**
     * 获取离线玩家参数
     */
    public @NotNull OfflinePlayer getOfflinePlayerArg(int index) {
        String name = getStringArg(index);
        OfflinePlayer player = Bukkit.getOfflinePlayer(name);
        if (player == null) {
            throw new CommandException("找不到玩家: " + name);
        }
        return player;
    }

    /**
     * 获取 UUID 参数
     */
    public @NotNull UUID getUUIDArg(int index) {
        try {
            return UUID.fromString(getStringArg(index));
        } catch (IllegalArgumentException e) {
            throw new CommandException("参数 #" + (index + 1) + " 必须是有效的 UUID!");
        }
    }

    // ==================== 消息发送 ====================

    /**
     * 发送消息给命令发送者
     */
    public void sendMessage(@NotNull String message) {
        msg.sendMessage(sender, message);
    }

    /**
     * 发送消息给指定玩家
     */
    public void sendTo(@NotNull Player player, @NotNull String message) {
        msg.sendMessage(player, message);
    }

    /**
     * 发送错误消息
     */
    public void sendError(@NotNull String message) {
        msg.sendMessage(sender, "&c" + message);
    }

    /**
     * 发送成功消息
     */
    public void sendSuccess(@NotNull String message) {
        msg.sendMessage(sender, "&a" + message);
    }

    /**
     * 发送警告消息
     */
    public void sendWarning(@NotNull String message) {
        msg.sendMessage(sender, "&e" + message);
    }

    /**
     * 发送 ActionBar 消息
     */
    public void sendActionBar(@NotNull String message) {
        if (sender instanceof Player) {
            msg.sendActionBar((Player) sender, message);
        }
    }

    // ==================== 权限检查 ====================

    /**
     * 检查是否有权限
     */
    public boolean hasPermission(@NotNull String permission) {
        return sender.hasPermission(permission);
    }

    /**
     * 要求有权限 (没有则抛出异常)
     */
    public void requirePermission(@NotNull String permission) {
        if (!hasPermission(permission)) {
            throw new CommandException("没有权限执行此操作!");
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 获取剩余参数 (从指定索引开始)
     */
    public @NotNull String[] getRemainingArgs(int startIndex) {
        if (startIndex >= args.length) {
            return new String[0];
        }
        String[] remaining = new String[args.length - startIndex];
        System.arraycopy(args, startIndex, remaining, 0, remaining.length);
        return remaining;
    }

    /**
     * 将所有剩余参数拼接为字符串
     */
    public @NotNull String getJoinedArgs() {
        return String.join(" ", args);
    }

    /**
     * 从指定索引开始拼接参数
     */
    public @NotNull String getJoinedArgs(int startIndex) {
        if (startIndex >= args.length) {
            return "";
        }
        String[] remaining = getRemainingArgs(startIndex);
        return String.join(" ", remaining);
    }
}
