package cn.guangdian.rpgcore.exception;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ExceptionHandler;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExceptionHandlerImpl implements ExceptionHandler {
    
    private final JavaPlugin plugin;
    private final Logger logger;
    private LogLevel logLevel = LogLevel.INFO;
    
    public ExceptionHandlerImpl(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    @Override
    public <T> T safeCall(Supplier<T> operation, T defaultValue) {
        try {
            return operation.get();
        } catch (Exception e) {
            handleException(e, "safeCall", null);
            return defaultValue;
        }
    }
    
    @Override
    public void safeRun(Runnable operation) {
        try {
            operation.run();
        } catch (Exception e) {
            handleException(e, "safeRun", null);
        }
    }
    
    @Override
    public <T> T safeCallWithLog(Supplier<T> operation, T defaultValue, String context) {
        try {
            return operation.get();
        } catch (Exception e) {
            handleException(e, context, null);
            return defaultValue;
        }
    }
    
    @Override
    public void safeRunWithLog(Runnable operation, String context) {
        try {
            operation.run();
        } catch (Exception e) {
            handleException(e, context, null);
        }
    }
    
    @Override
    public void handleException(Throwable throwable, String context) {
        handleException(throwable, context, null);
    }
    
    @Override
    public void handleException(Throwable throwable, String context, Player player) {
        Level level = mapLogLevel(this.logLevel);
        
        StringBuilder message = new StringBuilder();
        message.append("[").append(context).append("] ");
        message.append(throwable.getClass().getSimpleName());
        message.append(": ").append(throwable.getMessage());
        
        if (player != null) {
            message.append(" (Player: ").append(player.getName()).append(")");
        }
        
        logger.log(level, message.toString(), throwable);
        
        if (player != null && player.isOnline()) {
            MiniMessageService mm = MiniMessageService.getInstance();
            player.sendMessage(mm.red("发生错误: " + throwable.getMessage()));
        }
    }
    
    @Override
    public void setLogLevel(LogLevel level) {
        this.logLevel = level;
    }
    
    @Override
    public LogLevel getLogLevel() {
        return logLevel;
    }
    
    private Level mapLogLevel(LogLevel level) {
        return switch (level) {
            case TRACE -> Level.FINEST;
            case DEBUG -> Level.FINE;
            case INFO -> Level.INFO;
            case WARN -> Level.WARNING;
            case ERROR -> Level.SEVERE;
        };
    }
}
