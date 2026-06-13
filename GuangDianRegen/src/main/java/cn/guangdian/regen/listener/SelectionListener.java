package cn.guangdian.regen.listener;

import cn.guangdian.regen.GuangDianRegen;
import cn.guangdian.regen.manager.SelectionManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 选区监听器
 */
public class SelectionListener implements Listener {

    private final GuangDianRegen plugin;
    private final SelectionManager selectionManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 100;

    public SelectionListener(GuangDianRegen plugin, SelectionManager selectionManager) {
        this.plugin = plugin;
        this.selectionManager = selectionManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // 检查是否持有选区工具
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.WOODEN_AXE) {
            return;
        }

        // 检查权限
        if (!player.hasPermission("regen.admin")) {
            return;
        }

        // 冷却检查
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastTime = cooldowns.get(uuid);
        if (lastTime != null && currentTime - lastTime < COOLDOWN_MS) {
            return;
        }
        cooldowns.put(uuid, currentTime);

        // 处理选区
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // 左键设置第一个点
            selectionManager.setPos1(player, event.getClickedBlock().getLocation());

            String prefix = plugin.getConfig().getString("messages.prefix", "&6[矿场系统] &r");
            String msg = plugin.getConfig().getString("messages.selection_pos1",
                    "&a已设置第一个点: &e{x}, {y}, {z}");

            int x = event.getClickedBlock().getX();
            int y = event.getClickedBlock().getY();
            int z = event.getClickedBlock().getZ();

            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    prefix + msg.replace("{x}", String.valueOf(x))
                            .replace("{y}", String.valueOf(y))
                            .replace("{z}", String.valueOf(z))));

            event.setCancelled(true);

        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // 右键设置第二个点
            selectionManager.setPos2(player, event.getClickedBlock().getLocation());

            String prefix = plugin.getConfig().getString("messages.prefix", "&6[矿场系统] &r");
            String msg = plugin.getConfig().getString("messages.selection_pos2",
                    "&a已设置第二个点: &e{x}, {y}, {z}");

            int x = event.getClickedBlock().getX();
            int y = event.getClickedBlock().getY();
            int z = event.getClickedBlock().getZ();

            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    prefix + msg.replace("{x}", String.valueOf(x))
                            .replace("{y}", String.valueOf(y))
                            .replace("{z}", String.valueOf(z))));

            event.setCancelled(true);
        }
    }
}
