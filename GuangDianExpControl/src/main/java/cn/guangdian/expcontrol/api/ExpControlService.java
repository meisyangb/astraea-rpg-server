package cn.guangdian.expcontrol.api;

import org.bukkit.entity.Player;

/**
 * 经验控制服务API接口
 * 
 * <p>供其他插件调用来发放经验</p>
 * 
 * <h3>使用示例:</h3>
 * <pre>{@code
 * // 获取服务
 * ExpControlService service = Bukkit.getServicesManager().load(ExpControlService.class);
 * 
 * // 给玩家发放经验
 * service.giveExp(player, 100);
 * 
 * // 设置玩家经验
 * service.setExp(player, 500);
 * }</pre>
 */
public interface ExpControlService {
    
    /**
     * 给玩家发放经验
     * 
     * @param player 目标玩家
     * @param amount 经验数量
     * @return 实际发放的经验数量
     */
    int giveExp(Player player, int amount);
    
    /**
     * 给玩家发放经验（带来源标识）
     * 
     * @param player 目标玩家
     * @param amount 经验数量
     * @param source 经验来源（用于日志/调试）
     * @return 实际发放的经验数量
     */
    int giveExp(Player player, int amount, String source);
    
    /**
     * 设置玩家的经验值
     * 
     * @param player 目标玩家
     * @param amount 经验值
     */
    void setExp(Player player, int amount);
    
    /**
     * 设置玩家的等级
     * 
     * @param player 目标玩家
     * @param level 等级
     */
    void setLevel(Player player, int level);
    
    /**
     * 获取玩家的经验值
     * 
     * @param player 目标玩家
     * @return 当前经验值
     */
    int getExp(Player player);
    
    /**
     * 获取玩家的等级
     * 
     * @param player 目标玩家
     * @return 当前等级
     */
    int getLevel(Player player);
    
    /**
     * 获取玩家升到下一级所需的经验
     * 
     * @param player 目标玩家
     * @return 升级所需经验
     */
    int getExpToLevel(Player player);
    
    /**
     * 重置玩家的经验（归零）
     * 
     * @param player 目标玩家
     */
    void resetExp(Player player);
    
    /**
     * 扣除玩家的经验
     * 
     * @param player 目标玩家
     * @param amount 扣除数量
     * @return 实际扣除的经验数量
     */
    int takeExp(Player player, int amount);
    
    /**
     * 检查玩家是否可以获取经验
     * 
     * @param player 目标玩家
     * @return 是否可以获取经验
     */
    boolean canGainExp(Player player);
    
    /**
     * 计算等级对应的总经验
     * 
     * @param level 目标等级
     * @return 总经验值
     */
    int calculateTotalExpForLevel(int level);
}
