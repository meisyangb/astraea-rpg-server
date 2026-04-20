package cn.guangdian.auth;

import cn.guangdian.auth.adapter.AuthServiceAdapter;
import cn.guangdian.auth.command.AuthCommands;
import cn.guangdian.auth.data.AuthDataManager;
import cn.guangdian.auth.handler.AuthPacketHandler;
import cn.guangdian.auth.handler.SessionManager;
import cn.guangdian.auth.listener.AuthListener;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.api.GameLogger;
import cn.guangdian.rpgcore.database.CoreDatabase;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;

/**
 * GuangDianAuth - 光点登录认证插件
 * 
 * <p>本插件已优化集成 RPGCore 服务：</p>
 * <ul>
 *   <li>日志系统 - 使用 RPGCore GameLogger（带降级兼容）</li>
 *   <li>异步执行 - 使用 RPGCore AsyncExecutor（带降级兼容）</li>
 *   <li>消息发送 - 使用 RPGCore MiniMessageService（带降级兼容）</li>
 * </ul>
 * 
 * <p>当 RPGCore 不可用时，自动降级到 Bukkit 原生实现。</p>
 * 
 * @author GuangDian
 * @version 1.1.0
 * @since 1.0.0
 * @see OPTIMIZATION.md 优化详情
 */
public class GuangDianAuth extends AbstractRPGPlugin {

    private static GuangDianAuth instance;
    private AuthDataManager dataManager;
    private SessionManager sessionManager;
    private AuthPacketHandler packetHandler;
    private AuthConfig authConfig;
    private AuthServiceAdapter serviceAdapter;
    
    // RPGCore 服务
    private GameLogger gameLogger;
    private AsyncExecutor asyncExecutor;
    private MiniMessageService miniMessage;

    public static GuangDianAuth getInstance() {
        return instance;
    }

    @Override
    protected void onPluginEnable() {
        instance = this;
        
        // 初始化 RPGCore 服务
        initRPGCoreServices();
        
        if (!CoreDatabase.isEnabled()) {
            logSevere("CoreDatabase 未初始化！请确保 RPGCore 正确配置了数据库连接");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        saveDefaultConfig();
        authConfig = new AuthConfig(new File(getDataFolder(), "config.yml"));
        authConfig.load();
        
        dataManager = new AuthDataManager(this);
        dataManager.initialize();
        
        sessionManager = new SessionManager(this);
        
        packetHandler = new AuthPacketHandler(this);
        packetHandler.register();
        
        AuthCommands commands = new AuthCommands(this);
        commands.registerAll();
        
        getServer().getPluginManager().registerEvents(new AuthListener(this), this);
        
        serviceAdapter = new AuthServiceAdapter(this);
        
        logInfo("GuangDianAuth 已启动 - 独立登录系统已激活");
        logInfo("注册玩家数: " + dataManager.getRegisteredCount());
    }
    
    /**
     * 初始化 RPGCore 服务
     */
    private void initRPGCoreServices() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            this.gameLogger = rpgCore.getGameLogger();
            this.asyncExecutor = rpgCore.getAsyncExecutor();
            this.miniMessage = rpgCore.getMiniMessageService();
        } else {
            // 降级：使用 Bukkit 原生
            getLogger().warning("RPGCore 不可用，使用备用日志");
        }
    }

    @Override
    protected void onPluginDisable() {
        // 取消所有调度任务
        cancelAllTasks();
        
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        
        if (packetHandler != null) {
            packetHandler.unregister();
        }
        
        if (sessionManager != null) {
            sessionManager.saveAll();
        }
        
        logInfo("GuangDianAuth 已关闭");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianAuth";
    }

    // ==================== RPGCore 服务访问 ====================
    
    public GameLogger getGameLogger() {
        return gameLogger != null ? gameLogger : null;
    }
    
    public AsyncExecutor getAsyncExecutor() {
        return asyncExecutor != null ? asyncExecutor : null;
    }
    
    public MiniMessageService getMiniMessage() {
        return miniMessage != null ? miniMessage : null;
    }
    
    /**
     * 检查是否使用 RPGCore 服务
     */
    public boolean isUsingRPGCore() {
        return gameLogger != null;
    }
    
    // ==================== 日志快捷方法 ====================
    
    public void logInfo(String message) {
        if (gameLogger != null) {
            gameLogger.info(message);
        } else {
            getLogger().info(message);
        }
    }
    
    public void logWarning(String message) {
        if (gameLogger != null) {
            gameLogger.warning(message);
        } else {
            getLogger().warning(message);
        }
    }
    
    public void logSevere(String message) {
        if (gameLogger != null) {
            gameLogger.severe(message);
        } else {
            getLogger().severe(message);
        }
    }
    
    public void logSevere(String message, Throwable throwable) {
        if (gameLogger != null) {
            gameLogger.severe(message, throwable);
        } else {
            getLogger().severe(message + " - " + throwable.getMessage());
            throwable.printStackTrace();
        }
    }
    
    public void logDebug(String message) {
        if (gameLogger != null) {
            gameLogger.debug(message);
        }
    }

    // ==================== 业务方法 ====================

    public AuthDataManager getDataManager() {
        return dataManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public AuthPacketHandler getPacketHandler() {
        return packetHandler;
    }

    public AuthConfig getAuthConfig() {
        return authConfig;
    }

    public boolean isRegistered(String playerName) {
        return dataManager.isRegistered(playerName);
    }

    public boolean isLoggedIn(Player player) {
        return sessionManager.isLoggedIn(player.getUniqueId());
    }

    public void sendLoginPrompt(Player player) {
        if (miniMessage != null) {
            if (isRegistered(player.getName())) {
                player.sendMessage(miniMessage.yellow("请使用 /login <密码> 登录"));
            } else {
                player.sendMessage(miniMessage.yellow("请使用 /register <密码> <确认密码> 注册"));
            }
        } else {
            // 降级
            if (isRegistered(player.getName())) {
                player.sendMessage(Component.text("请使用 /login <密码> 登录").color(NamedTextColor.YELLOW));
            } else {
                player.sendMessage(Component.text("请使用 /register <密码> <确认密码> 注册").color(NamedTextColor.YELLOW));
            }
        }
    }

    public void kickPlayer(Player player, String reason) {
        scheduler.runSyncLater(() -> {
            if (miniMessage != null) {
                player.kick(miniMessage.red(reason));
            } else {
                player.kick(Component.text(reason).color(NamedTextColor.RED));
            }
        }, 10L);
    }
}
