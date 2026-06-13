package cn.guangdian.rpgcore.service.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * 灵魂绑定服务接口
 */
public interface SoulBindService {

    /**
     * 检查物品是否被绑定
     */
    boolean isBound(ItemStack item);

    /**
     * 检查物品是否绑定到指定玩家
     */
    boolean isBoundTo(ItemStack item, UUID playerId);

    /**
     * 获取绑定玩家的UUID
     */
    UUID getBoundPlayer(ItemStack item);

    /**
     * 获取绑定玩家的名称
     */
    String getBoundPlayerName(ItemStack item);

    /**
     * 绑定物品到玩家
     */
    boolean bindItem(ItemStack item, Player player);

    /**
     * 解除物品绑定
     */
    boolean unbindItem(ItemStack item);

    /**
     * 检查玩家是否可以与物品交互
     */
    boolean canInteract(ItemStack item, Player player);

    /**
     * 检查是否是MythicMobs物品
     */
    boolean isMythicMobsItem(ItemStack item);
}