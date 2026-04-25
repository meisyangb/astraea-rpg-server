package cn.guangdian.menu;

import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 传统菜单命令处理器 (降级处理 - 当 RPGCore 不可用时使用)
 */
public class LegacyMenuCommand implements CommandExecutor, TabCompleter {

    private final GuangDianMenu plugin;
    private final MiniMessageService miniMessage;

    public LegacyMenuCommand(GuangDianMenu plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessageService.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(miniMessage.red("该命令只能由玩家执行!"));
            return true;
        }

        if (!player.hasPermission("guangdian.menu.use")) {
            player.sendMessage(miniMessage.red("您没有权限执行此操作!"));
            return true;
        }

        String menuName = args.length > 0 ? args[0].toLowerCase() : plugin.getConfig().getString("default-menu", "main");
        plugin.openMenu(player, menuName);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return plugin.getMenuNamesAPI().stream()
                    .filter(name -> name.startsWith(partial))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
