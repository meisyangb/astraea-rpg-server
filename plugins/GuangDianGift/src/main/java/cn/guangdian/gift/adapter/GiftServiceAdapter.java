package cn.guangdian.gift.adapter;

import cn.guangdian.gift.GuangDianGift;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.service.api.GiftService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * GiftService 服务适配器
 * 
 * <p>将 GuangDianGift 的礼包功能注册到 RPGCore ServiceRegistry。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
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
        plugin.getLogger().info("[GiftServiceAdapter] 已注册 GiftService 到 RPGCore");
    }
    
    /**
     * 从 ServiceRegistry 注销服务
     */
    public void unregister() {
        serviceRegistry.unregisterService(GiftService.class);
        plugin.getLogger().info("[GiftServiceAdapter] 已注销 GiftService");
    }

    @Override
    public boolean giveGift(Player player, String giftName) {
        if (player == null || giftName == null) {
            return false;
        }
        
        // 使用礼包插件的命令逻辑
        return Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(), 
            "gift " + giftName + " " + player.getName()
        );
    }

    @Override
    public boolean hasGift(String giftName) {
        return plugin.getGiftItems().containsKey(giftName);
    }

    @Override
    public List<String> getGiftItems(String giftName) {
        List<String> items = plugin.getGiftItems().get(giftName);
        return items != null ? Collections.unmodifiableList(items) : Collections.emptyList();
    }

    @Override
    public Set<String> getGiftNames() {
        return Collections.unmodifiableSet(plugin.getGiftItems().keySet());
    }

    @Override
    public int getGiftCount() {
        return plugin.getGiftItems().size();
    }

    @Override
    public void reloadGifts() {
        // 重新加载礼包配置
        plugin.reloadConfig();
        plugin.getLogger().info("礼包配置已重新加载");
    }

    @Override
    public boolean isAvailable() {
        return plugin.isEnabled();
    }
}
