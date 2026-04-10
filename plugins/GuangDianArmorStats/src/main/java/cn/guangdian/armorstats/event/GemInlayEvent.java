package cn.guangdian.armorstats.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

import cn.guangdian.armorstats.data.AttributeValue;

/**
 * 宝石镶嵌事件
 * 
 * <p>当玩家完成宝石镶嵌时触发此事件。</p>
 * 
 * <p>此事件用于：</p>
 * <ul>
 *   <li>通知其他系统装备属性已变更</li>
 *   <li>触发装备缓存刷新</li>
 *   <li>记录镶嵌日志</li>
 * </ul>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class GemInlayEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player player;
    private final ItemStack equipment;
    private final ItemStack equipmentBefore;
    private final List<ItemStack> gemsInlaid;
    private final Map<String, AttributeValue> addedAttributes;
    private final int totalGems;
    private final boolean isRework;  // 是否是拆卸操作

    /**
     * 构造函数 - 镶嵌操作
     * 
     * @param player 执行镶嵌的玩家
     * @param equipment 镶嵌后的装备
     * @param equipmentBefore 镶嵌前的装备
     * @param gemsInlaid 镶嵌的宝石列表
     * @param addedAttributes 新增的属性
     */
    public GemInlayEvent(Player player, ItemStack equipment, ItemStack equipmentBefore,
                         List<ItemStack> gemsInlaid, Map<String, AttributeValue> addedAttributes) {
        this.player = player;
        this.equipment = equipment;
        this.equipmentBefore = equipmentBefore;
        this.gemsInlaid = gemsInlaid;
        this.addedAttributes = addedAttributes;
        this.totalGems = gemsInlaid != null ? gemsInlaid.size() : 0;
        this.isRework = false;
    }

    /**
     * 构造函数 - 拆卸操作
     * 
     * @param player 执行拆卸的玩家
     * @param equipment 拆卸后的装备
     * @param equipmentBefore 拆卸前的装备
     * @param isRework 标记为拆卸操作
     */
    public GemInlayEvent(Player player, ItemStack equipment, ItemStack equipmentBefore, boolean isRework) {
        this.player = player;
        this.equipment = equipment;
        this.equipmentBefore = equipmentBefore;
        this.gemsInlaid = null;
        this.addedAttributes = null;
        this.totalGems = 0;
        this.isRework = isRework;
    }

    /**
     * 获取执行操作的玩家
     * 
     * @return 玩家
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * 获取操作后的装备
     * 
     * @return 装备物品
     */
    public ItemStack getEquipment() {
        return equipment;
    }

    /**
     * 获取操作前的装备
     * 
     * @return 装备物品（原始状态）
     */
    public ItemStack getEquipmentBefore() {
        return equipmentBefore;
    }

    /**
     * 获取镶嵌的宝石列表
     * 
     * @return 宝石列表，拆卸操作返回 null
     */
    public List<ItemStack> getGemsInlaid() {
        return gemsInlaid;
    }

    /**
     * 获取新增的属性
     * 
     * @return 属性映射，拆卸操作返回 null
     */
    public Map<String, AttributeValue> getAddedAttributes() {
        return addedAttributes;
    }

    /**
     * 获取总宝石数量
     * 
     * @return 宝石数量
     */
    public int getTotalGems() {
        return totalGems;
    }

    /**
     * 是否是拆卸操作
     * 
     * @return 如果是拆卸返回 true，镶嵌返回 false
     */
    public boolean isRework() {
        return isRework;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}