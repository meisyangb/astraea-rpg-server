package cn.guangdian.monthlycard.lifecycle;

import cn.guangdian.monthlycard.GuangDianMonthlyCard;
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MonthlyCardDataHandler extends AbstractPlayerDataHandler {
    
    private final GuangDianMonthlyCard plugin;
    
    public MonthlyCardDataHandler(GuangDianMonthlyCard plugin) {
        super(plugin);
        this.plugin = plugin;
    }
    
    @Override
    protected void onPlayerLoad(Player player) {
        UUID playerId = player.getUniqueId();
        plugin.getCardManager().loadPlayerData(playerId);
    }
    
    @Override
    protected void onPlayerSave(Player player) {
        UUID playerId = player.getUniqueId();
        plugin.getCardManager().savePlayerData(playerId);
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
    
    @Override
    public String getHandlerName() {
        return "MonthlyCard";
    }
}
