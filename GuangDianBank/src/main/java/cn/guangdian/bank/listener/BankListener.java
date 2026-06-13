package cn.guangdian.bank.listener;

import cn.guangdian.bank.GuangDianBank;
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import org.bukkit.entity.Player;

public class BankListener extends AbstractPlayerDataHandler {
    
    private final GuangDianBank plugin;
    
    public BankListener(GuangDianBank plugin) {
        super(plugin);
        this.plugin = plugin;
    }
    
    @Override
    protected void onPlayerLoad(Player player) {
        if (!plugin.hasAccount(player.getUniqueId())) {
            plugin.getAccount(player.getUniqueId());
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
