package cn.guangdian.collection.command;

import cn.guangdian.collection.GuangDianCollection;
import cn.guangdian.collection.api.CollectionService;
import cn.guangdian.collection.gui.CollectionGUIListener;
import cn.guangdian.collection.model.CollectionSet;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

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
        MiniMessageService mm = plugin.getMiniMessage();
        
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sendMessage(sender, mm, "<red>该命令只能由玩家执行");
                return true;
            }
            guiListener.openMainGUI(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "open":
                if (!(sender instanceof Player player)) {
                    sendMessage(sender, mm, "<red>该命令只能由玩家执行");
                    return true;
                }
                guiListener.openMainGUI(player);
                return true;
                
            case "reload":
                if (!sender.hasPermission("collection.admin")) {
                    sendMessage(sender, mm, plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                plugin.getConfigManager().reload();
                collectionService.reloadData();
                sendMessage(sender, mm, plugin.getConfigManager().getPrefix() + "<green>配置已重新加载");
                return true;
                
            case "stats":
                if (!(sender instanceof Player player)) {
                    sendMessage(sender, mm, "<red>该命令只能由玩家执行");
                    return true;
                }
                showStats(player, mm);
                return true;
                
            case "debug":
                if (!(sender instanceof Player player)) {
                    sendMessage(sender, mm, "<red>该命令只能由玩家执行");
                    return true;
                }
                if (!sender.hasPermission("collection.admin")) {
                    sendMessage(sender, mm, plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                debugItem(player, mm);
                return true;
                
            case "help":
                showHelp(sender, mm);
                return true;
                
            default:
                sendMessage(sender, mm, plugin.getConfigManager().getPrefix() + "<red>未知命令，使用 /collection help 查看帮助");
                return true;
        }
    }
    
    private void sendMessage(CommandSender sender, MiniMessageService mm, String message) {
        if (mm != null) {
            sender.sendMessage(mm.colorize(message));
        } else {
            sender.sendMessage(Component.text(message));
        }
    }
    
    private void debugItem(Player player, MiniMessageService mm) {
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType() == Material.AIR) {
            sendMessage(player, mm, "<red>请手持一个物品");
            return;
        }
        
        sendMessage(player, mm, "<gold>===== 物品调试信息 =====");
        sendMessage(player, mm, "<yellow>物品类型: <white>" + item.getType().name());
        sendMessage(player, mm, "<yellow>物品数量: <white>" + item.getAmount());
        
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            
            sendMessage(player, mm, "<yellow>--- NBT 数据 ---");
            
            NamespacedKey typeKey = new NamespacedKey("mythicmobs", "type");
            NamespacedKey oldKey = new NamespacedKey("mythicmobs", "item");
            
            String typeId = pdc.get(typeKey, PersistentDataType.STRING);
            String oldId = pdc.get(oldKey, PersistentDataType.STRING);
            
            sendMessage(player, mm, "<aqua>mythicmobs:type = <white>" + (typeId != null ? typeId : "<gray>null"));
            sendMessage(player, mm, "<aqua>mythicmobs:item = <white>" + (oldId != null ? oldId : "<gray>null"));
            
            if (typeId != null) {
                sendMessage(player, mm, "<green>检测到 MythicMobs 物品: <white>" + typeId);
            } else if (oldId != null) {
                sendMessage(player, mm, "<green>检测到 MythicMobs 物品 (旧格式): <white>" + oldId);
            } else {
                sendMessage(player, mm, "<red>未检测到 MythicMobs 物品数据");
            }
            
            if (meta.displayName() != null) {
                sendMessage(player, mm, "<yellow>显示名称: <white>" + meta.displayName());
            }
        } else {
            sendMessage(player, mm, "<gray>物品没有元数据");
        }
    }
    
    private void showStats(Player player, MiniMessageService mm) {
        int totalItems = collectionService.getTotalItemsCollected(player.getUniqueId());
        
        sendMessage(player, mm, plugin.getConfigManager().getPrefix() + "<gold>===== 图鉴统计 =====");
        sendMessage(player, mm, "<yellow>收集物品总数: <white>" + totalItems);
        
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
                "<green>已完成" : 
                "<yellow>" + setProgress + "/" + setTotal;
            sendMessage(player, mm, "<gray>- " + set.getName() + ": " + status);
        }
    }
    
    private void showHelp(CommandSender sender, MiniMessageService mm) {
        sendMessage(sender, mm, plugin.getConfigManager().getPrefix() + "<gold>===== 图鉴帮助 =====");
        sendMessage(sender, mm, "<yellow>/collection <gray>- 打开图鉴主界面");
        sendMessage(sender, mm, "<yellow>/collection open <gray>- 打开图鉴主界面");
        sendMessage(sender, mm, "<yellow>/collection stats <gray>- 查看收集统计");
        sendMessage(sender, mm, "<yellow>/collection help <gray>- 显示帮助信息");
        
        if (sender.hasPermission("collection.admin")) {
            sendMessage(sender, mm, "<red>/collection reload <gray>- 重新加载配置");
            sendMessage(sender, mm, "<red>/collection debug <gray>- 调试手持物品");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("open", "stats", "help"));
            if (sender.hasPermission("collection.admin")) {
                subCommands.add("reload");
                subCommands.add("debug");
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
