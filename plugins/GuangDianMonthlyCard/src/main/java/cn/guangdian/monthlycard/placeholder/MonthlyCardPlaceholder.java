package cn.guangdian.monthlycard.placeholder;

import cn.guangdian.monthlycard.GuangDianMonthlyCard;
import cn.guangdian.monthlycard.data.MonthlyCardData;
import cn.guangdian.monthlycard.data.MonthlyCardType;
import cn.guangdian.rpgcore.integration.PlaceholderService;
import org.bukkit.OfflinePlayer;

import java.util.Optional;

public class MonthlyCardPlaceholder {
    
    private final GuangDianMonthlyCard plugin;
    
    public MonthlyCardPlaceholder(GuangDianMonthlyCard plugin) {
        this.plugin = plugin;
    }
    
    public void register() {
        PlaceholderService service = PlaceholderService.getInstance();
        if (service == null) return;
        
        service.register("gdmonthly", (player, params) -> {
            if (player == null) return "";
            
            String identifier = params.toLowerCase();
            
            switch (identifier) {
                case "has_card":
                case "active":
                    return plugin.hasActiveCard(player.getUniqueId()) ? "true" : "false";
                    
                case "remaining_days":
                case "days_left":
                    return String.valueOf(plugin.getRemainingDays(player.getUniqueId()));
                    
                case "claimed_days":
                case "total_claimed":
                    MonthlyCardData data = plugin.getPlayerData(player.getUniqueId());
                    return String.valueOf(data.getTotalClaimedDays());
                    
                case "can_claim":
                case "can_sign":
                    return plugin.canClaimToday(player.getUniqueId()) ? "true" : "false";
                    
                case "card_type":
                    MonthlyCardData cardData = plugin.getPlayerData(player.getUniqueId());
                    if (cardData.hasActiveCard()) {
                        Optional<MonthlyCardType> typeOpt = plugin.getCardType(cardData.getCardType());
                        return typeOpt.map(MonthlyCardType::getDisplayName).orElse(cardData.getCardType());
                    }
                    return "无";
                    
                case "card_type_id":
                    MonthlyCardData typeData = plugin.getPlayerData(player.getUniqueId());
                    return typeData.hasActiveCard() ? typeData.getCardType() : "none";
                    
                case "status":
                    MonthlyCardData statusData = plugin.getPlayerData(player.getUniqueId());
                    if (!statusData.hasActiveCard()) {
                        return "未激活";
                    }
                    return statusData.canClaimToday() ? "可领取" : "已领取";
                    
                default:
                    return handleComplexPlaceholder(player, identifier);
            }
        });
    }
    
    private String handleComplexPlaceholder(OfflinePlayer player, String identifier) {
        if (identifier.startsWith("price_")) {
            String cardTypeId = identifier.substring(6);
            Optional<MonthlyCardType> typeOpt = plugin.getCardType(cardTypeId);
            return typeOpt.map(t -> String.valueOf(t.getPrice())).orElse("0");
        }
        
        if (identifier.startsWith("name_")) {
            String cardTypeId = identifier.substring(5);
            Optional<MonthlyCardType> typeOpt = plugin.getCardType(cardTypeId);
            return typeOpt.map(MonthlyCardType::getDisplayName).orElse(cardTypeId);
        }
        
        if (identifier.startsWith("duration_")) {
            String cardTypeId = identifier.substring(9);
            Optional<MonthlyCardType> typeOpt = plugin.getCardType(cardTypeId);
            return typeOpt.map(t -> String.valueOf(t.getDurationDays())).orElse("0");
        }
        
        return null;
    }
    
    public void unregister() {
    }
}
