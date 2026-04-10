package cn.guangdian.mcp.tools.impl;

import cn.guangdian.mcp.GuangDianMCP;
import cn.guangdian.mcp.config.MCPConfig;
import cn.guangdian.mcp.tools.MCPTool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public class PluginManagementTool implements MCPTool {
    
    private final GuangDianMCP plugin;
    
    public PluginManagementTool(GuangDianMCP plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "plugin_management";
    }
    
    @Override
    public String getDescription() {
        return "管理插件: 列表、启用、禁用、重载等";
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
        actionEnum.add("enable");
        actionEnum.add("disable");
        actionEnum.add("reload");
        action.add("enum", actionEnum);
        action.addProperty("description", "要执行的操作");
        properties.add("action", action);
        
        JsonObject pluginName = new JsonObject();
        pluginName.addProperty("type", "string");
        pluginName.addProperty("description", "插件名称");
        properties.add("plugin", pluginName);
        
        schema.add("properties", properties);
        
        JsonArray required = new JsonArray();
        required.add("action");
        schema.add("required", required);
        
        return schema;
    }
    
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        MCPConfig config = plugin.getMCPConfig();
        if (!config.isAllowPluginManagement()) {
            return ToolResult.error("插件管理功能已禁用");
        }
        
        String action = (String) arguments.get("action");
        if (action == null) {
            return ToolResult.error("缺少action参数");
        }
        
        return switch (action.toLowerCase()) {
            case "list" -> listPlugins();
            case "info" -> {
                String pluginName = (String) arguments.get("plugin");
                if (pluginName == null) yield ToolResult.error("缺少plugin参数");
                yield getPluginInfo(pluginName);
            }
            case "enable" -> {
                String pluginName = (String) arguments.get("plugin");
                if (pluginName == null) yield ToolResult.error("缺少plugin参数");
                yield enablePlugin(pluginName);
            }
            case "disable" -> {
                String pluginName = (String) arguments.get("plugin");
                if (pluginName == null) yield ToolResult.error("缺少plugin参数");
                yield disablePlugin(pluginName);
            }
            case "reload" -> {
                String pluginName = (String) arguments.get("plugin");
                yield reloadPlugin(pluginName);
            }
            default -> ToolResult.error("未知操作: " + action);
        };
    }
    
    private ToolResult listPlugins() {
        JsonArray plugins = new JsonArray();
        for (Plugin p : plugin.getServer().getPluginManager().getPlugins()) {
            JsonObject pluginObj = new JsonObject();
            pluginObj.addProperty("name", p.getName());
            pluginObj.addProperty("version", p.getDescription().getVersion());
            pluginObj.addProperty("enabled", p.isEnabled());
            pluginObj.addProperty("main", p.getDescription().getMain());
            if (p.getDescription().getDescription() != null) {
                pluginObj.addProperty("description", p.getDescription().getDescription());
            }
            plugins.add(pluginObj);
        }
        
        JsonObject result = new JsonObject();
        result.addProperty("count", plugins.size());
        result.add("plugins", plugins);
        return ToolResult.success(result.toString());
    }
    
    private ToolResult getPluginInfo(String pluginName) {
        Plugin targetPlugin = plugin.getServer().getPluginManager().getPlugin(pluginName);
        if (targetPlugin == null) {
            return ToolResult.error("插件不存在: " + pluginName);
        }
        
        JsonObject result = new JsonObject();
        result.addProperty("name", targetPlugin.getName());
        result.addProperty("version", targetPlugin.getDescription().getVersion());
        result.addProperty("enabled", targetPlugin.isEnabled());
        result.addProperty("main", targetPlugin.getDescription().getMain());
        
        if (targetPlugin.getDescription().getDescription() != null) {
            result.addProperty("description", targetPlugin.getDescription().getDescription());
        }
        if (targetPlugin.getDescription().getAuthors() != null && !targetPlugin.getDescription().getAuthors().isEmpty()) {
            result.add("authors", new JsonArray());
            targetPlugin.getDescription().getAuthors().forEach(author -> 
                result.getAsJsonArray("authors").add(author)
            );
        }
        if (targetPlugin.getDescription().getWebsite() != null) {
            result.addProperty("website", targetPlugin.getDescription().getWebsite());
        }
        if (targetPlugin.getDescription().getDepend() != null && !targetPlugin.getDescription().getDepend().isEmpty()) {
            result.add("depend", new JsonArray());
            targetPlugin.getDescription().getDepend().forEach(dep -> 
                result.getAsJsonArray("depend").add(dep)
            );
        }
        if (targetPlugin.getDescription().getSoftDepend() != null && !targetPlugin.getDescription().getSoftDepend().isEmpty()) {
            result.add("softDepend", new JsonArray());
            targetPlugin.getDescription().getSoftDepend().forEach(dep -> 
                result.getAsJsonArray("softDepend").add(dep)
            );
        }
        
        return ToolResult.success(result.toString());
    }
    
    private ToolResult enablePlugin(String pluginName) {
        Plugin targetPlugin = plugin.getServer().getPluginManager().getPlugin(pluginName);
        if (targetPlugin == null) {
            return ToolResult.error("插件不存在: " + pluginName);
        }
        
        if (targetPlugin.isEnabled()) {
            return ToolResult.success("插件已启用: " + pluginName);
        }
        
        plugin.getServer().getPluginManager().enablePlugin(targetPlugin);
        return ToolResult.success("已启用插件: " + pluginName);
    }
    
    private ToolResult disablePlugin(String pluginName) {
        Plugin targetPlugin = plugin.getServer().getPluginManager().getPlugin(pluginName);
        if (targetPlugin == null) {
            return ToolResult.error("插件不存在: " + pluginName);
        }
        
        if (targetPlugin.getName().equals(plugin.getName())) {
            return ToolResult.error("无法禁用GuangDianMCP自身");
        }
        
        if (!targetPlugin.isEnabled()) {
            return ToolResult.success("插件已禁用: " + pluginName);
        }
        
        plugin.getServer().getPluginManager().disablePlugin(targetPlugin);
        return ToolResult.success("已禁用插件: " + pluginName);
    }
    
    private ToolResult reloadPlugin(String pluginName) {
        if (pluginName == null) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getServer().dispatchCommand(
                    plugin.getServer().getConsoleSender(),
                    "reload"
                );
            });
            return ToolResult.success("正在重载所有插件");
        }
        
        Plugin targetPlugin = plugin.getServer().getPluginManager().getPlugin(pluginName);
        if (targetPlugin == null) {
            return ToolResult.error("插件不存在: " + pluginName);
        }
        
        plugin.getServer().getPluginManager().disablePlugin(targetPlugin);
        plugin.getServer().getPluginManager().enablePlugin(targetPlugin);
        
        return ToolResult.success("已重载插件: " + pluginName);
    }
}
