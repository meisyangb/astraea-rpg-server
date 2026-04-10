package cn.guangdian.rpgcore.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 统一命令框架
 * 
 * <p>提供所有GuangDian插件的统一命令注册和处理机制。</p>
 * <p>支持子命令、权限检查、自动补全。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class CommandFramework implements TabExecutor {

    private final JavaPlugin plugin;
    private final String commandName;
    private final Map<String, SubCommand> subCommands = new HashMap<>();
    private String noPermissionMessage = "§c你没有权限执行此命令！";
    private String playerOnlyMessage = "§c只有玩家可以执行此命令！";
    private String unknownCommandMessage = "§c未知的子命令！";

    public CommandFramework(JavaPlugin plugin, String commandName) {
        this.plugin = plugin;
        this.commandName = commandName;
    }

    /**
     * 注册子命令
     */
    public CommandFramework register(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
        for (String alias : subCommand.getAliases()) {
            subCommands.put(alias.toLowerCase(), subCommand);
        }
        return this;
    }

    /**
     * 注册到插件
     */
    public void registerToPlugin() {
        org.bukkit.command.PluginCommand command = plugin.getCommand(commandName);
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        } else {
            plugin.getLogger().warning("命令 " + commandName + " 未在 plugin.yml 中注册！");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(Component.text(unknownCommandMessage, NamedTextColor.RED));
            return true;
        }

        // 权限检查
        if (subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(Component.text(noPermissionMessage, NamedTextColor.RED));
            return true;
        }

        // 玩家检查
        if (subCommand.isPlayerOnly() && !(sender instanceof Player)) {
            sender.sendMessage(Component.text(playerOnlyMessage, NamedTextColor.RED));
            return true;
        }

        // 执行子命令
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subArgs);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 补全子命令名称
            String partial = args[0].toLowerCase();
            for (Map.Entry<String, SubCommand> entry : subCommands.entrySet()) {
                if (entry.getKey().startsWith(partial)) {
                    SubCommand sub = entry.getValue();
                    // 过滤掉没有权限的命令
                    if (sub.getPermission() == null || sender.hasPermission(sub.getPermission())) {
                        // 只添加主名称，不添加别名
                        if (entry.getKey().equals(sub.getName().toLowerCase())) {
                            completions.add(sub.getName());
                        }
                    }
                }
            }
        } else if (args.length > 1) {
            // 子命令的补全
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                List<String> subCompletions = subCommand.tabComplete(sender, subArgs);
                if (subCompletions != null) {
                    completions.addAll(subCompletions);
                }
            }
        }

        return completions;
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== " + plugin.getName() + " 帮助 ===", NamedTextColor.GOLD));
        
        for (SubCommand subCommand : subCommands.values()) {
            // 避免重复显示（别名和主名称）
            if (subCommands.get(subCommand.getName().toLowerCase()) == subCommand) {
                if (subCommand.getPermission() == null || sender.hasPermission(subCommand.getPermission())) {
                    sender.sendMessage(Component.text()
                        .append(Component.text("/" + commandName + " " + subCommand.getName(), NamedTextColor.YELLOW))
                        .append(Component.text(" - " + subCommand.getDescription(), NamedTextColor.GRAY))
                    );
                }
            }
        }
    }

    // ========== Setters ==========

    public CommandFramework setNoPermissionMessage(String message) {
        this.noPermissionMessage = message;
        return this;
    }

    public CommandFramework setPlayerOnlyMessage(String message) {
        this.playerOnlyMessage = message;
        return this;
    }

    public CommandFramework setUnknownCommandMessage(String message) {
        this.unknownCommandMessage = message;
        return this;
    }

    // ========== Builder方法 ==========

    /**
     * 快速创建简单子命令
     */
    public CommandFramework registerSimple(String name, String permission, String description, 
                                            BiConsumer<CommandSender, String[]> executor) {
        register(new SubCommand(name, permission, description) {
            @Override
            public void execute(CommandSender sender, String[] args) {
                executor.accept(sender, args);
            }
        });
        return this;
    }
}