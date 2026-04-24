package cn.guangdian.classsystem.command;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.api.ClassService;
import cn.guangdian.classsystem.manager.AttributeManager;
import cn.guangdian.classsystem.manager.ClassManager;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.Description;
import cn.guangdian.rpgcore.command.SubCommand;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

@CommandInfo(name = "classadmin", description = "职业系统管理", permission = "guangdian.class.admin")
public class ClassAdminCommandFramework extends BaseCommand {

    private final GuangDianClass plugin;
    private final ClassService classService;
    private final ClassManager classManager;
    private final AttributeManager attributeManager;

    public ClassAdminCommandFramework(GuangDianClass plugin, ClassService classService,
                                      ClassManager classManager, AttributeManager attributeManager) {
        this.plugin = plugin;
        this.classService = classService;
        this.classManager = classManager;
        this.attributeManager = attributeManager;
    }

    @SubCommand(name = "")
    @Description("显示管理员帮助")
    public void showDefaultHelp(CommandContext ctx) {
        showHelp(ctx.getSender());
    }

    @SubCommand(name = "set", minArgs = 2, maxArgs = 2)
    @Description("设置玩家职业")
    public void set(CommandContext ctx) {
        Player target = ctx.getPlayerArg(0);
        String classId = ctx.getStringArg(1).toLowerCase();
        if (classService.setClass(target, classId)) {
            ctx.sendSuccess("职业已设置！");
        } else {
            ctx.sendError("设置失败！职业可能不存在。");
        }
    }

    @SubCommand(name = "addexp", minArgs = 2, maxArgs = 2)
    @Description("增加玩家经验")
    public void addExp(CommandContext ctx) {
        Player target = ctx.getPlayerArg(0);
        try {
            long amount = Long.parseLong(ctx.getStringArg(1));
            if (classService.addExp(target, amount)) {
                ctx.sendSuccess("已添加 " + amount + " 点经验！");
            } else {
                ctx.sendError("添加经验失败！");
            }
        } catch (NumberFormatException e) {
            ctx.sendError("无效的数量！");
        }
    }

    @SubCommand(name = "setexp", minArgs = 2, maxArgs = 2)
    @Description("设置玩家经验")
    public void setExp(CommandContext ctx) {
        Player target = ctx.getPlayerArg(0);
        try {
            long amount = Long.parseLong(ctx.getStringArg(1));
            if (classService.setExp(target, amount)) {
                ctx.sendSuccess("经验已设置为 " + amount + "！");
            } else {
                ctx.sendError("设置经验失败！");
            }
        } catch (NumberFormatException e) {
            ctx.sendError("无效的数量！");
        }
    }

    @SubCommand(name = "addattr", minArgs = 2, maxArgs = 2)
    @Description("增加玩家属性点")
    public void addAttr(CommandContext ctx) {
        Player target = ctx.getPlayerArg(0);
        try {
            int amount = Integer.parseInt(ctx.getStringArg(1));
            if (amount <= 0) {
                ctx.sendError("数量必须大于0！");
                return;
            }
            attributeManager.grantAttributePoints(target, amount);
            ctx.sendSuccess("已给予 " + target.getName() + " " + amount + " 点属性点！");
            MiniMessageService miniMessage = MiniMessageService.getInstance();
            target.sendMessage(miniMessage.colorize("<gold>你获得了 " + amount + " 点属性点！使用 /class attr 进行分配"));
        } catch (NumberFormatException e) {
            ctx.sendError("无效的数量！");
        }
    }

    @SubCommand(name = "reload")
    @Description("重载配置文件")
    public void reload(CommandContext ctx) {
        plugin.reloadConfig();
        classManager.reload();
        ctx.sendSuccess("配置已重新加载！");
    }

    @SubCommand(name = "help")
    @Description("显示管理员帮助")
    public void help(CommandContext ctx) {
        showHelp(ctx.getSender());
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        String subCommandName = subCommandMethod.getAnnotation(SubCommand.class).name();
        return switch (subCommandName) {
            case "set", "addexp", "setexp", "addattr" -> {
                if (context.getArgCount() <= 1) {
                    yield Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                } else if (context.getArgCount() == 2 && subCommandName.equals("set")) {
                    yield classManager.getAllClasses().stream().map(GameClass::getId).collect(Collectors.toList());
                }
                yield List.of();
            }
            default -> List.of();
        };
    }
}
