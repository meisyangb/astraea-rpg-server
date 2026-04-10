package cn.guangdian.battlepass.command;

import cn.guangdian.battlepass.GuangDianBattlePass;
import cn.guangdian.battlepass.model.PlayerBattlePass;
import cn.guangdian.battlepass.model.Season;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BattlePassCommand implements CommandExecutor {
    
    private final GuangDianBattlePass plugin;
    
    public BattlePassCommand(GuangDianBattlePass plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("只有玩家可以使用此命令！").color(NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            openBattlePass(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "open":
            case "打开":
                openBattlePass(player);
                break;
            case "info":
            case "信息":
                showInfo(player);
                break;
            case "tasks":
            case "任务":
                showTasks(player);
                break;
            case "help":
            case "帮助":
                sendHelp(player);
                break;
            default:
                sendHelp(player);
        }
        
        return true;
    }
    
    private void openBattlePass(Player player) {
        Season season = plugin.getSeasonManager().getCurrentSeason();
        if (season == null) {
            player.sendMessage(Component.text("当前没有进行中的赛季！").color(NamedTextColor.RED));
            return;
        }
        
        plugin.getBattlePassGUI().openBattlePass(player);
    }
    
    private void showInfo(Player player) {
        Season season = plugin.getSeasonManager().getCurrentSeason();
        if (season == null) {
            player.sendMessage(Component.text("当前没有进行中的赛季！").color(NamedTextColor.RED));
            return;
        }
        
        PlayerBattlePass bp = plugin.getBattlePassManager().getPlayerBattlePass(player.getUniqueId());
        if (bp == null) {
            player.sendMessage(Component.text("无法加载战令数据！").color(NamedTextColor.RED));
            return;
        }
        
        player.sendMessage(Component.text("========== 战令信息 ==========").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("赛季: " + season.getSeasonName()).color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("等级: " + bp.getLevel() + "/" + season.getMaxLevel()).color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("经验: " + bp.getCurrentExp()).color(NamedTextColor.AQUA));
        player.sendMessage(Component.text("总经验: " + bp.getTotalExp()).color(NamedTextColor.AQUA));
        player.sendMessage(Component.text("状态: " + (bp.isPremium() ? "高级战令" : "免费战令")).color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("剩余时间: " + season.getRemainingDays() + "天").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("==============================").color(NamedTextColor.GOLD));
    }
    
    private void showTasks(Player player) {
        player.sendMessage(Component.text("========== 战令任务 ==========").color(NamedTextColor.GOLD));
        
        for (cn.guangdian.battlepass.model.BattlePassTask task : plugin.getRewardManager().getDailyTasks()) {
            PlayerBattlePass bp = plugin.getBattlePassManager().getPlayerBattlePass(player.getUniqueId());
            int progress = bp != null ? bp.getTaskProgress(task.getTaskId()) : 0;
            String status = progress >= task.getRequiredAmount() ? "§a[已完成]" : "§7[" + progress + "/" + task.getRequiredAmount() + "]";
            player.sendMessage(Component.text(task.getTaskName() + " " + status + " §e+" + task.getExpReward() + " 经验").color(NamedTextColor.WHITE));
        }
        
        player.sendMessage(Component.text("==============================").color(NamedTextColor.GOLD));
    }
    
    private void sendHelp(Player player) {
        player.sendMessage(Component.text("========== 战令帮助 ==========").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/bp open - 打开战令界面").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/bp info - 查看战令信息").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/bp tasks - 查看任务列表").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/bp help - 显示帮助信息").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("==============================").color(NamedTextColor.GOLD));
    }
}
