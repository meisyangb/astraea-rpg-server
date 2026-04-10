package cn.guangdian.monthlycard.adapter;

import cn.guangdian.monthlycard.GuangDianMonthlyCard;
import cn.guangdian.monthlycard.api.MonthlyCardService;
import cn.guangdian.monthlycard.data.MonthlyCardData;
import cn.guangdian.monthlycard.data.MonthlyCardType;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.service.api.PointsService;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MonthlyCardServiceAdapter implements MonthlyCardService {
    
    private final GuangDianMonthlyCard plugin;
    private final boolean useRPGCore;
    
    public MonthlyCardServiceAdapter(GuangDianMonthlyCard plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        
        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                registry.registerService(MonthlyCardService.class, this);
                plugin.getLogger().info("已注册到 RPGCore 服务注册表: MonthlyCardService");
            } catch (Exception e) {
                plugin.getLogger().warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }
    
    public void unregister() {
        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.unregisterService(MonthlyCardService.class);
                plugin.getLogger().info("已从 RPGCore 服务注册表注销: MonthlyCardService");
            } catch (Exception e) {
                plugin.getLogger().warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }
    
    public boolean isUsingRPGCore() {
        return useRPGCore;
    }
    
    @Override
    public Optional<MonthlyCardType> getCardType(String typeId) {
        return plugin.getCardManager().getCardType(typeId);
    }
    
    @Override
    public List<MonthlyCardType> getAllCardTypes() {
        return plugin.getCardManager().getAllCardTypes();
    }
    
    @Override
    public MonthlyCardData getPlayerData(UUID playerId) {
        return plugin.getCardManager().getPlayerData(playerId);
    }
    
    @Override
    public boolean hasActiveCard(UUID playerId) {
        return plugin.getCardManager().hasActiveCard(playerId);
    }
    
    @Override
    public boolean activateCard(UUID playerId, String cardTypeId) {
        return activateCard(playerId, cardTypeId, true);
    }
    
    @Override
    public boolean activateCard(UUID playerId, String cardTypeId, boolean charge) {
        Optional<MonthlyCardType> typeOpt = getCardType(cardTypeId);
        if (typeOpt.isEmpty()) {
            return false;
        }
        
        MonthlyCardType type = typeOpt.get();
        
        if (charge) {
            if (!chargePlayer(playerId, type)) {
                return false;
            }
        }
        
        boolean success = plugin.getCardManager().activateCard(playerId, cardTypeId);
        
        if (success && !type.getInstantRewards().isEmpty()) {
            giveInstantRewards(playerId, type);
        }
        
        return success;
    }
    
    private boolean chargePlayer(UUID playerId, MonthlyCardType type) {
        String currencyType = type.getCurrencyType();
        long price = type.getPrice();
        
        if (price <= 0) {
            return true;
        }
        
        switch (currencyType.toLowerCase()) {
            case "points":
                return chargePoints(playerId, price);
            case "money":
            case "vault":
                return chargeMoney(playerId, price);
            default:
                plugin.getLogger().warning("未知的货币类型: " + currencyType);
                return false;
        }
    }
    
    private boolean chargePoints(UUID playerId, long amount) {
        if (useRPGCore) {
            try {
                PointsService pointsService = RPGCore.getInstance().getServiceRegistry()
                    .getService(PointsService.class);
                return pointsService.removeBalance(playerId, amount, "购买月卡");
            } catch (Exception e) {
                plugin.getLogger().warning("扣点券失败: " + e.getMessage());
                return false;
            }
        }
        return false;
    }
    
    private boolean chargeMoney(UUID playerId, long amount) {
        if (useRPGCore && plugin.getExternalServices() != null) {
            var player = Bukkit.getPlayer(playerId);
            if (player != null) {
                return plugin.getExternalServices().withdraw(player, amount);
            }
        }
        return false;
    }
    
    private void giveInstantRewards(UUID playerId, MonthlyCardType type) {
        var player = Bukkit.getPlayer(playerId);
        if (player == null) return;
        
        for (var item : type.getInstantRewards()) {
            player.getInventory().addItem(item.clone());
        }
    }
    
    @Override
    public boolean canClaimToday(UUID playerId) {
        return plugin.getCardManager().canClaimToday(playerId);
    }
    
    @Override
    public boolean claimDailyReward(UUID playerId) {
        return plugin.getCardManager().claimDailyReward(playerId);
    }
    
    @Override
    public long getRemainingDays(UUID playerId) {
        return plugin.getCardManager().getRemainingDays(playerId);
    }
    
    @Override
    public int getTotalClaimedDays(UUID playerId) {
        return plugin.getCardManager().getTotalClaimedDays(playerId);
    }
    
    @Override
    public void extendCard(UUID playerId, int additionalDays) {
        plugin.getCardManager().extendCard(playerId, additionalDays);
    }
    
    @Override
    public void setCard(UUID playerId, String cardTypeId, int durationDays) {
        plugin.getCardManager().setCard(playerId, cardTypeId, durationDays);
    }
    
    @Override
    public void removeCard(UUID playerId) {
        plugin.getCardManager().removeCard(playerId);
    }
    
    @Override
    public void reloadConfig() {
        plugin.reloadConfig();
        plugin.getCardManager().loadCardTypes();
    }
}
