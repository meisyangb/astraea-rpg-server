package cn.guangdian.chat.adapter;

import cn.guangdian.chat.GuangDianChat;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.service.api.ChatService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Chat 服务适配器
 *
 * <p>连接 GuangDianChat 与 RPGCore 服务系统。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class ChatServiceAdapter implements ChatService {

    private final GuangDianChat plugin;
    private final boolean useRPGCore;
    private EventBus eventBus;
    private Logger logger;
    private boolean chatFormattingEnabled = true;
    private ExternalServiceIntegration externalServices;

    public ChatServiceAdapter(GuangDianChat plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        this.logger = plugin.getLogger();
        this.chatFormattingEnabled = plugin.getConfig().getBoolean("formatting-enabled", true);

        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                this.eventBus = rpgCore.getEventBus();
                this.externalServices = rpgCore.getExternalServices();

                registry.registerService(ChatService.class, this);
                logger.info("已注册到 RPGCore: ChatService");

            } catch (Exception e) {
                logger.warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    // ==================== ChatService 实现 ====================

    @Override
    public Component getChatPrefix(Player player) {
        String prefix = externalServices != null ? externalServices.parsePlaceholders(player, "%luckperms_prefix%") : "";
        if (prefix == null || prefix.isEmpty()) {
            return Component.empty();
        }
        return LegacyComponentSerializer.legacySection().deserialize(prefix);
    }

    @Override
    public Component getChatSuffix(Player player) {
        String suffix = externalServices != null ? externalServices.parsePlaceholders(player, "%luckperms_suffix%") : "";
        if (suffix == null || suffix.isEmpty()) {
            return Component.empty();
        }
        return LegacyComponentSerializer.legacySection().deserialize(suffix);
    }

    @Override
    public Component formatChatMessage(Player player, String message) {
        Component prefix = getChatPrefix(player);
        Component suffix = getChatSuffix(player);
        Component guildTag = getGuildTag(player);
        Component titleDisplay = getTitleDisplay(player);

        // 构建完整聊天格式
        Component format = Component.empty();

        // 公会标签
        if (guildTag != null && !guildTag.equals(Component.empty())) {
            format = format.append(guildTag).append(Component.space());
        }

        // 称号
        if (titleDisplay != null && !titleDisplay.equals(Component.empty())) {
            format = format.append(titleDisplay).append(Component.space());
        }

        // 前缀
        if (prefix != null && !prefix.equals(Component.empty())) {
            format = format.append(prefix).append(Component.space());
        }

        // 玩家名
        format = format.append(Component.text(player.getName()));

        // 后缀
        if (suffix != null && !suffix.equals(Component.empty())) {
            format = format.append(Component.space()).append(suffix);
        }

        // 分隔符和消息
        format = format.append(Component.text(": ")).append(Component.text(message));

        return format;
    }

    @Override
    public Component getGuildTag(Player player) {
        String guildTag = externalServices != null ? externalServices.parsePlaceholders(player, "%guild_tag%") : "";
        if (guildTag == null || guildTag.isEmpty() || guildTag.equals("%guild_tag%")) {
            return null;
        }
        return LegacyComponentSerializer.legacySection().deserialize(guildTag);
    }

    @Override
    public Component getTitleDisplay(Player player) {
        String title = externalServices != null ? externalServices.parsePlaceholders(player, "%player_title%") : "";
        if (title == null || title.isEmpty() || title.equals("%player_title%")) {
            return Component.empty();
        }
        return LegacyComponentSerializer.legacySection().deserialize(title);
    }

    @Override
    public Component getLevelDisplay(Player player) {
        String level = externalServices != null ? externalServices.parsePlaceholders(player, "%player_level%") : "";
        if (level == null || level.isEmpty() || level.equals("%player_level%")) {
            return Component.text("Lv.1");
        }
        return LegacyComponentSerializer.legacySection().deserialize("Lv." + level);
    }

    @Override
    public boolean isChatFormattingEnabled() {
        return chatFormattingEnabled;
    }

    @Override
    public void setChatFormattingEnabled(boolean enabled) {
        this.chatFormattingEnabled = enabled;
        plugin.getConfig().set("formatting-enabled", enabled);
        plugin.saveConfig();
    }

    @Override
    public void reloadFormats() {
        plugin.reloadConfig();
        this.chatFormattingEnabled = plugin.getConfig().getBoolean("formatting-enabled", true);
        // 清理所有缓存
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearCache(player.getUniqueId());
        }
        logger.info("聊天格式配置已重新加载");
    }

    @Override
    public void clearCache(UUID playerId) {
        plugin.refreshPlayerCache(playerId);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * 注销服务
     */
    public void unregister() {
        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.unregisterService(ChatService.class);
                logger.info("已从 RPGCore 注销: ChatService");
            } catch (Exception e) {
                logger.warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }

    /**
     * 检查是否使用 RPGCore
     */
    public boolean isUsingRPGCore() {
        return useRPGCore;
    }
}