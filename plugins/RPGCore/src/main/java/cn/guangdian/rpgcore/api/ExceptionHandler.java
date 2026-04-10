package cn.guangdian.rpgcore.api;

import org.bukkit.entity.Player;

import java.util.function.Supplier;

public interface ExceptionHandler {
    
    <T> T safeCall(Supplier<T> operation, T defaultValue);
    
    void safeRun(Runnable operation);
    
    <T> T safeCallWithLog(Supplier<T> operation, T defaultValue, String context);
    
    void safeRunWithLog(Runnable operation, String context);
    
    void handleException(Throwable throwable, String context);
    
    void handleException(Throwable throwable, String context, Player player);
    
    void setLogLevel(LogLevel level);
    
    LogLevel getLogLevel();
    
    enum LogLevel {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
}
