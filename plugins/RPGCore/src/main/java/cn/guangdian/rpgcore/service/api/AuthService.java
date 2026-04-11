package cn.guangdian.rpgcore.service.api;

import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * 认证服务接口
 * 
 * <p>提供玩家登录/注册认证功能。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface AuthService {

    /**
     * 检查玩家是否已注册
     * 
     * @param playerName 玩家名称
     * @return 如果已注册返回 true
     */
    boolean isRegistered(String playerName);

    /**
     * 检查玩家是否已登录
     * 
     * @param player 玩家
     * @return 如果已登录返回 true
     */
    boolean isLoggedIn(Player player);

    /**
     * 检查玩家是否已登录
     * 
     * @param playerId 玩家UUID
     * @return 如果已登录返回 true
     */
    boolean isLoggedIn(UUID playerId);

    /**
     * 注册玩家
     * 
     * @param playerName 玩家名称
     * @param uuid 玩家UUID
     * @param password 密码
     * @param ip IP地址
     * @return 如果注册成功返回 true
     */
    boolean register(String playerName, UUID uuid, String password, String ip);

    /**
     * 验证密码
     * 
     * @param playerName 玩家名称
     * @param password 密码
     * @return 如果密码正确返回 true
     */
    boolean checkPassword(String playerName, String password);

    /**
     * 修改密码
     * 
     * @param playerName 玩家名称
     * @param newPassword 新密码
     * @return 如果修改成功返回 true
     */
    boolean changePassword(String playerName, String newPassword);

    /**
     * 注销账号
     * 
     * @param playerName 玩家名称
     */
    void unregister(String playerName);

    /**
     * 强制玩家登录
     * 
     * @param player 玩家
     */
    void forceLogin(Player player);

    /**
     * 强制玩家登出
     * 
     * @param player 玩家
     */
    void forceLogout(Player player);

    /**
     * 获取玩家注册时间
     * 
     * @param playerName 玩家名称
     * @return 注册时间戳，如果未注册返回空
     */
    Optional<Long> getRegisterDate(String playerName);

    /**
     * 获取玩家最后登录时间
     * 
     * @param playerName 玩家名称
     * @return 最后登录时间戳，如果未注册返回空
     */
    Optional<Long> getLastLogin(String playerName);

    /**
     * 获取已注册玩家数量
     * 
     * @return 已注册玩家数量
     */
    int getRegisteredCount();

    /**
     * 检查服务是否可用
     * 
     * @return 如果服务可用返回 true
     */
    boolean isAvailable();
}
