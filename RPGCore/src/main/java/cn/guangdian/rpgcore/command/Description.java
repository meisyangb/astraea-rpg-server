package cn.guangdian.rpgcore.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 命令描述注解
 *
 * <p>用于提供子命令的详细描述，在帮助信息中显示。</p>
 *
 * @author Astraea RPG Team
 * @since 1.1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Description {

    /**
     * 命令描述文本
     */
    String value();
}
