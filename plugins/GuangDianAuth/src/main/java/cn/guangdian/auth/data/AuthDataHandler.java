package cn.guangdian.auth.data;

import cn.guangdian.auth.GuangDianAuth;
import cn.guangdian.auth.handler.SessionManager;
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import org.bukkit.entity.Player;

/**
 * 认证数据处理器
 * 使用 RPGCore AbstractPlayerDataHandler 管理玩家认证数据生命周期
 */
public class AuthDataHandler extends AbstractPlayerDataHandler {

    private final GuangDianAuth plugin;
    private final AuthDataManager dataManager;
    private final SessionManager sessionManager;

    public AuthDataHandler(GuangDianAuth plugin, AuthDataManager dataManager, SessionManager sessionManager) {
        super(plugin);
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.sessionManager = sessionManager;
    }

    @Override
    protected void onPlayerLoad(Player player) {
        // 检查玩家是否已注册
        boolean isRegistered = dataManager.isRegistered(player.getName());
        
        if (isRegistered) {
            plugin.logDebug("玩家 " + player.getName() + " 已注册，等待登录");
            // 发送登录提示
            plugin.sendLoginPrompt(player);
        } else {
            plugin.logDebug("玩家 " + player.getName() + " 未注册，等待注册");
            // 发送注册提示
            plugin.sendLoginPrompt(player);
        }
        
        // 初始化会话（未登录状态）
        sessionManager.setLoggedIn(player.getUniqueId(), false);
    }

    @Override
    protected void onPlayerSave(Player player) {
        // 保存会话数据（SessionManager 中会话是临时的，无需持久化）
        sessionManager.saveAll();
        
        // 移除会话
        sessionManager.removeSession(player.getUniqueId());
        
        plugin.logDebug("玩家 " + player.getName() + " 认证数据已保存");
    }

    @Override
    public int getPriority() {
        // 认证数据优先级较高，确保在其他数据之前加载
        return 10;
    }

    @Override
    public String getHandlerName() {
        return "AuthData";
    }
}
