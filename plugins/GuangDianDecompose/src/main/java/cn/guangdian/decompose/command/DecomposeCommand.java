package cn.guangdian.decompose.command;

import cn.guangdian.decompose.GuangDianDecompose;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DecomposeCommand implements CommandExecutor {

    private final GuangDianDecompose plugin;

    public DecomposeCommand(GuangDianDecompose plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令只能由玩家执行!");
            return true;
        }

        if (!player.hasPermission("guangdian.decompose.use")) {
            player.sendMessage(plugin.getConfig().getString("messages.no-permission", "<red>你没有权限使用分解功能!").replace("&", "§"));
            return true;
        }

        plugin.getDecomposeGUI().open(player);
        return true;
    }
}
