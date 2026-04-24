package cn.guangdian.classsystem.command;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.api.ClassService;
import cn.guangdian.classsystem.manager.AttributeManager;
import cn.guangdian.classsystem.manager.ClassManager;
import cn.guangdian.classsystem.manager.ExpManager;
import cn.guangdian.classsystem.model.AttributeType;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.Description;
import cn.guangdian.rpgcore.command.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@CommandInfo(name = "class", description = "职业系统", permission = "guangdian.class.use")
public class ClassCommandFramework extends BaseCommand {

    private final GuangDianClass plugin;
    private final ClassService classService;
    private final ClassManager classManager;
    private final ExpManager expManager;
    private final AttributeManager attributeManager;

    public ClassCommandFramework(GuangDianClass plugin, ClassService classService,
                                 ClassManager classManager, ExpManager expManager,
                                 AttributeManager attributeManager) {
        this.plugin = plugin;
        this.classService = classService;
        this.classManager = classManager;
        this.expManager = expManager;
        this.attributeManager = attributeManager;
    }

    @SubCommand(name = "", playerOnly = true)
    @Description("打开职业系统主界面")
    public void openGUI(CommandContext ctx) {
        plugin.openMainGUI(ctx.requirePlayer());
    }

    @SubCommand(name = "gui", playerOnly = true)
    @Description("打开职业系统主界面")
    public void gui(CommandContext ctx) {
        plugin.openMainGUI(ctx.requirePlayer());
    }

    @SubCommand(name = "info", minArgs = 0, maxArgs = 1)
    @Description("查看职业信息")
    public void info(CommandContext ctx) {
        Player target;
        if (ctx.getArgCount() > 0) {
            ctx.requirePermission("guangdian.class.admin");
            target = ctx.getPlayerArg(0);
        } else {
            target = ctx.requirePlayer();
        }
        PlayerClassData data = classService.getPlayerData(target);
        if (data == null) {
            ctx.sendError("无法获取玩家数据！");
            return;
        }
        GameClass gameClass = classManager.getClass(data.getClassId());
        if (gameClass == null) {
            ctx.sendError("职业数据异常！");
            return;
        }
        ctx.sendMessage("<gold>========== 职业信息 ==========");
        ctx.sendMessage("<yellow>玩家: <white>" + target.getName());
        ctx.sendMessage("<yellow>职业: <white>" + gameClass.getName());
        ctx.sendMessage("<yellow>阶位: <white>" + data.getTier() + "阶");
        long exp = data.getExp();
        long required = classManager.getExpRequiredForNextTier(data.getTier());
        String expDisplay = required > 0 ? exp + "/" + required : exp + " (MAX)";
        ctx.sendMessage("<yellow>经验: <white>" + expDisplay);
        ctx.sendMessage("<yellow>转职: <white>" + data.getAdvancementName());
        List<GameClass> availableClasses = classManager.getAvailableClasses(data);
        if (!availableClasses.isEmpty()) {
            String nextClasses = availableClasses.stream()
                .map(GameClass::getName)
                .collect(Collectors.joining(", "));
            ctx.sendMessage("<yellow>可转职: <white>" + nextClasses);
        }
        ctx.sendMessage("<gold>==============================");
    }

    @SubCommand(name = "choose", playerOnly = true, minArgs = 1, maxArgs = 1)
    @Description("选择基础职业")
    public void choose(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        String classId = ctx.getStringArg(0).toLowerCase();
        GameClass targetClass = classManager.getClass(classId);
        if (targetClass == null) {
            ctx.sendError("职业不存在！");
            return;
        }
        if (!targetClass.isBaseClass()) {
            ctx.sendError("只能选择基础职业！请使用 /class advance 进行转职。");
            return;
        }
        if (classService.chooseClass(player, classId)) {
            ctx.sendSuccess("成功选择职业: " + targetClass.getName());
        } else {
            ctx.sendError("选择职业失败！你可能已经拥有职业。");
        }
    }

    @SubCommand(name = "advance", playerOnly = true, minArgs = 0, maxArgs = 1)
    @Description("进行职业转职")
    public void advance(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        PlayerClassData data = classService.getPlayerData(player);
        if (data == null) {
            ctx.sendError("无法获取玩家数据！");
            return;
        }
        List<GameClass> availableClasses = classManager.getAvailableClasses(data);
        if (availableClasses.isEmpty()) {
            ctx.sendError("没有可转职的职业！");
            return;
        }
        if (ctx.getArgCount() == 0) {
            ctx.sendMessage("<yellow>可转职的职业:");
            for (GameClass gc : availableClasses) {
                ctx.sendMessage("<white>- " + gc.getId() + ": " + gc.getName());
            }
            ctx.sendMessage("<yellow>用法: /class advance <职业ID>");
            return;
        }
        String targetClassId = ctx.getStringArg(0).toLowerCase();
        GameClass targetClass = classManager.getClass(targetClassId);
        if (targetClass == null) {
            ctx.sendError("职业不存在！");
            return;
        }
        if (!availableClasses.contains(targetClass)) {
            ctx.sendError("不满足转职条件！");
            return;
        }
        if (classService.advanceClass(player, targetClassId)) {
            ctx.sendMessage("<light_purple>恭喜转职成功！新职业: <white>" + targetClass.getName());
        } else {
            ctx.sendError("转职失败！");
        }
    }

    @SubCommand(name = "reset", playerOnly = true)
    @Description("重置职业数据")
    public void reset(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        if (classService.resetClass(player)) {
            ctx.sendSuccess("职业数据已重置！");
        } else {
            ctx.sendError("重置失败！");
        }
    }

    @SubCommand(name = "list")
    @Description("查看可选职业列表")
    public void list(CommandContext ctx) {
        ctx.sendMessage("<gold>========== 可选职业 ==========");
        List<GameClass> baseClasses = classManager.getBaseClasses();
        for (GameClass gc : baseClasses) {
            ctx.sendMessage("<yellow>" + gc.getId() + " - " + gc.getName() + " <gray>(" + gc.getDescription() + ")");
        }
        ctx.sendMessage("<gold>使用 /class choose <职业ID> 选择职业");
    }

    @SubCommand(name = "attr", playerOnly = true, minArgs = 0)
    @Description("属性点系统")
    public void attr(CommandContext ctx) {
        if (ctx.getArgCount() == 0) {
            plugin.openAttributeGUI(ctx.requirePlayer());
            return;
        }
        String subCommand = ctx.getStringArg(0).toLowerCase();
        switch (subCommand) {
            case "add" -> handleAttrAdd(ctx);
            case "remove" -> handleAttrRemove(ctx);
            case "reset" -> handleAttrReset(ctx);
            case "info" -> handleAttrInfo(ctx);
            default -> sendAttrHelp(ctx);
        }
    }

    private void handleAttrAdd(CommandContext ctx) {
        if (ctx.getArgCount() < 3) {
            ctx.sendError("用法: /class attr add <属性> <点数>");
            return;
        }
        AttributeType type = AttributeType.fromId(ctx.getStringArg(1).toLowerCase());
        if (type == null) {
            ctx.sendError("无效的属性类型！可用: strength, vitality, agility, intelligence, luck");
            return;
        }
        try {
            int points = Integer.parseInt(ctx.getStringArg(2));
            if (points <= 0) {
                ctx.sendError("点数必须大于0！");
                return;
            }
            Player player = ctx.requirePlayer();
            if (attributeManager.allocateAttribute(player, type, points)) {
                ctx.sendSuccess("成功分配 " + points + " 点到 " + type.getDisplayName());
            } else {
                ctx.sendError("属性点不足！当前可用: " + attributeManager.getAvailablePoints(player));
            }
        } catch (NumberFormatException e) {
            ctx.sendError("无效的点数！");
        }
    }

    private void handleAttrRemove(CommandContext ctx) {
        if (ctx.getArgCount() < 3) {
            ctx.sendError("用法: /class attr remove <属性> <点数>");
            return;
        }
        AttributeType type = AttributeType.fromId(ctx.getStringArg(1).toLowerCase());
        if (type == null) {
            ctx.sendError("无效的属性类型！");
            return;
        }
        try {
            int points = Integer.parseInt(ctx.getStringArg(2));
            if (points <= 0) {
                ctx.sendError("点数必须大于0！");
                return;
            }
            Player player = ctx.requirePlayer();
            if (attributeManager.deallocateAttribute(player, type, points)) {
                ctx.sendWarning("成功回收 " + points + " 点从 " + type.getDisplayName());
            } else {
                ctx.sendError("该属性点数不足！");
            }
        } catch (NumberFormatException e) {
            ctx.sendError("无效的点数！");
        }
    }

    private void handleAttrReset(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        attributeManager.resetAttributes(player);
        ctx.sendMessage("<gold>已重置所有属性点！");
    }

    private void handleAttrInfo(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        ctx.sendMessage("<gold>========== 属性点信息 ==========");
        ctx.sendMessage("<yellow>可用点数: <green>" + attributeManager.getAvailablePoints(player));
        ctx.sendMessage("<yellow>已分配: <white>" + attributeManager.getTotalAllocatedPoints(player));
        ctx.sendMessage("<gray>--- 属性详情 ---");
        for (AttributeType type : AttributeType.values()) {
            int allocated = attributeManager.getAllocatedPoints(player, type);
            ctx.sendMessage("<yellow>" + type.getDisplayName() + ": <white>" + allocated + " <gray>(" + type.getDescription() + ")");
        }
        ctx.sendMessage("<gold>================================");
    }

    private void sendAttrHelp(CommandContext ctx) {
        ctx.sendMessage("<gold>========== 属性点帮助 ==========");
        ctx.sendMessage("<yellow>/class attr - 打开属性加点界面");
        ctx.sendMessage("<yellow>/class attr info - 查看属性点信息");
        ctx.sendMessage("<yellow>/class attr add <属性> <点数> - 分配属性点");
        ctx.sendMessage("<yellow>/class attr remove <属性> <点数> - 回收属性点");
        ctx.sendMessage("<yellow>/class attr reset - 重置所有属性点");
        ctx.sendMessage("<gray>属性类型: strength vitality agility intelligence luck");
        ctx.sendMessage("<gold>================================");
    }

    @SubCommand(name = "help")
    @Description("显示帮助信息")
    public void help(CommandContext ctx) {
        showHelp(ctx.getSender());
        if (ctx.hasPermission("guangdian.class.admin")) {
            ctx.sendMessage("<gold>========== 管理员命令 ==========");
            ctx.sendMessage("<yellow>/classadmin set <玩家> <职业> - 设置职业");
            ctx.sendMessage("<yellow>/classadmin addexp <玩家> <数量> - 增加经验");
            ctx.sendMessage("<yellow>/classadmin setexp <玩家> <数量> - 设置经验");
            ctx.sendMessage("<yellow>/classadmin addattr <玩家> <数量> - 增加属性点");
            ctx.sendMessage("<yellow>/classadmin reload - 重载配置");
            ctx.sendMessage("<gold>================================");
        }
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        String subCommandName = subCommandMethod.getAnnotation(SubCommand.class).name();
        return switch (subCommandName) {
            case "info" -> {
                if (context.hasPermission("guangdian.class.admin")) {
                    yield Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                }
                yield new ArrayList<>();
            }
            case "choose", "advance" -> classManager.getAllClasses().stream().map(GameClass::getId).collect(Collectors.toList());
            case "attr" -> {
                if (context.getArgCount() == 0) {
                    yield Arrays.asList("add", "remove", "reset", "info");
                } else if (context.getArgCount() == 1) {
                    String sub = context.getStringArgOrDefault(0, "").toLowerCase();
                    if (sub.equals("add") || sub.equals("remove")) {
                        yield Arrays.asList("strength", "vitality", "agility", "intelligence", "luck");
                    }
                }
                yield new ArrayList<>();
            }
            default -> new ArrayList<>();
        };
    }
}
