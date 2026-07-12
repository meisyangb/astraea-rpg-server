package cn.guangdian.points.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * 消息工具类
 * 使用 MiniMessage 处理消息颜色和格式
 *
 * @author GuangDian
 * @since 1.2.0
 */
public class MessageUtils {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * 使用 MiniMessage 解析消息字符串
     *
     * @param message 消息字符串（支持 MiniMessage 格式）
     * @return 解析后的 Component
     */
    public static Component parse(String message) {
        return miniMessage.deserialize(message);
    }

    /**
     * 发送消息给接收者
     *
     * @param sender 接收者
     * @param message 消息内容
     */
    public static void send(CommandSender sender, String message) {
        // 使用 BukkitAudiences 或直接发送
        Component component = miniMessage.deserialize(message);
        // Paper 的 CommandSender 扩展了 Audience，可以直接发送 Component
        sender.sendRichMessage(message);
    }

    /**
     * 发送 Component 消息
     *
     * @param sender 接收者
     * @param component 组件
     */
    public static void send(CommandSender sender, Component component) {
        sender.sendRichMessage(miniMessage.serialize(component));
    }

    /**
     * 快捷方法：发送带 MiniMessage 格式的消息
     */
    public static void sendSuccess(CommandSender sender, String message) {
        send(sender, "<green>" + message);
    }

    public static void sendError(CommandSender sender, String message) {
        send(sender, "<red>" + message);
    }

    public static void sendInfo(CommandSender sender, String message) {
        send(sender, "<yellow>" + message);
    }

    public static void sendHighlight(CommandSender sender, String message) {
        send(sender, "<gold>" + message);
    }
}