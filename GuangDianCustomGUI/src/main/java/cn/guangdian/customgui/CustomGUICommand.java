package cn.guangdian.customgui;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CustomGUICommand implements CommandExecutor {

    private final GuangDianCustomGUI plugin;

    public CustomGUICommand(GuangDianCustomGUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("此命令仅限玩家使用!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "backpack" -> {
                plugin.openBackpack(player);
                player.sendMessage(Component.text("已打开自定义背包!", NamedTextColor.GREEN));
            }
            case "reload" -> {
                if (!player.hasPermission("customgui.reload")) {
                    player.sendMessage(Component.text("权限不足!", NamedTextColor.RED));
                    return true;
                }
                plugin.reloadConfig();
                player.sendMessage(Component.text("配置已重新加载!", NamedTextColor.GREEN));
            }
            case "resourcepack" -> {
                if (args.length > 1 && args[1].equalsIgnoreCase("send")) {
                    if (!player.hasPermission("customgui.resourcepack.send")) {
                        player.sendMessage(Component.text("权限不足!", NamedTextColor.RED));
                        return true;
                    }
                    String url = args.length > 2 ? args[2] : plugin.getResourcePackManager().getResourcePackUrl();
                    if (url != null && !url.isEmpty()) {
                        plugin.getResourcePackManager().forceSendToPlayer(player, url);
                        player.sendMessage(Component.text("资源包已发送!", NamedTextColor.GREEN));
                    } else {
                        player.sendMessage(Component.text("未配置资源包URL!", NamedTextColor.RED));
                    }
                } else {
                    player.sendMessage(Component.text("用法: /customgui resourcepack send [url]", NamedTextColor.YELLOW));
                }
            }
            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("========== GuangDianCustomGUI ==========", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/customgui backpack - 打开自定义背包", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/customgui reload - 重新加载配置 (需要权限)", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/customgui resourcepack send [url] - 发送资源包", NamedTextColor.WHITE));
        player.sendMessage(Component.text("========================================", NamedTextColor.GOLD));
    }
}
