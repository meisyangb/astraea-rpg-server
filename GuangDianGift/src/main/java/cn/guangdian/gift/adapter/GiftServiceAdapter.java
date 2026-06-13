package cn.guangdian.gift.adapter;

import cn.guangdian.gift.GuangDianGift;
import cn.guangdian.gift.model.GiftConfig;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.service.api.GiftService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * GiftService 服务适配器
 *
 * <p>将 GuangDianGift 的礼包功能注册到 RPGCore ServiceRegistry。</p>
 *
 * @author GuangDian
 * @since 2.0.0
 */
public class GiftServiceAdapter implements GiftService {

    private final GuangDianGift plugin;
    private final ServiceRegistry serviceRegistry;

    public GiftServiceAdapter(GuangDianGift plugin, ServiceRegistry serviceRegistry) {
        this.plugin = plugin;
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * 注册服务到 ServiceRegistry
     */
    public void register() {
        serviceRegistry.registerService(GiftService.class, this);
        plugin.logInfo("[GiftServiceAdapter] 已注册 GiftService 到 RPGCore");
    }

    /**
     * 从 ServiceRegistry 注销服务
     */
    public void unregister() {
        serviceRegistry.unregisterService(GiftService.class);
        plugin.logInfo("[GiftServiceAdapter] 已注销 GiftService");
    }

    @Override
    public boolean giveGift(Player player, String giftName) {
        if (player == null || giftName == null) {
            return false;
        }
        return plugin.giveGift(player, giftName);
    }

    @Override
    public boolean hasGift(String giftName) {
        return plugin.getGiftConfigs().containsKey(giftName);
    }

    @Override
    public List<String> getGiftItems(String giftName) {
        GiftConfig config = plugin.getGiftConfig(giftName);
        if (config == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(config.getItems());
    }

    @Override
    public Set<String> getGiftNames() {
        return Collections.unmodifiableSet(plugin.getGiftConfigs().keySet());
    }

    @Override
    public int getGiftCount() {
        return plugin.getGiftConfigs().size();
    }

    @Override
    public void reloadGifts() {
        // 重新加载礼包配置
        plugin.reloadConfig();
        plugin.logInfo("礼包配置已重新加载");
    }

    @Override
    public boolean isAvailable() {
        return plugin.isEnabled();
    }
}
