package cn.guangdian.rpgcore.service.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 强化服务接口
 *
 * <p>提供装备强化功能。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public interface EnhanceService {

    /**
     * 获取装备强化等级
     *
     * @param item 物品
     * @return 强化等级
     */
    int getEnhanceLevel(ItemStack item);

    /**
     * 执行强化
     *
     * @param player 玩家
     * @param item 物品
     * @return 强化结果
     */
    Object enhance(Player player, ItemStack item);

    /**
     * 计算成功率
     *
     * @param currentLevel 当前等级
     * @param item 物品
     * @return 成功率（0.0 - 1.0）
     */
    double calculateSuccessRate(int currentLevel, @Nullable ItemStack item);

    /**
     * 获取属性加成倍率
     *
     * @param level 强化等级
     * @return 属性加成倍率
     */
    double getAttributeMultiplier(int level);

    /**
     * 获取强化材料消耗
     *
     * @param currentLevel 当前等级
     * @return 材料列表
     */
    List<ItemStack> getMaterialCost(int currentLevel);

    /**
     * 检查是否可强化
     *
     * @param item 物品
     * @return 是否可强化
     */
    boolean canEnhance(ItemStack item);

    /**
     * 检查服务是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();
}
