package cn.guangdian.devour.command;

import cn.guangdian.devour.GuangDianDevour;
import cn.guangdian.devour.config.DevourWeaponConfig;
import cn.guangdian.devour.data.DevourData;
import cn.guangdian.devour.gui.DevourGUI;
import cn.guangdian.rpgcore.message.MiniMessageService;
import io.lumine.mythic.bukkit.MythicBukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 吞噬命令处理器
 * 
 * @author Astraea RPG Team
 * @since 1.0.0
 */
public class DevourCommand implements CommandExecutor, TabCompleter {
    
    private final GuangDianDevour plugin;
    private final MiniMessageService mm;
    
    public DevourCommand(GuangDianDevour plugin) {
        this.plugin = plugin;
        this.mm = plugin.getMiniMessage();
    }
    
    /**
     * 注册命令
     */
    public void register() {
        plugin.getCommand("devour").setExecutor(this);
        plugin.getCommand("devour").setTabCompleter(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // 默认打开 GUI
            return handleOpen(sender);
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "open":
                return handleOpen(sender);
                
            case "give":
                return handleGive(sender, args);
                
            case "info":
                return handleInfo(sender);
                
            case "reload":
                return handleReload(sender);
                
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    /**
     * 处理open命令
     */
    private boolean handleOpen(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该命令只能由玩家执行！", NamedTextColor.RED));
            return true;
        }
        
        // 直接打开 GUI
        DevourGUI gui = new DevourGUI(plugin, player);
        gui.open();
        
        return true;
    }
    
    /**
     * 处理give命令
     */
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.devour.admin")) {
            sender.sendMessage(Component.text("你没有权限执行此命令！", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(Component.text("用法: /devour give <玩家> <吞噬剑类型>", NamedTextColor.RED));
            return true;
        }
        
        String playerName = args[1];
        String weaponType = args[2];
        
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("玩家 " + playerName + " 不在线！", NamedTextColor.RED));
            return true;
        }
        
        DevourWeaponConfig config = plugin.getConfigManager().getDevourWeaponConfig(weaponType);
        if (config == null) {
            sender.sendMessage(Component.text("未知的吞噬剑类型: " + weaponType, NamedTextColor.RED));
            sender.sendMessage(Component.text("可用类型: " + String.join(", ", plugin.getConfigManager().getDevourWeapons().keySet()), NamedTextColor.GRAY));
            return true;
        }
        
        try {
            ItemStack weapon = MythicBukkit.inst().getItemManager().getItemStack(config.getMythicType());
            if (weapon == null) {
                sender.sendMessage(Component.text("无法生成吞噬剑: " + config.getMythicType(), NamedTextColor.RED));
                return true;
            }
            
            target.getInventory().addItem(weapon);
            target.sendMessage(Component.text("你获得了吞噬剑: " + weaponType, NamedTextColor.GREEN));
            sender.sendMessage(Component.text("已给予 " + playerName + " 吞噬剑: " + weaponType, NamedTextColor.GREEN));
            
        } catch (Exception e) {
            sender.sendMessage(Component.text("生成吞噬剑时出错: " + e.getMessage(), NamedTextColor.RED));
        }
        
        return true;
    }
    
    /**
     * 处理info命令
     */
    private boolean handleInfo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该命令只能由玩家执行！", NamedTextColor.RED));
            return true;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (!plugin.getDevourManager().isDevourWeapon(item)) {
            player.sendMessage(Component.text("请手持吞噬剑后再使用此命令！", NamedTextColor.RED));
            return true;
        }
        
        DevourData data = plugin.getDevourManager().getDevourData(item);
        if (data == null) {
            player.sendMessage(Component.text("无法读取吞噬数据！", NamedTextColor.RED));
            return true;
        }
        
        player.sendMessage(Component.text("========== 吞噬剑信息 ==========", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("类型: " + data.getMythicType(), NamedTextColor.GRAY));
        player.sendMessage(Component.text("最大槽位: " + data.getMaxSlots(), NamedTextColor.GRAY));
        player.sendMessage(Component.text("已占用槽位: " + data.getOccupiedSlotCount(), NamedTextColor.GRAY));
        
        for (int i = 1; i <= data.getMaxSlots(); i++) {
            if (data.isSlotOccupied(i)) {
                String itemName = data.getSlotItemName(i);
                player.sendMessage(Component.text("  槽位" + i + ": " + (itemName != null ? itemName : "未知"), NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("  槽位" + i + ": 空", NamedTextColor.RED));
            }
        }
        
        player.sendMessage(Component.text("================================", NamedTextColor.YELLOW));
        
        return true;
    }
    
    /**
     * 处理reload命令
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("guangdian.devour.admin")) {
            sender.sendMessage(Component.text("你没有权限执行此命令！", NamedTextColor.RED));
            return true;
        }
        
        plugin.reload();
        sender.sendMessage(Component.text("配置已重载", NamedTextColor.GREEN));
        
        return true;
    }
    
    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("========== 吞噬系统帮助 ==========", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/devour - 打开吞噬GUI", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/devour open - 打开吞噬GUI", NamedTextColor.GRAY));
        
        if (sender.hasPermission("guangdian.devour.admin")) {
            sender.sendMessage(Component.text("/devour give <玩家> <类型> - 给予吞噬剑", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("/devour reload - 重载配置", NamedTextColor.GRAY));
        }
        
        sender.sendMessage(Component.text("/devour info - 查看吞噬剑信息", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("================================", NamedTextColor.YELLOW));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("open");
            completions.add("info");
            
            if (sender.hasPermission("guangdian.devour.admin")) {
                completions.add("give");
                completions.add("reload");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            completions.addAll(plugin.getConfigManager().getDevourWeapons().keySet());
        }
        
        String lastArg = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(lastArg));
        
        return completions;
    }
}
