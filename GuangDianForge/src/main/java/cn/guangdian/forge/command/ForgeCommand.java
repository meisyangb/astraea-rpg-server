package cn.guangdian.forge.command;

import cn.guangdian.forge.GuangDianForge;
import cn.guangdian.forge.gui.RecipeSelectGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /forge 命令 - 玩家锻造系统
 */
public class ForgeCommand implements CommandExecutor {
    private final GuangDianForge plugin;

    public ForgeCommand(GuangDianForge plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该命令只能由玩家执行!");
            return true;
        }
        
        if (!player.hasPermission("guangdian.forge.use")) {
            player.sendMessage(Component.text("没有权限使用锻造系统!", NamedTextColor.RED));
            return true;
        }
        
        // 无参数时显示帮助
        if (args.length == 0) {
            showHelp(player);
            return true;
        }
        
        // 处理子命令
        switch (args[0].toLowerCase()) {
            case "help", "?" -> {
                showHelp(player);
                return true;
            }
            case "open", "forge" -> {
                handleOpen(player, args);
                return true;
            }
            case "learn" -> {
                handleLearn(player, args);
                return true;
            }
            case "info", "i" -> {
                handleInfo(player, args);
                return true;
            }
            case "list", "l", "recipes" -> {
                handleList(player, args);
                return true;
            }
            default -> {
                player.sendMessage(Component.text("未知命令: " + args[0], NamedTextColor.RED));
                player.sendMessage(Component.text("使用 /forge help 查看帮助", NamedTextColor.GRAY));
                return true;
            }
        }
    }
    
    /**
     * 显示帮助信息
     */
    private void showHelp(Player player) {
        player.sendMessage(Component.text("", NamedTextColor.WHITE));
        player.sendMessage(Component.text("═══ 锻造系统帮助 ═══", NamedTextColor.GOLD));
        player.sendMessage(Component.text("", NamedTextColor.WHITE));
        player.sendMessage(Component.text("  /forge open     - 打开锻造界面", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  /forge learn    - 学习图纸", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  /forge info     - 查看锻造信息", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  /forge list     - 查看图纸列表", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  /forge help     - 显示此帮助", NamedTextColor.GRAY));
        player.sendMessage(Component.text("", NamedTextColor.WHITE));
        player.sendMessage(Component.text("═══════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text("", NamedTextColor.WHITE));
    }
    
    /**
     * 打开锻造界面
     */
    private void handleOpen(Player player, String[] args) {
        if (args.length > 1) {
            // 直接打开指定图纸的锻造界面
            String recipeId = args[1];
            var recipe = plugin.getRecipeManager().getRecipe(recipeId);
            if (recipe == null) {
                player.sendMessage(Component.text("未知的图纸ID: " + recipeId, NamedTextColor.RED));
                return;
            }
            
            var data = plugin.getPlayerDataManager().get(player.getUniqueId());
            if (!data.hasLearned(recipeId)) {
                player.sendMessage(Component.text("你还没有学习这张图纸!", NamedTextColor.RED));
                player.sendMessage(Component.text("使用 /forge learn 学习图纸", NamedTextColor.GRAY));
                return;
            }
            
            // TODO: 打开指定图纸的锻造界面
            RecipeSelectGUI gui = new RecipeSelectGUI(plugin, player);
            gui.open();
        } else {
            // 打开图纸选择界面
            RecipeSelectGUI gui = new RecipeSelectGUI(plugin, player);
            gui.open();
        }
    }
    
    /**
     * 学习图纸
     */
    private void handleLearn(Player player, String[] args) {
        if (!player.hasPermission("guangdian.forge.learn")) {
            player.sendMessage(Component.text("没有权限学习图纸!", NamedTextColor.RED));
            return;
        }
        
        // TODO: 打开图纸学习界面或处理学习逻辑
        player.sendMessage(Component.text("图纸学习功能开发中...", NamedTextColor.YELLOW));
    }
    
    /**
     * 查看锻造信息
     */
    private void handleInfo(Player player, String[] args) {
        if (args.length > 1 && player.hasPermission("guangdian.forge.admin")) {
            // 管理员可以查看他人信息
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (target == null || !target.hasPlayedBefore()) {
                player.sendMessage(Component.text("玩家不存在: " + args[1], NamedTextColor.RED));
                return;
            }
            showPlayerInfo(player, target.getUniqueId());
        } else {
            // 查看自己的信息
            showPlayerInfo(player, player.getUniqueId());
        }
    }
    
    /**
     * 显示玩家锻造信息
     */
    private void showPlayerInfo(Player viewer, java.util.UUID targetId) {
        var data = plugin.getPlayerDataManager().get(targetId);
        var targetPlayer = Bukkit.getPlayer(targetId);
        String targetName = targetPlayer != null ? targetPlayer.getName() : "未知玩家";
        
        viewer.sendMessage(Component.text("", NamedTextColor.WHITE));
        viewer.sendMessage(Component.text("═══ 锻造信息 ═══", NamedTextColor.GOLD));
        if (!targetId.equals(viewer.getUniqueId())) {
            viewer.sendMessage(Component.text("  玩家: " + targetName, NamedTextColor.AQUA));
        }
        viewer.sendMessage(Component.text("", NamedTextColor.WHITE));
        viewer.sendMessage(Component.text("  锻造等级: ", NamedTextColor.GRAY)
            .append(Component.text(data.getForgeLevel(), NamedTextColor.AQUA)));
        viewer.sendMessage(Component.text("  锻造经验: ", NamedTextColor.GRAY)
            .append(Component.text(String.valueOf(data.getForgeExp()), NamedTextColor.YELLOW)));
        
        // 计算下一级所需经验
        var thresholds = getLevelThresholds();
        int nextLevel = data.getForgeLevel() + 1;
        if (thresholds.containsKey(nextLevel)) {
            long neededExp = thresholds.get(nextLevel) - data.getForgeExp();
            viewer.sendMessage(Component.text("  升级还需: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(neededExp), NamedTextColor.GREEN)));
        } else {
            viewer.sendMessage(Component.text("  已达最高等级", NamedTextColor.GRAY)
                .append(Component.text(" ✨", NamedTextColor.YELLOW)));
        }
        
        viewer.sendMessage(Component.text("", NamedTextColor.WHITE));
        viewer.sendMessage(Component.text("  总锻造次数: ", NamedTextColor.GRAY)
            .append(Component.text(String.valueOf(data.getTotalForges()), NamedTextColor.WHITE)));
        viewer.sendMessage(Component.text("  成功次数: ", NamedTextColor.GRAY)
            .append(Component.text(String.valueOf(data.getSuccessForges()), NamedTextColor.GREEN)));
        
        // 计算成功率
        if (data.getTotalForges() > 0) {
            double successRate = (double) data.getSuccessForges() / data.getTotalForges() * 100;
            viewer.sendMessage(Component.text("  成功率: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.1f%%", successRate), NamedTextColor.AQUA)));
        }
        
        viewer.sendMessage(Component.text("  已学图纸: ", NamedTextColor.GRAY)
            .append(Component.text(String.valueOf(data.getLearnedRecipes().size()), NamedTextColor.YELLOW)));
        viewer.sendMessage(Component.text("", NamedTextColor.WHITE));
        viewer.sendMessage(Component.text("═══════════════", NamedTextColor.GOLD));
        viewer.sendMessage(Component.text("", NamedTextColor.WHITE));
    }
    
    /**
     * 查看图纸列表
     */
    private void handleList(Player player, String[] args) {
        String filter = "learned"; // 默认显示已学
        if (args.length > 1) {
            filter = args[1].toLowerCase();
        }
        
        var data = plugin.getPlayerDataManager().get(player.getUniqueId());
        var allRecipes = plugin.getRecipeManager().getAllRecipes();
        
        player.sendMessage(Component.text("", NamedTextColor.WHITE));
        
        String title = switch (filter) {
            case "all" -> "═══ 所有图纸 ═══";
            case "unlearned" -> "═══ 未学图纸 ═══";
            default -> "═══ 已学图纸 ═══";
        };
        
        player.sendMessage(Component.text(title, NamedTextColor.GOLD));
        player.sendMessage(Component.text("", NamedTextColor.WHITE));
        
        int count = 0;
        for (var recipe : allRecipes) {
            boolean learned = data.hasLearned(recipe.getId());
            
            // 根据过滤器决定是否显示
            if (filter.equals("learned") && !learned) continue;
            if (filter.equals("unlearned") && learned) continue;
            
            count++;
            Component line = Component.text("  ● ", learned ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                .append(Component.text(recipe.getDisplayName(), NamedTextColor.WHITE));
            
            // 显示图纸等级要求
            if (recipe.getRequiredForgeLevel() > 0) {
                line = line.append(Component.text(" [Lv.", NamedTextColor.GRAY))
                    .append(Component.text(String.valueOf(recipe.getRequiredForgeLevel()), NamedTextColor.AQUA))
                    .append(Component.text("]", NamedTextColor.GRAY));
            }
            
            // 显示学习状态
            if (!learned) {
                line = line.append(Component.text(" (未学习)", NamedTextColor.RED));
            }
            
            player.sendMessage(line);
        }
        
        if (count == 0) {
            player.sendMessage(Component.text("  暂无图纸", NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("", NamedTextColor.WHITE));
            player.sendMessage(Component.text("  总计: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(count), NamedTextColor.YELLOW))
                .append(Component.text(" 张图纸", NamedTextColor.GRAY)));
        }
        
        player.sendMessage(Component.text("", NamedTextColor.WHITE));
        player.sendMessage(Component.text("═══════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text("", NamedTextColor.WHITE));
    }
    
    @SuppressWarnings("unchecked")
    private java.util.Map<Integer, Long> getLevelThresholds() {
        java.util.Map<Integer, Long> thresholds = new java.util.TreeMap<>();
        var section = plugin.getConfig().getConfigurationSection("level-thresholds");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    long exp = section.getLong(key);
                    thresholds.put(level, exp);
                } catch (NumberFormatException ignored) {}
            }
        }
        return thresholds;
    }
}
