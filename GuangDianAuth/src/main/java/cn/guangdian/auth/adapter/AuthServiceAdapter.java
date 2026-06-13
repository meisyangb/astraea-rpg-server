package cn.guangdian.auth.adapter;

import cn.guangdian.auth.GuangDianAuth;
import cn.guangdian.auth.data.AuthDataManager;
import cn.guangdian.auth.handler.SessionManager;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.service.api.AuthService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 认证服务适配器
 *
 * <p>连接 GuangDianAuth 实现与 AuthService 接口。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class AuthServiceAdapter implements AuthService {

    private final GuangDianAuth plugin;
    private final boolean useRPGCore;
    private Logger logger;

    public AuthServiceAdapter(GuangDianAuth plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        this.logger = plugin.getLogger();

        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();

                registry.registerService(AuthService.class, this);
                logger.info("已注册到 RPGCore: AuthService");

            } catch (Exception e) {
                logger.warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean isRegistered(String playerName) {
        return plugin.getDataManager().isRegistered(playerName);
    }

    @Override
    public boolean isLoggedIn(Player player) {
        return plugin.getSessionManager().isLoggedIn(player.getUniqueId());
    }

    @Override
    public boolean isLoggedIn(UUID playerId) {
        return plugin.getSessionManager().isLoggedIn(playerId);
    }

    @Override
    public boolean register(String playerName, UUID uuid, String password, String ip) {
        AuthDataManager dataManager = plugin.getDataManager();
        if (dataManager.isRegistered(playerName)) {
            return false;
        }
        dataManager.register(playerName, uuid, password, ip);
        return true;
    }

    @Override
    public boolean checkPassword(String playerName, String password) {
        return plugin.getDataManager().checkPassword(playerName, password);
    }

    @Override
    public boolean changePassword(String playerName, String newPassword) {
        AuthDataManager dataManager = plugin.getDataManager();
        if (!dataManager.isRegistered(playerName)) {
            return false;
        }
        dataManager.changePassword(playerName, newPassword);
        return true;
    }

    @Override
    public void unregister(String playerName) {
        plugin.getDataManager().unregister(playerName);
    }

    @Override
    public void forceLogin(Player player) {
        SessionManager sessionManager = plugin.getSessionManager();
        sessionManager.setLoggedIn(player.getUniqueId(), true);
        plugin.getDataManager().updateLastLogin(
            player.getName(), 
            player.getAddress().getAddress().getHostAddress()
        );
        plugin.getPacketHandler().notifyLoggedIn(player);
    }

    @Override
    public void forceLogout(Player player) {
        SessionManager sessionManager = plugin.getSessionManager();
        sessionManager.setLoggedIn(player.getUniqueId(), false);
    }

    @Override
    public Optional<Long> getRegisterDate(String playerName) {
        return plugin.getDataManager().getByName(playerName)
            .map(data -> data.getRegisterDate());
    }

    @Override
    public Optional<Long> getLastLogin(String playerName) {
        return plugin.getDataManager().getByName(playerName)
            .map(data -> data.getLastLogin());
    }

    @Override
    public int getRegisteredCount() {
        return plugin.getDataManager().getRegisteredCount();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    public void unregister() {
        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.unregisterService(AuthService.class);
                logger.info("已从 RPGCore 注销: AuthService");
            } catch (Exception e) {
                logger.warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }

    public boolean isUsingRPGCore() {
        return useRPGCore;
    }
}
