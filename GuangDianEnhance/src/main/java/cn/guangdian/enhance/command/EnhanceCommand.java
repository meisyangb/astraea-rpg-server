package cn.guangdian.enhance.command;

import cn.guangdian.enhance.GuangDianEnhance;
import cn.guangdian.enhance.config.EnhanceConfig;
import cn.guangdian.enhance.data.EnhanceResult;
import cn.guangdian.enhance.gui.EnhanceGUI;
import cn.guangdian.enhance.manager.EnhanceManager;
import cn.guangdian.enhance.manager.SuccessRateCalculator;
import cn.guangdian.enhance.stone.EnhanceStone;
import cn.guangdian.enhance.stone.EnhanceStoneManager;
import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnhanceCommand implements CommandExecutor, TabCompleter {

    private final GuangDianEnhance plugin;
    private final EnhanceManager enhanceManager;
    private final EnhanceConfig config;
    private final MiniMessageService miniMessage;
    private final EnhanceGUI enhanceGUI;
    private final EnhanceStoneManager stoneManager;

    public EnhanceCommand(GuangDianEnhance plugin, EnhanceManager enhanceManager, EnhanceGUI enhanceGUI, EnhanceStoneManager stoneManager) {
        this.plugin = plugin;
        this.enhanceManager = enhanceManager;
        this.config = plugin.getEnhanceConfig();
        this.miniMessage = plugin.getMiniMessage();
        this.enhanceGUI = enhanceGUI;
        this.stoneManager = stoneManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("该命令只能由玩家执行");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            enhanceGUI.open(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        return switch (subCommand) {
            case "info" -> handleInfo(player);
            case "enhance", "e" -> handleEnhance(player);
            case "gui" -> {
                enhanceGUI.open(player);
                yield true;
            }
            case "stone" -> handleStone(player, args);
            case "stones" -> handleStonesList(player);
            case "setlevel" -> handleSetLevel(player, args);
            case "reset" -> handleReset(player);
            case "reload" -> handleReload(player);
            case "help" -> {
                sendHelp(player);
                yield true;
            }
            default -> {
                player.sendMessage(miniMessage.colorize("<red>未知命令，使用 /enhance help 查看帮助"));
                yield true;
            }
        };
    }

    private boolean handleInfo(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(miniMessage.colorize("<red>请手持装备查看强化信息"));
            return true;
        }
        
        int level = enhanceManager.getLevel(item);
        double successRate = enhanceManager.getSuccessRate(level, item);
        double multiplier = enhanceManager.getAttributeMultiplier(level);
        
        int pityCount = enhanceManager.getPityCountForPlayer(player.getUniqueId(), level);
        double pityBonus = enhanceManager.getPityBonusForPlayer(player.getUniqueId(), level);
        
        SuccessRateCalculator calc = enhanceManager.getRateCalculator();
        
        player.sendMessage(miniMessage.colorize("<gold>========== 强化信息 =========="));
        player.sendMessage(miniMessage.colorize("<yellow>物品: <white>" + item.getType().name()));
        player.sendMessage(miniMessage.colorize("<yellow>强化等级: <green>+" + level));
        player.sendMessage(miniMessage.colorize("<yellow>属性加成: <aqua>" + String.format("%.1f%%", (multiplier - 1) * 100)));
        
        if (level < config.getMaxLevel()) {
            double totalRate = Math.min(1.0, successRate + pityBonus);
            player.sendMessage(miniMessage.colorize("<yellow>基础成功率: <white>" + calc.formatRate(successRate)));
            if (config.isPityEnabled() && pityCount > 0) {
                player.sendMessage(miniMessage.colorize("<yellow>保底加成: <green>+" + calc.formatRate(pityBonus) + " <gray>(失败" + pityCount + "次)"));
            }
            player.sendMessage(miniMessage.colorize("<yellow>实际成功率: <bold><green>" + calc.formatRate(totalRate)));
            player.sendMessage(miniMessage.colorize("<yellow>可强化: <green>是"));
        } else {
            player.sendMessage(miniMessage.colorize("<yellow>可强化: <red>已达最高等级"));
        }
        
        player.sendMessage(miniMessage.colorize("<gold>================================"));
        
        return true;
    }

    private boolean handleEnhance(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(miniMessage.colorize("<red>请手持装备进行强化"));
            return true;
        }
        
        EnhanceResult result = enhanceManager.enhance(player, item);
        
        if (result == EnhanceResult.NOT_ENHANCEABLE) {
            player.sendMessage(miniMessage.colorize("<red>该物品无法强化"));
        } else if (result == EnhanceResult.INSUFFICIENT_MATERIAL) {
            player.sendMessage(miniMessage.colorize("<red>材料不足，无法强化"));
        } else if (result == EnhanceResult.INSUFFICIENT_MONEY) {
            player.sendMessage(miniMessage.colorize("<red>金币不足，无法强化"));
        } else if (result == EnhanceResult.MAX_LEVEL_REACHED) {
            player.sendMessage(miniMessage.colorize("<red>已达到最高强化等级"));
        } else if (result == EnhanceResult.IN_COOLDOWN) {
            player.sendMessage(miniMessage.colorize("<red>强化冷却中，请稍后再试"));
        }
        
        return true;
    }

    private boolean handleSetLevel(Player player, String[] args) {
        if (!player.hasPermission("guangdian.enhance.admin")) {
            player.sendMessage(miniMessage.colorize("<red>你没有权限执行此命令"));
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(miniMessage.colorize("<red>用法: /enhance setlevel <等级>"));
            return true;
        }
        
        try {
            int level = Integer.parseInt(args[1]);
            
            if (level < 0 || level > config.getMaxLevel()) {
                player.sendMessage(miniMessage.colorize(
                    "<red>等级必须在 0-" + config.getMaxLevel() + " 之间"));
                return true;
            }
            
            ItemStack item = player.getInventory().getItemInMainHand();
            
            if (item == null || item.getType() == Material.AIR) {
                player.sendMessage(miniMessage.colorize("<red>请手持装备设置强化等级"));
                return true;
            }
            
            ItemStack modified = plugin.getEnhanceStorage().setLevel(item, level);
            player.getInventory().setItemInMainHand(modified);
            player.updateInventory();
            
            player.sendMessage(miniMessage.colorize(
                "<green>已将装备强化等级设置为 <bold>+" + level + "</bold>"));
            
        } catch (NumberFormatException e) {
            player.sendMessage(miniMessage.colorize("<red>请输入有效的数字"));
        }
        
        return true;
    }

    private boolean handleReset(Player player) {
        if (!player.hasPermission("guangdian.enhance.admin")) {
            player.sendMessage(miniMessage.colorize("<red>你没有权限执行此命令"));
            return true;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(miniMessage.colorize("<red>请手持装备重置强化等级"));
            return true;
        }
        
        ItemStack cleared = plugin.getEnhanceStorage().clearEnhanceData(item);
        player.getInventory().setItemInMainHand(cleared);
        player.updateInventory();
        
        player.sendMessage(miniMessage.colorize("<green>已重置装备强化等级"));
        
        return true;
    }

    private boolean handleReload(Player player) {
        if (!player.hasPermission("guangdian.enhance.admin")) {
            player.sendMessage(miniMessage.colorize("<red>你没有权限执行此命令"));
            return true;
        }
        
        config.load();
        player.sendMessage(miniMessage.colorize("<green>配置已重新加载"));
        
        return true;
    }
    
    private boolean handleStone(Player player, String[] args) {
        if (!player.hasPermission("guangdian.enhance.admin")) {
            player.sendMessage(miniMessage.colorize("<red>你没有权限执行此命令"));
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(miniMessage.colorize("<red>用法: /enhance stone <强化石ID> [数量]"));
            player.sendMessage(miniMessage.colorize("<gray>可用的强化石ID:"));
            for (EnhanceStone stone : stoneManager.getAllStones()) {
                player.sendMessage(miniMessage.colorize("<gray>  - " + stone.getId() + " (" + stone.getType().getDisplayName() + ")"));
            }
            return true;
        }
        
        String stoneId = args[1].toLowerCase();
        EnhanceStone stone = stoneManager.getStone(stoneId);
        
        if (stone == null) {
            player.sendMessage(miniMessage.colorize("<red>未知的强化石: " + stoneId));
            return true;
        }
        
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) amount = 1;
            } catch (NumberFormatException ignored) {
            }
        }
        
        ItemStack stoneItem = stoneManager.createStoneItem(stoneId);
        if (stoneItem == null) {
            player.sendMessage(miniMessage.colorize("<red>无法创建强化石物品"));
            return true;
        }
        
        stoneItem.setAmount(amount);
        player.getInventory().addItem(stoneItem);
        player.updateInventory();
        
        player.sendMessage(miniMessage.colorize(
            "<green>已获得 " + stone.getType().getDisplayName() + " x" + amount));
        
        return true;
    }
    
    private boolean handleStonesList(Player player) {
        player.sendMessage(miniMessage.colorize("<gold>========== 强化石列表 =========="));
        
        for (EnhanceStone stone : stoneManager.getAllStones()) {
            String info = String.format("<yellow>%s <gray>(%s) <white>- %s",
                stone.getType().getDisplayName(),
                "T" + stone.getTier(),
                stone.getType().getDescription());
            player.sendMessage(miniMessage.colorize(info));
            
            int count = stoneManager.countStone(player, stone);
            if (count > 0) {
                player.sendMessage(miniMessage.colorize("<green>  背包中: " + count + " 个"));
            }
        }
        
        player.sendMessage(miniMessage.colorize("<gold>===================================="));
        
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(miniMessage.colorize("<gold>========== 强化系统帮助 =========="));
        player.sendMessage(miniMessage.colorize("<yellow>/enhance <white>- 打开强化界面"));
        player.sendMessage(miniMessage.colorize("<yellow>/enhance info <white>- 查看手持装备强化信息"));
        player.sendMessage(miniMessage.colorize("<yellow>/enhance enhance <white>- 强化手持装备"));
        player.sendMessage(miniMessage.colorize("<yellow>/enhance stones <white>- 查看强化石列表"));
        
        if (player.hasPermission("guangdian.enhance.admin")) {
            player.sendMessage(miniMessage.colorize("<yellow>/enhance stone <ID> [数量] <white>- 获得强化石"));
            player.sendMessage(miniMessage.colorize("<yellow>/enhance setlevel <等级> <white>- 设置强化等级"));
            player.sendMessage(miniMessage.colorize("<yellow>/enhance reset <white>- 重置强化等级"));
            player.sendMessage(miniMessage.colorize("<yellow>/enhance reload <white>- 重载配置"));
        }
        
        player.sendMessage(miniMessage.colorize("<gold>===================================="));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("info", "enhance", "e", "help", "gui", "stones"));
            if (sender.hasPermission("guangdian.enhance.admin")) {
                completions.addAll(Arrays.asList("setlevel", "reset", "reload", "stone"));
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setlevel")) {
            for (int i = 0; i <= config.getMaxLevel(); i++) {
                completions.add(String.valueOf(i));
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("stone")) {
            for (EnhanceStone stone : stoneManager.getAllStones()) {
                completions.add(stone.getId());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("stone")) {
            completions.addAll(Arrays.asList("1", "10", "64"));
        }
        
        String lastArg = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(lastArg));
        
        return completions;
    }
}
