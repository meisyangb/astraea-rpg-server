package cn.guangdian.mcp.tools.impl;

import cn.guangdian.mcp.GuangDianMCP;
import cn.guangdian.mcp.tools.MCPTool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class TeleportTool implements MCPTool {
    
    private final GuangDianMCP plugin;
    
    public TeleportTool(GuangDianMCP plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "teleport";
    }
    
    @Override
    public String getDescription() {
        return "传送玩家到指定位置或其他玩家";
    }
    
    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        
        JsonObject properties = new JsonObject();
        
        JsonObject player = new JsonObject();
        player.addProperty("type", "string");
        player.addProperty("description", "要传送的玩家名称");
        properties.add("player", player);
        
        JsonObject target = new JsonObject();
        target.addProperty("type", "string");
        target.addProperty("description", "目标玩家名称");
        properties.add("target", target);
        
        JsonObject world = new JsonObject();
        world.addProperty("type", "string");
        world.addProperty("description", "目标世界名称");
        properties.add("world", world);
        
        JsonObject x = new JsonObject();
        x.addProperty("type", "number");
        x.addProperty("description", "X坐标");
        properties.add("x", x);
        
        JsonObject y = new JsonObject();
        y.addProperty("type", "number");
        y.addProperty("description", "Y坐标");
        properties.add("y", y);
        
        JsonObject z = new JsonObject();
        z.addProperty("type", "number");
        z.addProperty("description", "Z坐标");
        properties.add("z", z);
        
        schema.add("properties", properties);
        
        JsonArray required = new JsonArray();
        required.add("player");
        schema.add("required", required);
        
        return schema;
    }
    
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String playerName = (String) arguments.get("player");
        if (playerName == null) {
            return ToolResult.error("缺少player参数");
        }
        
        Player player = plugin.getServer().getPlayer(playerName);
        if (player == null) {
            return ToolResult.error("玩家不在线: " + playerName);
        }
        
        String targetName = (String) arguments.get("target");
        if (targetName != null) {
            return teleportToPlayer(player, targetName);
        }
        
        String worldName = (String) arguments.get("world");
        if (worldName != null && arguments.containsKey("x") && arguments.containsKey("y") && arguments.containsKey("z")) {
            double x = ((Number) arguments.get("x")).doubleValue();
            double y = ((Number) arguments.get("y")).doubleValue();
            double z = ((Number) arguments.get("z")).doubleValue();
            return teleportToLocation(player, worldName, x, y, z);
        }
        
        return ToolResult.error("需要指定target或world+x+y+z");
    }
    
    private ToolResult teleportToPlayer(Player player, String targetName) {
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) {
            return ToolResult.error("目标玩家不在线: " + targetName);
        }
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.teleport(target);
        });
        
        return ToolResult.success("已传送 " + player.getName() + " 到 " + target.getName());
    }
    
    private ToolResult teleportToLocation(Player player, String worldName, double x, double y, double z) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return ToolResult.error("世界不存在或未加载: " + worldName);
        }
        
        Location location = new Location(world, x, y, z);
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.teleport(location);
        });
        
        return ToolResult.success(String.format("已传送 %s 到 %s (%.1f, %.1f, %.1f)", 
            player.getName(), worldName, x, y, z));
    }
}
