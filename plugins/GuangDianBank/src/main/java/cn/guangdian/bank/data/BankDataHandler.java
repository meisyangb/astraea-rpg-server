package cn.guangdian.bank.data;

import cn.guangdian.bank.GuangDianBank;
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BankDataHandler extends AbstractPlayerDataHandler {
    
    private final GuangDianBank plugin;
    
    public BankDataHandler(GuangDianBank plugin) {
        super(plugin);
        this.plugin = plugin;
    }
    
    @Override
    protected void onPlayerLoad(Player player) {
        UUID playerId = player.getUniqueId();
        if (!plugin.hasAccount(playerId)) {
            plugin.getAccount(playerId);
            plugin.getLogger().info("为玩家 " + player.getName() + " 创建银行账户");
        }
    }
    
    @Override
    protected void onPlayerSave(Player player) {
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
    
    @Override
    public String getHandlerName() {
        return "Bank";
    }
}
