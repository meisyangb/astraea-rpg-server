package cn.guangdian.rpgcore.service.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 物品属性解析服务接口
 * 
 * <p>提供物品Lore属性解析功能，由 GuangDianArmorStats 实现。</p>
 * <p>其他插件（如 GuangDianAccessory）通过 RPGCore 服务注册表获取此服务。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface AttributeParseService {

    /**
     * 解析物品的属性
     * 
     * @param item 物品
     * @return 属性映射（属性名 -> 值），具体类型由实现定义
     */
    Map<String, Object> parseItemAttributes(ItemStack item);

    /**
     * 创建空的属性容器
     * 
     * @return 空属性容器
     */
    Object createEmptyAttributes();

    /**
     * 添加属性到容器
     * 
     * @param container 属性容器
     * @param attributeName 属性名
     * @param value 属性值
     */
    void addAttribute(Object container, String attributeName, Object value);

    /**
     * 合并属性到容器
     * 
     * @param container 属性容器
     * @param attributes 要合并的属性
     */
    void mergeAttributes(Object container, Map<String, Object> attributes);

    /**
     * 设置外部配饰属性
     * 用于独立的配饰槽位系统（如 GuangDianAccessory）
     * 
     * @param player 玩家
     * @param accessoryAttributes 配饰属性容器
     */
    void setExternalAccessoryStats(Player player, Object accessoryAttributes);

    /**
     * 检查服务是否可用
     * 
     * @return 服务是否可用
     */
    boolean isAvailable();
}
