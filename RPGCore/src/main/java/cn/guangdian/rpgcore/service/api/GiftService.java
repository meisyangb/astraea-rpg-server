package cn.guangdian.rpgcore.service.api;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

/**
 * 礼包服务接口
 * 
 * <p>提供礼包发放、查询等功能。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface GiftService {

    /**
     * 给予玩家礼包
     * 
     * @param player 玩家
     * @param giftName 礼包名称
     * @return 是否成功
     */
    boolean giveGift(Player player, String giftName);

    /**
     * 检查礼包是否存在
     * 
     * @param giftName 礼包名称
     * @return 是否存在
     */
    boolean hasGift(String giftName);

    /**
     * 获取礼包内容
     * 
     * @param giftName 礼包名称
     * @return 物品ID列表
     */
    List<String> getGiftItems(String giftName);

    /**
     * 获取所有礼包名称
     * 
     * @return 礼包名称集合
     */
    Set<String> getGiftNames();

    /**
     * 获取礼包数量
     * 
     * @return 礼包数量
     */
    int getGiftCount();

    /**
     * 重载礼包配置
     */
    void reloadGifts();

    /**
     * 检查服务是否可用
     * 
     * @return 如果服务可用返回 true
     */
    boolean isAvailable();
}
