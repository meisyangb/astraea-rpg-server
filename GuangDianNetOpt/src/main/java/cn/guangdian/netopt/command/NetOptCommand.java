package cn.guangdian.netopt.command;

import cn.guangdian.netopt.GuangDianNetOpt;
import cn.guangdian.netopt.manager.EntityOptimizer;
import cn.guangdian.netopt.manager.PacketCacheManager;
import cn.guangdian.netopt.config.NetOptConfig;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * 命令处理器
 */
public class NetOptCommand implements CommandExecutor {

    private final GuangDianNetOpt plugin;

    public NetOptCommand(GuangDianNetOpt plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "status":
                sendStatus(sender);
                break;
            case "stats":
                sendStats(sender);
                break;
            case "optimize":
                if (plugin.getEntityOptimizer() != null) {
                    plugin.getEntityOptimizer().disableEntityAI();
                    sender.sendMessage(ChatColor.GREEN + "实体优化已重新应用！");
                }
                break;
            case "reload":
                plugin.getNetOptConfig().reload();
                sender.sendMessage(ChatColor.GREEN + "配置已重载");
                break;
            default:
                sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== GuangDianNetOpt 实体优化器 ===");
        sender.sendMessage(ChatColor.AQUA + "职责：禁用静态实体AI，减少CPU和网络");
        sender.sendMessage(ChatColor.GRAY + "区块优化由GuangDianDynamicView负责");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "/netopt status" + ChatColor.WHITE + " - 查看优化状态");
        sender.sendMessage(ChatColor.YELLOW + "/netopt stats" + ChatColor.WHITE + " - 查看统计数据");
        sender.sendMessage(ChatColor.YELLOW + "/netopt optimize" + ChatColor.WHITE + " - 强制重新优化");
        sender.sendMessage(ChatColor.YELLOW + "/netopt reload" + ChatColor.WHITE + " - 重载配置");
    }

    private void sendStatus(CommandSender sender) {
        NetOptConfig config = plugin.getNetOptConfig();

        sender.sendMessage(ChatColor.GOLD + "=== 优化状态 ===");
        sender.sendMessage(ChatColor.WHITE + "实体AI优化: " + 
            (config.isEntityOptEnabled() ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        sender.sendMessage(ChatColor.WHITE + "  - 禁用AI: " + config.isDisableMobAI());
        sender.sendMessage(ChatColor.WHITE + "  - 禁用移动: " + config.isDisableMobMovement());
        
        sender.sendMessage(ChatColor.WHITE + "数据包缓存: " + 
            (config.isPacketCacheEnabled() ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
    }

    private void sendStats(CommandSender sender) {
        EntityOptimizer entities = plugin.getEntityOptimizer();
        PacketCacheManager packets = plugin.getPacketCache();

        sender.sendMessage(ChatColor.GOLD + "=== 优化统计 ===");
        
        if (entities != null) {
            sender.sendMessage(ChatColor.WHITE + "实体优化:");
            sender.sendMessage(ChatColor.WHITE + "  - AI已禁用: " + ChatColor.GREEN + entities.getAiDisabled());
            sender.sendMessage(ChatColor.WHITE + "  - 移动已禁用: " + ChatColor.GREEN + entities.getMovementDisabled());
            sender.sendMessage(ChatColor.AQUA + "效果: 减少CPU计算和位置更新包");
        }
        
        if (packets != null) {
            sender.sendMessage(ChatColor.WHITE + "数据包缓存:");
            sender.sendMessage(ChatColor.WHITE + "  - 缓存命中: " + ChatColor.GREEN + packets.getCacheHits());
            sender.sendMessage(ChatColor.WHITE + "  - 缓存未命中: " + ChatColor.RED + packets.getCacheMisses());
            sender.sendMessage(ChatColor.WHITE + "  - 节省包数: " + ChatColor.GREEN + packets.getPacketsSaved());
        }
    }
}