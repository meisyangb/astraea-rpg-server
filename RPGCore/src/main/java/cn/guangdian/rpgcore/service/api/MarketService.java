package cn.guangdian.rpgcore.service.api;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * 市场服务接口
 * 
 * <p>提供全球市场功能。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface MarketService {

    /**
     * 上架物品
     * 
     * @param sellerId 卖家UUID
     * @param item 物品
     * @param price 价格
     * @return 如果成功返回 true
     */
    boolean listItem(UUID sellerId, ItemStack item, long price);

    /**
     * 购买物品
     * 
     * @param buyerId 买家UUID
     * @param listingId 上架ID
     * @return 如果成功返回 true
     */
    boolean purchaseItem(UUID buyerId, String listingId);

    /**
     * 取消上架
     * 
     * @param sellerId 卖家UUID
     * @param listingId 上架ID
     * @return 如果成功返回 true
     */
    boolean cancelListing(UUID sellerId, String listingId);

    /**
     * 获取玩家的上架列表
     * 
     * @param sellerId 卖家UUID
     * @return 上架物品列表
     */
    List<Object> getPlayerListings(UUID sellerId);

    /**
     * 获取市场物品数量
     * 
     * @return 市场物品数量
     */
    int getMarketSize();

    /**
     * 获取玩家的上架数量
     * 
     * @param sellerId 卖家UUID
     * @return 上架数量
     */
    int getPlayerListingCount(UUID sellerId);

    /**
     * 检查服务是否可用
     * 
     * @return 如果服务可用返回 true
     */
    boolean isAvailable();
}