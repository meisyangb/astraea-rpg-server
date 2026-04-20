package cn.guangdian.socket.command;

import cn.guangdian.socket.GuangDianSocket;
import cn.guangdian.socket.gui.SocketInlayGUI;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SocketCommand implements CommandExecutor {

    private final GuangDianSocket plugin;
    private final MiniMessageService miniMessage;

    public SocketCommand(GuangDianSocket plugin) {
        this.plugin = plugin;
        RPGCore rpgCore = RPGCore.getInstance();
        this.miniMessage = rpgCore != null ? rpgCore.getMiniMessageService() : null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            if (miniMessage != null) {
                sender.sendMessage(miniMessage.red("只有玩家可以使用此命令!"));
            } else {
                sender.sendMessage("只有玩家可以使用此命令!");
            }
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            SocketInlayGUI gui = new SocketInlayGUI(plugin, player);
            gui.open();
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            if (miniMessage != null) {
                player.sendMessage(miniMessage.gold("===== 宝石镶嵌帮助 ====="));
                player.sendMessage(miniMessage.colorize("<yellow>/socket <gray>- 打开镶嵌界面"));
                player.sendMessage(miniMessage.colorize("<yellow>/socket gui <gray>- 打开镶嵌界面"));
                player.sendMessage(miniMessage.colorize("<yellow>/socket reload <gray>- 重载配置"));
            } else {
                player.sendMessage("===== 宝石镶嵌帮助 =====");
                player.sendMessage("/socket - 打开镶嵌界面");
                player.sendMessage("/socket gui - 打开镶嵌界面");
                player.sendMessage("/socket reload - 重载配置");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("guangdiansocket.admin")) {
                if (miniMessage != null) {
                    player.sendMessage(miniMessage.red("你没有权限使用此命令!"));
                } else {
                    player.sendMessage("你没有权限使用此命令!");
                }
                return true;
            }

            plugin.reloadConfig();

            // 重新初始化解析器
            cn.guangdian.socket.parser.SocketParser.initialize(
                plugin.getConfig().getConfigurationSection("socket_patterns"),
                plugin.getConfig().getConfigurationSection("gem_types")
            );

            if (miniMessage != null) {
                player.sendMessage(miniMessage.green("GuangDianSocket 配置已重载!"));
            } else {
                player.sendMessage("GuangDianSocket 配置已重载!");
            }
            return true;
        }

        if (miniMessage != null) {
            player.sendMessage(miniMessage.red("用法: /socket [gui|help|reload]"));
        } else {
            player.sendMessage("用法: /socket [gui|help|reload]");
        }
        return true;
    }
}
