package cn.guangdian.mcp.server;

import cn.guangdian.mcp.GuangDianMCP;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.Bukkit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.io.PrintWriter;

public class EventPusher implements Listener {
    
    private final GuangDianMCP plugin;
    private final SSEHttpHandler sseHandler;
    private boolean enabled = true;
    
    public EventPusher(GuangDianMCP plugin, SSEHttpHandler sseHandler) {
        this.plugin = plugin;
        this.sseHandler = sseHandler;
    }
    
    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("事件推送已启用");
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled || sseHandler == null) return;
        
        Player player = event.getPlayer();
        JsonObject data = new JsonObject();
        data.addProperty("type", "player_join");
        data.addProperty("player", player.getName());
        data.addProperty("uuid", player.getUniqueId().toString());
        data.addProperty("online", Bukkit.getOnlinePlayers().size());
        
        sseHandler.broadcast("server_event", data.toString());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!enabled || sseHandler == null) return;
        
        Player player = event.getPlayer();
        JsonObject data = new JsonObject();
        data.addProperty("type", "player_quit");
        data.addProperty("player", player.getName());
        data.addProperty("uuid", player.getUniqueId().toString());
        data.addProperty("online", Bukkit.getOnlinePlayers().size() - 1);
        
        sseHandler.broadcast("server_event", data.toString());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!enabled) return;
        
        Player player = event.getEntity();
        JsonObject data = new JsonObject();
        data.addProperty("type", "player_death");
        data.addProperty("player", player.getName());
        data.addProperty("uuid", player.getUniqueId().toString());
        data.addProperty("deathMessage", event.getDeathMessage());
        data.addProperty("world", player.getWorld().getName());
        data.addProperty("x", Math.round(player.getLocation().getX() * 10) / 10.0);
        data.addProperty("y", Math.round(player.getLocation().getY() * 10) / 10.0);
        data.addProperty("z", Math.round(player.getLocation().getZ() * 10) / 10.0);
        
        sseHandler.broadcast("server_event", data.toString());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerCommand(ServerCommandEvent event) {
        if (!enabled) return;
        
        String command = event.getCommand();
        
        if (command.startsWith("stop") || command.startsWith("restart") || command.startsWith("reload")) {
            JsonObject data = new JsonObject();
            data.addProperty("type", "server_command");
            data.addProperty("command", command);
            data.addProperty("sender", event.getSender().getName());
            
            sseHandler.broadcast("server_event", data.toString());
        }
    }
    
    public void pushCustomEvent(String eventType, JsonObject data) {
        if (!enabled || sseHandler == null) return;
        
        data.addProperty("type", eventType);
        data.addProperty("timestamp", System.currentTimeMillis());
        
        sseHandler.broadcast("custom_event", data.toString());
    }
    
    public void pushTPSWarning(double tps) {
        if (!enabled) return;
        
        JsonObject data = new JsonObject();
        data.addProperty("type", "tps_warning");
        data.addProperty("tps", Math.round(tps * 100) / 100.0);
        data.addProperty("timestamp", System.currentTimeMillis());
        
        sseHandler.broadcast("server_event", data.toString());
    }
    
    public void pushMemoryWarning(long usedMB, long maxMB) {
        if (!enabled) return;
        
        JsonObject data = new JsonObject();
        data.addProperty("type", "memory_warning");
        data.addProperty("usedMB", usedMB);
        data.addProperty("maxMB", maxMB);
        data.addProperty("percentage", Math.round((double) usedMB / maxMB * 100));
        data.addProperty("timestamp", System.currentTimeMillis());
        
        sseHandler.broadcast("server_event", data.toString());
    }
    
    public void pushChatMessage(String playerName, String message) {
        if (!enabled) return;
        
        JsonObject data = new JsonObject();
        data.addProperty("type", "chat");
        data.addProperty("player", playerName);
        data.addProperty("message", message);
        data.addProperty("timestamp", System.currentTimeMillis());
        
        sseHandler.broadcast("chat_event", data.toString());
    }
    
    public void pushPluginEvent(String pluginName, String action) {
        if (!enabled || sseHandler == null) return;
        
        JsonObject data = new JsonObject();
        data.addProperty("type", "plugin_event");
        data.addProperty("plugin", pluginName);
        data.addProperty("action", action);
        data.addProperty("timestamp", System.currentTimeMillis());
        
        sseHandler.broadcast("server_event", data.toString());
    }
}
