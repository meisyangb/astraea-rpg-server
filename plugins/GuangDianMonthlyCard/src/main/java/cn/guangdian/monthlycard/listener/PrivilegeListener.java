package cn.guangdian.monthlycard.listener;

import cn.guangdian.monthlycard.GuangDianMonthlyCard;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;

/**
 * 月卡特权监听器
 * 
 * 处理:
 * - 经验加成
 * - 掉落率加成
 */
public class PrivilegeListener implements Listener {

    private final GuangDianMonthlyCard plugin;

    public PrivilegeListener(GuangDianMonthlyCard plugin) {
        this.plugin = plugin;
    }

    /**
     * 经验加成
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getCardManager().hasActiveCard(player.getUniqueId())) {
            return;
        }

        double boost = plugin.getCardManager().getExpBoost(player.getUniqueId());
        if (boost > 1.0) {
            int originalAmount = event.getAmount();
            int boostedAmount = (int) Math.round(originalAmount * boost);
            int bonus = boostedAmount - originalAmount;
            
            if (bonus > 0) {
                event.setAmount(boostedAmount);
                // 可选: 发送提示消息
                // player.sendActionBar(Component.text("月卡经验加成: +" + bonus)
                //     .color(NamedTextColor.GREEN));
            }
        }
    }

    /**
     * 掉落率加成
     * 通过增加掉落数量来实现
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();
        if (player == null) return;
        
        if (!plugin.getCardManager().hasActiveCard(player.getUniqueId())) {
            return;
        }

        double boost = plugin.getCardManager().getDropBoost(player.getUniqueId());
        if (boost > 1.0) {
            // 增加掉落物数量
            event.getDrops().forEach(item -> {
                int originalAmount = item.getAmount();
                int boostedAmount = (int) Math.round(originalAmount * boost);
                if (boostedAmount > originalAmount) {
                    item.setAmount(boostedAmount);
                }
            });
        }
    }
}