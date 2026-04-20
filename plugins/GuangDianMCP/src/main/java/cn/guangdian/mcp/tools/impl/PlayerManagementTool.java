package cn.guangdian.mcp.tools.impl;

import cn.guangdian.mcp.GuangDianMCP;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.mcp.config.MCPConfig;
import cn.guangdian.mcp.tools.MCPTool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class PlayerManagementTool implements MCPTool {
    
    private final GuangDianMCP plugin;
    
    public PlayerManagementTool(GuangDianMCP plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "player_management";
    }
    
    @Override
    public String getDescription() {
        return "管理在线玩家: 列表、踢出、封禁、传送、发送消息等";
    }
    
    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        
        JsonObject properties = new JsonObject();
        
        JsonObject action = new JsonObject();
        action.addProperty("type", "string");
        JsonArray actionEnum = new JsonArray();
        actionEnum.add("list");
        actionEnum.add("info");
        actionEnum.add("kick");
        actionEnum.add("ban");
        actionEnum.add("unban");
        actionEnum.add("teleport");
        actionEnum.add("message");
        actionEnum.add("inventory");
        action.add("enum", actionEnum);
        action.addProperty("description", "要执行的操作");
        properties.add("action", action);
        
        JsonObject player = new JsonObject();
        player.addProperty("type", "string");
        player.addProperty("description", "玩家名称或UUID");
        properties.add("player", player);
        
        JsonObject reason = new JsonObject();
        reason.addProperty("type", "string");
        reason.addProperty("description", "踢出/封禁原因");
        properties.add("reason", reason);
        
        JsonObject target = new JsonObject();
        target.addProperty("type", "string");
        target.addProperty("description", "目标位置或玩家");
        properties.add("target", target);
        
        JsonObject message = new JsonObject();
        message.addProperty("type", "string");
        message.addProperty("description", "要发送的消息");
        properties.add("message", message);
        
        schema.add("properties", properties);
        schema.add("required", new JsonArray());
        schema.getAsJsonArray("required").add("action");
        
        return schema;
    }
    
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        MCPConfig config = plugin.getMCPConfig();
        if (!config.isAllowPlayerManagement()) {
            return ToolResult.error("玩家管理功能已禁用");
        }
        
        String action = (String) arguments.get("action");
        if (action == null) {
            return ToolResult.error("缺少action参数");
        }
        
        return switch (action.toLowerCase()) {
            case "list" -> listPlayers();
            case "info" -> {
                String playerName = (String) arguments.get("player");
                if (playerName == null) yield ToolResult.error("缺少player参数");
                yield getPlayerInfo(playerName);
            }
            case "kick" -> {
                String playerName = (String) arguments.get("player");
                String reason = (String) arguments.getOrDefault("reason", "被管理员踢出");
                if (playerName == null) yield ToolResult.error("缺少player参数");
                yield kickPlayer(playerName, reason);
            }
            case "ban" -> {
                String playerName = (String) arguments.get("player");
                String reason = (String) arguments.getOrDefault("reason", "被管理员封禁");
                if (playerName == null) yield ToolResult.error("缺少player参数");
                yield banPlayer(playerName, reason);
            }
            case "unban" -> {
                String playerName = (String) arguments.get("player");
                if (playerName == null) yield ToolResult.error("缺少player参数");
                yield unbanPlayer(playerName);
            }
            case "message" -> {
                String playerName = (String) arguments.get("player");
                String message = (String) arguments.get("message");
                if (playerName == null || message == null) yield ToolResult.error("缺少player或message参数");
                yield sendMessage(playerName, message);
            }
            default -> ToolResult.error("未知操作: " + action);
        };
    }
    
    private ToolResult listPlayers() {
        JsonArray players = new JsonArray();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            JsonObject player = new JsonObject();
            player.addProperty("name", p.getName());
            player.addProperty("uuid", p.getUniqueId().toString());
            player.addProperty("world", p.getWorld().getName());
            player.addProperty("health", Math.round(p.getHealth() * 10) / 10.0);
            player.addProperty("level", p.getLevel());
            player.addProperty("gameMode", p.getGameMode().name());
            players.add(player);
        }
        
        JsonObject result = new JsonObject();
        result.addProperty("count", players.size());
        result.add("players", players);
        return ToolResult.success(result.toString());
    }
    
    private ToolResult getPlayerInfo(String playerName) {
        Player player = plugin.getServer().getPlayer(playerName);
        if (player == null) {
            try {
                UUID uuid = UUID.fromString(playerName);
                player = plugin.getServer().getPlayer(uuid);
            } catch (IllegalArgumentException ignored) {}
        }
        
        if (player == null) {
            OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(playerName);
            if (offlinePlayer.hasPlayedBefore()) {
                JsonObject result = new JsonObject();
                result.addProperty("name", offlinePlayer.getName());
                result.addProperty("uuid", offlinePlayer.getUniqueId().toString());
                result.addProperty("online", false);
                result.addProperty("firstPlayed", offlinePlayer.getFirstPlayed());
                result.addProperty("lastPlayed", offlinePlayer.getLastPlayed());
                result.addProperty("banned", offlinePlayer.isBanned());
                result.addProperty("whitelisted", offlinePlayer.isWhitelisted());
                return ToolResult.success(result.toString());
            }
            return ToolResult.error("玩家不存在: " + playerName);
        }
        
        JsonObject result = new JsonObject();
        result.addProperty("name", player.getName());
        result.addProperty("displayName", player.getDisplayName());
        result.addProperty("uuid", player.getUniqueId().toString());
        result.addProperty("online", true);
        
        JsonObject location = new JsonObject();
        location.addProperty("world", player.getWorld().getName());
        location.addProperty("x", Math.round(player.getLocation().getX() * 100) / 100.0);
        location.addProperty("y", Math.round(player.getLocation().getY() * 100) / 100.0);
        location.addProperty("z", Math.round(player.getLocation().getZ() * 100) / 100.0);
        location.addProperty("yaw", Math.round(player.getLocation().getYaw() * 10) / 10.0);
        location.addProperty("pitch", Math.round(player.getLocation().getPitch() * 10) / 10.0);
        result.add("location", location);
        
        result.addProperty("health", Math.round(player.getHealth() * 10) / 10.0);
        result.addProperty("maxHealth", Math.round(player.getMaxHealth() * 10) / 10.0);
        result.addProperty("foodLevel", player.getFoodLevel());
        result.addProperty("saturation", Math.round(player.getSaturation() * 10) / 10.0);
        result.addProperty("level", player.getLevel());
        result.addProperty("exp", Math.round(player.getExp() * 100) / 100.0);
        result.addProperty("gameMode", player.getGameMode().name());
        result.addProperty("op", player.isOp());
        result.addProperty("flying", player.isFlying());
        result.addProperty("sneaking", player.isSneaking());
        result.addProperty("sprinting", player.isSprinting());
        
        return ToolResult.success(result.toString());
    }
    
    private ToolResult kickPlayer(String playerName, String reason) {
        Player player = plugin.getServer().getPlayer(playerName);
        if (player == null) {
            return ToolResult.error("玩家不在线: " + playerName);
        }
        
        String finalReason = reason;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.kickPlayer(finalReason);
        });
        
        return ToolResult.success("已踢出玩家: " + playerName);
    }
    
    private ToolResult banPlayer(String playerName, String reason) {
        OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerName);
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getServer().getBanList(org.bukkit.BanList.Type.NAME).addBan(
                playerName, reason, null, "GuangDianMCP"
            );
            
            Player onlinePlayer = player.getPlayer();
            if (onlinePlayer != null) {
                onlinePlayer.kickPlayer(reason);
            }
        });
        
        return ToolResult.success("已封禁玩家: " + playerName + ", 原因: " + reason);
    }
    
    private ToolResult unbanPlayer(String playerName) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getServer().getBanList(org.bukkit.BanList.Type.NAME).pardon(playerName);
        });
        
        return ToolResult.success("已解封玩家: " + playerName);
    }
    
    private ToolResult sendMessage(String playerName, String message) {
        Player player = plugin.getServer().getPlayer(playerName);
        if (player == null) {
            return ToolResult.error("玩家不在线: " + playerName);
        }
        
        player.sendMessage(message);
        return ToolResult.success("消息已发送给: " + playerName);
    }
}
