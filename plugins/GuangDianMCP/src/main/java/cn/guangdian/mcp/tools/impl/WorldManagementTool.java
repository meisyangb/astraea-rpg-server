package cn.guangdian.mcp.tools.impl;

import cn.guangdian.mcp.GuangDianMCP;
import cn.guangdian.mcp.config.MCPConfig;
import cn.guangdian.mcp.tools.MCPTool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.util.Map;

public class WorldManagementTool implements MCPTool {
    
    private final GuangDianMCP plugin;
    
    public WorldManagementTool(GuangDianMCP plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "world_management";
    }
    
    @Override
    public String getDescription() {
        return "管理世界: 列表、加载、卸载、传送、保存等";
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
        actionEnum.add("load");
        actionEnum.add("unload");
        actionEnum.add("save");
        actionEnum.add("create");
        action.add("enum", actionEnum);
        action.addProperty("description", "要执行的操作");
        properties.add("action", action);
        
        JsonObject world = new JsonObject();
        world.addProperty("type", "string");
        world.addProperty("description", "世界名称");
        properties.add("world", world);
        
        JsonObject environment = new JsonObject();
        environment.addProperty("type", "string");
        environment.addProperty("description", "世界环境类型(NORMAL/NETHER/THE_END)");
        properties.add("environment", environment);
        
        schema.add("properties", properties);
        
        JsonArray required = new JsonArray();
        required.add("action");
        schema.add("required", required);
        
        return schema;
    }
    
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        MCPConfig config = plugin.getMCPConfig();
        if (!config.isAllowWorldManagement()) {
            return ToolResult.error("世界管理功能已禁用");
        }
        
        String action = (String) arguments.get("action");
        if (action == null) {
            return ToolResult.error("缺少action参数");
        }
        
        return switch (action.toLowerCase()) {
            case "list" -> listWorlds();
            case "info" -> {
                String worldName = (String) arguments.get("world");
                if (worldName == null) yield ToolResult.error("缺少world参数");
                yield getWorldInfo(worldName);
            }
            case "load" -> {
                String worldName = (String) arguments.get("world");
                if (worldName == null) yield ToolResult.error("缺少world参数");
                yield loadWorld(worldName);
            }
            case "unload" -> {
                String worldName = (String) arguments.get("world");
                if (worldName == null) yield ToolResult.error("缺少world参数");
                yield unloadWorld(worldName);
            }
            case "save" -> {
                String worldName = (String) arguments.get("world");
                yield saveWorld(worldName);
            }
            case "create" -> {
                String worldName = (String) arguments.get("world");
                String env = (String) arguments.getOrDefault("environment", "NORMAL");
                if (worldName == null) yield ToolResult.error("缺少world参数");
                yield createWorld(worldName, env);
            }
            default -> ToolResult.error("未知操作: " + action);
        };
    }
    
    private ToolResult listWorlds() {
        JsonArray worlds = new JsonArray();
        for (World w : plugin.getServer().getWorlds()) {
            JsonObject world = new JsonObject();
            world.addProperty("name", w.getName());
            world.addProperty("environment", w.getEnvironment().name());
            world.addProperty("difficulty", w.getDifficulty().name());
            world.addProperty("players", w.getPlayers().size());
            world.addProperty("loadedChunks", w.getLoadedChunks().length);
            world.addProperty("time", w.getTime());
            world.addProperty("allowMonsters", w.getAllowMonsters());
            world.addProperty("allowAnimals", w.getAllowAnimals());
            worlds.add(world);
        }
        
        JsonObject result = new JsonObject();
        result.addProperty("count", worlds.size());
        result.add("worlds", worlds);
        return ToolResult.success(result.toString());
    }
    
    private ToolResult getWorldInfo(String worldName) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return ToolResult.error("世界不存在或未加载: " + worldName);
        }
        
        JsonObject result = new JsonObject();
        result.addProperty("name", world.getName());
        result.addProperty("environment", world.getEnvironment().name());
        result.addProperty("difficulty", world.getDifficulty().name());
        result.addProperty("seed", world.getSeed());
        result.addProperty("time", world.getTime());
        result.addProperty("fullTime", world.getFullTime());
        result.addProperty("dayTime", world.isDayTime());
        result.addProperty("players", world.getPlayers().size());
        result.addProperty("loadedChunks", world.getLoadedChunks().length);
        result.addProperty("entities", world.getEntities().size());
        result.addProperty("allowMonsters", world.getAllowMonsters());
        result.addProperty("allowAnimals", world.getAllowAnimals());
        result.addProperty("pvp", world.getPVP());
        result.addProperty("spawnLocation", 
            String.format("%.1f, %.1f, %.1f", 
                world.getSpawnLocation().getX(),
                world.getSpawnLocation().getY(),
                world.getSpawnLocation().getZ()
            )
        );
        
        return ToolResult.success(result.toString());
    }
    
    private ToolResult loadWorld(String worldName) {
        World world = plugin.getServer().getWorld(worldName);
        if (world != null) {
            return ToolResult.success("世界已加载: " + worldName);
        }
        
        // 使用 Paper AsyncScheduler 异步加载世界
        org.bukkit.Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> {
            WorldCreator creator = new WorldCreator(worldName);
            plugin.getServer().createWorld(creator);
        });
        
        return ToolResult.success("正在加载世界: " + worldName);
    }
    
    private ToolResult unloadWorld(String worldName) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return ToolResult.error("世界不存在或未加载: " + worldName);
        }
        
        if (world.getName().equals(plugin.getServer().getWorlds().get(0).getName())) {
            return ToolResult.error("无法卸载主世界");
        }
        
        // 使用 RPGCore SyncScheduler 同步卸载世界
        if (plugin.getRPGCoreScheduler() != null) {
            plugin.getRPGCoreScheduler().runSyncLater(() -> {
                plugin.getServer().unloadWorld(world, true);
            }, 0L);
        }
        
        return ToolResult.success("正在卸载世界: " + worldName);
    }
    
    private ToolResult saveWorld(String worldName) {
        if (worldName == null) {
            // 使用 RPGCore SyncScheduler 同步保存所有世界
            if (plugin.getRPGCoreScheduler() != null) {
                plugin.getRPGCoreScheduler().runSyncLater(() -> {
                    for (World w : plugin.getServer().getWorlds()) {
                        w.save();
                    }
                }, 0L);
            }
            return ToolResult.success("正在保存所有世界");
        }
        
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return ToolResult.error("世界不存在或未加载: " + worldName);
        }
        
        // 使用 RPGCore SyncScheduler 同步保存世界
        if (plugin.getRPGCoreScheduler() != null) {
            plugin.getRPGCoreScheduler().runSyncLater(() -> {
                world.save();
            }, 0L);
        }
        
        return ToolResult.success("正在保存世界: " + worldName);
    }
    
    private ToolResult createWorld(String worldName, String environment) {
        World world = plugin.getServer().getWorld(worldName);
        if (world != null) {
            return ToolResult.error("世界已存在: " + worldName);
        }
        
        World.Environment env;
        try {
            env = World.Environment.valueOf(environment.toUpperCase());
        } catch (IllegalArgumentException e) {
            env = World.Environment.NORMAL;
        }
        
        final World.Environment finalEnv = env;
        // 使用 Paper AsyncScheduler 异步创建世界
        org.bukkit.Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> {
            WorldCreator creator = new WorldCreator(worldName);
            creator.environment(finalEnv);
            creator.type(WorldType.NORMAL);
            plugin.getServer().createWorld(creator);
        });
        
        return ToolResult.success("正在创建世界: " + worldName + " (" + env + ")");
    }
}
