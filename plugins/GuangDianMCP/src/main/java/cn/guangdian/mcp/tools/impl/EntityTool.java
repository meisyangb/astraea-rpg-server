package cn.guangdian.mcp.tools.impl;

import cn.guangdian.mcp.GuangDianMCP;
import cn.guangdian.mcp.tools.MCPTool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class EntityTool implements MCPTool {
    
    private final GuangDianMCP plugin;
    
    public EntityTool(GuangDianMCP plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "entity";
    }
    
    @Override
    public String getDescription() {
        return "实体管理: 查询、清除、生成实体";
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
        actionEnum.add("count");
        actionEnum.add("kill");
        actionEnum.add("spawn");
        actionEnum.add("info");
        actionEnum.add("spawn_at");
        actionEnum.add("kill_near");
        action.add("enum", actionEnum);
        action.addProperty("description", "要执行的操作");
        properties.add("action", action);
        
        JsonObject world = new JsonObject();
        world.addProperty("type", "string");
        world.addProperty("description", "世界名称");
        properties.add("world", world);
        
        JsonObject type = new JsonObject();
        type.addProperty("type", "string");
        type.addProperty("description", "实体类型");
        properties.add("type", type);
        
        JsonObject radius = new JsonObject();
        radius.addProperty("type", "number");
        radius.addProperty("description", "半径范围");
        properties.add("radius", radius);
        
        JsonObject x = new JsonObject();
        x.addProperty("type", "number");
        x.addProperty("description", "X坐标");
        properties.add("x", x);
        
        JsonObject y = new JsonObject();
        y.addProperty("type", "number");
        y.addProperty("description", "Y坐标");
        properties.add("y", y);
        
        JsonObject z = new JsonObject();
        z.addProperty("type", "number");
        z.addProperty("description", "Z坐标");
        properties.add("z", z);
        
        JsonObject entityId = new JsonObject();
        entityId.addProperty("type", "string");
        entityId.addProperty("description", "实体UUID");
        properties.add("entityId", entityId);
        
        JsonObject amount = new JsonObject();
        amount.addProperty("type", "integer");
        amount.addProperty("description", "生成数量");
        properties.add("amount", amount);
        
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
        
        try {
            return switch (action.toLowerCase()) {
                case "list" -> {
                    String worldName = (String) arguments.get("world");
                    yield listEntities(worldName);
                }
                case "count" -> {
                    String worldName = (String) arguments.get("world");
                    String typeName = (String) arguments.get("type");
                    yield countEntities(worldName, typeName);
                }
                case "kill" -> {
                    String worldName = (String) arguments.get("world");
                    String typeName = (String) arguments.get("type");
                    yield killEntities(worldName, typeName);
                }
                case "spawn" -> {
                    String typeName = (String) arguments.get("type");
                    String worldName = (String) arguments.get("world");
                    Integer amount = arguments.containsKey("amount") ? ((Number) arguments.get("amount")).intValue() : 1;
                    if (typeName == null) yield ToolResult.error("缺少type参数");
                    yield spawnEntity(typeName, worldName, amount);
                }
                case "spawn_at" -> {
                    String typeName = (String) arguments.get("type");
                    String worldName = (String) arguments.get("world");
                    if (typeName == null) yield ToolResult.error("缺少type参数");
                    if (worldName == null) yield ToolResult.error("缺少world参数");
                    if (!arguments.containsKey("x") || !arguments.containsKey("y") || !arguments.containsKey("z")) {
                        yield ToolResult.error("缺少坐标参数x/y/z");
                    }
                    double x = ((Number) arguments.get("x")).doubleValue();
                    double y = ((Number) arguments.get("y")).doubleValue();
                    double z = ((Number) arguments.get("z")).doubleValue();
                    Integer amount = arguments.containsKey("amount") ? ((Number) arguments.get("amount")).intValue() : 1;
                    yield spawnEntityAt(typeName, worldName, x, y, z, amount);
                }
                case "info" -> {
                    String entityId = (String) arguments.get("entityId");
                    String worldName = (String) arguments.get("world");
                    if (entityId == null && worldName == null) {
                        yield ToolResult.error("需要entityId或world参数");
                    }
                    yield getEntityInfo(entityId, worldName);
                }
                case "kill_near" -> {
                    String worldName = (String) arguments.get("world");
                    String typeName = (String) arguments.get("type");
                    if (!arguments.containsKey("radius")) {
                        yield ToolResult.error("缺少radius参数");
                    }
                    double radius = ((Number) arguments.get("radius")).doubleValue();
                    double x = arguments.containsKey("x") ? ((Number) arguments.get("x")).doubleValue() : 0;
                    double y = arguments.containsKey("y") ? ((Number) arguments.get("y")).doubleValue() : 64;
                    double z = arguments.containsKey("z") ? ((Number) arguments.get("z")).doubleValue() : 0;
                    yield killEntitiesNear(worldName, typeName, x, y, z, radius);
                }
                default -> ToolResult.error("未知操作: " + action);
            };
        } catch (Exception e) {
            return ToolResult.error("执行失败: " + e.getMessage());
        }
    }
    
    private ToolResult listEntities(String worldName) {
        CompletableFuture<ToolResult> future = new CompletableFuture<>();
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                JsonObject result = new JsonObject();
                
                if (worldName != null) {
                    World world = plugin.getServer().getWorld(worldName);
                    if (world == null) {
                        future.complete(ToolResult.error("世界不存在: " + worldName));
                        return;
                    }
                    result.add("entities", getEntitiesInWorld(world));
                    result.addProperty("world", worldName);
                } else {
                    JsonArray allEntities = new JsonArray();
                    for (World world : plugin.getServer().getWorlds()) {
                        JsonObject worldEntities = new JsonObject();
                        worldEntities.addProperty("world", world.getName());
                        worldEntities.add("entities", getEntitiesInWorld(world));
                        allEntities.add(worldEntities);
                    }
                    result.add("worlds", allEntities);
                }
                
                future.complete(ToolResult.success(result.toString()));
            } catch (Exception e) {
                future.complete(ToolResult.error("执行失败: " + e.getMessage()));
            }
        });
        
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            return ToolResult.error("执行失败: " + e.getMessage());
        }
    }
    
    private JsonArray getEntitiesInWorld(World world) {
        JsonArray entities = new JsonArray();
        
        Map<EntityType, Integer> counts = new java.util.HashMap<>();
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof Player)) {
                counts.merge(entity.getType(), 1, Integer::sum);
            }
        }
        
        counts.forEach((type, count) -> {
            JsonObject entity = new JsonObject();
            entity.addProperty("type", type.name());
            entity.addProperty("count", count);
            entities.add(entity);
        });
        
        return entities;
    }
    
    private ToolResult countEntities(String worldName, String typeName) {
        CompletableFuture<ToolResult> future = new CompletableFuture<>();
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                int total = 0;
                JsonObject result = new JsonObject();
                
                if (typeName != null) {
                    EntityType targetType;
                    try {
                        targetType = EntityType.valueOf(typeName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        future.complete(ToolResult.error("无效的实体类型: " + typeName));
                        return;
                    }
                    
                    if (worldName != null) {
                        World world = plugin.getServer().getWorld(worldName);
                        if (world == null) {
                            future.complete(ToolResult.error("世界不存在: " + worldName));
                            return;
                        }
                        total = (int) world.getEntities().stream()
                            .filter(e -> e.getType() == targetType && !(e instanceof Player))
                            .count();
                        result.addProperty("world", worldName);
                    } else {
                        for (World world : plugin.getServer().getWorlds()) {
                            total += world.getEntities().stream()
                                .filter(e -> e.getType() == targetType && !(e instanceof Player))
                                .count();
                        }
                    }
                    result.addProperty("type", typeName);
                } else {
                    if (worldName != null) {
                        World world = plugin.getServer().getWorld(worldName);
                        if (world == null) {
                            future.complete(ToolResult.error("世界不存在: " + worldName));
                            return;
                        }
                        total = (int) world.getEntities().stream()
                            .filter(e -> !(e instanceof Player))
                            .count();
                        result.addProperty("world", worldName);
                    } else {
                        for (World world : plugin.getServer().getWorlds()) {
                            total += world.getEntities().stream()
                                .filter(e -> !(e instanceof Player))
                                .count();
                        }
                    }
                }
                
                result.addProperty("count", total);
                future.complete(ToolResult.success(result.toString()));
            } catch (Exception e) {
                future.complete(ToolResult.error("执行失败: " + e.getMessage()));
            }
        });
        
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            return ToolResult.error("执行失败: " + e.getMessage());
        }
    }
    
    private ToolResult killEntities(String worldName, String typeName) {
        CompletableFuture<ToolResult> future = new CompletableFuture<>();
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                int killed = 0;
                
                if (typeName != null) {
                    EntityType targetType;
                    try {
                        targetType = EntityType.valueOf(typeName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        future.complete(ToolResult.error("无效的实体类型: " + typeName));
                        return;
                    }
                    
                    if (targetType == EntityType.PLAYER) {
                        future.complete(ToolResult.error("不能清除玩家实体"));
                        return;
                    }
                    
                    if (worldName != null) {
                        World world = plugin.getServer().getWorld(worldName);
                        if (world == null) {
                            future.complete(ToolResult.error("世界不存在: " + worldName));
                            return;
                        }
                        for (Entity entity : world.getEntities()) {
                            if (entity.getType() == targetType) {
                                entity.remove();
                                killed++;
                            }
                        }
                    } else {
                        for (World world : plugin.getServer().getWorlds()) {
                            for (Entity entity : world.getEntities()) {
                                if (entity.getType() == targetType) {
                                    entity.remove();
                                    killed++;
                                }
                            }
                        }
                    }
                } else {
                    if (worldName != null) {
                        World world = plugin.getServer().getWorld(worldName);
                        if (world == null) {
                            future.complete(ToolResult.error("世界不存在: " + worldName));
                            return;
                        }
                        for (Entity entity : world.getEntities()) {
                            if (!(entity instanceof Player)) {
                                entity.remove();
                                killed++;
                            }
                        }
                    } else {
                        for (World world : plugin.getServer().getWorlds()) {
                            for (Entity entity : world.getEntities()) {
                                if (!(entity instanceof Player)) {
                                    entity.remove();
                                    killed++;
                                }
                            }
                        }
                    }
                }
                
                future.complete(ToolResult.success("已清除 " + killed + " 个实体"));
            } catch (Exception e) {
                future.complete(ToolResult.error("执行失败: " + e.getMessage()));
            }
        });
        
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            return ToolResult.error("执行失败: " + e.getMessage());
        }
    }
    
    private ToolResult spawnEntity(String typeName, String worldName, int amount) {
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ToolResult.error("无效的实体类型: " + typeName);
        }
        
        World world;
        if (worldName != null) {
            world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                return ToolResult.error("世界不存在: " + worldName);
            }
        } else {
            world = plugin.getServer().getWorlds().get(0);
        }
        
        final World finalWorld = world;
        final EntityType finalType = entityType;
        final int finalAmount = amount;
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (int i = 0; i < finalAmount; i++) {
                finalWorld.spawnEntity(finalWorld.getSpawnLocation(), finalType);
            }
        });
        
        return ToolResult.success("已在 " + world.getName() + " 生成 " + amount + " 个 " + typeName);
    }
    
    private ToolResult spawnEntityAt(String typeName, String worldName, double x, double y, double z, int amount) {
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ToolResult.error("无效的实体类型: " + typeName);
        }
        
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return ToolResult.error("世界不存在: " + worldName);
        }
        
        final World finalWorld = world;
        final EntityType finalType = entityType;
        final int finalAmount = amount;
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Location loc = new Location(finalWorld, x, y, z);
            for (int i = 0; i < finalAmount; i++) {
                finalWorld.spawnEntity(loc, finalType);
            }
        });
        
        return ToolResult.success(String.format("已在 %s (%.1f, %.1f, %.1f) 生成 %d 个 %s", 
            worldName, x, y, z, amount, typeName));
    }
    
    private ToolResult getEntityInfo(String entityId, String worldName) {
        CompletableFuture<ToolResult> future = new CompletableFuture<>();
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                if (entityId != null) {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(entityId);
                    } catch (IllegalArgumentException e) {
                        future.complete(ToolResult.error("无效的UUID格式"));
                        return;
                    }
                    
                    for (World world : plugin.getServer().getWorlds()) {
                        for (Entity entity : world.getEntities()) {
                            if (entity.getUniqueId().equals(uuid)) {
                                JsonObject result = new JsonObject();
                                result.addProperty("uuid", entity.getUniqueId().toString());
                                result.addProperty("type", entity.getType().name());
                                result.addProperty("world", entity.getWorld().getName());
                                result.addProperty("x", entity.getLocation().getX());
                                result.addProperty("y", entity.getLocation().getY());
                                result.addProperty("z", entity.getLocation().getZ());
                                
                                if (entity instanceof LivingEntity living) {
                                    result.addProperty("health", living.getHealth());
                                    result.addProperty("maxHealth", living.getMaxHealth());
                                    result.addProperty("canPickupItems", living.getCanPickupItems());
                                }
                                
                                result.addProperty("customName", entity.getCustomName());
                                result.addProperty("isDead", entity.isDead());
                                result.addProperty("isInsideVehicle", entity.isInsideVehicle());
                                
                                future.complete(ToolResult.success(result.toString()));
                                return;
                            }
                        }
                    }
                    future.complete(ToolResult.error("找不到实体: " + entityId));
                } else {
                    World world = plugin.getServer().getWorld(worldName);
                    if (world == null) {
                        future.complete(ToolResult.error("世界不存在: " + worldName));
                        return;
                    }
                    
                    JsonArray entities = new JsonArray();
                    int count = 0;
                    for (Entity entity : world.getEntities()) {
                        if (!(entity instanceof Player) && count < 50) {
                            JsonObject entityObj = new JsonObject();
                            entityObj.addProperty("uuid", entity.getUniqueId().toString());
                            entityObj.addProperty("type", entity.getType().name());
                            entityObj.addProperty("x", Math.round(entity.getLocation().getX() * 10) / 10.0);
                            entityObj.addProperty("y", Math.round(entity.getLocation().getY() * 10) / 10.0);
                            entityObj.addProperty("z", Math.round(entity.getLocation().getZ() * 10) / 10.0);
                            if (entity.getCustomName() != null) {
                                entityObj.addProperty("customName", entity.getCustomName());
                            }
                            entities.add(entityObj);
                            count++;
                        }
                    }
                    
                    JsonObject result = new JsonObject();
                    result.addProperty("world", worldName);
                    result.addProperty("totalEntities", world.getEntities().size() - world.getPlayers().size());
                    result.add("entities", entities);
                    future.complete(ToolResult.success(result.toString()));
                }
            } catch (Exception e) {
                future.complete(ToolResult.error("执行失败: " + e.getMessage()));
            }
        });
        
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            return ToolResult.error("执行失败: " + e.getMessage());
        }
    }
    
    private ToolResult killEntitiesNear(String worldName, String typeName, double x, double y, double z, double radius) {
        CompletableFuture<ToolResult> future = new CompletableFuture<>();
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                World world;
                if (worldName != null) {
                    world = plugin.getServer().getWorld(worldName);
                    if (world == null) {
                        future.complete(ToolResult.error("世界不存在: " + worldName));
                        return;
                    }
                } else {
                    world = plugin.getServer().getWorlds().get(0);
                }
                
                Location center = new Location(world, x, y, z);
                int killed = 0;
                
                EntityType targetType = null;
                if (typeName != null) {
                    try {
                        targetType = EntityType.valueOf(typeName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        future.complete(ToolResult.error("无效的实体类型: " + typeName));
                        return;
                    }
                    
                    if (targetType == EntityType.PLAYER) {
                        future.complete(ToolResult.error("不能清除玩家实体"));
                        return;
                    }
                }
                
                final EntityType finalTargetType = targetType;
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Player) continue;
                    
                    if (finalTargetType != null && entity.getType() != finalTargetType) continue;
                    
                    if (entity.getLocation().distance(center) <= radius) {
                        entity.remove();
                        killed++;
                    }
                }
                
                future.complete(ToolResult.success(String.format("已在半径 %.1f 内清除 %d 个实体", radius, killed)));
            } catch (Exception e) {
                future.complete(ToolResult.error("执行失败: " + e.getMessage()));
            }
        });
        
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            return ToolResult.error("执行失败: " + e.getMessage());
        }
    }
}
