package cn.guangdian.mcp.tools.impl;

import cn.guangdian.mcp.GuangDianMCP;
import cn.guangdian.mcp.scheduler.SchedulerManager;
import cn.guangdian.mcp.scheduler.SchedulerManager.ScheduledTask;
import cn.guangdian.mcp.tools.MCPTool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Map;

public class SchedulerTool implements MCPTool {
    
    private final GuangDianMCP plugin;
    
    public SchedulerTool(GuangDianMCP plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "scheduler";
    }
    
    @Override
    public String getDescription() {
        return "定时任务管理: 自动重启、定时备份、定时命令、定时公告等";
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
        actionEnum.add("get");
        actionEnum.add("enable");
        actionEnum.add("disable");
        actionEnum.add("status");
        action.add("enum", actionEnum);
        action.addProperty("description", "要执行的操作");
        properties.add("action", action);
        
        JsonObject name = new JsonObject();
        name.addProperty("type", "string");
        name.addProperty("description", "任务名称");
        properties.add("name", name);
        
        JsonObject type = new JsonObject();
        type.addProperty("type", "string");
        JsonArray typeEnum = new JsonArray();
        typeEnum.add("restart");
        typeEnum.add("backup");
        typeEnum.add("command");
        typeEnum.add("clear_entities");
        typeEnum.add("announcement");
        typeEnum.add("save_all");
        typeEnum.add("clear_lag");
        type.add("enum", typeEnum);
        type.addProperty("description", "任务类型");
        properties.add("type", type);
        
        JsonObject hour = new JsonObject();
        hour.addProperty("type", "integer");
        hour.addProperty("description", "每日执行时间-小时(0-23)");
        properties.add("hour", hour);
        
        JsonObject minute = new JsonObject();
        minute.addProperty("type", "integer");
        minute.addProperty("description", "每日执行时间-分钟(0-59)");
        properties.add("minute", minute);
        
        JsonObject intervalSeconds = new JsonObject();
        intervalSeconds.addProperty("type", "integer");
        intervalSeconds.addProperty("description", "间隔执行秒数");
        properties.add("intervalSeconds", intervalSeconds);
        
        JsonObject command = new JsonObject();
        command.addProperty("type", "string");
        command.addProperty("description", "要执行的命令(仅command类型)");
        properties.add("command", command);
        
        JsonObject message = new JsonObject();
        message.addProperty("type", "string");
        message.addProperty("description", "公告消息(仅announcement类型)");
        properties.add("message", message);
        
        JsonObject warningMessage = new JsonObject();
        warningMessage.addProperty("type", "string");
        warningMessage.addProperty("description", "重启警告消息(仅restart类型)");
        properties.add("warningMessage", warningMessage);
        
        JsonObject warningSeconds = new JsonObject();
        warningSeconds.addProperty("type", "integer");
        warningSeconds.addProperty("description", "重启警告秒数(仅restart类型)");
        properties.add("warningSeconds", warningSeconds);
        
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
        
        SchedulerManager scheduler = plugin.getSchedulerManager();
        if (scheduler == null) {
            return ToolResult.error("定时任务管理器未初始化");
        }
        
        return switch (action.toLowerCase()) {
            case "list" -> listTasks(scheduler);
            case "add" -> addTask(scheduler, arguments);
            case "remove" -> removeTask(scheduler, arguments);
            case "get" -> getTask(scheduler, arguments);
            case "enable" -> enableScheduler(scheduler);
            case "disable" -> disableScheduler(scheduler);
            case "status" -> getStatus(scheduler);
            default -> ToolResult.error("未知操作: " + action);
        };
    }
    
    private ToolResult listTasks(SchedulerManager scheduler) {
        JsonArray tasks = new JsonArray();
        for (ScheduledTask task : scheduler.getAllTasks()) {
            tasks.add(task.toJson());
        }
        
        JsonObject result = new JsonObject();
        result.addProperty("count", tasks.size());
        result.addProperty("enabled", scheduler.isEnabled());
        result.add("tasks", tasks);
        
        return ToolResult.success(result.toString());
    }
    
    private ToolResult addTask(SchedulerManager scheduler, Map<String, Object> arguments) {
        String name = (String) arguments.get("name");
        String type = (String) arguments.get("type");
        
        if (name == null || type == null) {
            return ToolResult.error("缺少name或type参数");
        }
        
        ScheduledTask task = new ScheduledTask(name, type);
        
        if (arguments.containsKey("hour") && arguments.containsKey("minute")) {
            int hour = ((Number) arguments.get("hour")).intValue();
            int minute = ((Number) arguments.get("minute")).intValue();
            task.setDailyTime(hour, minute);
        } else if (arguments.containsKey("intervalSeconds")) {
            long interval = ((Number) arguments.get("intervalSeconds")).longValue();
            task.setIntervalSeconds(interval);
        } else {
            return ToolResult.error("需要指定hour+minute(每日定时)或intervalSeconds(间隔执行)");
        }
        
        if (arguments.containsKey("command")) {
            task.setCommand((String) arguments.get("command"));
        }
        if (arguments.containsKey("message")) {
            task.setMessage((String) arguments.get("message"));
        }
        if (arguments.containsKey("warningMessage")) {
            task.setWarningMessage((String) arguments.get("warningMessage"));
        }
        if (arguments.containsKey("warningSeconds")) {
            task.setWarningSeconds(((Number) arguments.get("warningSeconds")).intValue());
        }
        
        if (plugin.getEventPusher() != null) {
            task.setEventPusher(plugin.getEventPusher());
        }
        
        scheduler.addTask(task);
        
        return ToolResult.success("已添加定时任务: " + name);
    }
    
    private ToolResult removeTask(SchedulerManager scheduler, Map<String, Object> arguments) {
        String name = (String) arguments.get("name");
        if (name == null) {
            return ToolResult.error("缺少name参数");
        }
        
        ScheduledTask existing = scheduler.getTask(name);
        if (existing == null) {
            return ToolResult.error("任务不存在: " + name);
        }
        
        scheduler.removeTask(name);
        return ToolResult.success("已移除定时任务: " + name);
    }
    
    private ToolResult getTask(SchedulerManager scheduler, Map<String, Object> arguments) {
        String name = (String) arguments.get("name");
        if (name == null) {
            return ToolResult.error("缺少name参数");
        }
        
        ScheduledTask task = scheduler.getTask(name);
        if (task == null) {
            return ToolResult.error("任务不存在: " + name);
        }
        
        return ToolResult.success(task.toJson().toString());
    }
    
    private ToolResult enableScheduler(SchedulerManager scheduler) {
        scheduler.setEnabled(true);
        return ToolResult.success("定时任务管理器已启用");
    }
    
    private ToolResult disableScheduler(SchedulerManager scheduler) {
        scheduler.setEnabled(false);
        return ToolResult.success("定时任务管理器已禁用");
    }
    
    private ToolResult getStatus(SchedulerManager scheduler) {
        JsonObject status = new JsonObject();
        status.addProperty("enabled", scheduler.isEnabled());
        status.addProperty("taskCount", scheduler.getAllTasks().size());
        return ToolResult.success(status.toString());
    }
}
