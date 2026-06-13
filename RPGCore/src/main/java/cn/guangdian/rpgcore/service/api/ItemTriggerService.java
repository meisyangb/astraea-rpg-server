package cn.guangdian.rpgcore.service.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * 物品触发器服务接口
 * 
 * <p>提供物品触发器相关功能的服务接口，
 * 支持通过物品Lore关键词触发各种动作。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface ItemTriggerService {

    /**
     * 触发指定触发器
     * 
     * @param player 玩家
     * @param triggerName 触发器名称
     * @return 是否触发成功
     */
    boolean trigger(Player player, String triggerName);

    /**
     * 检查触发器是否存在
     * 
     * @param triggerName 触发器名称
     * @return 是否存在
     */
    boolean hasTrigger(String triggerName);

    /**
     * 获取触发器冷却时间
     * 
     * @param playerId 玩家UUID
     * @param triggerName 触发器名称
     * @return 剩余冷却时间（毫秒），0表示无冷却
     */
    long getCooldown(UUID playerId, String triggerName);

    /**
     * 检查触发器是否可用（不在冷却中）
     * 
     * @param playerId 玩家UUID
     * @param triggerName 触发器名称
     * @return 是否可用
     */
    boolean isAvailable(UUID playerId, String triggerName);

    /**
     * 重置玩家触发器冷却
     * 
     * @param playerId 玩家UUID
     * @param triggerName 触发器名称
     */
    void resetCooldown(UUID playerId, String triggerName);

    /**
     * 获取所有触发器名称
     * 
     * @return 触发器名称列表
     */
    List<String> getTriggerNames();

    /**
     * 获取触发器数量
     * 
     * @return 触发器数量
     */
    int getTriggerCount();

    /**
     * 检查物品是否包含触发关键词
     * 
     * @param item 物品
     * @return 是否包含触发关键词
     */
    boolean hasTriggerKeyword(ItemStack item);

    /**
     * 检查服务是否可用
     * 
     * @return 是否可用
     */
    boolean isAvailable();
}