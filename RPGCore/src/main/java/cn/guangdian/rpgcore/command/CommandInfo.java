package cn.guangdian.rpgcore.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 命令信息注解
 *
 * <p>用于标注命令类的基本信息。</p>
 *
 * <h2>使用示例:</h2>
 * <pre>{@code
 * @CommandInfo(name="points", description="点数管理", permission="guangdian.points.use")
 * public class PointsCommand extends BaseCommand {
 *     // ...
 * }
 * }</pre>
 *
 * @author Astraea RPG Team
 * @since 1.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandInfo {

    /**
     * 命令名称 (对应 plugin.yml 中的命令名)
     */
    String name();

    /**
     * 命令描述
     */
    String description() default "";

    /**
     * 权限节点 (留空表示无权限要求)
     */
    String permission() default "";

    /**
     * 是否仅玩家可用
     */
    boolean playerOnly() default false;

    /**
     * 命令别名
     */
    String[] aliases() default {};
}
