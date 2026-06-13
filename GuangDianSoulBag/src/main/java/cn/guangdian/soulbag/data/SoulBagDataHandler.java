package cn.guangdian.soulbag.data;

import cn.guangdian.soulbag.GuangDianSoulBag;
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SoulBagDataHandler extends AbstractPlayerDataHandler {
    
    private final GuangDianSoulBag plugin;
    
    public SoulBagDataHandler(GuangDianSoulBag plugin) {
        super(plugin);
        this.plugin = plugin;
    }
    
    @Override
    protected void onPlayerLoad(Player player) {
        UUID playerId = player.getUniqueId();
        if (!plugin.getBagManager().hasBag(playerId)) {
            plugin.getBagManager().getBag(playerId);
            plugin.getLogger().info("为玩家 " + player.getName() + " 创建灵魂背包");
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
        return "SoulBag";
    }
}
