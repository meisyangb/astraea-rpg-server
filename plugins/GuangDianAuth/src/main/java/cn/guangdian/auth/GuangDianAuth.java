package cn.guangdian.auth;

import cn.guangdian.auth.command.AuthCommands;
import cn.guangdian.auth.data.AuthDataManager;
import cn.guangdian.auth.handler.AuthPacketHandler;
import cn.guangdian.auth.handler.SessionManager;
import cn.guangdian.auth.listener.AuthListener;
import cn.guangdian.rpgcore.database.CoreDatabase;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;

public class GuangDianAuth extends AbstractRPGPlugin {

    private static GuangDianAuth instance;
    private AuthDataManager dataManager;
    private SessionManager sessionManager;
    private AuthPacketHandler packetHandler;
    private AuthConfig authConfig;

    public static GuangDianAuth getInstance() {
        return instance;
    }

    @Override
    protected void onPluginEnable() {
        instance = this;
        
        if (!CoreDatabase.isEnabled()) {
            getLogger().severe("CoreDatabase 未初始化！请确保 RPGCore 正确配置了数据库连接");
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
        
        getLogger().info("GuangDianAuth 已启动 - 独立登录系统已激活");
        getLogger().info("注册玩家数: " + dataManager.getRegisteredCount());
    }

    @Override
    protected void onPluginDisable() {
        if (packetHandler != null) {
            packetHandler.unregister();
        }
        
        if (sessionManager != null) {
            sessionManager.saveAll();
        }
        
        getLogger().info("GuangDianAuth 已关闭");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianAuth";
    }

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
        if (isRegistered(player.getName())) {
            player.sendMessage(Component.text("请使用 /login <密码> 登录").color(NamedTextColor.YELLOW));
        } else {
            player.sendMessage(Component.text("请使用 /register <密码> <确认密码> 注册").color(NamedTextColor.YELLOW));
        }
    }

    public void kickPlayer(Player player, String reason) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            player.kick(Component.text(reason).color(NamedTextColor.RED));
        }, 10L);
    }
}
