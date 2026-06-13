package cn.guangdian.rpgcore.concurrency;

/**
 * 锁超时异常
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class LockTimeoutException extends Exception {

    public LockTimeoutException(String message) {
        super(message);
    }

    public LockTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}