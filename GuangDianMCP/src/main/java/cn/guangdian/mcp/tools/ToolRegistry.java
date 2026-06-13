package cn.guangdian.mcp.tools;

import cn.guangdian.mcp.GuangDianMCP;
import cn.guangdian.mcp.tools.impl.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ToolRegistry {
    
    private final GuangDianMCP plugin;
    private final Map<String, MCPTool> tools = new HashMap<>();
    
    public ToolRegistry(GuangDianMCP plugin) {
        this.plugin = plugin;
    }
    
    public void registerDefaultTools() {
        registerTool(new ServerInfoTool(plugin));
        registerTool(new PlayerManagementTool(plugin));
        registerTool(new CommandExecutorTool(plugin));
        registerTool(new WorldManagementTool(plugin));
        registerTool(new PluginManagementTool(plugin));
        registerTool(new ConfigReaderTool(plugin));
        registerTool(new ConfigWriterTool(plugin));
        registerTool(new LogReaderTool(plugin));
        registerTool(new WhitelistTool(plugin));
        registerTool(new BanTool(plugin));
        registerTool(new TeleportTool(plugin));
        registerTool(new ItemTool(plugin));
        registerTool(new EntityTool(plugin));
        registerTool(new SchedulerTool(plugin));
        
        plugin.getLogger().info("已注册 " + tools.size() + " 个MCP工具");
    }
    
    public void registerTool(MCPTool tool) {
        tools.put(tool.getName(), tool);
    }
    
    public void unregisterTool(String name) {
        tools.remove(name);
    }
    
    public MCPTool getTool(String name) {
        return tools.get(name);
    }
    
    public Collection<MCPTool> getTools() {
        return tools.values();
    }
    
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }
}
