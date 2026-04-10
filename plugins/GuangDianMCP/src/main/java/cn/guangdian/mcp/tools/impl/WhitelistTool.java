package cn.guangdian.mcp.tools.impl;

import cn.guangdian.mcp.GuangDianMCP;
import cn.guangdian.mcp.tools.MCPTool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.OfflinePlayer;

import java.util.Map;

public class WhitelistTool implements MCPTool {
    
    private final GuangDianMCP plugin;
    
    public WhitelistTool(GuangDianMCP plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "whitelist";
    }
    
    @Override
    public String getDescription() {
        return "管理服务器白名单";
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
        actionEnum.add("add");
        actionEnum.add("remove");
        actionEnum.add("on");
        actionEnum.add("off");
        actionEnum.add("status");
        action.add("enum", actionEnum);
        action.addProperty("description", "要执行的操作");
        properties.add("action", action);
        
        JsonObject player = new JsonObject();
        player.addProperty("type", "string");
        player.addProperty("description", "玩家名称");
        properties.add("player", player);
        
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
            case "list" -> listWhitelist();
            case "add" -> {
                String player = (String) arguments.get("player");
                if (player == null) yield ToolResult.error("缺少player参数");
                yield addToWhitelist(player);
            }
            case "remove" -> {
                String player = (String) arguments.get("player");
                if (player == null) yield ToolResult.error("缺少player参数");
                yield removeFromWhitelist(player);
            }
            case "on" -> enableWhitelist();
            case "off" -> disableWhitelist();
            case "status" -> getWhitelistStatus();
            default -> ToolResult.error("未知操作: " + action);
        };
    }
    
    private ToolResult listWhitelist() {
        JsonArray players = new JsonArray();
        for (OfflinePlayer p : plugin.getServer().getWhitelistedPlayers()) {
            JsonObject player = new JsonObject();
            player.addProperty("name", p.getName());
            player.addProperty("uuid", p.getUniqueId().toString());
            players.add(player);
        }
        
        JsonObject result = new JsonObject();
        result.addProperty("count", players.size());
        result.add("players", players);
        return ToolResult.success(result.toString());
    }
    
    private ToolResult addToWhitelist(String playerName) {
        OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerName);
        player.setWhitelisted(true);
        return ToolResult.success("已将 " + playerName + " 添加到白名单");
    }
    
    private ToolResult removeFromWhitelist(String playerName) {
        OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerName);
        player.setWhitelisted(false);
        return ToolResult.success("已将 " + playerName + " 从白名单移除");
    }
    
    private ToolResult enableWhitelist() {
        plugin.getServer().setWhitelist(true);
        return ToolResult.success("白名单已启用");
    }
    
    private ToolResult disableWhitelist() {
        plugin.getServer().setWhitelist(false);
        return ToolResult.success("白名单已禁用");
    }
    
    private ToolResult getWhitelistStatus() {
        JsonObject result = new JsonObject();
        result.addProperty("enabled", plugin.getServer().hasWhitelist());
        result.addProperty("count", plugin.getServer().getWhitelistedPlayers().size());
        return ToolResult.success(result.toString());
    }
}
