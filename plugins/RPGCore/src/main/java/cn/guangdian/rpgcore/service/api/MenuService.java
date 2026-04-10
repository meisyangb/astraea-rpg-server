package cn.guangdian.rpgcore.service.api;

import org.bukkit.entity.Player;

import java.util.List;

/**
 * 菜单服务接口
 * 
 * <p>提供菜单系统功能。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface MenuService {

    /**
     * 打开菜单
     * 
     * @param player 玩家
     * @param menuName 菜单名称
     * @return 如果成功返回 true
     */
    boolean openMenu(Player player, String menuName);

    /**
     * 关闭菜单
     * 
     * @param player 玩家
     */
    void closeMenu(Player player);

    /**
     * 检查菜单是否存在
     * 
     * @param menuName 菜单名称
     * @return 如果存在返回 true
     */
    boolean hasMenu(String menuName);

    /**
     * 获取所有菜单名称
     * 
     * @return 菜单名称列表
     */
    List<String> getMenuNames();

    /**
     * 重新加载菜单配置
     */
    void reloadMenus();

    /**
     * 检查服务是否可用
     * 
     * @return 如果服务可用返回 true
     */
    boolean isAvailable();
}