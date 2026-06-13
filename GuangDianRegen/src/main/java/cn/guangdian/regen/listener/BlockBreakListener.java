package cn.guangdian.regen.listener;

import cn.guangdian.regen.manager.RegenManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * 方块破坏监听器
 */
public class BlockBreakListener implements Listener {

    private final RegenManager regenManager;

    public BlockBreakListener(RegenManager regenManager) {
        this.regenManager = regenManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // 检查玩家是否有使用权限
        if (!player.hasPermission("regen.use")) {
            return;
        }

        // 处理方块破坏
        regenManager.handleBlockBreak(player, block);
    }
}
