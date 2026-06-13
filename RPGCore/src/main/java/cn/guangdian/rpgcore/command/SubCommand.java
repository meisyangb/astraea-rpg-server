package cn.guangdian.rpgcore.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 子命令注解
 *
 * <p>用于标注子命令方法。</p>
 *
 * <h2>使用示例:</h2>
 * <pre>{@code
 * @SubCommand(name="give", permission="guangdian.points.admin")
 * @Description("给予玩家点数")
 * public void give(CommandContext ctx) {
 *     Player target = ctx.getPlayerArg(0);
 *     long amount = ctx.getLongArg(1);
 *     // 业务逻辑...
 * }
 * }</pre>
 *
 * @author Astraea RPG Team
 * @since 1.1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubCommand {

    /**
     * 子命令名称
     */
    String name();

    /**
     * 权限节点 (留空表示无权限要求)
     */
    String permission() default "";

    /**
     * 是否仅玩家可用
     */
    boolean playerOnly() default false;

    /**
     * 最小参数数量
     */
    int minArgs() default 0;

    /**
     * 最大参数数量 (-1 表示无限制)
     */
    int maxArgs() default -1;
}
