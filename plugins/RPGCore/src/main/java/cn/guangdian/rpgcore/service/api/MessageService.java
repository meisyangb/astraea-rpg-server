package cn.guangdian.rpgcore.service.api;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 统一消息服务接口
 *
 * <p>提供统一的消息发送功能，包括：</p>
 * <ul>
 *   <li>普通消息发送（玩家、命令发送者、广播）</li>
 *   <li>ActionBar 消息</li>
 *   <li>Title 标题</li>
 *   <li>声音播放</li>
 *   <li>带交互的消息（悬停、点击）</li>
 * </ul>
 *
 * <p>此服务整合了原 AudienceService 和 MessageService 的功能，
 * 提供统一的消息发送入口。</p>
 *
 * @author GuangDian
 * @since 2.0.0
 */
public interface MessageService {

    // ==================== 基础消息发送 ====================

    /**
     * 发送消息给命令发送者
     *
     * @param sender 接收者
     * @param message 消息内容（支持MiniMessage格式）
     */
    void send(CommandSender sender, String message);

    /**
     * 发送消息给玩家
     *
     * @param player 接收玩家
     * @param message 消息内容（支持MiniMessage格式）
     */
    void send(Player player, String message);

    /**
     * 发送消息给指定UUID的玩家（如果在线）
     *
     * @param playerId 玩家UUID
     * @param message 消息内容
     * @return 是否发送成功（玩家是否在线）
     */
    boolean send(UUID playerId, String message);

    /**
     * 发送消息给多个玩家
     *
     * @param players 玩家集合
     * @param message 消息内容
     */
    void send(Collection<? extends Player> players, String message);

    /**
     * 广播消息给所有在线玩家
     *
     * @param message 消息内容
     */
    void broadcast(String message);

    /**
     * 根据条件过滤广播消息
     *
     * @param message 消息内容
     * @param filter 过滤条件
     */
    void broadcast(String message, Predicate<Player> filter);

    // ==================== 快捷消息方法 ====================

    /**
     * 发送成功消息（绿色）
     *
     * @param sender 接收者
     * @param message 消息内容
     */
    void sendSuccess(CommandSender sender, String message);

    /**
     * 发送错误消息（红色）
     *
     * @param sender 接收者
     * @param message 消息内容
     */
    void sendError(CommandSender sender, String message);

    /**
     * 发送警告消息（黄色）
     *
     * @param sender 接收者
     * @param message 消息内容
     */
    void sendWarning(CommandSender sender, String message);

    /**
     * 发送信息消息（青色）
     *
     * @param sender 接收者
     * @param message 消息内容
     */
    void sendInfo(CommandSender sender, String message);

    // ==================== 交互式消息 ====================

    /**
     * 发送带悬停提示的消息
     *
     * @param sender 接收者
     * @param text 显示文本
     * @param hoverText 悬停时显示的文本
     */
    void sendHoverable(CommandSender sender, String text, String hoverText);

    /**
     * 发送可点击的消息
     *
     * @param sender 接收者
     * @param text 显示文本
     * @param clickAction 点击动作类型
     * @param clickValue 点击动作值
     */
    void sendClickable(CommandSender sender, String text, String clickAction, String clickValue);

    /**
     * 发送带命令建议的消息（点击后建议输入命令）
     *
     * @param sender 接收者
     * @param text 显示文本
     * @param suggestCommand 建议的命令
     */
    void sendSuggestible(CommandSender sender, String text, String suggestCommand);

    // ==================== ActionBar ====================

    /**
     * 发送 ActionBar 消息给玩家
     *
     * @param player 接收玩家
     * @param message 消息内容
     */
    void sendActionBar(Player player, String message);

    /**
     * 发送 ActionBar 消息给多个玩家
     *
     * @param players 玩家集合
     * @param message 消息内容
     */
    void sendActionBar(Collection<? extends Player> players, String message);

    /**
     * 清除玩家的 ActionBar
     *
     * @param player 目标玩家
     */
    void clearActionBar(Player player);

    /**
     * 清除多个玩家的 ActionBar
     *
     * @param players 玩家集合
     */
    void clearActionBar(Collection<? extends Player> players);

    // ==================== Title ====================

    /**
     * 显示标题给玩家
     *
     * @param player 目标玩家
     * @param title 主标题
     * @param subtitle 副标题
     */
    void showTitle(Player player, String title, String subtitle);

    /**
     * 显示标题给玩家（自定义时间）
     *
     * @param player 目标玩家
     * @param title 主标题
     * @param subtitle 副标题
     * @param fadeIn 淡入时间（tick）
     * @param stay 停留时间（tick）
     * @param fadeOut 淡出时间（tick）
     */
    void showTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    /**
     * 显示标题给多个玩家
     *
     * @param players 玩家集合
     * @param title 主标题
     * @param subtitle 副标题
     */
    void showTitle(Collection<? extends Player> players, String title, String subtitle);

    /**
     * 显示标题给多个玩家（自定义时间）
     *
     * @param players 玩家集合
     * @param title 主标题
     * @param subtitle 副标题
     * @param fadeIn 淡入时间（tick）
     * @param stay 停留时间（tick）
     * @param fadeOut 淡出时间（tick）
     */
    void showTitle(Collection<? extends Player> players, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    /**
     * 清除玩家的标题
     *
     * @param player 目标玩家
     */
    void clearTitle(Player player);

    /**
     * 清除多个玩家的标题
     *
     * @param players 玩家集合
     */
    void clearTitle(Collection<? extends Player> players);

    // ==================== 声音 ====================

    /**
     * 播放声音给玩家
     *
     * @param player 目标玩家
     * @param soundKey 声音键名
     * @param volume 音量
     * @param pitch 音调
     */
    void playSound(Player player, String soundKey, float volume, float pitch);

    /**
     * 播放声音给多个玩家
     *
     * @param players 玩家集合
     * @param soundKey 声音键名
     * @param volume 音量
     * @param pitch 音调
     */
    void playSound(Collection<? extends Player> players, String soundKey, float volume, float pitch);

    /**
     * 停止播放指定声音
     *
     * @param player 目标玩家
     * @param soundKey 声音键名
     */
    void stopSound(Player player, String soundKey);

    /**
     * 停止播放所有声音
     *
     * @param player 目标玩家
     */
    void stopAllSounds(Player player);

    // ==================== 工具方法 ====================

    /**
     * 解析 MiniMessage 格式文本为 Component
     *
     * @param text MiniMessage格式文本
     * @return 解析后的 Component
     */
    Component parse(String text);

    /**
     * 解析带悬停提示的文本
     *
     * @param text 主文本
     * @param hoverText 悬停文本
     * @return 解析后的 Component
     */
    Component parseWithHover(String text, @Nullable String hoverText);

    /**
     * 解析带点击动作的文本
     *
     * @param text 主文本
     * @param hoverText 悬停文本
     * @param clickAction 点击动作类型
     * @param clickValue 点击动作值
     * @return 解析后的 Component
     */
    Component parseWithClick(String text, @Nullable String hoverText, @Nullable String clickAction, @Nullable String clickValue);

    /**
     * 替换文本中的占位符
     *
     * @param text 原文本
     * @param keyValues 键值对（key1, value1, key2, value2...）
     * @return 替换后的文本
     */
    String replacePlaceholders(String text, String... keyValues);

    // ==================== 快捷颜色方法 ====================

    /**
     * 将 MiniMessage 格式文本解析为 Component
     *
     * <p>快捷方法，等同于 {@link #parse(String)}</p>
     *
     * @param text MiniMessage格式文本
     * @return 解析后的 Component
     */
    default Component colorize(String text) {
        return parse(text);
    }

    /**
     * 创建绿色文本
     *
     * @param text 文本内容
     * @return 绿色 Component
     */
    default Component green(String text) {
        return parse("<green>" + text);
    }

    /**
     * 创建红色文本
     *
     * @param text 文本内容
     * @return 红色 Component
     */
    default Component red(String text) {
        return parse("<red>" + text);
    }

    /**
     * 创建黄色文本
     *
     * @param text 文本内容
     * @return 黄色 Component
     */
    default Component yellow(String text) {
        return parse("<yellow>" + text);
    }

    /**
     * 创建金色文本
     *
     * @param text 文本内容
     * @return 金色 Component
     */
    default Component gold(String text) {
        return parse("<gold>" + text);
    }

    /**
     * 创建青色文本
     *
     * @param text 文本内容
     * @return 青色 Component
     */
    default Component aqua(String text) {
        return parse("<aqua>" + text);
    }
}
