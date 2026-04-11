package cn.guangdian.auth.listener;

import cn.guangdian.auth.GuangDianAuth;
import cn.guangdian.auth.handler.SessionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthListener implements Listener {

    private final GuangDianAuth plugin;
    private final Map<UUID, Long> reminderTasks = new ConcurrentHashMap<>();

    public AuthListener(GuangDianAuth plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        plugin.getSessionManager().createSession(player);
        
        if (!plugin.isLoggedIn(player)) {
            if (plugin.isRegistered(player.getName())) {
                plugin.getPacketHandler().notifyNeedLogin(player);
            } else {
                plugin.getPacketHandler().notifyNeedRegister(player);
            }
            
            plugin.getScheduler().runSyncLater(() -> {
                showLoginPrompt(player);
            }, 20L);
            
            startLoginReminder(player);
        }
    }

    private void startLoginReminder(Player player) {
        int timeout = plugin.getAuthConfig().getLoginTimeout();
        if (timeout <= 0) return;
        
        long taskId = plugin.getScheduler().runSyncRepeating(() -> {
            SessionManager.Session session = plugin.getSessionManager().getSession(player.getUniqueId());
            if (session == null || session.isLoggedIn()) {
                cancelReminder(player.getUniqueId());
                return;
            }
            
            long joinTime = session.getJoinTime();
            long elapsed = (System.currentTimeMillis() - joinTime) / 1000;
            long remaining = timeout - elapsed;
            
            if (remaining <= 0) {
                cancelReminder(player.getUniqueId());
                return;
            }
            
            if (remaining <= 30 || remaining % 30 == 0) {
                player.sendActionBar(
                    Component.text("请在 " + remaining + " 秒内登录").color(NamedTextColor.YELLOW)
                );
            }
        }, 20L, 20L);
        
        reminderTasks.put(player.getUniqueId(), taskId);
    }

    private void cancelReminder(UUID playerId) {
        Long taskId = reminderTasks.remove(playerId);
        if (taskId != null) {
            plugin.getScheduler().cancelTask(taskId);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        cancelReminder(player.getUniqueId());
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
