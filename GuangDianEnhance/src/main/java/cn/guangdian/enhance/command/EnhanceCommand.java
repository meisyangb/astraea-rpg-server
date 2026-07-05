package cn.guangdian.enhance.command;

import cn.guangdian.enhance.GuangDianEnhance;
import cn.guangdian.enhance.gui.EnhanceGUI;
import cn.guangdian.enhance.stone.EnhanceStoneType;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 强化系统命令 — 参考 GuangDianSocket 的 SocketCommand 风格
 * 每次执行 /enhance 创建新的 per-player GUI 实例
 */
public class EnhanceCommand implements CommandExecutor {

    private final GuangDianEnhance plugin;
    private final MiniMessageService miniMessage;

    public EnhanceCommand(GuangDianEnhance plugin) {
        this.plugin = plugin;
        RPGCore rpgCore = RPGCore.getInstance();
        this.miniMessage = rpgCore != null ? rpgCore.getMiniMessageService() : null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("只有玩家可以使用此命令!");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            EnhanceGUI gui = new EnhanceGUI(plugin, player);
            gui.open();
            return true;
        }

        if (args[0].equalsIgnoreCase("stones")) {
            player.sendMessage(Component.text("========== 强化石列表 ==========", NamedTextColor.GOLD));
            for (EnhanceStoneType t : EnhanceStoneType.values()) {
                player.sendMessage(Component.text(t.getDisplayName() + " - " + t.getDescription(), NamedTextColor.YELLOW));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            player.sendMessage(Component.text("===== 强化系统帮助 =====", NamedTextColor.GOLD));
            player.sendMessage(Component.text("/enhance", NamedTextColor.YELLOW)
                .append(Component.text(" - 打开强化界面", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/enhance gui", NamedTextColor.YELLOW)
                .append(Component.text(" - 打开强化界面", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/enhance stones", NamedTextColor.YELLOW)
                .append(Component.text(" - 强化石列表", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/enhance reload", NamedTextColor.YELLOW)
                .append(Component.text(" - 重载配置", NamedTextColor.GRAY)));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("guangdian.enhance.admin")) {
                player.sendMessage(Component.text("你没有权限使用此命令!", NamedTextColor.RED));
                return true;
            }
            plugin.getEnhanceConfig().load();
            player.sendMessage(Component.text("GuangDianEnhance 配置已重载!", NamedTextColor.GREEN));
            return true;
        }

        if (args[0].equalsIgnoreCase("setlevel")) {
            if (!player.hasPermission("guangdian.enhance.admin")) {
                player.sendMessage(Component.text("你没有权限使用此命令!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(Component.text("用法: /enhance setlevel <等级>", NamedTextColor.RED));
                return true;
            }
            try {
                int level = Integer.parseInt(args[1]);
                org.bukkit.inventory.ItemStack item = player.getInventory().getItemInMainHand();
                org.bukkit.inventory.ItemStack modified = plugin.getEnhanceStorage().setLevel(item, level);
                player.getInventory().setItemInMainHand(modified);
                player.updateInventory();
                player.sendMessage(Component.text("设置强化等级: +" + level, NamedTextColor.GREEN));
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("请输入数字", NamedTextColor.RED));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            if (!player.hasPermission("guangdian.enhance.admin")) {
                player.sendMessage(Component.text("你没有权限使用此命令!", NamedTextColor.RED));
                return true;
            }
            org.bukkit.inventory.ItemStack item = player.getInventory().getItemInMainHand();
            org.bukkit.inventory.ItemStack cleared = plugin.getEnhanceStorage().clearEnhanceData(item);
            player.getInventory().setItemInMainHand(cleared);
            player.updateInventory();
            player.sendMessage(Component.text("已重置强化等级", NamedTextColor.GREEN));
            return true;
        }

        player.sendMessage(Component.text("用法: /enhance [gui|stones|help|reload|setlevel|reset]", NamedTextColor.RED));
        return true;
    }
}
