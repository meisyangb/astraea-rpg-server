package cn.guangdian.mcp.tools.impl;

import cn.guangdian.mcp.GuangDianMCP;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.mcp.config.MCPConfig;
import cn.guangdian.mcp.tools.MCPTool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ConfigWriterTool implements MCPTool {
    
    private final GuangDianMCP plugin;
    
    public ConfigWriterTool(GuangDianMCP plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "write_config";
    }
    
    @Override
    public String getDescription() {
        return "安全写入服务器配置文件(需要启用配置写入功能)";
    }
    
    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        
        JsonObject properties = new JsonObject();
        
        JsonObject action = new JsonObject();
        action.addProperty("type", "string");
        JsonArray actionEnum = new JsonArray();
        actionEnum.add("set_property");
        actionEnum.add("set_plugin_config");
        actionEnum.add("backup");
        actionEnum.add("restore");
        action.add("enum", actionEnum);
        action.addProperty("description", "要执行的操作");
        properties.add("action", action);
        
        JsonObject file = new JsonObject();
        file.addProperty("type", "string");
        file.addProperty("description", "文件路径");
        properties.add("file", file);
        
        JsonObject pluginName = new JsonObject();
        pluginName.addProperty("type", "string");
        pluginName.addProperty("description", "插件名称");
        properties.add("plugin", pluginName);
        
        JsonObject key = new JsonObject();
        key.addProperty("type", "string");
        key.addProperty("description", "配置键");
        properties.add("key", key);
        
        JsonObject value = new JsonObject();
        value.addProperty("type", "string");
        value.addProperty("description", "配置值");
        properties.add("value", value);
        
        schema.add("properties", properties);
        
        JsonArray required = new JsonArray();
        required.add("action");
        schema.add("required", required);
        
        return schema;
    }
    
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        MCPConfig config = plugin.getMCPConfig();
        if (!config.isAllowConfigWrite()) {
            return ToolResult.error("配置写入功能已禁用，请在config.yml中启用allow-config-write");
        }
        
        String action = (String) arguments.get("action");
        if (action == null) {
            return ToolResult.error("缺少action参数");
        }
        
        if (config.isLogSensitiveOperations()) {
            plugin.getLogger().info("配置写入操作: " + action + " - " + arguments);
        }
        
        return switch (action.toLowerCase()) {
            case "set_property" -> {
                String file = (String) arguments.get("file");
                String key = (String) arguments.get("key");
                String value = (String) arguments.get("value");
                if (file == null || key == null || value == null) {
                    yield ToolResult.error("缺少file/key/value参数");
                }
                yield setProperty(file, key, value);
            }
            case "set_plugin_config" -> {
                String pluginName = (String) arguments.get("plugin");
                String key = (String) arguments.get("key");
                String value = (String) arguments.get("value");
                if (pluginName == null || key == null || value == null) {
                    yield ToolResult.error("缺少plugin/key/value参数");
                }
                yield setPluginConfig(pluginName, key, value);
            }
            case "backup" -> {
                String file = (String) arguments.get("file");
                yield backupConfig(file);
            }
            case "restore" -> {
                String file = (String) arguments.get("file");
                if (file == null) yield ToolResult.error("缺少file参数");
                yield restoreConfig(file);
            }
            default -> ToolResult.error("未知操作: " + action);
        };
    }
    
    private ToolResult setProperty(String filePath, String key, String value) {
        File serverDir = plugin.getServer().getWorldContainer().getParentFile();
        File file = new File(serverDir, filePath);
        
        if (!file.exists()) {
            return ToolResult.error("文件不存在: " + filePath);
        }
        
        if (filePath.equals("server.properties")) {
            return setServerProperty(file, key, value);
        } else if (filePath.endsWith(".yml")) {
            return setYamlProperty(file, key, value);
        } else {
            return ToolResult.error("不支持的文件类型: " + filePath);
        }
    }
    
    private ToolResult setServerProperty(File file, String key, String value) {
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            boolean found = false;
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.startsWith(key + "=")) {
                    lines.set(i, key + "=" + value);
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                lines.add(key + "=" + value);
            }
            
            Files.write(file.toPath(), lines);
            
            return ToolResult.success("已设置 server.properties: " + key + "=" + value + " (需要重启服务器生效)");
        } catch (IOException e) {
            return ToolResult.error("写入失败: " + e.getMessage());
        }
    }
    
    private ToolResult setYamlProperty(File file, String key, String value) {
        try {
            org.bukkit.configuration.file.YamlConfiguration yaml = 
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            
            Object parsedValue = parseValue(value);
            yaml.set(key, parsedValue);
            yaml.save(file);
            
            return ToolResult.success("已设置 " + file.getName() + ": " + key + "=" + value);
        } catch (Exception e) {
            return ToolResult.error("写入失败: " + e.getMessage());
        }
    }
    
    private Object parseValue(String value) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {}
        
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {}
        
        return value;
    }
    
    private ToolResult setPluginConfig(String pluginName, String key, String value) {
        org.bukkit.plugin.Plugin targetPlugin = plugin.getServer().getPluginManager().getPlugin(pluginName);
        if (targetPlugin == null) {
            return ToolResult.error("插件不存在: " + pluginName);
        }
        
        if (targetPlugin.getName().equals(plugin.getName())) {
            return ToolResult.error("不能通过此方式修改GuangDianMCP配置");
        }
        
        CompletableFuture<ToolResult> future = new CompletableFuture<>();
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                org.bukkit.configuration.file.FileConfiguration config = targetPlugin.getConfig();
                Object parsedValue = parseValue(value);
                config.set(key, parsedValue);
                targetPlugin.saveConfig();
                
                future.complete(ToolResult.success("已设置 " + pluginName + " 配置: " + key + "=" + value));
            } catch (Exception e) {
                future.complete(ToolResult.error("写入失败: " + e.getMessage()));
            }
        });
        
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            return ToolResult.error("写入失败: " + e.getMessage());
        }
    }
    
    private ToolResult backupConfig(String filePath) {
        File serverDir = plugin.getServer().getWorldContainer().getParentFile();
        File backupDir = new File(serverDir, "config_backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        
        if (filePath == null) {
            int count = 0;
            String[] configFiles = {"server.properties", "bukkit.yml", "spigot.yml", "paper.yml"};
            for (String configName : configFiles) {
                File source = new File(serverDir, configName);
                if (source.exists()) {
                    File backup = new File(backupDir, configName + ".backup." + System.currentTimeMillis());
                    try {
                        Files.copy(source.toPath(), backup.toPath());
                        count++;
                    } catch (IOException e) {
                        plugin.getLogger().warning("备份失败: " + configName + " - " + e.getMessage());
                    }
                }
            }
            return ToolResult.success("已备份 " + count + " 个配置文件到 config_backups/");
        }
        
        File source = new File(serverDir, filePath);
        if (!source.exists()) {
            return ToolResult.error("文件不存在: " + filePath);
        }
        
        File backup = new File(backupDir, filePath.replace("/", "_") + ".backup." + System.currentTimeMillis());
        try {
            Files.copy(source.toPath(), backup.toPath());
            return ToolResult.success("已备份到: config_backups/" + backup.getName());
        } catch (IOException e) {
            return ToolResult.error("备份失败: " + e.getMessage());
        }
    }
    
    private ToolResult restoreConfig(String backupFileName) {
        File serverDir = plugin.getServer().getWorldContainer().getParentFile();
        File backupDir = new File(serverDir, "config_backups");
        File backupFile = new File(backupDir, backupFileName);
        
        if (!backupFile.exists()) {
            return ToolResult.error("备份文件不存在: " + backupFileName);
        }
        
        String originalName = backupFileName.split("\\.backup\\.")[0].replace("_", "/");
        File targetFile = new File(serverDir, originalName);
        
        try {
            Files.copy(backupFile.toPath(), targetFile.toPath(), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return ToolResult.success("已恢复配置: " + originalName + " (需要重启服务器生效)");
        } catch (IOException e) {
            return ToolResult.error("恢复失败: " + e.getMessage());
        }
    }
}
