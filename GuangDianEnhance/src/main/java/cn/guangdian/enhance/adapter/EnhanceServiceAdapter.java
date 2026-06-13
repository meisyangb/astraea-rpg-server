package cn.guangdian.enhance.adapter;

import cn.guangdian.enhance.GuangDianEnhance;
import cn.guangdian.enhance.data.EnhanceResult;
import cn.guangdian.enhance.manager.EnhanceManager;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.service.api.EnhanceService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Logger;

public class EnhanceServiceAdapter implements EnhanceService {

    private final GuangDianEnhance plugin;
    private final EnhanceManager enhanceManager;
    private final boolean useRPGCore;
    private Logger logger;

    public EnhanceServiceAdapter(GuangDianEnhance plugin, EnhanceManager enhanceManager) {
        this.plugin = plugin;
        this.enhanceManager = enhanceManager;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        this.logger = plugin.getLogger();
        
        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                
                registry.registerService(EnhanceService.class, this);
                logger.info("已注册到 RPGCore: EnhanceService");
            } catch (Exception e) {
                logger.warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    @Override
    public int getEnhanceLevel(ItemStack item) {
        return enhanceManager.getLevel(item);
    }

    @Override
    public EnhanceResult enhance(Player player, ItemStack item) {
        return enhanceManager.enhance(player, item);
    }

    @Override
    public double calculateSuccessRate(int currentLevel, @Nullable ItemStack item) {
        return enhanceManager.getSuccessRate(currentLevel, item);
    }

    @Override
    public double getAttributeMultiplier(int level) {
        return enhanceManager.getAttributeMultiplier(level);
    }

    @Override
    public List<ItemStack> getMaterialCost(int currentLevel) {
        return List.of();
    }

    @Override
    public boolean canEnhance(ItemStack item) {
        return plugin.getEnhanceConfig().isEnhanceable(item);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    public void unregister() {
        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.unregisterService(EnhanceService.class);
                plugin.getLogger().info("已从 RPGCore 注销: EnhanceService");
            } catch (Exception e) {
                plugin.getLogger().warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }

    public boolean isUsingRPGCore() {
        return useRPGCore;
    }
}
