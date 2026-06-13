package cn.guangdian.mcp.tools.impl;

import cn.guangdian.mcp.GuangDianMCP;
import cn.guangdian.mcp.config.MCPConfig;
import cn.guangdian.mcp.tools.MCPTool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigReaderTool implements MCPTool {
    
    private final GuangDianMCP plugin;
    
    public ConfigReaderTool(GuangDianMCP plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "read_config";
    }
    
    @Override
    public String getDescription() {
        return "读取服务器配置文件(server.properties, bukkit.yml等)";
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
        actionEnum.add("read");
        actionEnum.add("server_properties");
        actionEnum.add("bukkit");
        actionEnum.add("spigot");
        actionEnum.add("plugin_config");
        action.add("enum", actionEnum);
        action.addProperty("description", "要执行的操作");
        properties.add("action", action);
        
        JsonObject file = new JsonObject();
        file.addProperty("type", "string");
        file.addProperty("description", "文件路径或名称");
        properties.add("file", file);
        
        JsonObject pluginName = new JsonObject();
        pluginName.addProperty("type", "string");
        pluginName.addProperty("description", "插件名称(用于读取插件配置)");
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
        if (!config.isAllowConfigRead()) {
            return ToolResult.error("配置读取功能已禁用");
        }
        
        String action = (String) arguments.get("action");
        if (action == null) {
            return ToolResult.error("缺少action参数");
        }
        
        return switch (action.toLowerCase()) {
            case "list" -> listConfigFiles();
            case "read" -> {
                String file = (String) arguments.get("file");
                if (file == null) yield ToolResult.error("缺少file参数");
                yield readFile(file);
            }
            case "server_properties" -> readServerProperties();
            case "bukkit" -> readBukkitConfig();
            case "spigot" -> readSpigotConfig();
            case "plugin_config" -> {
                String pluginName = (String) arguments.get("plugin");
                if (pluginName == null) yield ToolResult.error("缺少plugin参数");
                yield readPluginConfig(pluginName);
            }
            default -> ToolResult.error("未知操作: " + action);
        };
    }
    
    private ToolResult listConfigFiles() {
        JsonArray files = new JsonArray();
        File serverDir = plugin.getServer().getWorldContainer().getParentFile();
        
        addConfigFile(files, serverDir, "server.properties");
        addConfigFile(files, serverDir, "bukkit.yml");
        addConfigFile(files, serverDir, "spigot.yml");
        addConfigFile(files, serverDir, "paper.yml");
        addConfigFile(files, serverDir, "permissions.yml");
        addConfigFile(files, serverDir, "commands.yml");
        addConfigFile(files, serverDir, "help.yml");
        addConfigFile(files, serverDir, "eula.txt");
        
        File pluginsDir = new File(serverDir, "plugins");
        if (pluginsDir.exists() && pluginsDir.isDirectory()) {
            for (File pluginDir : pluginsDir.listFiles()) {
                if (pluginDir.isDirectory()) {
                    for (File f : pluginDir.listFiles()) {
                        if (f.getName().endsWith(".yml")) {
                            JsonObject file = new JsonObject();
                            file.addProperty("name", f.getName());
                            file.addProperty("path", "plugins/" + pluginDir.getName() + "/" + f.getName());
                            file.addProperty("size", f.length());
                            files.add(file);
                        }
                    }
                }
            }
        }
        
        JsonObject result = new JsonObject();
        result.addProperty("count", files.size());
        result.add("files", files);
        return ToolResult.success(result.toString());
    }
    
    private void addConfigFile(JsonArray files, File dir, String name) {
        File file = new File(dir, name);
        if (file.exists()) {
            JsonObject fileObj = new JsonObject();
            fileObj.addProperty("name", name);
            fileObj.addProperty("path", name);
            fileObj.addProperty("size", file.length());
            files.add(fileObj);
        }
    }
    
    private ToolResult readFile(String filePath) {
        File serverDir = plugin.getServer().getWorldContainer().getParentFile();
        File file = new File(serverDir, filePath);
        
        if (!file.exists()) {
            return ToolResult.error("文件不存在: " + filePath);
        }
        
        if (!file.isFile()) {
            return ToolResult.error("不是文件: " + filePath);
        }
        
        try {
            String content = Files.readString(file.toPath());
            
            JsonObject result = new JsonObject();
            result.addProperty("path", filePath);
            result.addProperty("size", file.length());
            result.addProperty("content", content);
            
            return ToolResult.success(result.toString());
        } catch (IOException e) {
            return ToolResult.error("读取文件失败: " + e.getMessage());
        }
    }
    
    private ToolResult readServerProperties() {
        File serverDir = plugin.getServer().getWorldContainer().getParentFile();
        File file = new File(serverDir, "server.properties");
        
        if (!file.exists()) {
            return ToolResult.error("server.properties 不存在");
        }
        
        try {
            String content = Files.readString(file.toPath());
            
            JsonObject result = new JsonObject();
            result.addProperty("path", "server.properties");
            result.addProperty("size", file.length());
            
            JsonObject properties = new JsonObject();
            for (String line : content.split("\n")) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    int idx = line.indexOf('=');
                    if (idx > 0) {
                        String key = line.substring(0, idx).trim();
                        String value = line.substring(idx + 1).trim();
                        properties.addProperty(key, value);
                    }
                }
            }
            result.add("properties", properties);
            
            return ToolResult.success(result.toString());
        } catch (IOException e) {
            return ToolResult.error("读取文件失败: " + e.getMessage());
        }
    }
    
    private ToolResult readBukkitConfig() {
        File serverDir = plugin.getServer().getWorldContainer().getParentFile();
        File file = new File(serverDir, "bukkit.yml");
        
        if (!file.exists()) {
            return ToolResult.error("bukkit.yml 不存在");
        }
        
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        
        JsonObject result = new JsonObject();
        result.addProperty("path", "bukkit.yml");
        result.add("config", convertToJson(yaml));
        
        return ToolResult.success(result.toString());
    }
    
    private ToolResult readSpigotConfig() {
        File serverDir = plugin.getServer().getWorldContainer().getParentFile();
        File file = new File(serverDir, "spigot.yml");
        
        if (!file.exists()) {
            return ToolResult.error("spigot.yml 不存在");
        }
        
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        
        JsonObject result = new JsonObject();
        result.addProperty("path", "spigot.yml");
        result.add("config", convertToJson(yaml));
        
        return ToolResult.success(result.toString());
    }
    
    private ToolResult readPluginConfig(String pluginName) {
        org.bukkit.plugin.Plugin targetPlugin = plugin.getServer().getPluginManager().getPlugin(pluginName);
        if (targetPlugin == null) {
            return ToolResult.error("插件不存在: " + pluginName);
        }
        
        FileConfiguration pluginConfig = targetPlugin.getConfig();
        if (pluginConfig == null) {
            return ToolResult.error("插件没有配置文件: " + pluginName);
        }
        
        JsonObject result = new JsonObject();
        result.addProperty("plugin", pluginName);
        result.add("config", convertToJson(pluginConfig));
        
        return ToolResult.success(result.toString());
    }
    
    private JsonObject convertToJson(FileConfiguration config) {
        JsonObject json = new JsonObject();
        for (String key : config.getKeys(false)) {
            Object value = config.get(key);
            if (value instanceof String) {
                json.addProperty(key, (String) value);
            } else if (value instanceof Integer) {
                json.addProperty(key, (Integer) value);
            } else if (value instanceof Double) {
                json.addProperty(key, (Double) value);
            } else if (value instanceof Boolean) {
                json.addProperty(key, (Boolean) value);
            } else if (value instanceof Long) {
                json.addProperty(key, (Long) value);
            } else {
                json.addProperty(key, String.valueOf(value));
            }
        }
        return json;
    }
}
