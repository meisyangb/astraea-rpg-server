package cn.guangdian.mobhealth.adapter;

import cn.guangdian.mobhealth.GuangDianMobHealth;
import cn.guangdian.mobhealth.MobHealthDisplayManager;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.service.api.MobHealthService;
import org.bukkit.entity.LivingEntity;

/**
 * MobHealthService 服务适配器
 * 
 * <p>将 GuangDianMobHealth 的怪物血量显示功能注册到 RPGCore ServiceRegistry。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class MobHealthServiceAdapter implements MobHealthService {

    private final GuangDianMobHealth plugin;
    private final ServiceRegistry serviceRegistry;
    
    public MobHealthServiceAdapter(GuangDianMobHealth plugin, ServiceRegistry serviceRegistry) {
        this.plugin = plugin;
        this.serviceRegistry = serviceRegistry;
    }
    
    /**
     * 注册服务到 ServiceRegistry
     */
    public void register() {
        serviceRegistry.registerService(MobHealthService.class, this);
        plugin.getLogger().info("[MobHealthServiceAdapter] 已注册 MobHealthService 到 RPGCore");
    }
    
    /**
     * 从 ServiceRegistry 注销服务
     */
    public void unregister() {
        serviceRegistry.unregisterService(MobHealthService.class);
        plugin.getLogger().info("[MobHealthServiceAdapter] 已注销 MobHealthService");
    }

    @Override
    public void showHealth(LivingEntity entity) {
        if (entity == null || !entity.isValid()) {
            return;
        }
        
        MobHealthDisplayManager displayManager = plugin.getDisplayManager();
        if (displayManager != null) {
            displayManager.showHealth(entity);
        }
    }

    @Override
    public void hideHealth(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        
        MobHealthDisplayManager displayManager = plugin.getDisplayManager();
        if (displayManager != null) {
            displayManager.hideHealth(entity);
        }
    }

    @Override
    public void updateHealth(LivingEntity entity) {
        if (entity == null || !entity.isValid()) {
            return;
        }
        
        MobHealthDisplayManager displayManager = plugin.getDisplayManager();
        if (displayManager != null) {
            displayManager.updateDisplay(entity);
        }
    }

    @Override
    public void clearAll() {
        MobHealthDisplayManager displayManager = plugin.getDisplayManager();
        if (displayManager != null) {
            displayManager.clear();
        }
    }

    @Override
    public boolean isEnabled() {
        return plugin.isPluginEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        plugin.getConfig().set("enabled", enabled);
        plugin.saveConfig();
    }

    @Override
    public void reload() {
        plugin.reloadConfiguration();
    }

    @Override
    public boolean isAvailable() {
        return plugin.isEnabled() && plugin.getDisplayManager() != null;
    }
}
