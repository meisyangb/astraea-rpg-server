package cn.guangdian.rpgcore.command;

/**
 * 命令执行异常
 *
 * <p>用于在命令执行过程中抛出业务逻辑错误。</p>
 *
 * @author Astraea RPG Team
 * @since 1.1.0
 */
public class CommandException extends RuntimeException {

    public CommandException(String message) {
        super(message);
    }

    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
