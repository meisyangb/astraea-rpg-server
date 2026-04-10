package cn.guangdian.world.command;

import cn.guangdian.world.GuangDianWorld;
import cn.guangdian.world.model.GDWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GWorldTPCommand implements CommandExecutor, TabCompleter {

    private final GuangDianWorld plugin;

    public GWorldTPCommand(GuangDianWorld plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("此命令只能由玩家执行!", NamedTextColor.RED));
            return true;
        }

        // 权限检查
        if (!player.hasPermission("guangdian.world.tp")) {
            player.sendMessage(Component.text("你没有权限执行此命令!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("用法: /gworldtp <世界名>", NamedTextColor.RED));
            return true;
        }

        String worldName = args[0];
        if (plugin.getWorldManager().teleportToWorld(player, worldName)) {
            String displayName = plugin.getWorldManager().getWorld(worldName).getDisplayName();
            player.sendMessage(Component.text("已传送到世界: " + displayName, NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("传送失败，世界不存在或未加载!", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(plugin.getWorldManager().getWorldNames());
            String lastArg = args[0].toLowerCase();
            completions.removeIf(s -> !s.toLowerCase().startsWith(lastArg));
        }
        return completions;
    }
}
