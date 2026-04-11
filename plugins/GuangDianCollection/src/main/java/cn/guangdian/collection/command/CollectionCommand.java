package cn.guangdian.collection.command;

import cn.guangdian.collection.GuangDianCollection;
import cn.guangdian.collection.api.CollectionService;
import cn.guangdian.collection.gui.CollectionGUIListener;
import cn.guangdian.collection.model.CollectionSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CollectionCommand implements CommandExecutor, TabCompleter {
    
    private final GuangDianCollection plugin;
    private final CollectionService collectionService;
    private final CollectionGUIListener guiListener;
    
    public CollectionCommand(GuangDianCollection plugin, CollectionService collectionService, CollectionGUIListener guiListener) {
        this.plugin = plugin;
        this.collectionService = collectionService;
        this.guiListener = guiListener;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("该命令只能由玩家执行").color(NamedTextColor.RED));
                return true;
            }
            guiListener.openMainGUI(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "open":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("该命令只能由玩家执行").color(NamedTextColor.RED));
                    return true;
                }
                guiListener.openMainGUI(player);
                return true;
                
            case "reload":
                if (!sender.hasPermission("collection.admin")) {
                    sender.sendMessage(Component.text(plugin.getConfigManager().getMessage("no-permission")).color(NamedTextColor.RED));
                    return true;
                }
                plugin.getConfigManager().reload();
                collectionService.reloadData();
                sender.sendMessage(Component.text(plugin.getConfigManager().getPrefix() + "§a配置已重新加载"));
                return true;
                
            case "stats":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("该命令只能由玩家执行").color(NamedTextColor.RED));
                    return true;
                }
                showStats(player);
                return true;
                
            case "help":
                showHelp(sender);
                return true;
                
            default:
                sender.sendMessage(Component.text(plugin.getConfigManager().getPrefix() + "§c未知命令，使用 /collection help 查看帮助"));
                return true;
        }
    }
    
    private void showStats(Player player) {
        int totalItems = collectionService.getTotalItemsCollected(player.getUniqueId());
        
        player.sendMessage(Component.text(plugin.getConfigManager().getPrefix() + "§6===== 图鉴统计 ====="));
        player.sendMessage(Component.text("§e收集物品总数: §f" + totalItems));
        
        for (CollectionSet set : collectionService.getSets().values()) {
            int setProgress = 0;
            int setTotal = 0;
            
            for (String categoryId : set.getCategoryIds()) {
                setProgress += collectionService.getCategoryProgress(player, categoryId);
                java.util.Optional<cn.guangdian.collection.model.CollectionCategory> catOpt = 
                    collectionService.getCategory(categoryId);
                if (catOpt.isPresent()) {
                    setTotal += catOpt.get().getTotalEntries();
                }
            }
            
            String status = setProgress >= setTotal ? 
                net.kyori.adventure.text.Component.text("已完成", net.kyori.adventure.text.format.NamedTextColor.GREEN).toString() : 
                net.kyori.adventure.text.Component.text(setProgress + "/" + setTotal, net.kyori.adventure.text.format.NamedTextColor.YELLOW).toString();
            player.sendMessage(Component.text("- " + set.getName() + ": " + status, net.kyori.adventure.text.format.NamedTextColor.GRAY));
        }
    }
    
    private void showHelp(CommandSender sender) {
        sender.sendMessage(Component.text(plugin.getConfigManager().getPrefix() + "§6===== 图鉴帮助 ====="));
        sender.sendMessage(Component.text("§e/collection §7- 打开图鉴主界面"));
        sender.sendMessage(Component.text("§e/collection open §7- 打开图鉴主界面"));
        sender.sendMessage(Component.text("§e/collection stats §7- 查看收集统计"));
        sender.sendMessage(Component.text("§e/collection help §7- 显示帮助信息"));
        
        if (sender.hasPermission("collection.admin")) {
            sender.sendMessage(Component.text("§c/collection reload §7- 重新加载配置"));
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("open", "stats", "help"));
            if (sender.hasPermission("collection.admin")) {
                subCommands.add("reload");
            }
            return filterStartsWith(subCommands, args[0]);
        }
        
        return new ArrayList<>();
    }
    
    private List<String> filterStartsWith(List<String> list, String prefix) {
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(s);
            }
        }
        return result;
    }
}
