package cn.guangdian.battlepass.adapter;

import cn.guangdian.battlepass.GuangDianBattlePass;
import cn.guangdian.battlepass.api.BattlePassService;
import cn.guangdian.battlepass.manager.BattlePassManager;
import cn.guangdian.battlepass.model.PlayerBattlePass;
import cn.guangdian.rpgcore.RPGCore;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BattlePassServiceAdapter implements BattlePassService {
    
    private final GuangDianBattlePass plugin;
    private final BattlePassManager manager;
    private boolean usingRPGCore;
    
    public BattlePassServiceAdapter(GuangDianBattlePass plugin) {
        this.plugin = plugin;
        this.manager = plugin.getBattlePassManager();
        
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().registerService(BattlePassService.class, this);
            this.usingRPGCore = true;
            plugin.getLogger().info("已注册到 RPGCore ServiceRegistry");
        } else {
            this.usingRPGCore = false;
        }
    }
    
    public void unregister() {
        if (usingRPGCore) {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getServiceRegistry().unregisterService(BattlePassService.class);
            }
        }
    }
    
    public boolean isUsingRPGCore() {
        return usingRPGCore;
    }
    
    @Override
    public int getPlayerLevel(UUID playerId) {
        PlayerBattlePass bp = manager.getPlayerBattlePass(playerId);
        return bp != null ? bp.getLevel() : 1;
    }
    
    @Override
    public int getPlayerExp(UUID playerId) {
        PlayerBattlePass bp = manager.getPlayerBattlePass(playerId);
        return bp != null ? bp.getCurrentExp() : 0;
    }
    
    @Override
    public boolean isPremium(UUID playerId) {
        PlayerBattlePass bp = manager.getPlayerBattlePass(playerId);
        return bp != null && bp.isPremium();
    }
    
    @Override
    public void addExp(UUID playerId, int exp) {
        manager.addExp(playerId, exp);
    }
    
    @Override
    public boolean purchasePremium(UUID playerId) {
        return manager.purchasePremium(playerId);
    }
    
    @Override
    public int getProgress(UUID playerId) {
        return manager.getProgress(playerId);
    }
}
