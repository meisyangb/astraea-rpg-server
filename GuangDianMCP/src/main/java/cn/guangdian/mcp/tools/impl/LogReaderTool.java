package cn.guangdian.mcp.tools.impl;

import cn.guangdian.mcp.GuangDianMCP;
import cn.guangdian.mcp.config.MCPConfig;
import cn.guangdian.mcp.tools.MCPTool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LogReaderTool implements MCPTool {
    
    private final GuangDianMCP plugin;
    
    public LogReaderTool(GuangDianMCP plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "read_logs";
    }
    
    @Override
    public String getDescription() {
        return "读取服务器日志文件";
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
        actionEnum.add("latest");
        actionEnum.add("search");
        action.add("enum", actionEnum);
        action.addProperty("description", "要执行的操作");
        properties.add("action", action);
        
        JsonObject file = new JsonObject();
        file.addProperty("type", "string");
        file.addProperty("description", "日志文件名");
        properties.add("file", file);
        
        JsonObject lines = new JsonObject();
        lines.addProperty("type", "integer");
        lines.addProperty("description", "读取的行数(默认100)");
        properties.add("lines", lines);
        
        JsonObject keyword = new JsonObject();
        keyword.addProperty("type", "string");
        keyword.addProperty("description", "搜索关键词");
        properties.add("keyword", keyword);
        
        schema.add("properties", properties);
        
        JsonArray required = new JsonArray();
        required.add("action");
        schema.add("required", required);
        
        return schema;
    }
    
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        MCPConfig config = plugin.getMCPConfig();
        if (!config.isAllowLogRead()) {
            return ToolResult.error("日志读取功能已禁用");
        }
        
        String action = (String) arguments.get("action");
        if (action == null) {
            return ToolResult.error("缺少action参数");
        }
        
        return switch (action.toLowerCase()) {
            case "list" -> listLogFiles();
            case "read" -> {
                String file = (String) arguments.get("file");
                Integer lines = arguments.containsKey("lines") ? ((Number) arguments.get("lines")).intValue() : 100;
                if (file == null) yield ToolResult.error("缺少file参数");
                yield readLogFile(file, lines);
            }
            case "latest" -> {
                Integer lines = arguments.containsKey("lines") ? ((Number) arguments.get("lines")).intValue() : 100;
                yield readLatestLog(lines);
            }
            case "search" -> {
                String keyword = (String) arguments.get("keyword");
                if (keyword == null) yield ToolResult.error("缺少keyword参数");
                yield searchLogs(keyword);
            }
            default -> ToolResult.error("未知操作: " + action);
        };
    }
    
    private ToolResult listLogFiles() {
        JsonArray files = new JsonArray();
        File serverDir = plugin.getServer().getWorldContainer().getParentFile();
        File logsDir = new File(serverDir, "logs");
        
        if (!logsDir.exists() || !logsDir.isDirectory()) {
            return ToolResult.error("日志目录不存在");
        }
        
        File[] logFiles = logsDir.listFiles((dir, name) -> 
            name.endsWith(".log") || name.endsWith(".log.gz") || name.endsWith(".txt")
        );
        
        if (logFiles != null) {
            for (File f : logFiles) {
                JsonObject file = new JsonObject();
                file.addProperty("name", f.getName());
                file.addProperty("size", f.length());
                file.addProperty("lastModified", f.lastModified());
                files.add(file);
            }
        }
        
        JsonObject result = new JsonObject();
        result.addProperty("count", files.size());
        result.add("files", files);
        return ToolResult.success(result.toString());
    }
    
    private ToolResult readLogFile(String fileName, int lines) {
        File serverDir = plugin.getServer().getWorldContainer().getParentFile();
        File logsDir = new File(serverDir, "logs");
        File logFile = new File(logsDir, fileName);
        
        if (!logFile.exists()) {
            return ToolResult.error("日志文件不存在: " + fileName);
        }
        
        try {
            List<String> allLines = Files.readAllLines(logFile.toPath());
            List<String> resultLines;
            
            if (lines > 0 && allLines.size() > lines) {
                resultLines = allLines.subList(allLines.size() - lines, allLines.size());
            } else {
                resultLines = allLines;
            }
            
            JsonObject result = new JsonObject();
            result.addProperty("file", fileName);
            result.addProperty("totalLines", allLines.size());
            result.addProperty("returnedLines", resultLines.size());
            result.add("lines", new JsonArray());
            resultLines.forEach(line -> result.getAsJsonArray("lines").add(line));
            
            return ToolResult.success(result.toString());
        } catch (IOException e) {
            return ToolResult.error("读取日志失败: " + e.getMessage());
        }
    }
    
    private ToolResult readLatestLog(int lines) {
        File serverDir = plugin.getServer().getWorldContainer().getParentFile();
        File logsDir = new File(serverDir, "logs");
        File latestLog = new File(logsDir, "latest.log");
        
        if (!latestLog.exists()) {
            return ToolResult.error("latest.log 不存在");
        }
        
        try {
            List<String> allLines = Files.readAllLines(latestLog.toPath());
            List<String> resultLines;
            
            if (lines > 0 && allLines.size() > lines) {
                resultLines = allLines.subList(allLines.size() - lines, allLines.size());
            } else {
                resultLines = allLines;
            }
            
            JsonObject result = new JsonObject();
            result.addProperty("file", "latest.log");
            result.addProperty("totalLines", allLines.size());
            result.addProperty("returnedLines", resultLines.size());
            result.add("lines", new JsonArray());
            resultLines.forEach(line -> result.getAsJsonArray("lines").add(line));
            
            return ToolResult.success(result.toString());
        } catch (IOException e) {
            return ToolResult.error("读取日志失败: " + e.getMessage());
        }
    }
    
    private ToolResult searchLogs(String keyword) {
        File serverDir = plugin.getServer().getWorldContainer().getParentFile();
        File logsDir = new File(serverDir, "logs");
        File latestLog = new File(logsDir, "latest.log");
        
        if (!latestLog.exists()) {
            return ToolResult.error("latest.log 不存在");
        }
        
        try {
            List<String> allLines = Files.readAllLines(latestLog.toPath());
            List<String> matchingLines = allLines.stream()
                .filter(line -> line.toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
            
            JsonObject result = new JsonObject();
            result.addProperty("keyword", keyword);
            result.addProperty("totalLines", allLines.size());
            result.addProperty("matches", matchingLines.size());
            result.add("lines", new JsonArray());
            
            int maxResults = Math.min(matchingLines.size(), 100);
            matchingLines.subList(0, maxResults).forEach(line -> 
                result.getAsJsonArray("lines").add(line)
            );
            
            return ToolResult.success(result.toString());
        } catch (IOException e) {
            return ToolResult.error("搜索日志失败: " + e.getMessage());
        }
    }
}
