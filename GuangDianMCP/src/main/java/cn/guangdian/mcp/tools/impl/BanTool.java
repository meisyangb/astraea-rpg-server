package cn.guangdian.mcp.tools.impl;

import cn.guangdian.mcp.GuangDianMCP;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.mcp.tools.MCPTool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.BanList;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.Set;

public class BanTool implements MCPTool {
    
    private final GuangDianMCP plugin;
    
    public BanTool(GuangDianMCP plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "ban";
    }
    
    @Override
    public String getDescription() {
        return "管理封禁列表(玩家封禁和IP封禁)";
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
        actionEnum.add("ban");
        actionEnum.add("unban");
        actionEnum.add("ban_ip");
        actionEnum.add("unban_ip");
        actionEnum.add("pardon");
        action.add("enum", actionEnum);
        action.addProperty("description", "要执行的操作");
        properties.add("action", action);
        
        JsonObject target = new JsonObject();
        target.addProperty("type", "string");
        target.addProperty("description", "玩家名称或IP地址");
        properties.add("target", target);
        
        JsonObject reason = new JsonObject();
        reason.addProperty("type", "string");
        reason.addProperty("description", "封禁原因");
        properties.add("reason", reason);
        
        schema.add("properties", properties);
        
        JsonArray required = new JsonArray();
        required.add("action");
        schema.add("required", required);
        
        return schema;
    }
    
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String action = (String) arguments.get("action");
        if (action == null) {
            return ToolResult.error("缺少action参数");
        }
        
        return switch (action.toLowerCase()) {
            case "list" -> listBans();
            case "ban" -> {
                String target = (String) arguments.get("target");
                String reason = (String) arguments.getOrDefault("reason", "被封禁");
                if (target == null) yield ToolResult.error("缺少target参数");
                yield banPlayer(target, reason);
            }
            case "unban", "pardon" -> {
                String target = (String) arguments.get("target");
                if (target == null) yield ToolResult.error("缺少target参数");
                yield unbanPlayer(target);
            }
            case "ban_ip" -> {
                String target = (String) arguments.get("target");
                String reason = (String) arguments.getOrDefault("reason", "IP被封禁");
                if (target == null) yield ToolResult.error("缺少target参数");
                yield banIp(target, reason);
            }
            case "unban_ip" -> {
                String target = (String) arguments.get("target");
                if (target == null) yield ToolResult.error("缺少target参数");
                yield unbanIp(target);
            }
            default -> ToolResult.error("未知操作: " + action);
        };
    }
    
    private ToolResult listBans() {
        JsonObject result = new JsonObject();
        
        JsonArray playerBans = new JsonArray();
        Set<org.bukkit.BanEntry> banEntries = plugin.getServer().getBanList(BanList.Type.NAME).getBanEntries();
        for (org.bukkit.BanEntry entry : banEntries) {
            JsonObject ban = new JsonObject();
            ban.addProperty("target", entry.getTarget());
            ban.addProperty("reason", entry.getReason());
            ban.addProperty("source", entry.getSource());
            ban.addProperty("created", entry.getCreated().getTime());
            if (entry.getExpiration() != null) {
                ban.addProperty("expiration", entry.getExpiration().getTime());
            }
            playerBans.add(ban);
        }
        result.add("playerBans", playerBans);
        result.addProperty("playerBanCount", playerBans.size());
        
        JsonArray ipBans = new JsonArray();
        Set<org.bukkit.BanEntry> ipBanEntries = plugin.getServer().getBanList(BanList.Type.IP).getBanEntries();
        for (org.bukkit.BanEntry entry : ipBanEntries) {
            JsonObject ban = new JsonObject();
            ban.addProperty("target", entry.getTarget());
            ban.addProperty("reason", entry.getReason());
            ban.addProperty("source", entry.getSource());
            ipBans.add(ban);
        }
        result.add("ipBans", ipBans);
        result.addProperty("ipBanCount", ipBans.size());
        
        return ToolResult.success(result.toString());
    }
    
    private ToolResult banPlayer(String playerName, String reason) {
        plugin.getServer().getBanList(BanList.Type.NAME).addBan(
            playerName, reason, null, "GuangDianMCP"
        );
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerName);
            if (player.isOnline()) {
                player.getPlayer().kickPlayer(reason);
            }
        });
        
        return ToolResult.success("已封禁玩家: " + playerName);
    }
    
    private ToolResult unbanPlayer(String playerName) {
        plugin.getServer().getBanList(BanList.Type.NAME).pardon(playerName);
        return ToolResult.success("已解封玩家: " + playerName);
    }
    
    private ToolResult banIp(String ip, String reason) {
        plugin.getServer().getBanList(BanList.Type.IP).addBan(
            ip, reason, null, "GuangDianMCP"
        );
        return ToolResult.success("已封禁IP: " + ip);
    }
    
    private ToolResult unbanIp(String ip) {
        plugin.getServer().getBanList(BanList.Type.IP).pardon(ip);
        return ToolResult.success("已解封IP: " + ip);
    }
}
