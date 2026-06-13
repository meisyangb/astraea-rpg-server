package cn.guangdian.mcp.tools.impl;

import cn.guangdian.mcp.GuangDianMCP;
import cn.guangdian.mcp.tools.MCPTool;
import com.google.gson.JsonObject;

import java.util.Map;

public class ServerInfoTool implements MCPTool {
    
    private final GuangDianMCP plugin;
    
    public ServerInfoTool(GuangDianMCP plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "server_info";
    }
    
    @Override
    public String getDescription() {
        return "获取Minecraft服务器的详细运行状态和信息";
    }
    
    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", new JsonObject());
        return schema;
    }
    
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        JsonObject result = new JsonObject();
        
        result.addProperty("serverName", plugin.getServer().getName());
        result.addProperty("version", plugin.getServer().getVersion());
        result.addProperty("bukkitVersion", plugin.getServer().getBukkitVersion());
        result.addProperty("onlineMode", plugin.getServer().getOnlineMode());
        
        result.addProperty("maxPlayers", plugin.getServer().getMaxPlayers());
        result.addProperty("onlinePlayers", plugin.getServer().getOnlinePlayers().size());
        
        double[] tps = plugin.getServer().getTPS();
        result.addProperty("tps1m", Math.round(tps[0] * 100.0) / 100.0);
        result.addProperty("tps5m", Math.round(tps[1] * 100.0) / 100.0);
        result.addProperty("tps15m", Math.round(tps[2] * 100.0) / 100.0);
        
        Runtime runtime = Runtime.getRuntime();
        JsonObject memory = new JsonObject();
        memory.addProperty("maxMB", runtime.maxMemory() / 1024 / 1024);
        memory.addProperty("totalMB", runtime.totalMemory() / 1024 / 1024);
        memory.addProperty("freeMB", runtime.freeMemory() / 1024 / 1024);
        memory.addProperty("usedMB", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        result.add("memory", memory);
        
        result.addProperty("worldsCount", plugin.getServer().getWorlds().size());
        result.addProperty("pluginsCount", plugin.getServer().getPluginManager().getPlugins().length);
        
        return ToolResult.success(result.toString());
    }
}
