package cn.guangdian.rpgcore.command;

import cn.guangdian.rpgcore.message.UnifiedMessageService;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 命令基类 - 所有注解驱动的命令都应继承此类
 *
 * <h2>使用示例:</h2>
 * <pre>{@code
 * @CommandInfo(name="points", description="点数管理", permission="guangdian.points.use")
 * public class PointsCommand extends BaseCommand {
 *
 *     @SubCommand(name="give", permission="guangdian.points.admin")
 *     @Description("给予玩家点数")
 *     public void give(CommandContext ctx) {
 *         Player target = ctx.getPlayerArg(0);
 *         long amount = ctx.getLongArg(1);
 *         pointsService.addPoints(target.getUniqueId(), amount);
 *         ctx.sendSuccess("已给予 " + target.getName() + " " + amount + " 点数");
 *     }
 *
 *     @SubCommand(name="balance")
 *     @Description("查看余额")
 *     public void balance(CommandContext ctx) {
 *         Player player = ctx.requirePlayer();
 *         long balance = pointsService.getBalance(player.getUniqueId());
 *         ctx.sendMessage("<green>你的余额: " + balance);
 *     }
 * }
 * }</pre>
 *
 * @author Astraea RPG Team
 * @since 1.1.0
 */
public abstract class BaseCommand {

    protected final UnifiedMessageService msg = UnifiedMessageService.getInstance();

    /**
     * 获取子命令方法 (通过注解查找)
     */
    public Method getSubCommandMethod(@NotNull String subCommandName) {
        for (Method method : getClass().getMethods()) {
            SubCommand annotation = method.getAnnotation(SubCommand.class);
            if (annotation != null && annotation.name().equalsIgnoreCase(subCommandName)) {
                return method;
            }
        }
        return null;
    }

    /**
     * 获取所有子命令方法
     */
    public List<Method> getSubCommandMethods() {
        List<Method> methods = new ArrayList<>();
        for (Method method : getClass().getMethods()) {
            if (method.isAnnotationPresent(SubCommand.class)) {
                methods.add(method);
            }
        }
        return methods;
    }

    /**
     * 显示帮助信息
     */
    public void showHelp(@NotNull CommandSender sender) {
        CommandInfo info = getClass().getAnnotation(CommandInfo.class);
        if (info == null) {
            msg.sendMessage(sender, "<red>命令配置错误!");
            return;
        }

        msg.sendMessage(sender, "<gold>========== " + info.description() + " ==========");

        for (Method method : getSubCommandMethods()) {
            SubCommand subCmd = method.getAnnotation(SubCommand.class);
            Description desc = method.getAnnotation(Description.class);

            // 检查权限
            if (!subCmd.permission().isEmpty() && !sender.hasPermission(subCmd.permission())) {
                continue;
            }

            String commandText = "<yellow>/" + info.name() + " " + subCmd.name();
            String descriptionText = desc != null ? " <gray>- " + desc.value() : "";

            msg.sendMessage(sender, commandText + descriptionText);
        }

        msg.sendMessage(sender, "<gold>==================================");
    }

    /**
     * Tab 补全实现 (可重写以提供自定义补全)
     */
    public List<String> onTabComplete(@NotNull Method subCommandMethod, @NotNull CommandContext context) {
        // 默认返回空列表，子类可以重写提供更智能的补全
        return new ArrayList<>();
    }
}
