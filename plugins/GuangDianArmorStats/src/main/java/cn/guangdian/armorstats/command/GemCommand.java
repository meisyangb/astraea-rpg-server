package cn.guangdian.armorstats.command;

import cn.guangdian.armorstats.gui.GemInlayGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GemCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以使用此命令!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("inlay") || args[0].equalsIgnoreCase("gui")) {
            GemInlayGUI gui = new GemInlayGUI(player);
            gui.open();
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            player.sendMessage("§6===== 宝石镶嵌帮助 =====");
            player.sendMessage("§e/gem §7- 打开镶嵌界面");
            player.sendMessage("§e/gem inlay §7- 打开镶嵌界面");
            return true;
        }

        player.sendMessage("§c用法: /gem");
        return true;
    }
}
