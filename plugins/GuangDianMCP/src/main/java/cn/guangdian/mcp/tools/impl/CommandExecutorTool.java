package cn.guangdian.mcp.tools.impl;

import cn.guangdian.mcp.GuangDianMCP;
import cn.guangdian.mcp.config.MCPConfig;
import cn.guangdian.mcp.tools.MCPTool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CommandExecutorTool implements MCPTool {
    
    private final GuangDianMCP plugin;
    
    public CommandExecutorTool(GuangDianMCP plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "execute_command";
    }
    
    @Override
    public String getDescription() {
        return "在服务器控制台执行Minecraft命令";
    }
    
    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        
        JsonObject properties = new JsonObject();
        
        JsonObject command = new JsonObject();
        command.addProperty("type", "string");
        command.addProperty("description", "要执行的命令(不需要/前缀)");
        properties.add("command", command);
        
        schema.add("properties", properties);
        
        JsonArray required = new JsonArray();
        required.add("command");
        schema.add("required", required);
        
        return schema;
    }
    
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        MCPConfig config = plugin.getMCPConfig();
        if (!config.isAllowCommands()) {
            return ToolResult.error("命令执行功能已禁用");
        }
        
        String command = (String) arguments.get("command");
        if (command == null || command.isEmpty()) {
            return ToolResult.error("缺少command参数");
        }
        
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        final String finalCommand = command;
        
        if (config.isLogSensitiveOperations()) {
            plugin.getLogger().info("执行命令: " + finalCommand);
        }

        CompletableFuture<String> future = new CompletableFuture<>();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                boolean success = plugin.getServer().dispatchCommand(
                    plugin.getServer().getConsoleSender(), 
                    finalCommand
                );
                future.complete(success ? "命令执行成功" : "命令执行失败");
            } catch (Exception e) {
                future.complete("命令执行异常: " + e.getMessage());
            }
        });
        
        try {
            String result = future.get();
            return ToolResult.success(result);
        } catch (InterruptedException | ExecutionException e) {
            return ToolResult.error("命令执行失败: " + e.getMessage());
        }
    }
}
