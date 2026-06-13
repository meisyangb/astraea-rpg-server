package cn.guangdian.classsystem.command;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.api.ClassService;
import cn.guangdian.classsystem.manager.AttributeManager;
import cn.guangdian.classsystem.manager.ClassManager;
import cn.guangdian.classsystem.manager.ExpManager;
import cn.guangdian.classsystem.model.AttributeType;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClassCommand implements CommandExecutor, TabCompleter {
    
    private final GuangDianClass plugin;
    private final ClassService classService;
    private final ClassManager classManager;
    private final ExpManager expManager;
    private final AttributeManager attributeManager;
    
    public ClassCommand(GuangDianClass plugin, ClassService classService,
                       ClassManager classManager, ExpManager expManager, AttributeManager attributeManager) {
        this.plugin = plugin;
        this.classService = classService;
        this.classManager = classManager;
        this.expManager = expManager;
        this.attributeManager = attributeManager;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                            @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("class")) {
            return handleClassCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("classadmin")) {
            return handleAdminCommand(sender, args);
        }
        return false;
    }
    
    private boolean handleClassCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                plugin.openMainGUI(player);
                return true;
            }
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        return switch (subCommand) {
            case "info" -> handleInfo(sender, args);
            case "choose" -> handleChoose(sender, args);
            case "advance" -> handleAdvance(sender, args);
            case "reset" -> handleReset(sender, args);
            case "list" -> handleList(sender, args);
            case "attr", "attribute" -> handleAttribute(sender, args);
            case "gui" -> handleGUI(sender);
            case "skills" -> handleSkills(sender);
            case "help" -> {
                sendHelp(sender);
                yield true;
            }
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }
    
    private boolean handleInfo(CommandSender sender, String[] args) {
        Player target;
        
        if (args.length > 1) {
            if (!sender.hasPermission("guangdian.class.admin")) {
                sender.sendMessage(Component.text("你没有权限查看其他玩家的职业信息！").color(NamedTextColor.RED));
                return true;
            }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("玩家不存在或不在线！").color(NamedTextColor.RED));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("只有玩家可以使用此命令！").color(NamedTextColor.RED));
                return true;
            }
            target = (Player) sender;
        }
        
        PlayerClassData data = classService.getPlayerData(target);
        if (data == null) {
            sender.sendMessage(Component.text("无法获取玩家数据！").color(NamedTextColor.RED));
            return true;
        }
        
        GameClass gameClass = classManager.getClass(data.getClassId());
        if (gameClass == null) {
            sender.sendMessage(Component.text("职业数据异常！").color(NamedTextColor.RED));
            return true;
        }
        
        sender.sendMessage(Component.text("========== 职业信息 ==========").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("玩家: ").color(NamedTextColor.YELLOW)
            .append(Component.text(target.getName()).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("职业: ").color(NamedTextColor.YELLOW)
            .append(Component.text(gameClass.getName()).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("阶位: ").color(NamedTextColor.YELLOW)
            .append(Component.text(data.getTier() + "阶").color(NamedTextColor.WHITE)));
        
        long exp = data.getExp();
        long required = classManager.getExpRequiredForNextTier(data.getTier());
        String expDisplay = required > 0 ? exp + "/" + required : exp + " (MAX)";
        sender.sendMessage(Component.text("经验: ").color(NamedTextColor.YELLOW)
            .append(Component.text(expDisplay).color(NamedTextColor.WHITE)));
        
        sender.sendMessage(Component.text("转职: ").color(NamedTextColor.YELLOW)
            .append(Component.text(data.getAdvancementName()).color(NamedTextColor.WHITE)));
        
        List<GameClass> availableClasses = classManager.getAvailableClasses(data);
        if (!availableClasses.isEmpty()) {
            String nextClasses = availableClasses.stream()
                .map(GameClass::getName)
                .collect(Collectors.joining(", "));
            sender.sendMessage(Component.text("可转职: ").color(NamedTextColor.YELLOW)
                .append(Component.text(nextClasses).color(NamedTextColor.WHITE)));
        }
        
        sender.sendMessage(Component.text("==============================").color(NamedTextColor.GOLD));
        return true;
    }
    
    private boolean handleChoose(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("只有玩家可以使用此命令！").color(NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /class choose <职业ID>").color(NamedTextColor.RED));
            return true;
        }
        
        String classId = args[1].toLowerCase();
        GameClass targetClass = classManager.getClass(classId);
        
        if (targetClass == null) {
            sender.sendMessage(Component.text("职业不存在！").color(NamedTextColor.RED));
            return true;
        }
        
        if (!targetClass.isBaseClass()) {
            sender.sendMessage(Component.text("只能选择基础职业！请使用 /class advance 进行转职。").color(NamedTextColor.RED));
            return true;
        }
        
        if (classService.chooseClass(player, classId)) {
            plugin.getDataHandler().savePlayerData(player.getUniqueId(), 
                plugin.getDataHandler().getPlayerData(player.getUniqueId()));
            
            plugin.getEffectManager().applyEffects(player);
            
            refreshArmorStats(player);
            
            sender.sendMessage(Component.text("成功选择职业: ").color(NamedTextColor.GREEN)
                .append(Component.text(targetClass.getName()).color(NamedTextColor.WHITE)));
        } else {
            sender.sendMessage(Component.text("选择职业失败！你可能已经拥有职业。").color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    private void refreshArmorStats(Player player) {
        try {
            cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
            if (rpgCore != null) {
                cn.guangdian.rpgcore.api.ServiceRegistry registry = rpgCore.getServiceRegistry();
                if (registry != null) {
                    cn.guangdian.rpgcore.service.api.StatsService statsService = 
                        registry.getOptionalService(cn.guangdian.rpgcore.service.api.StatsService.class).orElse(null);
                    if (statsService != null) {
                        statsService.refreshPlayerStats(player);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
    
    private boolean handleAdvance(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("只有玩家可以使用此命令！").color(NamedTextColor.RED));
            return true;
        }
        
        PlayerClassData data = classService.getPlayerData(player);
        if (data == null) {
            sender.sendMessage(Component.text("无法获取玩家数据！").color(NamedTextColor.RED));
            return true;
        }
        
        List<GameClass> availableClasses = classManager.getAvailableClasses(data);
        
        if (availableClasses.isEmpty()) {
            sender.sendMessage(Component.text("没有可转职的职业！").color(NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(Component.text("可转职的职业:").color(NamedTextColor.YELLOW));
            for (GameClass gc : availableClasses) {
                sender.sendMessage(Component.text("- " + gc.getId() + ": " + gc.getName())
                    .color(NamedTextColor.WHITE));
            }
            sender.sendMessage(Component.text("用法: /class advance <职业ID>").color(NamedTextColor.YELLOW));
            return true;
        }
        
        String targetClassId = args[1].toLowerCase();
        GameClass targetClass = classManager.getClass(targetClassId);
        
        if (targetClass == null) {
            sender.sendMessage(Component.text("职业不存在！").color(NamedTextColor.RED));
            return true;
        }
        
        if (!availableClasses.contains(targetClass)) {
            sender.sendMessage(Component.text("不满足转职条件！").color(NamedTextColor.RED));
            return true;
        }
        
        if (classService.advanceClass(player, targetClassId)) {
            sender.sendMessage(Component.text("★ 恭喜转职成功！新职业: ")
                .color(NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(targetClass.getName()).color(NamedTextColor.WHITE)));
        } else {
            sender.sendMessage(Component.text("转职失败！").color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleReset(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("只有玩家可以使用此命令！").color(NamedTextColor.RED));
            return true;
        }
        
        if (classService.resetClass(player)) {
            sender.sendMessage(Component.text("职业数据已重置！").color(NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("重置失败！").color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleList(CommandSender sender, String[] args) {
        sender.sendMessage(Component.text("========== 可选职业 ==========").color(NamedTextColor.GOLD));
        
        List<GameClass> baseClasses = classManager.getBaseClasses();
        for (GameClass gc : baseClasses) {
            sender.sendMessage(Component.text(gc.getId() + " - " + gc.getName())
                .color(NamedTextColor.YELLOW)
                .append(Component.text(" (" + gc.getDescription() + ")").color(NamedTextColor.GRAY)));
        }
        
        sender.sendMessage(Component.text("使用 /class choose <职业ID> 选择职业").color(NamedTextColor.GOLD));
        return true;
    }
    
    private boolean handleAttribute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("只有玩家可以使用此命令！").color(NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            plugin.openAttributeGUI(player);
            return true;
        }
        
        String subCommand = args[1].toLowerCase();
        
        return switch (subCommand) {
            case "add" -> handleAttributeAdd(player, args);
            case "remove" -> handleAttributeRemove(player, args);
            case "reset" -> handleAttributeReset(player);
            case "info" -> handleAttributeInfo(player);
            default -> {
                sendAttributeHelp(sender);
                yield true;
            }
        };
    }
    
    private boolean handleAttributeAdd(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(Component.text("用法: /class attr add <属性> <点数>").color(NamedTextColor.RED));
            return true;
        }
        
        AttributeType type = AttributeType.fromId(args[2].toLowerCase());
        if (type == null) {
            player.sendMessage(Component.text("无效的属性类型！可用: strength, vitality, agility, intelligence, luck")
                .color(NamedTextColor.RED));
            return true;
        }
        
        try {
            int points = Integer.parseInt(args[3]);
            if (points <= 0) {
                player.sendMessage(Component.text("点数必须大于0！").color(NamedTextColor.RED));
                return true;
            }
            
            if (attributeManager.allocateAttribute(player, type, points)) {
                player.sendMessage(Component.text("成功分配 " + points + " 点到 " + type.getDisplayName())
                    .color(NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("属性点不足！当前可用: " + attributeManager.getAvailablePoints(player))
                    .color(NamedTextColor.RED));
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("无效的点数！").color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleAttributeRemove(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(Component.text("用法: /class attr remove <属性> <点数>").color(NamedTextColor.RED));
            return true;
        }
        
        AttributeType type = AttributeType.fromId(args[2].toLowerCase());
        if (type == null) {
            player.sendMessage(Component.text("无效的属性类型！").color(NamedTextColor.RED));
            return true;
        }
        
        try {
            int points = Integer.parseInt(args[3]);
            if (points <= 0) {
                player.sendMessage(Component.text("点数必须大于0！").color(NamedTextColor.RED));
                return true;
            }
            
            if (attributeManager.deallocateAttribute(player, type, points)) {
                player.sendMessage(Component.text("成功回收 " + points + " 点从 " + type.getDisplayName())
                    .color(NamedTextColor.YELLOW));
            } else {
                player.sendMessage(Component.text("该属性点数不足！").color(NamedTextColor.RED));
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("无效的点数！").color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleAttributeReset(Player player) {
        attributeManager.resetAttributes(player);
        player.sendMessage(Component.text("已重置所有属性点！").color(NamedTextColor.GOLD));
        return true;
    }
    
    private boolean handleGUI(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("只有玩家可以使用此命令！").color(NamedTextColor.RED));
            return true;
        }
        
        plugin.openMainGUI(player);
        return true;
    }
    
    private boolean handleSkills(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("只有玩家可以使用此命令！").color(NamedTextColor.RED));
            return true;
        }
        
        // 打开技能空间GUI
        cn.guangdian.classsystem.gui.SkillSpaceGUI skillSpaceGUI = 
            new cn.guangdian.classsystem.gui.SkillSpaceGUI(plugin);
        skillSpaceGUI.open(player);
        return true;
    }
    
    private boolean handleAttributeInfo(Player player) {
        player.sendMessage(Component.text("========== 属性点信息 ==========").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("可用点数: ").color(NamedTextColor.YELLOW)
            .append(Component.text(attributeManager.getAvailablePoints(player)).color(NamedTextColor.GREEN)));
        player.sendMessage(Component.text("已分配: ").color(NamedTextColor.YELLOW)
            .append(Component.text(attributeManager.getTotalAllocatedPoints(player)).color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("--- 属性详情 ---").color(NamedTextColor.GRAY));
        
        for (AttributeType type : AttributeType.values()) {
            int allocated = attributeManager.getAllocatedPoints(player, type);
            player.sendMessage(Component.text(type.getDisplayName() + ": ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(allocated).color(NamedTextColor.WHITE))
                .append(Component.text(" (" + type.getDescription() + ")").color(NamedTextColor.GRAY)));
        }
        
        player.sendMessage(Component.text("================================").color(NamedTextColor.GOLD));
        return true;
    }
    
    private void sendAttributeHelp(CommandSender sender) {
        sender.sendMessage(Component.text("========== 属性点帮助 ==========").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/class attr - 打开属性加点界面").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/class attr info - 查看属性点信息").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/class attr add <属性> <点数> - 分配属性点").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/class attr remove <属性> <点数> - 回收属性点").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/class attr reset - 重置所有属性点").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("属性类型: strength(力量) vitality(体质) agility(敏捷) intelligence(智力) luck(幸运)")
            .color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("================================").color(NamedTextColor.GOLD));
    }
    
    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.class.admin")) {
            sender.sendMessage(Component.text("你没有权限执行此操作！").color(NamedTextColor.RED));
            return true;
        }
        
        if (args.length == 0) {
            sendAdminHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        return switch (subCommand) {
            case "set" -> handleAdminSet(sender, args);
            case "addexp" -> handleAdminAddExp(sender, args);
            case "setexp" -> handleAdminSetExp(sender, args);
            case "addattr" -> handleAdminAddAttr(sender, args);
            case "reload" -> handleAdminReload(sender);
            default -> {
                sendAdminHelp(sender);
                yield true;
            }
        };
    }
    
    private boolean handleAdminSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("用法: /classadmin set <玩家> <职业ID>").color(NamedTextColor.RED));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("玩家不存在或不在线！").color(NamedTextColor.RED));
            return true;
        }
        
        String classId = args[2].toLowerCase();
        if (classService.setClass(target, classId)) {
            sender.sendMessage(Component.text("职业已设置！").color(NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("设置失败！职业可能不存在。").color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleAdminAddExp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("用法: /classadmin addexp <玩家> <数量>").color(NamedTextColor.RED));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("玩家不存在或不在线！").color(NamedTextColor.RED));
            return true;
        }
        
        try {
            long amount = Long.parseLong(args[2]);
            if (classService.addExp(target, amount)) {
                sender.sendMessage(Component.text("已添加 " + amount + " 点经验！").color(NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("添加经验失败！").color(NamedTextColor.RED));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("无效的数量！").color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleAdminSetExp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("用法: /classadmin setexp <玩家> <数量>").color(NamedTextColor.RED));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("玩家不存在或不在线！").color(NamedTextColor.RED));
            return true;
        }
        
        try {
            long amount = Long.parseLong(args[2]);
            if (classService.setExp(target, amount)) {
                sender.sendMessage(Component.text("经验已设置为 " + amount + "！").color(NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("设置经验失败！").color(NamedTextColor.RED));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("无效的数量！").color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleAdminAddAttr(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("用法: /classadmin addattr <玩家> <数量>").color(NamedTextColor.RED));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("玩家不存在或不在线！").color(NamedTextColor.RED));
            return true;
        }
        
        try {
            int amount = Integer.parseInt(args[2]);
            if (amount <= 0) {
                sender.sendMessage(Component.text("数量必须大于0！").color(NamedTextColor.RED));
                return true;
            }
            
            attributeManager.grantAttributePoints(target, amount);
            sender.sendMessage(Component.text("已给予 " + target.getName() + " " + amount + " 点属性点！")
                .color(NamedTextColor.GREEN));
            target.sendMessage(Component.text("你获得了 " + amount + " 点属性点！使用 /class attr 进行分配")
                .color(NamedTextColor.GOLD));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("无效的数量！").color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleAdminReload(CommandSender sender) {
        plugin.reloadConfig();
        classManager.reload();
        sender.sendMessage(Component.text("配置已重新加载！").color(NamedTextColor.GREEN));
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("========== 职业系统帮助 ==========").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/class - 打开职业主界面").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/class info [玩家] - 查看职业信息").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/class choose <职业> - 选择职业").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/class advance - 进行转职").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/class list - 查看可选职业").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/class attr - 属性加点系统").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/class gui - 打开职业GUI").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/class skills - 查看技能列表").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/class reset - 重置职业").color(NamedTextColor.YELLOW));
        if (sender.hasPermission("guangdian.class.admin")) {
            sender.sendMessage(Component.text("========== 管理员命令 ==========").color(NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/classadmin set <玩家> <职业> - 设置职业").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/classadmin addexp <玩家> <数量> - 增加经验").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/classadmin setexp <玩家> <数量> - 设置经验").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/classadmin addattr <玩家> <数量> - 增加属性点").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/classadmin reload - 重载配置").color(NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text("================================").color(NamedTextColor.GOLD));
    }
    
    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(Component.text("========== 管理员命令 ==========").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/classadmin set <玩家> <职业> - 设置职业").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/classadmin addexp <玩家> <数量> - 增加经验").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/classadmin setexp <玩家> <数量> - 设置经验").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/classadmin addattr <玩家> <数量> - 增加属性点").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/classadmin reload - 重载配置").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("================================").color(NamedTextColor.GOLD));
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("class")) {
            if (args.length == 1) {
                completions.addAll(Arrays.asList("info", "choose", "advance", "list", "reset", "attr", "gui", "skills", "help"));
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("choose") || args[0].equalsIgnoreCase("advance")) {
                    completions.addAll(classManager.getAllClasses().stream()
                        .map(GameClass::getId)
                        .toList());
                } else if (args[0].equalsIgnoreCase("info") && 
                          sender.hasPermission("guangdian.class.admin")) {
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList());
                } else if (args[0].equalsIgnoreCase("attr")) {
                    completions.addAll(Arrays.asList("add", "remove", "reset", "info"));
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("attr")) {
                if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove")) {
                    completions.addAll(Arrays.asList("strength", "vitality", "agility", "intelligence", "luck"));
                }
            }
        } else if (command.getName().equalsIgnoreCase("classadmin")) {
            if (args.length == 1) {
                completions.addAll(Arrays.asList("set", "addexp", "setexp", "addattr", "reload"));
            } else if (args.length == 2) {
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList());
            } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
                completions.addAll(classManager.getAllClasses().stream()
                    .map(GameClass::getId)
                    .toList());
            }
        }
        
        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(lastArg))
            .collect(Collectors.toList());
    }
}
