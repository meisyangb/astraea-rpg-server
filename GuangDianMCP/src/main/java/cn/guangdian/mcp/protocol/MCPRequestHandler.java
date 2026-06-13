package cn.guangdian.mcp.protocol;

import cn.guangdian.mcp.GuangDianMCP;
import cn.guangdian.mcp.tools.ToolRegistry;
import cn.guangdian.mcp.tools.MCPTool;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class MCPRequestHandler {
    
    private final GuangDianMCP plugin;
    private final Gson gson;
    private final ToolRegistry toolRegistry;
    
    public MCPRequestHandler(GuangDianMCP plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.toolRegistry = plugin.getToolRegistry();
    }
    
    public String handleRequest(String body) {
        try {
            JsonObject request = JsonParser.parseString(body).getAsJsonObject();
            String method = request.get("method").getAsString();
            JsonObject params = request.has("params") ? request.getAsJsonObject("params") : new JsonObject();
            String id = request.has("id") ? request.get("id").getAsString() : null;
            
            JsonObject response = new JsonObject();
            if (id != null) {
                response.addProperty("id", id);
            }
            
            switch (method) {
                case "initialize":
                    response.add("result", handleInitialize(params));
                    break;
                case "tools/list":
                    response.add("result", handleToolsList());
                    break;
                case "tools/call":
                    response.add("result", handleToolsCall(params));
                    break;
                case "resources/list":
                    response.add("result", handleResourcesList());
                    break;
                case "resources/read":
                    response.add("result", handleResourcesRead(params));
                    break;
                case "prompts/list":
                    response.add("result", handlePromptsList());
                    break;
                case "prompts/get":
                    response.add("result", handlePromptsGet(params));
                    break;
                default:
                    response.add("error", createError(-32601, "Method not found: " + method));
            }
            
            return gson.toJson(response);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "处理MCP请求失败", e);
            JsonObject errorResponse = new JsonObject();
            errorResponse.add("error", createError(-32603, "Internal error: " + e.getMessage()));
            return gson.toJson(errorResponse);
        }
    }
    
    private JsonObject handleInitialize(JsonObject params) {
        JsonObject result = new JsonObject();
        
        JsonObject protocolVersion = new JsonObject();
        protocolVersion.addProperty("name", "guangdian-mcp");
        protocolVersion.addProperty("version", "1.0.0");
        result.add("protocolVersion", protocolVersion);
        
        JsonObject capabilities = new JsonObject();
        capabilities.add("tools", new JsonObject());
        capabilities.add("resources", new JsonObject());
        capabilities.add("prompts", new JsonObject());
        result.add("capabilities", capabilities);
        
        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "GuangDianMCP");
        serverInfo.addProperty("version", "1.0.0");
        result.add("serverInfo", serverInfo);
        
        return result;
    }
    
    private JsonObject handleToolsList() {
        JsonObject result = new JsonObject();
        JsonArray tools = new JsonArray();
        
        for (MCPTool tool : toolRegistry.getTools()) {
            JsonObject toolObj = new JsonObject();
            toolObj.addProperty("name", tool.getName());
            toolObj.addProperty("description", tool.getDescription());
            toolObj.add("inputSchema", tool.getInputSchema());
            tools.add(toolObj);
        }
        
        result.add("tools", tools);
        return result;
    }
    
    private JsonObject handleToolsCall(JsonObject params) {
        String toolName = params.get("name").getAsString();
        JsonObject arguments = params.has("arguments") ? params.getAsJsonObject("arguments") : new JsonObject();
        
        MCPTool tool = toolRegistry.getTool(toolName);
        if (tool == null) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Tool not found: " + toolName);
            return error;
        }
        
        try {
            Map<String, Object> args = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : arguments.entrySet()) {
                args.put(entry.getKey(), gson.fromJson(entry.getValue(), Object.class));
            }
            
            MCPTool.ToolResult result = tool.execute(args);
            
            JsonObject response = new JsonObject();
            JsonArray content = new JsonArray();
            
            JsonObject contentItem = new JsonObject();
            contentItem.addProperty("type", "text");
            contentItem.addProperty("text", result.getContent());
            content.add(contentItem);
            
            response.add("content", content);
            response.addProperty("isError", result.isError());
            
            return response;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "执行工具失败: " + toolName, e);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Tool execution failed: " + e.getMessage());
            return error;
        }
    }
    
    private JsonObject handleResourcesList() {
        JsonObject result = new JsonObject();
        JsonArray resources = new JsonArray();
        
        addResource(resources, "server://info", "服务器信息", "application/json");
        addResource(resources, "server://players", "在线玩家列表", "application/json");
        addResource(resources, "server://worlds", "世界列表", "application/json");
        addResource(resources, "server://plugins", "插件列表", "application/json");
        addResource(resources, "server://tps", "服务器TPS", "application/json");
        addResource(resources, "server://memory", "内存使用情况", "application/json");
        
        result.add("resources", resources);
        return result;
    }
    
    private void addResource(JsonArray resources, String uri, String name, String mimeType) {
        JsonObject resource = new JsonObject();
        resource.addProperty("uri", uri);
        resource.addProperty("name", name);
        resource.addProperty("mimeType", mimeType);
        resources.add(resource);
    }
    
    private JsonObject handleResourcesRead(JsonObject params) {
        String uri = params.get("uri").getAsString();
        JsonObject result = new JsonObject();
        JsonArray contents = new JsonArray();
        
        JsonObject content = new JsonObject();
        content.addProperty("uri", uri);
        content.addProperty("mimeType", "application/json");
        
        String data = readResourceData(uri);
        content.addProperty("text", data);
        
        contents.add(content);
        result.add("contents", contents);
        
        return result;
    }
    
    private String readResourceData(String uri) {
        return switch (uri) {
            case "server://info" -> {
                JsonObject info = new JsonObject();
                info.addProperty("name", plugin.getServer().getName());
                info.addProperty("version", plugin.getServer().getVersion());
                info.addProperty("bukkitVersion", plugin.getServer().getBukkitVersion());
                info.addProperty("onlineMode", plugin.getServer().getOnlineMode());
                info.addProperty("maxPlayers", plugin.getServer().getMaxPlayers());
                info.addProperty("onlinePlayers", plugin.getServer().getOnlinePlayers().size());
                yield info.toString();
            }
            case "server://players" -> {
                JsonArray players = new JsonArray();
                plugin.getServer().getOnlinePlayers().forEach(p -> {
                    JsonObject player = new JsonObject();
                    player.addProperty("name", p.getName());
                    player.addProperty("uuid", p.getUniqueId().toString());
                    player.addProperty("world", p.getWorld().getName());
                    player.addProperty("x", p.getLocation().getX());
                    player.addProperty("y", p.getLocation().getY());
                    player.addProperty("z", p.getLocation().getZ());
                    player.addProperty("health", p.getHealth());
                    player.addProperty("level", p.getLevel());
                    players.add(player);
                });
                yield players.toString();
            }
            case "server://worlds" -> {
                JsonArray worlds = new JsonArray();
                plugin.getServer().getWorlds().forEach(w -> {
                    JsonObject world = new JsonObject();
                    world.addProperty("name", w.getName());
                    world.addProperty("environment", w.getEnvironment().name());
                    world.addProperty("difficulty", w.getDifficulty().name());
                    world.addProperty("players", w.getPlayers().size());
                    world.addProperty("loadedChunks", w.getLoadedChunks().length);
                    worlds.add(world);
                });
                yield worlds.toString();
            }
            case "server://plugins" -> {
                JsonArray plugins = new JsonArray();
                for (org.bukkit.plugin.Plugin p : plugin.getServer().getPluginManager().getPlugins()) {
                    JsonObject pluginObj = new JsonObject();
                    pluginObj.addProperty("name", p.getName());
                    pluginObj.addProperty("version", p.getDescription().getVersion());
                    pluginObj.addProperty("enabled", p.isEnabled());
                    plugins.add(pluginObj);
                }
                yield plugins.toString();
            }
            case "server://tps" -> {
                JsonObject tps = new JsonObject();
                double[] tpsValues = plugin.getServer().getTPS();
                tps.addProperty("1m", tpsValues[0]);
                tps.addProperty("5m", tpsValues[1]);
                tps.addProperty("15m", tpsValues[2]);
                yield tps.toString();
            }
            case "server://memory" -> {
                JsonObject memory = new JsonObject();
                Runtime runtime = Runtime.getRuntime();
                memory.addProperty("max", runtime.maxMemory() / 1024 / 1024);
                memory.addProperty("total", runtime.totalMemory() / 1024 / 1024);
                memory.addProperty("free", runtime.freeMemory() / 1024 / 1024);
                memory.addProperty("used", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
                yield memory.toString();
            }
            default -> "{\"error\": \"Unknown resource: " + uri + "\"}";
        };
    }
    
    private JsonObject handlePromptsList() {
        JsonObject result = new JsonObject();
        JsonArray prompts = new JsonArray();
        
        JsonObject prompt1 = new JsonObject();
        prompt1.addProperty("name", "server-status");
        prompt1.addProperty("description", "获取服务器当前状态报告");
        prompts.add(prompt1);
        
        JsonObject prompt2 = new JsonObject();
        prompt2.addProperty("name", "player-report");
        prompt2.addProperty("description", "生成指定玩家的详细报告");
        prompts.add(prompt2);
        
        result.add("prompts", prompts);
        return result;
    }
    
    private JsonObject handlePromptsGet(JsonObject params) {
        String promptName = params.get("name").getAsString();
        JsonObject result = new JsonObject();
        
        String template = switch (promptName) {
            case "server-status" -> """
                请分析以下Minecraft服务器的当前状态:
                - 服务器版本: {version}
                - 在线玩家: {online}/{max}
                - TPS: {tps}
                - 内存使用: {memory}
                请提供优化建议和潜在问题分析。
                """;
            case "player-report" -> """
                请生成玩家 {player} 的详细报告:
                - 位置: {location}
                - 健康值: {health}
                - 游戏模式: {gamemode}
                - 背包物品: {inventory}
                """;
            default -> "Unknown prompt template";
        };
        
        result.addProperty("template", template);
        return result;
    }
    
    private JsonObject createError(int code, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        return error;
    }
}
