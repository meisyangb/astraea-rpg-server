package cn.guangdian.rpgcore.service.api;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 聊天服务接口
 * 
 * <p>提供玩家聊天格式化功能，包括称号、公会前缀、等级显示等。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface ChatService {

    /**
     * 获取玩家的聊天前缀
     * 
     * @param player 玩家
     * @return 前缀组件
     */
    Component getChatPrefix(Player player);

    /**
     * 获取玩家的聊天后缀
     * 
     * @param player 玩家
     * @return 后缀组件
     */
    Component getChatSuffix(Player player);

    /**
     * 获取玩家的完整聊天格式
     * 
     * @param player 玩家
     * @param message 原始消息
     * @return 格式化后的聊天组件
     */
    Component formatChatMessage(Player player, String message);

    /**
     * 获取玩家的公会标签
     * 
     * @param player 玩家
     * @return 公会标签，无公会返回null
     */
    Component getGuildTag(Player player);

    /**
     * 获取玩家的称号显示
     * 
     * @param player 玩家
     * @return 称号组件
     */
    Component getTitleDisplay(Player player);

    /**
     * 获取玩家的等级显示
     * 
     * @param player 玩家
     * @return 等级显示组件
     */
    Component getLevelDisplay(Player player);

    /**
     * 是否启用聊天格式化
     * 
     * @return 是否启用
     */
    boolean isChatFormattingEnabled();

    /**
     * 设置聊天格式化开关
     * 
     * @param enabled 是否启用
     */
    void setChatFormattingEnabled(boolean enabled);

    /**
     * 重新加载聊天格式配置
     */
    void reloadFormats();

    /**
     * 清理玩家的聊天缓存
     * 
     * @param playerId 玩家UUID
     */
    void clearCache(UUID playerId);

    /**
     * 检查服务是否可用
     * 
     * @return 如果服务可用返回 true
     */
    boolean isAvailable();
}