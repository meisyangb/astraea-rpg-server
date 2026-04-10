package cn.guangdian.auth.listener;

import cn.guangdian.auth.GuangDianAuth;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.time.Duration;

public class AuthListener implements Listener {

    private final GuangDianAuth plugin;

    public AuthListener(GuangDianAuth plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        plugin.getSessionManager().createSession(player);
        
        if (!plugin.isLoggedIn(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                showLoginPrompt(player);
                plugin.getPacketHandler().notifyNeedLogin(player);
            }, 20L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getSessionManager().removeSession(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (plugin.getAuthConfig().isAllowMovementBeforeLogin()) return;
        
        Player player = event.getPlayer();
        if (!plugin.isLoggedIn(player)) {
            if (event.getFrom().getX() != event.getTo().getX() || 
                event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (plugin.getAuthConfig().isAllowChatBeforeLogin()) return;
        
        Player player = event.getPlayer();
        if (!plugin.isLoggedIn(player)) {
            event.setCancelled(true);
            plugin.sendLoginPrompt(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (plugin.isLoggedIn(player)) return;
        
        String cmd = event.getMessage().toLowerCase();
        if (cmd.startsWith("/login") || cmd.startsWith("/l ") || 
            cmd.startsWith("/register") || cmd.startsWith("/reg ")) {
            return;
        }
        
        event.setCancelled(true);
        plugin.sendLoginPrompt(player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isLoggedIn(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isLoggedIn(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(PlayerAttemptPickupItemEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isLoggedIn(player)) {
            event.setCancelled(true);
        }
    }

    private void showLoginPrompt(Player player) {
        Title title = Title.title(
            Component.text("欢迎来到阿斯特瑞亚").color(NamedTextColor.GOLD),
            Component.text(plugin.isRegistered(player.getName()) ? 
                "请使用 /login <密码> 登录" : 
                "请使用 /register <密码> <确认密码> 注册").color(NamedTextColor.YELLOW),
            Title.Times.times(
                Duration.ofMillis(500),
                Duration.ofSeconds(5),
                Duration.ofMillis(500)
            )
        );
        
        player.showTitle(title);
        plugin.sendLoginPrompt(player);
    }
}
