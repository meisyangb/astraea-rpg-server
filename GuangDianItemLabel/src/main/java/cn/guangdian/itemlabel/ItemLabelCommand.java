package cn.guangdian.itemlabel;

import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ItemLabelCommand implements CommandExecutor, TabCompleter {

    private final GuangDianItemLabel plugin;
    private final MiniMessageService miniMessage;

    public ItemLabelCommand(GuangDianItemLabel plugin) {
        this.plugin = plugin;
        this.miniMessage = plugin.getMiniMessage();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "clear":
                return handleClear(sender);
            case "reload":
                return handleReload(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleClear(CommandSender sender) {
        if (!sender.hasPermission("guangdianitemlabel.admin")) {
            sender.sendMessage(miniMessage.red("你没有权限执行此命令！"));
            return true;
        }

        int count = plugin.getItemLabelManager().getLabelCount();
        plugin.getItemLabelManager().clearAllLabels();
        sender.sendMessage(miniMessage.green("已清除所有物品标签 (共 " + count + " 个)"));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("guangdianitemlabel.admin")) {
            sender.sendMessage(miniMessage.red("你没有权限执行此命令！"));
            return true;
        }

        plugin.reloadConfig();
        sender.sendMessage(miniMessage.green("配置已重载！"));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(miniMessage.colorize("<yellow>===== GuangDianItemLabel 命令 ====="));
        sender.sendMessage(miniMessage.colorize("<green>/itemlabel clear <gray>- 清除所有物品标签"));
        sender.sendMessage(miniMessage.colorize("<green>/itemlabel reload <gray>- 重载配置文件"));
        sender.sendMessage(miniMessage.colorize("<green>/itemlabel help <gray>- 显示帮助信息"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("clear", "reload", "help");
        }
        return Collections.emptyList();
    }
}
