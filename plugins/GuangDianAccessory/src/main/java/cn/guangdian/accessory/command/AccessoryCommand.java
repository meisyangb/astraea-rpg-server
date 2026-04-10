package cn.guangdian.accessory.command;

import cn.guangdian.accessory.GuangDianAccessory;
import cn.guangdian.accessory.api.AccessoryService;
import cn.guangdian.accessory.model.Accessory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AccessoryCommand implements TabExecutor {
    
    private final GuangDianAccessory plugin;
    
    public AccessoryCommand(GuangDianAccessory plugin) {
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
            case "open":
                return handleOpen(sender);
            case "give":
                return handleGive(sender, args);
            case "reload":
                return handleReload(sender);
            case "list":
                return handleList(sender);
            case "info":
                return handleInfo(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean handleOpen(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("此命令只能由玩家执行").color(NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        if (!player.hasPermission("gdaccessory.open")) {
            player.sendMessage(Component.text("你没有权限执行此命令").color(NamedTextColor.RED));
            return true;
        }
        
        plugin.getAccessoryGUI().openGUI(player);
        return true;
    }
    
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gdaccessory.admin")) {
            sender.sendMessage(Component.text("你没有权限执行此命令").color(NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(Component.text("用法: /accessory give <玩家> <饰品ID> [数量]")
                .color(NamedTextColor.RED));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("玩家 " + args[1] + " 不在线").color(NamedTextColor.RED));
            return true;
        }
        
        String accessoryId = args[2];
        AccessoryService service = plugin.getAccessoryService();
        
        if (service.getAccessory(accessoryId).isEmpty()) {
            sender.sendMessage(Component.text("饰品 " + accessoryId + " 不存在").color(NamedTextColor.RED));
            return true;
        }
        
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("数量必须是数字").color(NamedTextColor.RED));
                return true;
            }
        }
        
        if (service.giveAccessory(target, accessoryId, amount)) {
            sender.sendMessage(Component.text("已给予 " + target.getName() + " " + amount + " 个 " + accessoryId)
                .color(NamedTextColor.GREEN));
            target.sendMessage(Component.text("你收到了 " + amount + " 个饰品")
                .color(NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("给予饰品失败，玩家背包可能已满").color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("gdaccessory.admin")) {
            sender.sendMessage(Component.text("你没有权限执行此命令").color(NamedTextColor.RED));
            return true;
        }
        
        plugin.getAccessoryManager().reloadAccessories();
        sender.sendMessage(Component.text("饰品配置已重新加载").color(NamedTextColor.GREEN));
        return true;
    }
    
    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("gdaccessory.list")) {
            sender.sendMessage(Component.text("你没有权限执行此命令").color(NamedTextColor.RED));
            return true;
        }
        
        AccessoryService service = plugin.getAccessoryService();
        sender.sendMessage(Component.text("=== 饰品列表 ===").color(NamedTextColor.GOLD));
        
        for (Accessory accessory : service.getAllAccessories()) {
            sender.sendMessage(Component.text("- " + accessory.getId() + " (" + accessory.getName() + ")")
                .color(NamedTextColor.YELLOW));
        }
        
        return true;
    }
    
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gdaccessory.info")) {
            sender.sendMessage(Component.text("你没有权限执行此命令").color(NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /accessory info <饰品ID>").color(NamedTextColor.RED));
            return true;
        }
        
        String accessoryId = args[1];
        AccessoryService service = plugin.getAccessoryService();
        
        Accessory accessory = service.getAccessory(accessoryId).orElse(null);
        if (accessory == null) {
            sender.sendMessage(Component.text("饰品 " + accessoryId + " 不存在").color(NamedTextColor.RED));
            return true;
        }
        
        sender.sendMessage(Component.text("=== " + accessory.getName() + " ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("ID: " + accessory.getId()).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("槽位: " + accessory.getSlot().getDisplayName()).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("稀有度: " + accessory.getRarity()).color(NamedTextColor.YELLOW));
        
        if (!accessory.getAttributes().isEmpty()) {
            sender.sendMessage(Component.text("属性:").color(NamedTextColor.YELLOW));
            accessory.getAttributes().forEach((attr, value) -> {
                sender.sendMessage(Component.text("  " + attr + ": +" + value).color(NamedTextColor.GREEN));
            });
        }
        
        if (!accessory.getDescription().isEmpty()) {
            sender.sendMessage(Component.text("描述: " + accessory.getDescription()).color(NamedTextColor.GRAY));
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== 饰品系统帮助 ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/accessory open - 打开饰品界面").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/accessory list - 查看饰品列表").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/accessory info <ID> - 查看饰品信息").color(NamedTextColor.YELLOW));
        
        if (sender.hasPermission("gdaccessory.admin")) {
            sender.sendMessage(Component.text("/accessory give <玩家> <ID> [数量] - 给予饰品").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/accessory reload - 重载配置").color(NamedTextColor.YELLOW));
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("open", "list", "info"));
            if (sender.hasPermission("gdaccessory.admin")) {
                completions.addAll(Arrays.asList("give", "reload"));
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("give")) {
                Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
            } else if (subCommand.equals("info")) {
                completions.addAll(plugin.getAccessoryManager().getAllAccessories().stream()
                    .map(Accessory::getId)
                    .collect(Collectors.toList()));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            completions.addAll(plugin.getAccessoryManager().getAllAccessories().stream()
                .map(Accessory::getId)
                .collect(Collectors.toList()));
        }
        
        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(lastArg))
            .collect(Collectors.toList());
    }
}
