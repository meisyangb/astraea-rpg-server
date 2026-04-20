package cn.guangdian.market.lifecycle;

import cn.guangdian.market.GuangDianMarket;
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MarketDataHandler extends AbstractPlayerDataHandler {
    
    private final GuangDianMarket plugin;
    
    public MarketDataHandler(GuangDianMarket plugin) {
        super(plugin);
        this.plugin = plugin;
    }
    
    @Override
    protected void onPlayerLoad(Player player) {
        List<ItemStack> returns = plugin.getOfflineReturns().remove(player.getUniqueId());
        if (returns != null && !returns.isEmpty()) {
            for (ItemStack item : returns) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                if (!leftover.isEmpty()) {
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
            }
            player.sendMessage(plugin.colorize(plugin.getConfig().getString("messages.offline-returns", "<yellow>你有 %count% 个过期物品已返还!"))
                .replace("%count%", String.valueOf(returns.size())));
        }
    }
    
    @Override
    protected void onPlayerSave(Player player) {
        plugin.getOpenGUIs().remove(player.getUniqueId());
        plugin.getSearchModePlayers().remove(player.getUniqueId());
    }
    
    @Override
    public int getPriority() {
        return 200;
    }
    
    @Override
    public String getHandlerName() {
        return "Market";
    }
}
