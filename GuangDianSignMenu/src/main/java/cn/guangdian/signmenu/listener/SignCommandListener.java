package cn.guangdian.signmenu.listener;

import cn.guangdian.signmenu.GuangDianSignMenu;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class SignCommandListener implements Listener {

    private final GuangDianSignMenu plugin;
    private final NamespacedKey adminSignKey;

    public SignCommandListener(GuangDianSignMenu plugin) {
        this.plugin = plugin;
        this.adminSignKey = new NamespacedKey(plugin, "admin_sign");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignCreate(SignChangeEvent event) {
        Player player = event.getPlayer();
        
        if (!player.hasPermission("signmenu.admin")) {
            return;
        }
        
        String[] lines = event.getLines();
        for (String line : lines) {
            List<String> commands = plugin.getConfigManager().getCommands(line);
            if (commands != null && !commands.isEmpty()) {
                Sign sign = (Sign) event.getBlock().getState();
                sign.getPersistentDataContainer()
                    .set(adminSignKey, PersistentDataType.BYTE, (byte) 1);
                sign.update();
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§a已创建功能告示牌: §e" + line);
                return;
            }
        }
    }

    @EventHandler
    public void onSignInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        if (!(block.getState() instanceof Sign sign)) {
            return;
        }

        Byte isAdminSign = sign.getPersistentDataContainer()
            .get(adminSignKey, PersistentDataType.BYTE);
        
        if (isAdminSign == null || isAdminSign != 1) {
            return;
        }

        Player player = event.getPlayer();
        
        String[] lines = sign.getLines();
        for (String line : lines) {
            List<String> commands = plugin.getConfigManager().getCommands(line);
            if (commands != null && !commands.isEmpty()) {
                event.setCancelled(true);
                executeCommands(player, commands);
                return;
            }
        }
    }

    private void executeCommands(Player player, List<String> commands) {
        for (String cmd : commands) {
            String processed = cmd.replace("{player}", player.getName());
            plugin.getServer().dispatchCommand(player, processed);
        }
    }
}
