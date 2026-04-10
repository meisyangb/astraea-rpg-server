package cn.guangdian.battlepass.command;

import cn.guangdian.battlepass.GuangDianBattlePass;
import cn.guangdian.battlepass.model.Season;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BattlePassAdminCommand implements CommandExecutor {
    
    private final GuangDianBattlePass plugin;
    
    public BattlePassAdminCommand(GuangDianBattlePass plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("battlepass.admin")) {
            sender.sendMessage(Component.text("没有权限！").color(NamedTextColor.RED));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                return handleReload(sender);
            case "create":
                return handleCreate(sender, args);
            case "activate":
                return handleActivate(sender, args);
            case "giveexp":
                return handleGiveExp(sender, args);
            case "setlevel":
                return handleSetLevel(sender, args);
            case "givepremium":
                return handleGivePremium(sender, args);
            case "info":
                return handleInfo(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean handleReload(CommandSender sender) {
        plugin.getSeasonManager().reload();
        plugin.getRewardManager().reload();
        sender.sendMessage(Component.text("配置已重新加载！").color(NamedTextColor.GREEN));
        return true;
    }
    
    private boolean handleCreate(CommandSender sender, String[] args) {
        if (args.length < 6) {
            sender.sendMessage(Component.text("用法: /bpa create <ID> <名称> <开始时间> <结束时间> <最大等级>").color(NamedTextColor.RED));
            sender.sendMessage(Component.text("时间格式: yyyy-MM-dd HH:mm:ss").color(NamedTextColor.GRAY));
            return true;
        }
        
        try {
            int seasonId = Integer.parseInt(args[1]);
            String name = args[2];
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime startTime = LocalDateTime.parse(args[3] + " " + args[4], formatter);
            LocalDateTime endTime = LocalDateTime.parse(args[5] + " " + args[6], formatter);
            int maxLevel = Integer.parseInt(args[7]);
            
            plugin.getSeasonManager().createSeason(seasonId, name, startTime, endTime, maxLevel);
            sender.sendMessage(Component.text("赛季 " + name + " 创建成功！").color(NamedTextColor.GREEN));
        } catch (Exception e) {
            sender.sendMessage(Component.text("创建失败: " + e.getMessage()).color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleActivate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /bpa activate <赛季ID>").color(NamedTextColor.RED));
            return true;
        }
        
        try {
            int seasonId = Integer.parseInt(args[1]);
            plugin.getSeasonManager().activateSeason(seasonId);
            sender.sendMessage(Component.text("赛季 " + seasonId + " 已激活！").color(NamedTextColor.GREEN));
        } catch (Exception e) {
            sender.sendMessage(Component.text("激活失败: " + e.getMessage()).color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleGiveExp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("用法: /bpa giveexp <玩家> <经验>").color(NamedTextColor.RED));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("玩家不在线！").color(NamedTextColor.RED));
            return true;
        }
        
        try {
            int exp = Integer.parseInt(args[2]);
            plugin.getBattlePassManager().addExp(target.getUniqueId(), exp);
            sender.sendMessage(Component.text("已给予 " + target.getName() + " " + exp + " 经验！").color(NamedTextColor.GREEN));
            target.sendMessage(Component.text("你获得了 " + exp + " 战令经验！").color(NamedTextColor.YELLOW));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("无效的经验值！").color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleSetLevel(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("用法: /bpa setlevel <玩家> <等级>").color(NamedTextColor.RED));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("玩家不在线！").color(NamedTextColor.RED));
            return true;
        }
        
        try {
            int level = Integer.parseInt(args[2]);
            var bp = plugin.getBattlePassManager().getPlayerBattlePass(target.getUniqueId());
            if (bp != null) {
                bp.setLevel(level);
                sender.sendMessage(Component.text("已设置 " + target.getName() + " 的等级为 " + level).color(NamedTextColor.GREEN));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("无效的等级！").color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleGivePremium(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /bpa givepremium <玩家>").color(NamedTextColor.RED));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("玩家不在线！").color(NamedTextColor.RED));
            return true;
        }
        
        if (plugin.getBattlePassManager().purchasePremium(target.getUniqueId())) {
            sender.sendMessage(Component.text("已给予 " + target.getName() + " 高级战令！").color(NamedTextColor.GREEN));
            target.sendMessage(Component.text("你获得了高级战令！").color(NamedTextColor.LIGHT_PURPLE));
        } else {
            sender.sendMessage(Component.text("该玩家已拥有高级战令！").color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleInfo(CommandSender sender, String[] args) {
        Season season = plugin.getSeasonManager().getCurrentSeason();
        if (season == null) {
            sender.sendMessage(Component.text("当前没有进行中的赛季！").color(NamedTextColor.RED));
            return true;
        }
        
        sender.sendMessage(Component.text("========== 当前赛季信息 ==========").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("赛季ID: " + season.getSeasonId()).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("赛季名称: " + season.getSeasonName()).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("最大等级: " + season.getMaxLevel()).color(NamedTextColor.GREEN));
        sender.sendMessage(Component.text("剩余时间: " + season.getRemainingDays() + "天").color(NamedTextColor.AQUA));
        sender.sendMessage(Component.text("=================================").color(NamedTextColor.GOLD));
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("========== 战令管理帮助 ==========").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bpa reload - 重载配置").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/bpa create <ID> <名称> <开始> <结束> <等级> - 创建赛季").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/bpa activate <ID> - 激活赛季").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/bpa giveexp <玩家> <经验> - 给予经验").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/bpa setlevel <玩家> <等级> - 设置等级").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/bpa givepremium <玩家> - 给予高级战令").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/bpa info - 查看当前赛季信息").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("=================================").color(NamedTextColor.GOLD));
    }
}
