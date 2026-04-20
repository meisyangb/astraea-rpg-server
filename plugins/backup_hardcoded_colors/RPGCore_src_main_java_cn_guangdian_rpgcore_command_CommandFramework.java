package cn.guangdian.rpgcore.command;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.UnifiedMessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

/**
 * 命令框架 - RPGCore 核心框架 (注解驱动版本)
 *
 * <p>提供注解驱动的命令系统，支持子命令、权限检查、参数自动解析。</p>
 *
 * <h2>使用示例:</h2>
 * <pre>{@code
 * @CommandInfo(name="points", description="点数管理", permission="guangdian.points.use")
 * public class PointsCommand extends BaseCommand {
 *
 *     @SubCommand(name="give", permission="guangdian.points.admin")
 *     @Description("给予玩家点数")
 *     public void give(CommandContext ctx) {
 *         Player target = ctx.getPlayerArg(0);
 *         long amount = ctx.getLongArg(1);
 *         // 业务逻辑...
 *     }
 *
 *     @SubCommand(name="balance")
 *     @Description("查看余额")
 *     public void balance(CommandContext ctx) {
 *         Player player = ctx.getSender().asPlayer();
 *         // 业务逻辑...
 *     }
 * }
 *
 * // 注册命令
 * CommandFramework framework = CommandFramework.getInstance();
 * framework.registerCommand(new PointsCommand());
 * }</pre>
 *
 * @author Astraea RPG Team
 * @since 1.1.0
 */
public final class CommandFramework implements CommandExecutor, TabCompleter {

    private static CommandFramework instance;

    private final Map<String, BaseCommand> commands;
    private final Logger logger;
    private final UnifiedMessageService msg;

    private CommandFramework() {
        this.commands = new HashMap<>();
        RPGCore rpgCore = RPGCore.getInstance();
        this.logger = rpgCore != null ? rpgCore.getLogger() : Logger.getLogger("CommandFramework");
        this.msg = UnifiedMessageService.getInstance();
    }

    public static synchronized CommandFramework getInstance() {
        if (instance == null) {
            instance = new CommandFramework();
        }
        return instance;
    }

    /**
     * 注册命令
     */
    public void registerCommand(@NotNull BaseCommand command) {
        CommandInfo info = command.getClass().getAnnotation(CommandInfo.class);
        if (info == null) {
            logger.severe("[CommandFramework] 命令类缺少 @CommandInfo 注解: " + command.getClass().getName());
            return;
        }

        String commandName = info.name().toLowerCase();
        commands.put(commandName, command);

        // 注册到 Bukkit (Paper 1.21.6 兼容)
        org.bukkit.plugin.Plugin plugin = RPGCore.getInstance();
        if (plugin != null) {
            org.bukkit.command.PluginCommand bukkitCommand = plugin.getServer().getPluginCommand(commandName);
            if (bukkitCommand != null) {
                bukkitCommand.setExecutor(this);
                bukkitCommand.setTabCompleter(this);
                logger.info("[CommandFramework] 已注册命令: /" + commandName);
            } else {
                logger.warning("[CommandFramework] 未在 plugin.yml 中找到命令: " + commandName);
            }
        }
    }

    /**
     * 注销命令
     */
    public void unregisterCommand(@NotNull String commandName) {
        commands.remove(commandName.toLowerCase());
    }

    /**
     * 获取所有已注册的命令
     */
    public @NotNull Set<String> getRegisteredCommands() {
        return Collections.unmodifiableSet(commands.keySet());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String commandName = command.getName().toLowerCase();
        BaseCommand baseCommand = commands.get(commandName);

        if (baseCommand == null) {
            msg.sendMessage(sender, "&c未知命令!");
            return true;
        }

        // 检查主命令权限
        CommandInfo info = baseCommand.getClass().getAnnotation(CommandInfo.class);
        if (info != null && !info.permission().isEmpty()) {
            if (!sender.hasPermission(info.permission())) {
                msg.sendMessage(sender, "&c没有权限执行此命令!");
                return true;
            }
        }

        // 检查是否仅玩家可用
        if (info != null && info.playerOnly() && !(sender instanceof Player)) {
            msg.sendMessage(sender, "&c此命令只能由玩家执行!");
            return true;
        }

        // 如果没有参数，显示帮助
        if (args.length == 0) {
            baseCommand.showHelp(sender);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        // 查找子命令
        Method subCommandMethod = baseCommand.getSubCommandMethod(subCommandName);
        if (subCommandMethod == null) {
            msg.sendMessage(sender, "&c未知子命令: " + subCommandName);
            baseCommand.showHelp(sender);
            return true;
        }

        // 检查子命令权限
        SubCommand subCmdAnnotation = subCommandMethod.getAnnotation(SubCommand.class);
        if (subCmdAnnotation != null && !subCmdAnnotation.permission().isEmpty()) {
            if (!sender.hasPermission(subCmdAnnotation.permission())) {
                msg.sendMessage(sender, "&c没有权限执行此子命令!");
                return true;
            }
        }

        // 检查是否仅玩家可用
        if (subCmdAnnotation != null && subCmdAnnotation.playerOnly() && !(sender instanceof Player)) {
            msg.sendMessage(sender, "&c此子命令只能由玩家执行!");
            return true;
        }

        // 检查参数数量
        if (subCmdAnnotation != null) {
            if (subArgs.length < subCmdAnnotation.minArgs()) {
                msg.sendMessage(sender, "&c参数不足! 需要至少 " + subCmdAnnotation.minArgs() + " 个参数");
                return true;
            }
            if (subCmdAnnotation.maxArgs() != -1 && subArgs.length > subCmdAnnotation.maxArgs()) {
                msg.sendMessage(sender, "&c参数过多! 最多允许 " + subCmdAnnotation.maxArgs() + " 个参数");
                return true;
            }
        }

        // 构建命令上下文
        CommandContext context = new CommandContext(sender, subArgs);

        // 执行子命令
        try {
            subCommandMethod.invoke(baseCommand, context);
        } catch (CommandException e) {
            msg.sendMessage(sender, "&c" + e.getMessage());
        } catch (Exception e) {
            logger.severe("[CommandFramework] 执行命令失败: /" + commandName + " " + subCommandName);
            e.printStackTrace();
            msg.sendMessage(sender, "&c命令执行失败: " + e.getCause().getMessage());
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        String commandName = command.getName().toLowerCase();
        BaseCommand baseCommand = commands.get(commandName);

        if (baseCommand == null) {
            return Collections.emptyList();
        }

        // 第一级参数：子命令名称
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();

            for (Method method : baseCommand.getSubCommandMethods()) {
                SubCommand subCmd = method.getAnnotation(SubCommand.class);
                if (subCmd != null) {
                    // 检查权限
                    if (!subCmd.permission().isEmpty() && !sender.hasPermission(subCmd.permission())) {
                        continue;
                    }

                    String subName = subCmd.name().toLowerCase();
                    if (subName.startsWith(partial)) {
                        completions.add(subName);
                    }
                }
            }

            Collections.sort(completions);
            return completions;
        }

        // 更深层的参数：调用子命令的 tab complete 方法
        if (args.length > 1) {
            String subCommandName = args[0].toLowerCase();
            Method subCommandMethod = baseCommand.getSubCommandMethod(subCommandName);

            if (subCommandMethod != null) {
                try {
                    String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                    CommandContext context = new CommandContext(sender, subArgs);
                    return baseCommand.onTabComplete(subCommandMethod, context);
                } catch (Exception e) {
                    logger.warning("[CommandFramework] TabComplete 失败: " + e.getMessage());
                }
            }
        }

        return Collections.emptyList();
    }
}
